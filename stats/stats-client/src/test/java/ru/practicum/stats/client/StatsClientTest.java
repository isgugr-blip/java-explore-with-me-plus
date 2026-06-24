package ru.practicum.stats.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StatsClientTest {
    private static final String SERVER_URL = "http://localhost:9090";

    private StatsClient statsClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        statsClient = new StatsClient(SERVER_URL, restTemplate);
    }

    @Test
    void saveHitShouldSendPostRequest() {
        EndpointHitDto hit = new EndpointHitDto(
                "ewm-main-service",
                "/events/1",
                "192.163.0.1",
                LocalDateTime.of(2022, 9, 6, 11, 0, 23)
        );

        mockServer.expect(requestTo(SERVER_URL + "/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.app").value("ewm-main-service"))
                .andExpect(jsonPath("$.uri").value("/events/1"))
                .andExpect(jsonPath("$.ip").value("192.163.0.1"))
                .andExpect(jsonPath("$.timestamp").value("2022-09-06 11:00:23"))
                .andRespond(withStatus(HttpStatus.CREATED));

        statsClient.saveHit(hit);

        mockServer.verify();
    }

    @Test
    void getStatsShouldSendEncodedGetRequestAndReadResponse() {
        String response = "[{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\",\"hits\":3}]";
        String expectedUrl = SERVER_URL
                + "/stats?start=2022-09-06+11%3A00%3A00"
                + "&end=2022-09-06+12%3A00%3A00"
                + "&uris=%2Fevents%2F1"
                + "&uris=%2Fevents%2F2"
                + "&unique=true";

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        List<ViewStatsDto> result = statsClient.getStats(
                LocalDateTime.of(2022, 9, 6, 11, 0),
                LocalDateTime.of(2022, 9, 6, 12, 0),
                List.of("/events/1", "/events/2"),
                true
        );

        assertThat(result).containsExactly(new ViewStatsDto("ewm-main-service", "/events/1", 3L));
        mockServer.verify();
    }

    @Test
    void getStatsShouldReturnEmptyListWhenResponseBodyIsEmpty() {
        String expectedUrl = SERVER_URL
                + "/stats?start=2022-09-06+11%3A00%3A00"
                + "&end=2022-09-06+12%3A00%3A00"
                + "&unique=false";

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        List<ViewStatsDto> result = statsClient.getStats(
                LocalDateTime.of(2022, 9, 6, 11, 0),
                LocalDateTime.of(2022, 9, 6, 12, 0)
        );

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void getStatsShouldRejectWrongDateRange() {
        assertThatThrownBy(() -> statsClient.getStats(
                LocalDateTime.of(2022, 9, 6, 12, 0),
                LocalDateTime.of(2022, 9, 6, 11, 0)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
