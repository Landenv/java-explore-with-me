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
    private static final String EVENT_URI_PREFIX = "/events/";
    private static final int STATS_YEARS_RANGE = 1;
    private static final Long DEFAULT_VIEWS_COUNT = 0L;

    public void saveHit(String uri, String ip) {
        log.info("Saving hit - uri: {}, ip: {}", uri, ip);
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(APP_NAME)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.saveHit(hitDto);
    }

    public Long getEventViews(Long eventId) {
        LocalDateTime start = LocalDateTime.now().minusYears(STATS_YEARS_RANGE);
        LocalDateTime end = LocalDateTime.now().plusYears(STATS_YEARS_RANGE);
        List<String> uris = List.of(EVENT_URI_PREFIX + eventId);

        List<ViewStats> stats = statsClient.getStats(start, end, uris, true);
        if (stats.isEmpty()) {
            return DEFAULT_VIEWS_COUNT;
        }
        return stats.get(0).getHits();
    }

    public List<ViewStats> getEventsViews(List<Long> eventIds, LocalDateTime start, LocalDateTime end, Boolean unique) {
        List<String> uris = eventIds.stream()
                .map(id -> EVENT_URI_PREFIX + id)
                .collect(java.util.stream.Collectors.toList());
        return statsClient.getStats(start, end, uris, unique);
    }
}