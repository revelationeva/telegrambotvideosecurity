package telegram.bot.video.security.entity;
// Generated May 11, 2018 7:26:25 PM by Hibernate Tools 3.2.2.GA


import javax.persistence.*;
import java.util.Date;

/**
 * CaptureStatistics generated by hbm2java
 */
@Entity
@Table(name = "capture_statistics", catalog = "telegrambotvideosecurity")
public class CaptureStatistics extends IEntity {

    private Capture capture;
    private Date dateReported;
    private Long minorDetections = 0L;
    private Long meduimDetections = 0L;
    private Long majorDetections = 0L;

    @Transient
    public void incrementMinor(long incr) {
        minorDetections += incr;
    }

    @Transient
    public void incrementMedium(long incr) {
        meduimDetections += incr;
    }

    @Transient
    public void incrementMajor(long incr) {
        majorDetections += incr;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capture_id", nullable = false)
    public Capture getCapture() {
        return this.capture;
    }

    public void setCapture(Capture capture) {
        this.capture = capture;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_reported", nullable = false, length = 19)
    public Date getDateReported() {
        return this.dateReported;
    }

    public void setDateReported(Date dateReported) {
        this.dateReported = dateReported;
    }

    @Column(name = "minor_detections")
    public Long getMinorDetections() {
        return this.minorDetections;
    }

    public void setMinorDetections(Long minorDetections) {
        this.minorDetections = minorDetections;
    }

    @Column(name = "meduim_detections")
    public Long getMeduimDetections() {
        return this.meduimDetections;
    }

    public void setMeduimDetections(Long meduimDetections) {
        this.meduimDetections = meduimDetections;
    }

    @Column(name = "major_detections")
    public Long getMajorDetections() {
        return this.majorDetections;
    }

    public void setMajorDetections(Long majorDetections) {
        this.majorDetections = majorDetections;
    }
}


