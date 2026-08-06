package ewm.client;

import ewm.client.dto.ConfirmedRequestCount;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "request-service",
        fallback = RequestClientFallback.class,
        configuration = RequestClientConfig.class)
public interface RequestClient {

    @GetMapping("/requests/confirmed")
    List<ConfirmedRequestCount> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds);
}