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
        EndpointHitDto hit = EndpointHitDto.builder()
                .app(appName)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.saveHit(hit);
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        return statsClient.getStats(start, end, uris, unique);
    }

    public Long getViewsForEvent(Long eventId) {
        LocalDateTime start = LocalDateTime.now().minusYears(10);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        List<String> uris = List.of("/events/" + eventId);

        List<ViewStats> stats = getStats(start, end, uris, true);

        if (stats.isEmpty()) {
            return 0L;
        }

        ViewStats eventStats = stats.stream()
                .filter(s -> s.getUri().equals("/events/" + eventId))
                .findFirst()
                .orElse(null);

        return eventStats != null ? eventStats.getHits() : 0L;
    }
}