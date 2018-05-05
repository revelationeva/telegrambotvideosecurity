package telegram.bot.video.security;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.video.Video;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;

public class VideoRecorder {

    VideoDisplay<MBFImage> display;

    public void startVideoCapture() {
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

    public void stopVideoCapture() {
        if (display != null) {
            display.close();
        }
    }

    public void detectMotion() {
        try {
            final VideoCapture c = new VideoCapture(1280, 720);
            display = VideoDisplay.createVideoDisplay(c);
            // get the first frame
            FImage last = c.getNextFrame().flatten();
            // iterate through the frames
            for (final MBFImage frame : c) {
                final FImage current = frame.flatten();

                // compute the squared difference from the last frame
                float val = 0;
                for (int y = 0; y < current.height; y++) {
                    for (int x = 0; x < current.width; x++) {
                        final float diff = (current.pixels[y][x] - last.pixels[y][x]);
                        val += diff * diff;
                    }
                }

                // might need adjust threshold:
                if (val > 500) {
                    System.out.println("motion");
                }

                // set the current frame to the last frame
                last = current;
            }

            //c.close();
        } catch (VideoCaptureException e) {
            e.printStackTrace();
        }
    }
}
