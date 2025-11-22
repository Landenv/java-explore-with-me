package ru.practicum.statsclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsClientTest {

    @Mock
    private RestTemplate restTemplate;

    private StatsClient statsClient;

    @BeforeEach
    void setUp() {
        statsClient = new StatsClient("http://localhost:9090", restTemplate);
    }

    @Test
    void saveHit_WhenCalled_ShouldSendPostRequest() {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.saveHit(hitDto);

        verify(restTemplate, times(1)).postForEntity(
                eq("http://localhost:9090/hit"),
                any(),
                eq(Object.class)
        );
    }

    @Test
    void getStats_WhenCalled_ShouldSendGetRequest() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<String> uris = List.of("/events/1");

        ViewStats[] response = new ViewStats[]{
                new ViewStats("test-app", "/events/1", 5L)
        };

        when(restTemplate.getForEntity(anyString(), eq(ViewStats[].class), anyMap()))
                .thenReturn(org.springframework.http.ResponseEntity.ok(response));

        List<ViewStats> result = statsClient.getStats(start, end, uris, true);

        assert result.size() == 1;
        assert result.getFirst().getHits() == 5L;
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(ViewStats[].class), anyMap());
    }
}