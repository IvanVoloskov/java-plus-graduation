package ewm.client;

import ewm.client.dto.ConfirmedRequestCount;
import ewm.client.dto.ParticipationRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public List<ConfirmedRequestCount> getConfirmedCounts(List<Long> eventIds) {
        log.warn("request-service недоступен, confirmedRequests=0 для {} событий", eventIds.size());
        return List.of();
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.warn("request-service недоступен, заявки пользователя: id = {} недоступны", userId);
        return List.of();
    }
}