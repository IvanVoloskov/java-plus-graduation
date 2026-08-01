package client;

import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class StatClient {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${stats.service.id:stats-server}")
    private String statsServiceId;

    public StatClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.builder().build();

        this.retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    public void hit(HitDto hitDto) {
        try {
            restClient.post()
                    .uri(makeUri("/hit"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.info("Ошибка при отправке hit в сервис статистики");
        }
    }

    public List<StatsDto> get(ParamDto paramDto) {
        List<StatsDto> stats;

        Optional<List<String>> uris = (paramDto.uris() == null || paramDto.uris().isEmpty())
                ? Optional.empty()
                : Optional.of(paramDto.uris());

        try {
            URI baseUri = makeUri("/stats");
            stats = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme(baseUri.getScheme())
                            .host(baseUri.getHost())
                            .port(baseUri.getPort())
                            .path(baseUri.getPath())
                            .queryParam("start", paramDto.start().format(formatter))
                            .queryParam("end", paramDto.end().format(formatter))
                            .queryParamIfPresent("uris", uris)
                            .queryParamIfPresent("unique", Optional.ofNullable(paramDto.unique()))
                            .build())
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (Exception e) {
            log.info("Ошибка при отправке get в сервис статистики");
            stats = List.of(new StatsDto("ewm-main-service", "/fake-uri", 0L));
        }
        return stats;
    }
}