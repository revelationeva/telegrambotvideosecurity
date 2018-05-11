package telegram.bot.video.security;

class Control {
    boolean shutdown;
    Camera cam;

    String getStats() {
        return cam.stats.getInHumanReadableFormat();
    }
}
