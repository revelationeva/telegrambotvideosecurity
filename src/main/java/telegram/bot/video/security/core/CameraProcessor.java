package telegram.bot.video.security.core;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.video.capture.Device;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;
import telegram.bot.video.security.entity.CaptureStatistics;
import telegram.bot.video.security.option.ControlOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

class CameraProcessor {

    private static final Logger LOG = Logger.getLogger(CameraProcessor.class.getCanonicalName());

    private Control control;
    private Device device;
    private int thresholdSensivity;
    private int minorSensivity;
    private int mediumSensivity;
    private int majorSensivity;
    private int pollInterval;
    private ControlOptions co;
    private List<CaptureStatistics> stats = new ArrayList<>();

    CameraProcessor(Control c, Device d, ControlOptions co) {
        this.control = c;
        this.device = d;
        this.co = co;
        this.thresholdSensivity = co.sensivity / 2;
        this.minorSensivity = co.sensivity;
        this.mediumSensivity = co.sensivity * 5;
        this.majorSensivity = co.sensivity * 10;
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
            //final VideoDisplay<MBFImage> display = VideoDisplay.createVideoDisplay(capture);

            FImage last = capture.getNextFrame().flatten();
            for (final MBFImage frame : capture) {
                if (control.shutdown) {
                    shutdown(timer, capture);
                    break;
                }
                final FImage current = frame.flatten();
                float val = 0;
                for (int y = 0; y < current.height; y++) {
                    for (int x = 0; x < current.width; x++) {
                        final float diff = (current.pixels[y][x] - last.pixels[y][x]);
                        val += diff * diff;
                    }
                }

                CaptureStatistics cs = stats.get(0);
                if (val < thresholdSensivity) {
                    // Ignore
                } else if (val < minorSensivity) {
                    cs.incrementMinor((long) val);
                } else if (val < mediumSensivity) {
                    cs.incrementMedium((long) val);
                } else if (val < majorSensivity) {
                    cs.incrementMajor((long) val);
                } else {
                    sendAlert();
                }

                System.out.println("motion: " + val);

                last = current;
            }
        } catch (VideoCaptureException e) {
            LOG.log(Level.SEVERE, "", e);
        }
    }
}
