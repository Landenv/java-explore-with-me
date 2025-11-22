package ru.practicum.statsclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class StatsClient {
    private final String serverUrl;
    private final RestTemplate restTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(String serverUrl, RestTemplate restTemplate) {
        this.serverUrl = serverUrl;
        this.restTemplate = restTemplate;
    }

    public void saveHit(EndpointHitDto hitDto) {
        String url = serverUrl + "/hit";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(hitDto, headers);

        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(url, requestEntity, Object.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                log.info("Hit successfully saved: {}", hitDto);
            }
        } catch (Exception e) {
            log.error("Failed to save hit: {}", e.getMessage());
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String url = serverUrl + "/stats?start={start}&end={end}&unique={unique}";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", start.format(formatter));
        parameters.put("end", end.format(formatter));
        parameters.put("unique", unique != null ? unique : false);

        if (uris != null && !uris.isEmpty()) {
            url += "&uris={uris}";
            parameters.put("uris", String.join(",", uris));
        }

        try {
            ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(url, ViewStats[].class, parameters);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage());
        }

        return List.of();
    }
}