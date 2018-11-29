package telegram.bot.video.security.core.camera;

import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import telegram.bot.video.security.core.CameraProcessor;
import telegram.bot.video.security.core.PersonManager;
import telegram.bot.video.security.entity.Person;
import telegram.bot.video.security.face.FaceRecognizer;
import telegram.bot.video.security.face.Matcher;
import telegram.bot.video.security.option.ControlOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import static telegram.bot.video.security.face.Matcher.NO_MATCH;

public class FaceRecognition extends CameraFunctionality {

    private volatile boolean faceDetectionLatch = true;
    private FaceRecognizer fr = new Matcher();
    public static Map<KEDetectedFace, Person> facesCache;
    private static final String UNK_FILE_NAME = "Unk.jpg";

    public FaceRecognition(ControlOptions controlOptions) {
        super(controlOptions);
        PersonManager personManager = new PersonManager();
        personManager.init();
        facesCache = personManager.getFaces();
    }

    @Override
    public void doBefore(CameraProcessor proc) {
    }

    @Override
    public void doMain(int width, int height, FImage current, FImage last) {
        detectFace(current);
    }

    @Override
    public void onShutdown() {
    }

    private void processDetected(List<KEDetectedFace> faces, FImage cframe) {
        for (KEDetectedFace face : faces) {
            String response = fr.detectFaces(new ArrayList<>(facesCache.keySet()), Collections.singletonList(face));
            LOG.info(response);
            if (NO_MATCH.equalsIgnoreCase(response)) {
                File f = null;
                try {
                    Path path = Paths.get(UNK_FILE_NAME);
                    Files.deleteIfExists(path);
                    Files.createFile(path);
                    f = new File(UNK_FILE_NAME);
                    ImageUtilities.write(cframe, f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File finalF = f;
                if (f != null) {
                    Executors.newSingleThreadExecutor().submit(() -> control.sendFile(finalF, "Unknown face detected!"));
                } else {
                    Executors.newSingleThreadExecutor().submit(() -> control.sendMsg("Unknown face detected!"));
                }
            }
        }
    }

    private void detectFace(FImage frame) {
        if (faceDetectionLatch) {
            faceDetectionLatch = false;
            FImage cframe = frame.clone();
            Executors.newSingleThreadScheduledExecutor().submit(() -> {
                try {
                    List<KEDetectedFace> detectedFaces = FaceRecognizer.faceDetector.detectFaces(cframe);
                    if (!detectedFaces.isEmpty()) {
                        LOG.info("Face detected.");
                        processDetected(detectedFaces, cframe);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Throwable e) {
                    LOG.log(Level.SEVERE, "", e);
                } finally {
                    faceDetectionLatch = true;
                }
            });
        }
    }
}
