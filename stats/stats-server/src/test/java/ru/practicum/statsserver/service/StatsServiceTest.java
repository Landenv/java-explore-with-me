package ru.practicum.statsserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.statsserver.model.EndpointHit;
import ru.practicum.statsserver.repository.StatsRepository;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsService statsService;

    private EndpointHitDto hitDto;
    private EndpointHit savedHit;

    @BeforeEach
    void setUp() {
        hitDto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        savedHit = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void saveHit_WhenValid_ShouldReturnSavedHit() {
        when(statsRepository.save(any(EndpointHit.class))).thenReturn(savedHit);

        EndpointHit result = statsService.saveHit(hitDto);

        assertNotNull(result);
        assertEquals(savedHit.getId(), result.getId());
        assertEquals(savedHit.getApp(), result.getApp());
        verify(statsRepository, times(1)).save(any(EndpointHit.class));
    }

    @Test
    void getStats_WhenNotUnique_ShouldCallCorrectRepositoryMethod() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<String> uris = List.of("/events/1", "/events/2");
        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 10L)
        );

        when(statsRepository.getStats(start, end, uris)).thenReturn(expectedStats);

        List<ViewStats> result = statsService.getStats(start, end, uris, false);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(statsRepository, times(1)).getStats(start, end, uris);
        verify(statsRepository, never()).getUniqueStats(any(), any(), any());
    }

    @Test
    void getStats_WhenUnique_ShouldCallUniqueRepositoryMethod() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<String> uris = List.of("/events/1");
        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 5L)
        );

        when(statsRepository.getUniqueStats(start, end, uris)).thenReturn(expectedStats);

        List<ViewStats> result = statsService.getStats(start, end, uris, true);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(statsRepository, times(1)).getUniqueStats(start, end, uris);
        verify(statsRepository, never()).getStats(any(), any(), any());
    }

    @Test
    void getStats_WhenUrisNull_ShouldPassNullToRepository() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<ViewStats> expectedStats = List.of();

        when(statsRepository.getStats(start, end, null)).thenReturn(expectedStats);

        List<ViewStats> result = statsService.getStats(start, end, null, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(statsRepository, times(1)).getStats(start, end, null);
    }
}