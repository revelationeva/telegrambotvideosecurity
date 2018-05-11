package telegram.bot.video.security;

public class ControlOptions {
    String command;

    public static ControlOptions parseControlOptions(String txt) {
        String[] split = txt.split(" ");
        ControlOptions o = new ControlOptions();
        if (split.length > 0 && txt.contains(" ")) {
            if (split.length > 1) {
                o.command = split[1];
            }
        }
        return o;
    }
}
