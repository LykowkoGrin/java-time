package mts.java.time;

import java.time.Instant;

public class IncidentParser {

    static public Incident parse(String line) {
        String[] fragments = line.split(";");
        if (fragments.length != 4){
            throw new RuntimeException("Строка содержит не 4 параметра инцидента разделенных ';'");
        }
        String id = fragments[0];
        String service = fragments[1];
        if (fragments[2].isBlank()) throw new IllegalArgumentException("Стартовое время пусто!");
        if (fragments[3].isBlank()) throw new IllegalArgumentException("Конечное время пусто!");
        Instant started = Instant.parse(fragments[2]);
        Instant resolved = Instant.parse(fragments[3]);
        if (id.isBlank()) throw new IllegalArgumentException("Id не должен быть пустым!");
        if (service.isBlank()) throw new IllegalArgumentException("Название сервиса не должно ьыть пустым!");

        if(resolved.isBefore(started)) throw new IllegalArgumentException("Время решения должно идти после времени начала инцидента!");
        return new Incident(id, service, started, resolved);
    }
}
