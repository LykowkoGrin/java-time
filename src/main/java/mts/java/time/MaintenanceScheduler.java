package mts.java.time;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

public final class MaintenanceScheduler {
    private Clock clock;
    public MaintenanceScheduler(Clock clock){
        if (clock == null){
            throw new NullPointerException("Clock должен быть не null!");
        }
        this.clock = clock;
    }
    public MaintenanceWindow nextWindow(
            LocalDate date, LocalTime localStart, Duration duration,
            ZoneId zone, OverlapPolicy policy) {
        if (policy == null){
            throw new NullPointerException("Политика смещения не задана!");
        }
        if (duration.isZero() || duration.isNegative()){
            throw new IllegalArgumentException("Длительность должна быть положительной!");
        }

        LocalDateTime local = date.atTime(localStart);

        if (zone.getRules().getValidOffsets(local).isEmpty()){
            throw new DateTimeException("Местного времени не существует!");
        }

        ZonedDateTime zoned = local.atZone(zone);

        if (policy == OverlapPolicy.EARLIER){
            zoned = zoned.withEarlierOffsetAtOverlap();
        } else{
            zoned = zoned.withLaterOffsetAtOverlap();
        }

        Instant start = zoned.toInstant();
        Instant end = start.plus(duration);

        return new MaintenanceWindow(start, end);
    }
    public List<MaintenanceWindow> upcomingWindows(
            int days, LocalTime localStart, Duration duration,
            ZoneId zone, OverlapPolicy policy) {
        if (days < 0){
            throw new IllegalArgumentException("Количество дней долажно быть положительно!");
        }

        List<MaintenanceWindow> windows = new ArrayList<>();

        if (days == 0){
            return windows;
        }

        LocalDate date = LocalDate.now(clock.withZone(zone));

        for (int day = 0; day < days; day++){
            windows.add(nextWindow(date.plusDays(day), localStart, duration, zone, policy));
        }

        return windows;
    }
}
