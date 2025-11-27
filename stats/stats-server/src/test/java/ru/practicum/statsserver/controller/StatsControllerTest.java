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
import java.util.Arrays;
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
    private ViewStats viewStats1, viewStats2;

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

        viewStats1 = new ViewStats("app1", "/events/1", 5L);
        viewStats2 = new ViewStats("app2", "/events/2", 10L);
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
    void getStats_WhenUrlEncodedDates_ShouldParseCorrectly() {
        String start = "2023-01-01%2010:00:00";
        String end = "2023-01-02%2010:00:00";
        List<String> uris = List.of("/events/1");
        List<ViewStats> expectedStats = List.of(viewStats1);

        when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris), eq(false)))
                .thenReturn(expectedStats);

        List<ViewStats> result = statsController.getStats(start, end, uris, false);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(statsService).getStats(
                eq(LocalDateTime.of(2023, 1, 1, 10, 0, 0)),
                eq(LocalDateTime.of(2023, 1, 2, 10, 0, 0)),
                eq(uris),
                eq(false)
        );
    }

    @Test
    void getStats_WhenIsoDateFormat_ShouldParseCorrectly() {
        String start = "2023-01-01T10:00:00";
        String end = "2023-01-02T10:00:00";
        List<ViewStats> expectedStats = List.of(viewStats1);

        when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(null), eq(true)))
                .thenReturn(expectedStats);

        List<ViewStats> result = statsController.getStats(start, end, null, true);

        assertNotNull(result);

        verify(statsService).getStats(
                eq(LocalDateTime.of(2023, 1, 1, 10, 0, 0)),
                eq(LocalDateTime.of(2023, 1, 2, 10, 0, 0)),
                eq(null),
                eq(true)
        );
    }

    @Test
    void getStats_WhenStartAfterEnd_ShouldThrowException() {
        String start = "2023-01-02 10:00:00";
        String end = "2023-01-01 10:00:00";

        assertThrows(IllegalArgumentException.class,
                () -> statsController.getStats(start, end, null, false));

        verify(statsService, never()).getStats(any(), any(), any(), any());
    }

    @Test
    void getStats_WhenMultipleStats_ShouldReturnSortedByHitsDesc() {
        String start = "2023-01-01 10:00:00";
        String end = "2023-01-02 10:00:00";

        List<ViewStats> unsortedStats = Arrays.asList(viewStats1, viewStats2);

        when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(null), eq(true)))
                .thenReturn(unsortedStats);

        List<ViewStats> result = statsController.getStats(start, end, null, true);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getHits());
        assertEquals(5L, result.get(1).getHits());
    }

    @Test
    void getStats_WhenSingleStat_ShouldNotSort() {
        String start = "2023-01-01 10:00:00";
        String end = "2023-01-02 10:00:00";
        List<ViewStats> singleStat = List.of(viewStats1);

        when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(null), eq(false)))
                .thenReturn(singleStat);

        List<ViewStats> result = statsController.getStats(start, end, null, false);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(viewStats1, result.get(0));
    }

    @Test
    void getStats_WhenInvalidDateFormat_ShouldThrowException() {
        String start = "invalid-date";
        String end = "2023-01-02 10:00:00";

        assertThrows(IllegalArgumentException.class,
                () -> statsController.getStats(start, end, null, false));
    }
}