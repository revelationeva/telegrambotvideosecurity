package telegram.bot.video.security.core;

import telegram.bot.video.security.entity.Capture;
import telegram.bot.video.security.entity.CaptureStatistics;

import java.io.File;
import java.util.Date;

public class Control {
    volatile boolean shutdown;
    volatile boolean isRunning;
    public volatile boolean receiveAlerts = true;

    CameraProcessor camProcessor;
    public Capture capture;

    public void sendMsg(String s) {
        CoreController.getInstance().sendMsg(s);
    }

    public void sendAlert() {
        CoreController.getInstance().sendMsg("Camera (" + capture.getUid() + ") " + capture.getCamName() + " detected intensive motion!");
    }

    public void sendFile(File f, String caption) {
        CoreController.getInstance().sendFile(f, caption);
    }

    public void finishCapture() {
        capture.setDateFinished(new Date());
        SingleContext.getInstance().persist(capture, true);
    }

    public void persistStats(CaptureStatistics cs) {
        cs.setCapture(capture);
        cs.setDateReported(new Date());
        SingleContext.getInstance().persist(cs, true);
    }
}
