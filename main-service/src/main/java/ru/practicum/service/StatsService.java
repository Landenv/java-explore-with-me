package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsclient.StatsClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsClient statsClient;

    @Value("${app.name:ewm-main-service}")
    private String appName;

    public void saveHit(String uri, String ip) {
        System.out.println("=== DEBUG saveHit ===");
        System.out.println("URI: " + uri);
        System.out.println("IP: " + ip);
        System.out.println("App: " + appName);

        EndpointHitDto hit = EndpointHitDto.builder()
                .app(appName)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        System.out.println("Saving hit: " + hit);
        statsClient.saveHit(hit);
        System.out.println("Hit saved successfully");
        System.out.println("=== END DEBUG saveHit ===");
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        System.out.println("=== DEBUG getStats ===");
        System.out.println("Start: " + start);
        System.out.println("End: " + end);
        System.out.println("URIs: " + uris);
        System.out.println("Unique: " + unique);

        List<ViewStats> stats = statsClient.getStats(start, end, uris, unique);

        System.out.println("Received stats: " + stats);
        System.out.println("=== END DEBUG getStats ===");
        return stats;
    }

    public Long getViewsForEvent(Long eventId) {
        System.out.println("=== DEBUG getViewsForEvent ===");
        System.out.println("Event ID: " + eventId);

        LocalDateTime start = LocalDateTime.now().minusYears(10); // вместо 1 года
        LocalDateTime end = LocalDateTime.now().plusHours(1);     // добавляем запас

        List<String> uris = List.of("/events/" + eventId);

        System.out.println("Start date: " + start);
        System.out.println("End date: " + end);
        System.out.println("URIs to search: " + uris);

        List<ViewStats> stats = getStats(start, end, uris, true);

        System.out.println("All stats for event " + eventId + ": " + stats);

        if (stats.isEmpty()) {
            System.out.println("No stats found for event " + eventId + ", returning 0");
            System.out.println("=== END DEBUG getViewsForEvent === (return 0)");
            return 0L;
        }

        ViewStats eventStats = stats.stream()
                .filter(s -> s.getUri().equals("/events/" + eventId))
                .findFirst()
                .orElse(null);

        System.out.println("Found event stats: " + eventStats);

        Long views = eventStats != null ? eventStats.getHits() : 0L;
        System.out.println("Final views count for event " + eventId + ": " + views);
        System.out.println("=== END DEBUG getViewsForEvent === (return " + views + ")");

        return views;
    }
}