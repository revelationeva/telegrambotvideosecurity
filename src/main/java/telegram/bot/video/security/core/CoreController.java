package telegram.bot.video.security.core;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openimaj.video.capture.Device;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import telegram.bot.video.security.TelegramBot;
import telegram.bot.video.security.core.camera.CameraFunctionality;
import telegram.bot.video.security.core.camera.FaceRecognition;
import telegram.bot.video.security.core.camera.MotionDetection;
import telegram.bot.video.security.entity.Capture;
import telegram.bot.video.security.entity.CaptureReport;
import telegram.bot.video.security.entity.CaptureStatistics;
import telegram.bot.video.security.helper.CriteriaQueryBuilder;
import telegram.bot.video.security.option.ControlOptions;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Join;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoreController {

    private static final Logger LOG = Logger.getLogger(CoreController.class.getCanonicalName());

    private static volatile CoreController instance;

    private CoreController() {
    }

    public static CoreController getInstance() {
        if (instance == null) {
            synchronized (CoreController.class) {
                if (instance == null) {
                    instance = new CoreController();
                }
            }
        }
        return instance;
    }

    private SingleContext ctx;
    private TelegramBot bot;

    public void startBot(String botToken, String botName) {
        ctx = SingleContext.getInstance();
        bot = TelegramBot.start(botToken, botName);
    }

    void sendMsg(String text) {
        bot.sendMsg(text);
    }

    void sendFile(File f, String caption) {
        try {
            bot.sendFile(f, caption);
        } catch (TelegramApiException e) {
            LOG.log(Level.SEVERE, "Failed to send file to client!", e);
        }
    }

    private ExecutorService pool = Executors.newFixedThreadPool(4);
    private Map<String, Control> controls = new HashMap<>();

    public String runDetector(ControlOptions co) {
        final String response = ctx.useCam(co);
        if (response != null) {
            return response;
        }

        CameraFunctionality func = new MotionDetection(co);
        final Control c = getControl(co, func);
        func.setControl(c);

        if (!c.isRunning) {
            Capture capture = new Capture();
            capture.setMinorName("Minor");
            capture.setMediumName("Medium");
            capture.setMajorName("Major");
            capture.setUid(ctx.current.key);
            capture.setCamName(ctx.current.value.getNameStr());
            capture.setSensivity(co.sensitivity);
            capture.setPollInterval(co.pollInterval);
            capture.setDateStarted(new Date());
            ctx.persist(capture, true);
            c.capture = capture;

            c.isRunning = true;

            pool.execute(() -> c.camProcessor.run(co.width, co.height));
            return "Motion detection started.";
        } else {
            return "Process already launched.";
        }
    }

    public String runFaceRecognition(ControlOptions co) {
        final String response = ctx.useCam(co);
        if (response != null) {
            return response;
        }

        CameraFunctionality func = new FaceRecognition(co);
        final Control c = getControl(co, func);
        func.setControl(c);

        if (!c.isRunning) {
            c.isRunning = true;
            pool.execute(() -> c.camProcessor.run(co.width, co.height));
            return "Face recognition started.";
        } else {
            return "Process already launched.";
        }
    }

    private Control getControl(ControlOptions co, CameraFunctionality cf) {
        return controls.computeIfAbsent(co.uid, s -> {
            Control p = new Control();
            p.camProcessor = new CameraProcessor(p, ctx.current.value, cf);
            return p;
        });
    }

    public String shutdown(String commandKey) {
        Control control = controls.get(commandKey);
        if (control != null) {
            control.shutdown = true;
            control.isRunning = false;
            ctx.freeCam(commandKey);
            return "(" + commandKey + ") shutdown complete.";
        }
        return "(" + commandKey + ") no such entity to shutdown.";
    }

    public File getStats(ControlOptions co) {
        final EntityManager em = ctx.getEm();

        final List<CaptureStatistics> list = new CriteriaQueryBuilder(em).getList(CaptureStatistics.class, (b, q, r) -> {
            Join c = (Join) r.fetch("capture");
            q.where(b.equal(c.get("uid"), co.uid));
            q.orderBy(b.desc(c.get("id")));
        });
        if (list.isEmpty()) {
            LOG.info("No detection statistics found for report generation!");
            return null;
        }
        final Capture capture = list.iterator().next().getCapture();
        list.removeIf(captureStatistics -> !captureStatistics.getCapture().getId().equals(capture.getId()));

        final DateFormat df = new SimpleDateFormat("dd-mm-yyyy HH-mm-ss");
        final String reportDate = df.format(capture.getDateStarted()).replace(" ", "_");
        final String fileName = co.uid + "___" + capture.getCamName().replace(" ", "_").replace(".", "_") +
                "___from_" + reportDate + ".pdf";

        final CaptureReport report = new CriteriaQueryBuilder(em).find(CaptureReport.class,
                (b, q, r) -> q.where(b.equal(r.get("reportName"), fileName)), r -> {
                });
        if (report != null) {
            File f = new File(report.getReportName());
            f.deleteOnExit();
            try {
                Files.write(Paths.get(f.getPath()), report.getReport());
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to write persisted report to file!", e);
                return null;
            }
            return f;
        }

        try {
            final JasperReport jasperReport = JasperCompileManager.compileReport(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("DetectionsChart.jrxml"));
            final JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(list);
            final JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
            if (jasperPrint.getPages().size() > 1) {
                JRPrintPage p = jasperPrint.getPages().get(1);
                while (jasperPrint.getPages().size() != 0) {
                    jasperPrint.removePage(0);
                }
                jasperPrint.addPage(p);
            }

            JasperExportManager.exportReportToPdfFile(jasperPrint, "D:/" + fileName);
            File f = new File("D:/" + fileName);
            f.deleteOnExit();

            CaptureReport cr = new CaptureReport();
            cr.setCapture(capture);
            cr.setReportName(fileName);
            try {
                cr.setReport(Files.readAllBytes(Paths.get(f.getPath())));
                SingleContext.getInstance().persist(cr, true);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to persist report!", e);
                return null;
            }
            return f;
        } catch (JRException e) {
            LOG.log(Level.SEVERE, "Failed to generate report!", e);
            return null;
        }
    }

    public String getAllCameras() {
        Spliterator<Map.Entry<String, Device>> spl = ctx.availableCameras.entrySet().spliterator();
        final StringBuilder b = new StringBuilder();
        b.append("Available cameras:\n");
        build(spl, b);
        if (!ctx.busyCameras.isEmpty()) {
            b.append("Busy cameras:\n");
            spl = ctx.busyCameras.entrySet().spliterator();
            build(spl, b);
        }
        return b.toString();
    }

    private void build(Spliterator<Map.Entry<String, Device>> spl, StringBuilder b) {
        while (spl.tryAdvance(e ->
                b.append("(").append(e.getKey()).append(") ").append(e.getValue().getNameStr()).append("\n"))) ;
    }
}
