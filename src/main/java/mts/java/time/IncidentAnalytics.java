package mts.java.time;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class IncidentAnalytics {
    public static Duration totalDowntime(List<Incident> incidents){
        Duration total = Duration.ZERO;
        for(Incident incident : incidents){
            total = total.plus(incident.duration());
        }
        return total;
    }
    public static Duration averageDowntime(List<Incident> incidents){
        if (incidents.isEmpty()){
            return Duration.ZERO;
        }
        return totalDowntime(incidents).dividedBy(incidents.size());
    }
    public static Optional<Incident> longestIncident(List<Incident> incidents){
        return incidents.stream()
                .sorted(Comparator.comparing(Incident::duration).reversed()
                        .thenComparing(Incident::startedAt)
                        .thenComparing(Incident::id)).findFirst();
    }
    public static Map<LocalDate, List<Incident>> groupByStartDate(List<Incident> incidents, ZoneId zone){
        Map<LocalDate, List<Incident>> result = new TreeMap<>();

        for (Incident incident : incidents){
            LocalDate date = incident.startedAt().atZone(zone).toLocalDate();

            List<Incident> group = result.get(date);
            if (group == null){
                group = new ArrayList<>();
                result.put(date, group);
            }

            group.add(incident);
        }

        for (List<Incident> group : result.values()){
            group.sort(Comparator.comparing(Incident::startedAt).thenComparing(Incident::id));
        }

        return result;
    }
}
