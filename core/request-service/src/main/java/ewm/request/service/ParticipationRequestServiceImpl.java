package ewm.request.service;

import ewm.client.EventClient;
import ewm.client.UserClient;
import ewm.client.dto.EventInfoDto;
import ewm.client.dto.UserShortDto;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import ewm.exception.ValidationException;
import ewm.request.dto.EventRequestStatusUpdateRequest;
import ewm.request.dto.EventRequestStatusUpdateResult;
import ewm.request.dto.ParticipationRequestDto;
import ewm.request.mapper.ParticipationRequestMapper;
import ewm.request.model.ConfirmedRequestCount;
import ewm.request.model.ParticipationRequest;
import ewm.request.model.ParticipationStatus;
import ewm.request.repository.ParticipationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper requestMapper;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    public List<ParticipationRequestDto> getRequestByUserId(Long userId) {
        checkUserExists(userId);

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        log.info("Получен список заявок на участия в событиях пользователя с id = {}", userId);
        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        checkUserExists(userId);

        EventInfoDto event = getEventInfoOrThrow(eventId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Participation request already exists");
        }

        if (event.initiatorId().equals(userId)) {
            throw new ConflictException("The initiator of the event cannot add a request to participate in their own event");
        }

        if (!"PUBLISHED".equals(event.state())) {
            throw new ConflictException("The event has not been published yet");
        }

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

        if (event.participantLimit() != 0 && event.participantLimit() <= confirmedRequests) {
            throw new ConflictException("The participant limit for this event has been reached");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);
        request.setEventId(eventId);

        if (Boolean.FALSE.equals(event.requestModeration()) || event.participantLimit() == 0) {
            request.setStatus(ParticipationStatus.CONFIRMED);
        } else {
            request.setStatus(ParticipationStatus.PENDING);
        }

        ParticipationRequest saveRequest = requestRepository.save(request);

        log.info("Запрос на участие в событии добавлен");

        return requestMapper.mapToRequestDto(saveRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        checkUserExists(userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request с id=" + requestId + " was not found"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ValidationException("You can only cancel your own request");
        }

        request.setStatus(ParticipationStatus.CANCELED);
        requestRepository.save(request);

        log.info("Заявка на событие отменена");

        return requestMapper.mapToRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        checkUserExists(userId);

        EventInfoDto event = getEventInfoOrThrow(eventId);

        if (!event.initiatorId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user with id=" + userId);
        }

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        log.info("Получен список заявок на участие в событии с id = {}", eventId);
        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest requestUpdate) {
        checkUserExists(userId);

        EventInfoDto event = getEventInfoOrThrow(eventId);

        if (!event.initiatorId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user with id=" + userId);
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(requestUpdate.requestIds());

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

        if (event.participantLimit() != 0 && confirmedRequests >= event.participantLimit()) {
            throw new ConflictException("The participant limit for this event has been reached");
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEventId().equals(eventId)) {
                throw new ValidationException("Request does not belong to this event");
            }
            if (request.getStatus() != ParticipationStatus.PENDING) {
                throw new ConflictException("Request status must be PENDING");
            }

            if ("REJECTED".equals(requestUpdate.status())) {
                request.setStatus(ParticipationStatus.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(request));
            } else if ("CONFIRMED".equals(requestUpdate.status())) {
                if (event.participantLimit() == 0 || confirmedRequests < event.participantLimit()) {
                    request.setStatus(ParticipationStatus.CONFIRMED);
                    confirmed.add(requestMapper.mapToRequestDto(request));
                    confirmedRequests++;
                } else {
                    request.setStatus(ParticipationStatus.REJECTED);
                    rejected.add(requestMapper.mapToRequestDto(request));
                }
            }
        }

        requestRepository.saveAll(requests);
        log.info("Обновлён статус заявок на участие в событии с id = {}", eventId);

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<ConfirmedRequestCount> getConfirmedCounts(List<Long> eventIds) {
        return requestRepository.findAllConfirmedRequests(eventIds);
    }

    private void checkUserExists(Long userId) {
        List<UserShortDto> users = userClient.findAll(List.of(userId), 0, 1);
        if (users.isEmpty()) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private EventInfoDto getEventInfoOrThrow(Long eventId) {
        try {
            return eventClient.getEventInfo(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
    }
}