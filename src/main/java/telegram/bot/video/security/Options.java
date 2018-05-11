package telegram.bot.video.security;

import java.util.UUID;
import java.util.logging.Logger;

public class Options {

    private static final Logger LOG = Logger.getLogger(Options.class.getCanonicalName());

    int width = 640;
    int height = 480;
    String key = UUID.randomUUID().toString().toUpperCase().substring(0, 6);

    public static Options parseOptions(String txt) {
        String[] split = txt.split(" ");
        Options o = new Options();
        if (split.length > 0 && txt.contains(" ")) {
            if (split.length > 1) {
                o.key = split[1];
                o.key = o.key == null || o.key.isEmpty() ? UUID.randomUUID().toString().toUpperCase().substring(0, 6) : o.key;
            }
            if (split.length > 2) {
                try {
                    o.width = Integer.parseInt(split[2]);
                } catch (NumberFormatException ex) {
                    LOG.warning("Failed to define capture width, value will be set to " + o.width);
                }
            }
            if (split.length > 3) {
                try {
                    o.height = Integer.parseInt(split[3]);
                } catch (NumberFormatException ex) {
                    LOG.warning("Failed to define capture height, value will be set to " + o.height);
                }
            }
        }
        return o;
    }
}
