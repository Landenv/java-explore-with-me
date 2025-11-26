package ru.practicum.statsclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class StatsClient {
    private final String serverUrl;
    private final RestTemplate restTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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
        String startFormatted = start.format(formatter);
        String endFormatted = end.format(formatter);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", startFormatted)
                .queryParam("end", endFormatted)
                .queryParam("unique", unique != null ? unique : false);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", uris.toArray());
        }

        String url = builder.toUriString();
        log.debug("Requesting stats with URL: {}", url);

        try {
            ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(url, ViewStats[].class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception exception) {
            log.error("Failed to get stats: {}", exception.getMessage());
        }

        return List.of();
    }
}