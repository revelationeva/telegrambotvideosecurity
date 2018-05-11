package telegram.bot.video.security;

import java.util.ArrayList;
import java.util.List;

public class CameraDetectionStatistics {

    List<Long> minor = new ArrayList<>();
    List<Long> medium = new ArrayList<>();
    List<Long> major = new ArrayList<>();

    public String getInHumanReadableFormat() {
        return "Minor detections: " + minor.size() + "\nMedium detections: " + medium.size() + "\nMajor detections: " + major.size();
    }
}
