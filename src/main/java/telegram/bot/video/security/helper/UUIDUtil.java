package telegram.bot.video.security.helper;

import java.util.UUID;

public class UUIDUtil {

    public static String getUpperUID6() {
        return UUID.randomUUID().toString().toUpperCase().substring(0, 6);
    }
}
