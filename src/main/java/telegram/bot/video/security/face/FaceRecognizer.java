package telegram.bot.video.security.face;

import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;

import java.util.List;
import java.util.logging.Logger;

public abstract class FaceRecognizer {
    static final Logger LOG = Logger.getLogger(FaceRecognizer.class.getCanonicalName());

    public static final FKEFaceDetector faceDetector = new FKEFaceDetector(new HaarCascadeDetector());

    /*private final EigenFaceRecogniser<KEDetectedFace, Person> faceRecognizer =
            EigenFaceRecogniser.create(20, new RotateScaleAligner(), 1, DoubleFVComparison.CORRELATION, 0.9f);
    private final FaceRecognitionEngine<KEDetectedFace, Person> faceEngine = FaceRecognitionEngine.create(faceDetector,
            faceRecognizer);*/

    public abstract String detectFaces(List<? extends DetectedFace> groundTruth, List<? extends DetectedFace> detected);
}
