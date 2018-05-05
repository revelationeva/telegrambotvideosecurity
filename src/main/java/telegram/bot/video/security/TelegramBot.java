package telegram.bot.video.security;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.logging.Logger;

public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger LOG = Logger.getLogger(TelegramBot.class.getCanonicalName());

    private static final String BOT_TOKEN = "460589759:AAFplB6qvnG-1leEZkoU6FnTZ6PPsUJ_gDk";
    private static final String BOT_NAME = "JTelegramVideoSecurityBot";

    public static void main(String[] args) {
        LOG.info("Bot name: " + BOT_NAME);
        ApiContextInitializer.init();
        TelegramBotsApi botapi = new TelegramBotsApi();
        try {
            botapi.registerBot(new TelegramBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    private static final String[] COMMANDS = {"/start", "/whatareyou", "/video", "/motion"};

    @Override
    public void onUpdateReceived(Update e) {
        Message msg = e.getMessage();
        String txt = msg.getText();
        LOG.info("Message received: " + txt);
        if (txt.equals(COMMANDS[0])) {
            sendMsg(msg, "Hello, world! This is simple bot!");
        } else if (txt.equals(COMMANDS[1])) {
            sendMsg(msg, "I am a telegram bot designed to inform you about video security system detections.");
        } else if (txt.equals(COMMANDS[2])) {
            new VideoRecorder().startVideoCapture();
        } else if(txt.equals(COMMANDS[3])){
            new VideoRecorder().detectMotion();
        } else {
            StringBuilder b = new StringBuilder();
            Arrays.asList(COMMANDS).forEach(r -> b.append(r).append(" "));
            sendMsg(msg, "Sorry, for now i know only the following commands: " + b.toString());
        }
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    private void sendMsg(Message msg, String text) {
        SendMessage s = new SendMessage();
        s.setChatId(msg.getChatId());
        s.setText(text);
        try {
            sendMessage(s);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
