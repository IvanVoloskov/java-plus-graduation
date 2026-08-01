package ewm.client;

import ewm.client.dto.UserShortDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping
    List<UserShortDto> findAll(@RequestParam("ids") List<Long> ids,
                               @RequestParam(value = "from", defaultValue = "0") Integer from,
                               @RequestParam(value = "size", defaultValue = "10") Integer size);
}