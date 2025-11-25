package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsClient statsClient;

    private static final String APP_NAME = "ewm-main-service";

    public void saveHit(String app, String uri, String ip) {
        log.info("Saving hit - app: {}, uri: {}, ip: {}", app, uri, ip);
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.saveHit(hitDto);
    }

    public Long getEventViews(Long eventId) {
        LocalDateTime start = LocalDateTime.now().minusYears(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/events/" + eventId);

        try {
            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);
            log.info("Stats for event {}: {}", eventId, stats);

            if (stats.isEmpty()) {
                return 0L;
            }
            return stats.get(0).getHits();
        } catch (Exception e) {
            log.error("Error getting stats for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    public List<ViewStats> getEventsViews(List<Long> eventIds, LocalDateTime start, LocalDateTime end, Boolean unique) {
        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(java.util.stream.Collectors.toList());
        return statsClient.getStats(start, end, uris, unique);
    }
}