package ru.practicum.statsserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.statsserver.model.EndpointHit;
import ru.practicum.statsserver.service.StatsService;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;

import jakarta.validation.Valid;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHit saveHit(@Valid @RequestBody EndpointHitDto hitDto) {
        log.info("Received hit: app={}, uri={}, ip={}, timestamp={}",
                hitDto.getApp(), hitDto.getUri(), hitDto.getIp(), hitDto.getTimestamp());

        return statsService.saveHit(hitDto);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(required = false, defaultValue = "false") Boolean unique) {

        LocalDateTime startDate = parseDateTimeFlexibly(start);
        LocalDateTime endDate = parseDateTimeFlexibly(end);

        log.info("Getting stats from {} to {}, uris: {}, unique: {}", startDate, endDate, uris, unique);

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        List<ViewStats> stats = statsService.getStats(startDate, endDate, uris, unique);

        if (stats != null && stats.size() > 1) {
            List<ViewStats> sortedStats = new ArrayList<>(stats);
            sortedStats.sort((s1, s2) -> Long.compare(s2.getHits(), s1.getHits()));
            return sortedStats;
        }

        return stats;
    }

    private LocalDateTime parseDateTimeFlexibly(String dateTimeStr) {
        try {
            String decoded = URLDecoder.decode(dateTimeStr, StandardCharsets.UTF_8);
            String cleaned = decoded.trim();

            if (cleaned.contains("T")) {
                return LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                return LocalDateTime.parse(cleaned, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateTimeStr, e);
        }
    }
}