package ru.practicum.statsserver.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.statsserver.model.EndpointHit;
import ru.practicum.statsserver.service.StatsService;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @Mock
    private StatsService statsService;

    @InjectMocks
    private StatsController statsController;

    private EndpointHitDto hitDto;
    private EndpointHit savedHit;
    private ViewStats viewStats;

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

        viewStats = new ViewStats("ewm-main-service", "/events/1", 10L);
    }

    @Test
    void saveHit_WhenValid_ShouldReturnSavedHit() {
        when(statsService.saveHit(any(EndpointHitDto.class))).thenReturn(savedHit);

        EndpointHit result = statsController.saveHit(hitDto);

        assertNotNull(result);
        assertEquals(savedHit.getId(), result.getId());
        verify(statsService, times(1)).saveHit(hitDto);
    }

    @Test
    void getStats_WhenValidParams_ShouldReturnStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<String> uris = List.of("/events/1");
        List<ViewStats> expectedStats = List.of(viewStats);

        when(statsService.getStats(start, end, uris, false)).thenReturn(expectedStats);

        List<ViewStats> result = statsController.getStats(start, end, uris, false);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(viewStats.getApp(), result.getFirst().getApp());
        verify(statsService, times(1)).getStats(start, end, uris, false);
    }

    @Test
    void getStats_WhenStartAfterEnd_ShouldThrowException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        assertThrows(IllegalArgumentException.class,
                () -> statsController.getStats(start, end, null, false));
    }
}