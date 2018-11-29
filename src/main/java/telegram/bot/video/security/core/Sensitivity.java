package telegram.bot.video.security.core;

public class Sensitivity {

    private int thresholdSensitivity;
    private int baseSensitivity;

    public Sensitivity(int baseSensitivity) {
        this.baseSensitivity = baseSensitivity;
        thresholdSensitivity = baseSensitivity / 2;
    }

    public int getLevelSensitivity(int level) {
        return baseSensitivity * level;
    }

    public int getThresholdSensitivity() {
        return thresholdSensitivity;
    }
}
