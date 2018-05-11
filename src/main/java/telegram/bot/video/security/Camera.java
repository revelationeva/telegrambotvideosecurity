package telegram.bot.video.security;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.video.Video;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Camera {

    private static final Logger LOG = Logger.getLogger(Camera.class.getCanonicalName());

    private VideoDisplay<MBFImage> display;

    private Control control;
    public CameraDetectionStatistics stats;

    Camera(Control c) {
        this.control = c;
        stats = new CameraDetectionStatistics();
    }

    void startVideoCapture() {
        try {
            Video<MBFImage> video = new VideoCapture(1280, 720);
            display = VideoDisplay.createVideoDisplay(video);
            /*for (MBFImage mbfImage : video) {
                DisplayUtilities.displayName(mbfImage.process(new CannyEdgeDetector()), "videoFrames");
            }*/
        } catch (VideoCaptureException e) {
            e.printStackTrace();
        }
    }

    void detectMotion() {
        detectMotion(1280, 720);
    }

    void detectMotion(int width, int height) {
        LOG.info("Start cam with w:" + width + " h:" + height);
        try {
            final VideoCapture c = new VideoCapture(width, height);
            display = VideoDisplay.createVideoDisplay(c);
            display.getScreen().getRootPane().getParent().setVisible(false);

            FImage last = c.getNextFrame().flatten();
            for (final MBFImage frame : c) {
                if (control.shutdown) {
                    LOG.info("Received shutdown signal. Shutting down...");
                    display.close();
                    c.close();
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

                if (val < 100) {
                    stats.minor.add((long) val);
                } else if (val < 500) {
                    stats.medium.add((long) val);
                } else {
                    stats.major.add((long) val);
                }
                if (val > 500) {
                    System.out.println("motion: " + val);
                } else {
                    System.out.println("motion: " + val);
                }

                last = current;
            }
        } catch (VideoCaptureException e) {
            LOG.log(Level.SEVERE, "", e);
        }
    }
}
