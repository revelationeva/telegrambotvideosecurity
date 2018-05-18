package telegram.bot.video.security;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import telegram.bot.video.security.core.CoreController;
import telegram.bot.video.security.option.ControlOptions;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger LOG = Logger.getLogger(TelegramBot.class.getCanonicalName());

    private static String botToken;
    private static String botName;

    private static CoreController core;
    private static Message lastReceived;

    public static TelegramBot start(String botToken, String botName) {
        TelegramBot.botToken = botToken;
        TelegramBot.botName = botName;
        core = CoreController.getInstance();

        LOG.info("Bot name: " + TelegramBot.botName);
        ApiContextInitializer.init();
        TelegramBotsApi botapi = new TelegramBotsApi();
        try {
            TelegramBot telegramBot = new TelegramBot();
            botapi.registerBot(telegramBot);
            return telegramBot;
        } catch (TelegramApiException e) {
            LOG.log(Level.SEVERE, "", e);
            return null;
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    private static final String[] COMMANDS = {"/whatareyou", "/list"};
    public static final String[] CONTROL_COMMANDS = {"/stop", "/motion", "/stats"};

    @Override
    public void onUpdateReceived(Update e) {
        final Message msg = e.getMessage();
        lastReceived = msg;
        final String txtMessage = msg.getText();
        LOG.info("Message received: " + txtMessage);
        final String commandKey = extractCommand(txtMessage);
        if (!commandKey.contains("/")) {
            processControl(commandKey, msg);
        } else {
            processCommand(commandKey, msg);
        }
    }

    @SuppressWarnings("deprecation")
    private void sendMsg(Message msg, String text) {
        SendMessage s = new SendMessage();
        s.setChatId(msg.getChatId());
        s.setText(text);
        try {
            sendMessage(s);
        } catch (TelegramApiException e) {
            LOG.log(Level.SEVERE, "Failed to send message!", e);
        }
    }

    public void sendMsg(String text) {
        sendMsg(lastReceived, text);
    }

    private void processControl(String commandUid, Message msg) {
        final String txtMsg = msg.getText();
        final ControlOptions co = ControlOptions.parseControlOptions(txtMsg);
        if (CONTROL_COMMANDS[0].startsWith(co.command)) {
            sendMsg(msg, core.shutdown(commandUid));
        } else if (CONTROL_COMMANDS[1].startsWith(co.command)) {
            sendMsg(msg, core.runDetector(co));
        } else if (CONTROL_COMMANDS[2].startsWith(co.command)) {
            try {
                sendMsg(msg, "Report generation started...");
                File f = core.getStats(co);
                if (f == null) {
                    sendMsg(msg, "Failed to generate report.");
                } else {
                    sendDocUploadingAFile(msg, f, "Report");
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (CONTROL_COMMANDS[5].startsWith(co.command)) {
            try {
                sendMsg(msg, "Report generation started...");
                File f = core.getStats(co);
                if (f == null) {
                    sendMsg(msg, "Failed to generate report.");
                } else {
                    sendDocUploadingAFile(msg, f, "Report");
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            StringBuilder b = new StringBuilder();
            Arrays.asList(CONTROL_COMMANDS).forEach(r -> b.append(r).append(" "));
            sendMsg(msg, "Sorry, for now i know only the following control commands: " + b.toString());
        }
    }

    private void processCommand(String textCommand, Message msg) {
        if (COMMANDS[0].startsWith(textCommand)) {
            sendMsg(msg, "I am a telegram bot designed to inform you about video security system detections.");
        } else if (COMMANDS[1].startsWith(textCommand)) {
            sendMsg(msg, core.getAllCameras());
        } else {
            StringBuilder b = new StringBuilder();
            Arrays.asList(COMMANDS).forEach(r -> b.append(r).append(" "));
            sendMsg(msg, "Sorry, for now i know only the following commands: " + b.toString());
        }
    }

    private String extractCommand(String txt) {
        if (txt.startsWith("/")) {
            int i = txt.indexOf(" ") - 1;
            return txt.substring(0, i <= 0 ? txt.length() - 1 : i);
        }
        final String[] s = txt.split(" ");
        if (s.length > 1 && s[1].startsWith("/")) {
            return s[0];
        }
        return txt;
    }

    private void sendDocUploadingAFile(Message msg, File save, String caption) throws TelegramApiException {
        SendDocument sendDocumentRequest = new SendDocument();
        sendDocumentRequest.setChatId(msg.getChatId());
        sendDocumentRequest.setNewDocument(save);
        sendDocumentRequest.setCaption(caption);
        sendDocument(sendDocumentRequest);
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
