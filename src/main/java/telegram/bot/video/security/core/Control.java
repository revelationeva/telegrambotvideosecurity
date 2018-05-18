package telegram.bot.video.security.core;

import telegram.bot.video.security.entity.Capture;
import telegram.bot.video.security.entity.CaptureStatistics;

import java.util.Date;

class Control {
    volatile boolean shutdown;
    volatile boolean isRunning;
    volatile boolean receiveAlerts = true;

    CameraProcessor camController;
    Capture capture;

    void sendAlert() {
        CoreController.getInstance().sendAlert("Camera (" + capture.getUid() + ") " + capture.getCamName() + " detected intensive motion!");
    }

    void finishCapture() {
        capture.setDateFinished(new Date());
        SingleContext.getInstance().persist(capture, true);
    }

    void persistStats(CaptureStatistics cs) {
        cs.setCapture(capture);
        cs.setDateReported(new Date());
        SingleContext.getInstance().persist(cs, true);
    }
}
