package mts.java.time;

import java.time.Duration;
import java.time.Instant;

public record MaintenanceWindow(Instant start, Instant end) {
    public MaintenanceWindow {
        if (start == null){
            throw new NullPointerException("start не null!");
        }
        if (end == null){
            throw new NullPointerException("end не null!");
        }
        if (!end.isAfter(start)){
            throw new IllegalArgumentException("start должен < end!");
        }
    }
    public Duration duration(){
        return Duration.between(start, end);
    }
}
