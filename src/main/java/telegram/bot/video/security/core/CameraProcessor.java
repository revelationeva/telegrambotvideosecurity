package telegram.bot.video.security.core;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.capture.Device;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;
import org.openimaj.video.xuggle.XuggleVideoWriter;
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
import java.util.logging.Level;
import java.util.logging.Logger;

class CameraProcessor {

    private static final Logger LOG = Logger.getLogger(CameraProcessor.class.getCanonicalName());

    private Control control;
    private Device device;
    private int thresholdSensivity;
    private int sensivity1;
    private int sensivity2;
    private int sensivity3;
    private int sensivity4;
    private int sensivity5;
    private int sensivity6;
    private int sensivity7;
    private int sensivity8;
    private int sensivity9;
    private int sensivity10;
    private int pollInterval;
    private ControlOptions co;
    private List<CaptureStatistics> stats = new ArrayList<>();

    CameraProcessor(Control c, Device d, ControlOptions co) {
        this.control = c;
        this.device = d;
        this.co = co;
        this.thresholdSensivity = co.sensivity / 2;
        this.sensivity1 = co.sensivity;
        this.sensivity2 = co.sensivity * 2;
        this.sensivity3 = co.sensivity * 3;
        this.sensivity4 = co.sensivity * 4;
        this.sensivity5 = co.sensivity * 5;
        this.sensivity6 = co.sensivity * 6;
        this.sensivity7 = co.sensivity * 7;
        this.sensivity8 = co.sensivity * 8;
        this.sensivity9 = co.sensivity * 9;
        this.sensivity10 = co.sensivity * 10;
        this.pollInterval = co.pollInterval;
        stats.add(new CaptureStatistics());
        stats.add(new CaptureStatistics());
    }

    static List<Device> getAllCameras() {
        return VideoCapture.getVideoDevices();
    }

    private void flushStats() {
        stats.add(new CaptureStatistics());
        control.persistStats(stats.remove(0));
    }

    private void shutdown(Timer timer, VideoCapture capture) {
        control.shutdown = false;

        timer.cancel();
        control.finishCapture();
        flushStats();

        LOG.info("Received shutdown signal. Shutting down...");
        //display.close();
        capture.close();
    }

    private boolean latch = true;

    private void sendAlert() {
        if (latch && control.receiveAlerts) {
            latch = false;
            control.sendAlert();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    latch = true;
                }
            }, 10000L);
        }
    }

    private long motionSequence = 0L;
    private List<MBFImage> frames = new ArrayList<>();

    void detectMotion(int width, int height) {
        LOG.info("Start camController with w:" + width + " h:" + height);
        try {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    flushStats();
                }
            }, pollInterval * 2, pollInterval);

            final VideoCapture capture = device == null ? new VideoCapture(width, height) : new VideoCapture(width, height, device);
            VideoDisplay.createVideoDisplay(capture);

            FImage last = capture.getNextFrame().flatten();
            LOG.info("Performing calibration...");
            int i = 20;
            for (final MBFImage frame : capture) {
                if (control.shutdown) {
                    shutdown(timer, capture);
                    break;
                }
                final FImage current = frame.flatten();
                if (i > 0) {
                    i--;
                    last = current;
                    if (i == 0) {
                        LOG.info("Calibration finished, motion detector started.");
                    }
                    continue;
                }

                final float val = getDifference(current, last);
                collectStats(width, height, (long) val, frame);

                System.out.println("motion: " + val);

                last = current;
            }
        } catch (VideoCaptureException e) {
            LOG.log(Level.SEVERE, "", e);
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

    private void collectStats(int width, int height, long val, MBFImage frame) {
        CaptureStatistics cs = stats.get(0);
        if (val < thresholdSensivity) {
            // Ignore
            decrement(frame, 3L, width, height);
        } else if (val < sensivity1) {
            cs.incrementMinor(val);
            decrement(frame, 2L, width, height);
        } else if (val < sensivity2) {
            cs.incrementMinor(val);
            decrement(frame, 1L, width, height);
        } else if (val < sensivity3) {
            cs.incrementMinor(val);
            cs.incrementMedium(val);
            decrement(frame, 1L, width, height);
        } else if (val < sensivity4) {
            cs.incrementMedium(val);
            increment(frame, 1);
        } else if (val < sensivity5) {
            cs.incrementMedium(val);
            increment(frame, 1);
        } else if (val < sensivity6) {
            cs.incrementMedium(val);
            increment(frame, 2);
        } else if (val < sensivity7) {
            cs.incrementMedium(val);
            cs.incrementMajor(val);
            increment(frame, 2);
        } else if (val < sensivity8) {
            cs.incrementMajor(val);
            increment(frame, 2);
        } else if (val < sensivity9) {
            cs.incrementMajor(val);
            increment(frame, 3);
        } else if (val < sensivity10) {
            cs.incrementMajor(val);
            increment(frame, 4);
        } else {
            increment(frame, 5);
            sendAlert();
        }
    }

    private void increment(MBFImage frame, long increment) {
        if (motionSequence <= 0) {
            motionSequence = 200;
        }
        frames.add(frame.clone());
        motionSequence += increment;
    }

    private boolean decrement(MBFImage frame, long decrement, int width, int height) {
        if (motionSequence > 0) {
            motionSequence -= decrement;
            frames.add(frame.clone());
            if (motionSequence <= 0) {
                motionSequence = 0;

                final List<MBFImage> copy = frames;
                frames = new ArrayList<>();
                Executors.newSingleThreadExecutor().execute(() -> {
                    LOG.info("Flushing " + copy.size() + " frames...");
                    final DateFormat df = new SimpleDateFormat("dd-mm-yyyy HH-mm-ss");
                    final String name = control.capture.getUid() + "___" + df.format(control.capture.getDateStarted()).replace(" ", "_") + ".mp4";
                    XuggleVideoWriter w = new XuggleVideoWriter(name, width, height, 20);
                    for (MBFImage i : copy) {
                        w.addFrame(i);
                    }
                    w.close();
                    LOG.info("Flush finished.");
                    File f = new File(name);
                    f.deleteOnExit();
                    control.sendFile(f, "Suspicious motions detected!");
                });
            }
            return true;
        }
        return false;
    }
}
