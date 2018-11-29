package telegram.bot.video.security.core.camera;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.video.xuggle.XuggleVideoWriter;
import telegram.bot.video.security.core.CameraProcessor;
import telegram.bot.video.security.core.Sensitivity;
import telegram.bot.video.security.entity.CaptureStatistics;
import telegram.bot.video.security.option.ControlOptions;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import static telegram.bot.video.security.core.CameraProcessor.FRAMERATE;

public class MotionDetection extends CameraFunctionality {

    private static final int MAX_FRAMES_TO_KEEP = 1000;

    private List<CaptureStatistics> stats = new ArrayList<>();
    private long motionSequence = 0L;
    private List<FImage> frames = new ArrayList<>(MAX_FRAMES_TO_KEEP);
    private Sensitivity sensitivity;
    private Timer timer;

    private volatile boolean sendAlertLatch = true;

    public MotionDetection(ControlOptions controlOptions) {
        super(controlOptions);
        stats.add(new CaptureStatistics());
        stats.add(new CaptureStatistics());
        this.sensitivity = new Sensitivity(controlOptions.sensitivity);
    }

    @Override
    public void doBefore(CameraProcessor proc) {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                flushStats();
            }
        }, controlOptions.pollInterval * 2, controlOptions.pollInterval);
    }

    @Override
    public void doMain(int width, int height, FImage current, FImage last) {
        final float val = getDifference(current, last);
        collectStats(width, height, (long) val, current);
    }

    @Override
    public void onShutdown() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void sendAlert() {
        if (sendAlertLatch && control.receiveAlerts) {
            sendAlertLatch = false;
            control.sendAlert();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendAlertLatch = true;
                }
            }, 10000L);
        }
    }

    private float getDifference(FImage current, FImage last) {
        float val = 0;
        for (int y = 0; y < current.height; y++) {
            for (int x = 0; x < current.width; x++) {
                final float diff = (current.pixels[y][x] - last.pixels[y][x]);
                val += diff * diff;
            }
        }
        return val;
    }

    private void flushStats() {
        stats.add(new CaptureStatistics());
        control.persistStats(stats.remove(0));
    }

    private void collectStats(int width, int height, long val, FImage frame) {
        CaptureStatistics cs = stats.get(0);
        if (val < sensitivity.getThresholdSensitivity()) {
            // Ignore
            decrement(frame, 3L, width, height);
        } else if (val < sensitivity.getLevelSensitivity(1)) {
            cs.incrementMinor(val);
            decrement(frame, 2L, width, height);
        } else if (val < sensitivity.getLevelSensitivity(2)) {
            cs.incrementMinor(val);
            decrement(frame, 1L, width, height);
        } else if (val < sensitivity.getLevelSensitivity(3)) {
            cs.incrementMinor(val);
            cs.incrementMedium(val);
            decrement(frame, 1L, width, height);
        } else if (val < sensitivity.getLevelSensitivity(4)) {
            cs.incrementMedium(val);
            increment(frame, 1);
        } else if (val < sensitivity.getLevelSensitivity(5)) {
            cs.incrementMedium(val);
            increment(frame, 1);
        } else if (val < sensitivity.getLevelSensitivity(6)) {
            cs.incrementMedium(val);
            increment(frame, 2);
        } else if (val < sensitivity.getLevelSensitivity(7)) {
            cs.incrementMedium(val);
            cs.incrementMajor(val);
            increment(frame, 2);
        } else if (val < sensitivity.getLevelSensitivity(8)) {
            cs.incrementMajor(val);
            increment(frame, 2);
        } else if (val < sensitivity.getLevelSensitivity(9)) {
            cs.incrementMajor(val);
            increment(frame, 3);
        } else if (val < sensitivity.getLevelSensitivity(10)) {
            cs.incrementMajor(val);
            increment(frame, 4);
        } else {
            increment(frame, 5);
            sendAlert();
        }
    }

    private void increment(FImage frame, long increment) {
        if (motionSequence <= 0) {
            motionSequence = 200;
        }
        addFrame(frame);
        motionSequence += increment;
    }

    private void addFrame(FImage frame) {
        if (frames.size() > MAX_FRAMES_TO_KEEP) {
            frames.subList(0, 10).clear();
        }
        frames.add(frame.clone());
    }

    private void decrement(FImage frame, long decrement, int width, int height) {
        if (motionSequence > 0) {
            motionSequence -= decrement;
            addFrame(frame);
            if (motionSequence <= 0) {
                System.out.println("Video creation started...");
                motionSequence = 0;
                final List<FImage> copy = frames;
                frames = new ArrayList<>(MAX_FRAMES_TO_KEEP);
                Executors.newSingleThreadExecutor().submit(() ->
                        control.sendFile(saveVideo(copy, width, height), "Suspicious motions detected!"));
            }
        }
    }

    private File saveVideo(List<FImage> copy, int width, int height) {
        LOG.info("Flushing " + copy.size() + " frames...");
        final DateFormat df = new SimpleDateFormat("dd-mm-yyyy HH-mm-ss");
        final String name = control.capture.getUid() + "___" + df.format(control.capture.getDateStarted()).replace(" ", "_") + ".mp4";
        try (XuggleVideoWriter w = new XuggleVideoWriter(name, width, height, (double) FRAMERATE / 3)) {
            for (FImage i : copy) {
                w.addFrame(new MBFImage(i));
            }
        }
        copy.clear();
        LOG.info("Flush finished.");
        File f = new File(name);
        f.deleteOnExit();
        return f;
    }
}
