package ru.practicum.stats.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class StatsClient {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String serverUrl;
    private final RestTemplate restTemplate;

    public StatsClient(String serverUrl) {
        this(serverUrl, new RestTemplate());
    }

    public StatsClient(String serverUrl, RestTemplate restTemplate) {
        this.serverUrl = normalizeServerUrl(serverUrl);
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate must not be null");
    }

    public void hit(String app, String uri, String ip, LocalDateTime timestamp) {
        saveHit(new EndpointHitDto(app, uri, ip, timestamp));
    }

    public void saveHit(EndpointHitDto hit) {
        Objects.requireNonNull(hit, "hit must not be null");
        restTemplate.postForLocation(URI.create(serverUrl + "/hit"), hit);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end) {
        return getStats(start, end, List.of(), false);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, Collection<String> uris) {
        return getStats(start, end, uris, false);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       Collection<String> uris,
                                       boolean unique) {
        validateDates(start, end);
        URI statsUri = buildStatsUri(start, end, uris, unique);
        ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                statsUri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        List<ViewStatsDto> body = response.getBody();
        if (body == null) {
            return List.of();
        }
        return body;
    }

    private URI buildStatsUri(LocalDateTime start,
                              LocalDateTime end,
                              Collection<String> uris,
                              boolean unique) {
        StringBuilder query = new StringBuilder()
                .append("start=")
                .append(encode(format(start)))
                .append("&end=")
                .append(encode(format(end)));

        if (uris != null) {
            uris.stream()
                    .filter(Objects::nonNull)
                    .filter(uri -> !uri.isBlank())
                    .forEach(uri -> query.append("&uris=").append(encode(uri)));
        }

        query.append("&unique=").append(unique);
        return URI.create(serverUrl + "/stats?" + query);
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must not be before start");
        }
    }

    private String format(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeServerUrl(String serverUrl) {
        Objects.requireNonNull(serverUrl, "serverUrl must not be null");
        String trimmedServerUrl = serverUrl.trim();
        if (trimmedServerUrl.isEmpty()) {
            throw new IllegalArgumentException("serverUrl must not be blank");
        }
        while (trimmedServerUrl.endsWith("/")) {
            trimmedServerUrl = trimmedServerUrl.substring(0, trimmedServerUrl.length() - 1);
        }
        return trimmedServerUrl;
    }
}
