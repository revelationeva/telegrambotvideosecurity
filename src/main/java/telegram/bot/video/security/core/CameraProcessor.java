package telegram.bot.video.security.core;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.capture.Device;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;
import telegram.bot.video.security.core.camera.CameraFunctionality;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CameraProcessor {

    private static final Logger LOG = Logger.getLogger(CameraProcessor.class.getCanonicalName());

    private Control control;
    private Device device;

    public static final int FRAMERATE = 30;

    private final CameraFunctionality func;

    CameraProcessor(Control control, Device device, CameraFunctionality func) {
        this.control = control;
        this.device = device;
        this.func = func;
    }

    static List<Device> getAllCameras() {
        return VideoCapture.getVideoDevices();
    }

    private void shutdown(VideoCapture capture, VideoDisplay<MBFImage> display) {
        control.shutdown = false;

        func.onShutdown();
        control.finishCapture();

        LOG.info("Received shutdown signal. Shutting down...");
        if (display != null) {
            display.close();
        }
        capture.stopCapture();
        capture.close();
    }

    void run(int width, int height) {
        LOG.info("Start camProcessor with w:" + width + " h:" + height);
        try {
            func.doBefore(this);

            final VideoCapture capture = new VideoCapture(width, height, FRAMERATE, device);
            final VideoDisplay<MBFImage> videoDisplay = VideoDisplay.createVideoDisplay(capture);

            calibrate(capture);
            FImage last = flatten(capture.getNextFrame());
            for (final MBFImage frame : capture) {
                if (control.shutdown) {
                    shutdown(capture, videoDisplay);
                    break;
                }
                final FImage current = flatten(frame);

                func.doMain(width, height, current, last);

                last = current;
            }
        } catch (VideoCaptureException e) {
            LOG.log(Level.SEVERE, "", e);
        }
    }

    private void calibrate(final VideoCapture capture) {
        LOG.info("Performing calibration...");
        int i = 20;
        for (final MBFImage ignored : capture) {
            i--;
            if (i <= 0) {
                LOG.info("Calibration finished.");
                break;
            }
        }
    }

    private FImage flatten(MBFImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();

        final FImage out = new FImage(width, height);
        final float[][] outp = out.pixels;
        final int nb = image.numBands();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int i = 1; i < nb; i++) {
                    final float[][] bnd = image.bands.get(i).pixels;
                    outp[y][x] += bnd[y][x];
                }
                outp[y][x] = outp[y][x] / nb;
            }
        }

        return out;
    }
}
