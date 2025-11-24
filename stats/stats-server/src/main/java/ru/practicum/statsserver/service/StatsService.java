package ru.practicum.statsserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.statsserver.model.EndpointHit;
import ru.practicum.statsserver.repository.StatsRepository;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final StatsRepository statsRepository;

    @Transactional
    public EndpointHit saveHit(EndpointHitDto hitDto) {
        EndpointHit hit = EndpointHit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(hitDto.getTimestamp())
                .build();

        return statsRepository.save(hit);
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (unique != null && unique) {
            return statsRepository.getUniqueStats(start, end, uris);
        } else {
            return statsRepository.getStats(start, end, uris);
        }
    }
}