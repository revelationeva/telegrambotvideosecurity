package telegram.bot.video.security.option;

import telegram.bot.video.security.helper.UUIDUtil;

import java.util.logging.Logger;

public class Options {

    private static final Logger LOG = Logger.getLogger(Options.class.getCanonicalName());

    public String key;

    public static Options parseOptions(String txt) {
        String[] split = txt.split(" ");
        Options o = new Options();
        if (split.length > 0 && txt.contains(" ")) {
            if (split.length > 1) {
                o.key = split[1];
                o.key = o.key == null || o.key.isEmpty() ? UUIDUtil.getUpperUID6() : o.key;
            }
        }
        return o;
    }
}
