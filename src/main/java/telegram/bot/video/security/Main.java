package telegram.bot.video.security;

import telegram.bot.video.security.core.CoreController;

public class Main {

    private static final String BOT_TOKEN = "460589759:AAFplB6qvnG-1leEZkoU6FnTZ6PPsUJ_gDk";
    private static final String BOT_NAME = "JTelegramVideoSecurityBot";

    public static void main(String[] args) {
        CoreController.getInstance().startBot(BOT_TOKEN, BOT_NAME);
    }
}
