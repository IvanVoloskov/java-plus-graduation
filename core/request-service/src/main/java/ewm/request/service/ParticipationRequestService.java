package ewm.request.service;

import ewm.request.dto.EventRequestStatusUpdateRequest;
import ewm.request.dto.EventRequestStatusUpdateResult;
import ewm.request.dto.ParticipationRequestDto;
import ewm.request.model.ConfirmedRequestCount;

import java.util.List;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getRequestByUserId(Long userId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest requestUpdate);

    List<ConfirmedRequestCount> getConfirmedCounts(List<Long> eventIds);
}
