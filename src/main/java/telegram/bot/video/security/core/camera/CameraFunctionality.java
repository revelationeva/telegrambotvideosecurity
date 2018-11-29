package telegram.bot.video.security.core.camera;

import org.openimaj.image.FImage;
import telegram.bot.video.security.core.CameraProcessor;
import telegram.bot.video.security.core.Control;
import telegram.bot.video.security.option.ControlOptions;

import java.util.logging.Logger;

public abstract class CameraFunctionality {

    static final Logger LOG = Logger.getLogger(CameraFunctionality.class.getCanonicalName());

    final ControlOptions controlOptions;
    Control control;

    CameraFunctionality(ControlOptions controlOptions) {
        this.controlOptions = controlOptions;
    }

    public abstract void doBefore(CameraProcessor proc);

    public abstract void doMain(int width, int height, FImage current, FImage last);

    public abstract void onShutdown();

    public void setControl(Control c) {
        control = c;
    }
}
