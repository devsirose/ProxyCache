import java.time.LocalTime;

public class TimeRange {
    private static LocalTime startTime;
    private static LocalTime endTime;

    public TimeRange(LocalTime startTime, LocalTime endTime) {
        TimeRange.startTime = startTime;
        TimeRange.endTime = endTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public boolean contain(LocalTime time) {
        return startTime.compareTo(time) <= 0 && endTime.compareTo(time) >= 0;
    }
}
