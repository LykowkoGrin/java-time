package mts.java.time;

import java.time.Duration;
import java.time.Instant;

public record Incident(
        String id,
        String service,
        Instant startedAt,
        Instant resolvedAt) {
    public Duration duration() {
        return Duration.between(startedAt, resolvedAt);
    }
}
