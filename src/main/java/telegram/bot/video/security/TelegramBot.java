package telegram.bot.video.security;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger LOG = Logger.getLogger(TelegramBot.class.getCanonicalName());

    private static final String BOT_TOKEN = "460589759:AAFplB6qvnG-1leEZkoU6FnTZ6PPsUJ_gDk";
    private static final String BOT_NAME = "JTelegramVideoSecurityBot";

    private static ExecutorService pool;
    private static Map<String, Control> controls = new HashMap<>();

    public static void main(String[] args) {
        LOG.info("Bot name: " + BOT_NAME);
        ApiContextInitializer.init();
        TelegramBotsApi botapi = new TelegramBotsApi();
        try {
            botapi.registerBot(new TelegramBot());
        } catch (TelegramApiException e) {
            LOG.log(Level.SEVERE, "", e);
        }
        pool = Executors.newFixedThreadPool(4);
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    private static final String[] COMMANDS = {"/start", "/whatareyou", "/video", "/motion"};
    private static final String[] CONTROL_COMMANDS = {"/stop", "/cancelsubscribe", "/setpollinterval", "/statsnow"};

    @Override
    public void onUpdateReceived(Update e) {
        final Message msg = e.getMessage();
        final String txtMessage = msg.getText();
        LOG.info("Message received: " + txtMessage);
        final String textCommand = extractCommand(txtMessage);
        Control control = controls.get(textCommand);
        if (control != null) {
            processControl(control, msg);
        } else {
            processCommand(textCommand, msg);
        }
    }

    private void processControl(Control control, Message msg) {
        final String txtMsg = msg.getText();
        final ControlOptions co = ControlOptions.parseControlOptions(txtMsg);
        if (CONTROL_COMMANDS[0].startsWith(co.command)) {
            control.shutdown = true;
            sendMsg(msg, "Process shutdown complete.");
        } else if (CONTROL_COMMANDS[1].startsWith(co.command)) {
            sendMsg(msg, "Not yet implemented.");
        } else if (CONTROL_COMMANDS[2].startsWith(co.command)) {
            sendMsg(msg, "Not yet implemented.");
        } else if (CONTROL_COMMANDS[3].startsWith(co.command)) {
            sendMsg(msg, control.getStats());
        } else {
            StringBuilder b = new StringBuilder();
            Arrays.asList(CONTROL_COMMANDS).forEach(r -> b.append(r).append(" "));
            sendMsg(msg, "Sorry, for now i know only the following control commands: " + b.toString());
        }
    }

    private void processCommand(String textCommand, Message msg) {
        final String txtMsg = msg.getText();
        if (COMMANDS[0].startsWith(textCommand)) {
            sendMsg(msg, "Hello, world! This is simple bot!");
        } else if (COMMANDS[1].startsWith(textCommand)) {
            sendMsg(msg, "I am a telegram bot designed to inform you about video security system detections.");
        } else if (COMMANDS[2].startsWith(textCommand)) {
            Options o = Options.parseOptions(txtMsg);
            controls.put(o.key, runCapture());
            sendMsg(msg, "To control launched task use control key: " + o.key);
        } else if (COMMANDS[3].startsWith(textCommand)) {
            Options o = Options.parseOptions(txtMsg);
            controls.put(o.key, runDetector(o));
            sendMsg(msg, "To control launched task use control key: " + o.key);
        } else {
            StringBuilder b = new StringBuilder();
            Arrays.asList(COMMANDS).forEach(r -> b.append(r).append(" "));
            sendMsg(msg, "Sorry, for now i know only the following commands: " + b.toString());
        }
    }

    private Control runCapture() {
        Control c = new Control();
        Camera cam = new Camera(c);
        c.cam = cam;
        pool.execute(cam::startVideoCapture);
        return c;
    }

    private Control runDetector(Options o) {
        Control c = new Control();
        Camera cam = new Camera(c);
        c.cam = cam;
        pool.execute(() -> cam.detectMotion(o.width, o.height));
        return c;
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
