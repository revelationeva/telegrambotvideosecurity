package telegram.bot.video.security.option;

public class ControlOptions {
    public String uid;
    public String command;
    public String[] options;

    public int sensivity = 500;
    public int pollInterval = 1000;
    public int width = 640;
    public int height = 480;

    public static ControlOptions parseControlOptions(String txt) {
        String[] split = txt.split(" ");
        ControlOptions o = new ControlOptions();
        if (split.length > 0 && txt.contains(" ")) {
            o.uid = split[0];
            if (split.length > 1) {
                o.command = split[1];
            }
            if (split.length > 2) {
                o.sensivity = Integer.parseInt(split[2]);
            }
            if (split.length > 3) {
                o.pollInterval = Integer.parseInt(split[3]);
            }
            if (split.length > 4) {
                o.width = Integer.parseInt(split[4]);
            }
            if (split.length > 5) {
                o.height = Integer.parseInt(split[5]);
            }
        }
        return o;
    }
}
