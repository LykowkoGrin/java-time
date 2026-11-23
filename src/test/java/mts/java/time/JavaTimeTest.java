package mts.java.time;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaTimeTest {

    private final ZoneId zone = ZoneId.of("Europe/Berlin");
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-23T00:00:00Z"), ZoneOffset.UTC);
    private final MaintenanceScheduler scheduler = new MaintenanceScheduler(clock);

    @Test
    void parse(){
        Incident incident = IncidentParser.parse(
                "INC-1;payments;2026-11-23T00:00:00Z;2026-11-23T02:30:00Z");

        assertEquals("INC-1", incident.id());
        assertEquals("payments", incident.service());
        assertEquals(Instant.parse("2026-11-23T00:00:00Z"), incident.startedAt());
        assertEquals(Instant.parse("2026-11-23T02:30:00Z"), incident.resolvedAt());
        assertEquals(Duration.ofMinutes(150), incident.duration());

        assertThrows(DateTimeParseException.class, () -> IncidentParser.parse("INC-1;payments;wrong-time;2026-11-23T02:30:00Z"));
        assertThrows(IllegalArgumentException.class, () -> IncidentParser.parse(";payments;2026-11-23T00:00:00Z;2026-11-23T02:30:00Z"));
        assertThrows(IllegalArgumentException.class, () -> IncidentParser.parse("INC-1;payments;2026-11-23T02:30:00Z;2026-11-23T00:00:00Z"));
    }

    @Test
    void analytics(){
        Instant start = Instant.parse("2026-11-23T00:00:00Z");
        Instant before = start.minusSeconds(1);
        Instant later = start.plusSeconds(1);
        Incident late = new Incident("A", "api", later, later.plusNanos(2));
        Incident second = new Incident("B", "api", start, start.plusNanos(2));
        Incident first = new Incident("A", "api", start, start.plusNanos(2));
        Incident shortIncident = new Incident("0", "api", before, before.plusNanos(1));
        List<Incident> incidents = List.of(late, second, shortIncident, first);

        assertEquals(Duration.ofNanos(7), IncidentAnalytics.totalDowntime(incidents));
        assertEquals(Duration.ofNanos(1), IncidentAnalytics.averageDowntime(incidents));
        assertEquals(Optional.of(first), IncidentAnalytics.longestIncident(incidents));

        assertEquals(Duration.ZERO, IncidentAnalytics.totalDowntime(List.of()));
        assertEquals(Duration.ZERO, IncidentAnalytics.averageDowntime(List.of()));
        assertEquals(Optional.empty(), IncidentAnalytics.longestIncident(List.of()));
    }

    @Test
    void groups(){
        Instant beforeMidnight = Instant.parse("2026-11-23T22:00:00Z");
        Instant afterMidnight = Instant.parse("2026-11-23T23:00:00Z");
        Instant later = Instant.parse("2026-11-23T23:30:00Z");
        Incident old = new Incident("old", "api", beforeMidnight, beforeMidnight.plusSeconds(60));
        Incident early = new Incident("early", "api", afterMidnight, afterMidnight.plusSeconds(60));
        Incident first = new Incident("A", "api", later, later.plusSeconds(60));
        Incident second = new Incident("B", "api", later, later.plusSeconds(60));

        Map<LocalDate, List<Incident>> groups = IncidentAnalytics.groupByStartDate(List.of(second, first, early, old), zone);

        assertEquals(List.of(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 24)), new ArrayList<>(groups.keySet()));
        assertEquals(List.of(old), groups.get(LocalDate.of(2026, 11, 23)));
        assertEquals(List.of(early, first, second), groups.get(LocalDate.of(2026, 11, 24)));
    }

    @Test
    void gap(){
        assertThrows(DateTimeException.class, () -> scheduler.nextWindow(
                LocalDate.of(2026, 3, 29), LocalTime.of(2, 30),
                Duration.ofMinutes(30), zone, OverlapPolicy.EARLIER));
    }

    @Test
    void overlap(){
        MaintenanceWindow earlier = scheduler.nextWindow(
                LocalDate.of(2026, 10, 25), LocalTime.of(2, 30),
                Duration.ofMinutes(30), zone, OverlapPolicy.EARLIER);
        MaintenanceWindow later = scheduler.nextWindow(
                LocalDate.of(2026, 10, 25), LocalTime.of(2, 30),
                Duration.ofMinutes(30), zone, OverlapPolicy.LATER);

        assertEquals(ZoneOffset.ofHours(2), earlier.start().atZone(zone).getOffset());
        assertEquals(ZoneOffset.ofHours(1), later.start().atZone(zone).getOffset());
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), earlier.start());
        assertEquals(Instant.parse("2026-10-25T01:30:00Z"), later.start());
        assertEquals(Duration.ofHours(1), Duration.between(earlier.start(), later.start()));
        assertEquals(earlier.start().plusSeconds(1800), earlier.end());
        assertEquals(later.start().plusSeconds(1800), later.end());
        assertEquals(Duration.ofMinutes(30), earlier.duration());
        assertEquals(Duration.ofMinutes(30), later.duration());
    }

    @Test
    void windows(){
        MaintenanceScheduler scheduler = new MaintenanceScheduler(Clock.fixed(Instant.parse("2026-03-28T00:00:00Z"), ZoneOffset.UTC));
        List<MaintenanceWindow> windows = scheduler.upcomingWindows(2, LocalTime.of(3, 30), Duration.ofMinutes(30), zone, OverlapPolicy.EARLIER);

        assertEquals(2, windows.size());
        assertEquals(Instant.parse("2026-03-28T02:30:00Z"), windows.get(0).start());
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), windows.get(1).start());
        assertEquals(LocalTime.of(3, 30), windows.get(0).start().atZone(zone).toLocalTime());
        assertEquals(LocalTime.of(3, 30), windows.get(1).start().atZone(zone).toLocalTime());
        assertEquals(Duration.ofHours(23), Duration.between(windows.get(0).start(), windows.get(1).start()));
        assertEquals(windows.get(0).start().plusSeconds(1800), windows.get(0).end());
        assertEquals(windows.get(1).start().plusSeconds(1800), windows.get(1).end());

        assertEquals(windows, scheduler.upcomingWindows(2, LocalTime.of(3, 30), Duration.ofMinutes(30), zone, OverlapPolicy.EARLIER));
        assertTrue(scheduler.upcomingWindows(0, LocalTime.of(3, 30), Duration.ofMinutes(30), zone, OverlapPolicy.EARLIER).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> scheduler.upcomingWindows(-1, LocalTime.of(3, 30), Duration.ofMinutes(30), zone, OverlapPolicy.EARLIER));
    }

    @Test
    void report(){
        MaintenanceWindow window = new MaintenanceWindow(
                Instant.parse("2026-11-23T01:00:00Z"), Instant.parse("2026-11-23T02:00:00Z"));

        assertEquals("23.11.2026 02:00 +01:00 (Europe/Berlin) – " + "23.11.2026 03:00 +01:00 (Europe/Berlin)",
                ReportFormatter.formatWindow(window, zone, Locale.GERMANY));

        Instant instant = Instant.parse("2026-11-23T01:00:00.123Z");
        Date date = LegacyDateAdapter.toDate(instant);
        assertEquals(1795395600123L, date.getTime());
        assertEquals(instant, LegacyDateAdapter.toInstant(date));

        ZonedDateTime dateTime = instant.atZone(zone);
        GregorianCalendar calendar = LegacyDateAdapter.toGregorianCalendar(dateTime);
        assertEquals(dateTime.toInstant(), calendar.toInstant());
        assertEquals(zone, calendar.getTimeZone().toZoneId());
        assertEquals(dateTime, LegacyDateAdapter.toZonedDateTime(calendar));
    }
}
