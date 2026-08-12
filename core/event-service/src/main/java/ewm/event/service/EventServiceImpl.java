package ewm.event.service;

import client.AnalyzerClient;
import client.CollectorClient;
import com.querydsl.core.BooleanBuilder;
import ewm.category.model.Category;
import ewm.category.repository.CategoryRepository;
import ewm.client.RequestClient;
import ewm.client.UserClient;
import ewm.client.dto.ConfirmedRequestCount;
import ewm.client.dto.UserShortDto;
import ewm.event.dto.AdminEventSearchFilter;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventInfoDto;
import ewm.event.dto.EventPreviewDto;
import ewm.event.dto.EventShortDto;
import ewm.event.dto.NewEventDto;
import ewm.event.dto.PublicEventParamDto;
import ewm.event.dto.UpdateEventAdminRequest;
import ewm.event.dto.UpdateEventUserRequest;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Event;
import ewm.event.model.EventState;
import ewm.event.model.Location;
import ewm.event.model.QEvent;
import ewm.event.repository.EventRepository;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import ewm.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CategoryRepository categoryRepository;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;
    private final UserClient userClient;
    private final RequestClient requestClient;

    private static final String SORT_RATING = "RATING";
    private static final String EVENT_DATE = "EVENT_DATE";

    @Override
    public List<EventShortDto> getEventsPrivate(Long userId, Integer from, Integer size) {
        log.info("Getting events for user id={}, from={}, size={}", userId, from, size);

        UserShortDto user = getUserOrThrow(userId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);
        Map<Long, Double> ratingsMap = getRatingsMap(eventIds);

        return events.stream()
                .map(eventMapper::toShortDto)
                .peek(shortDto -> {
                    shortDto.setInitiator(user);
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
                    shortDto.setRating(ratingsMap.getOrDefault(shortDto.getId(), 0.0));
                })
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto addEventPrivate(Long userId, NewEventDto dto) {
        log.info("Adding event for user id={}", userId);

        UserShortDto user = getUserOrThrow(userId);

        if (dto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }

        Category category = categoryRepository.findById(dto.category())
                .orElseThrow(() -> new NotFoundException("Category with id= " + dto.category() + " was not found"));

        Event event = eventMapper.toEvent(dto);

        event.setCategory(category);
        event.setInitiatorId(userId);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event saved = eventRepository.save(event);
        log.info("Event created successfully: id={}", saved.getId());

        EventFullDto fullDto = eventMapper.toFullDto(saved);
        fullDto.setInitiator(user);
        fullDto.setConfirmedRequests(0L);
        return fullDto;
    }

    @Override
    public EventFullDto getEventByIdPrivate(Long userId, Long eventId, String url) {
        log.info("Getting event id={} for user id={}", eventId, userId);

        UserShortDto user = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Event does not belong to user");
        }

        EventFullDto fullDto = eventMapper.toFullDto(event);
        fullDto.setInitiator(user);

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(List.of(eventId));
        fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(eventId, 0L));
        Map<Long, Double> ratingsMap = getRatingsMap(List.of(eventId));
        fullDto.setRating(ratingsMap.getOrDefault(eventId, 0.0));

        return fullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventPrivate(Long userId, Long eventId, UpdateEventUserRequest dto) {
        log.info("Updating event id={} for user id={}", eventId, userId);

        UserShortDto user = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Event does not belong to user");
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (dto.eventDate() != null &&
                dto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }

        eventMapper.updateEventMap(dto, event);

        if (dto.stateAction() != null) {
            switch (dto.stateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }

        Event updated = eventRepository.save(event);
        log.info("Event updated successfully: id={}", updated.getId());

        EventFullDto fullDto = eventMapper.toFullDto(updated);
        fullDto.setInitiator(user);

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(List.of(eventId));
        fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(eventId, 0L));
        return fullDto;
    }

    @Override
    public List<EventShortDto> getEventsPublic(PublicEventParamDto eventParamDto, HttpServletRequest request) {
        if (eventParamDto.rangeStart() != null && eventParamDto.rangeEnd() != null &&
                eventParamDto.rangeStart().isAfter(eventParamDto.rangeEnd())) {
            throw new ValidationException("End date cannot be before start date");
        }

        QEvent event = QEvent.event;
        BooleanBuilder paramFilter = new BooleanBuilder();

        if (eventParamDto.text() != null && !eventParamDto.text().isBlank()) {
            paramFilter.and(event.annotation.containsIgnoreCase(eventParamDto.text())
                    .or(event.description.containsIgnoreCase(eventParamDto.text())));
        }

        if (eventParamDto.category() != null && !eventParamDto.category().isEmpty()) {
            paramFilter.and(event.category.id.in(eventParamDto.category()));
        }

        if (eventParamDto.paid() != null) {
            paramFilter.and(event.paid.eq(eventParamDto.paid()));
        }

        LocalDateTime start = eventParamDto.rangeStart() != null ? eventParamDto.rangeStart() : LocalDateTime.now();
        paramFilter.and(event.eventDate.goe(start));

        if (eventParamDto.rangeEnd() != null) {
            paramFilter.and(event.eventDate.loe(eventParamDto.rangeEnd()));
        }

        paramFilter.and(event.state.eq(EventState.PUBLISHED));

        Sort sortEventDate = Sort.unsorted();
        if (eventParamDto.sort() != null && eventParamDto.sort().equalsIgnoreCase(EVENT_DATE)) {
            sortEventDate = Sort.by("eventDate").ascending();
        }

        Pageable pageable = PageRequest.of(eventParamDto.from() / eventParamDto.size(),
                eventParamDto.size(), sortEventDate);

        List<Event> events = eventRepository.findAll(paramFilter, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);

        if (eventParamDto.onlyAvailable()) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0
                            || e.getParticipantLimit() > confirmedRequestsMap.getOrDefault(e.getId(), 0L))
                    .toList();
        }

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Double> ratingsMap = getRatingsMap(eventIds);

        List<EventShortDto> shortsDto = new ArrayList<>(events.stream()
                .map(eventMapper::toShortDto)
                .peek(shortDto -> {
                    shortDto.setInitiator(initiatorsMap.get(shortDto.getId()));
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
                    shortDto.setRating(ratingsMap.getOrDefault(shortDto.getId(), 0.0));
                })
                .toList());

        if (eventParamDto.sort() != null && eventParamDto.sort().equalsIgnoreCase(SORT_RATING)) {
            shortsDto.sort(Comparator.comparing(EventShortDto::getRating).reversed());
        }

        log.info("Получен список запросов по указанным фильтрам");

        return shortsDto;
    }

    @Override
    public EventFullDto getEventByIdPublic(Long id, Long userId) {
        Event event = getEventOrThrow(id);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие должно иметь статус: Опубликовано");
        }

        collectorClient.sendView(userId, id);

        EventFullDto fullDto = eventMapper.toFullDto(event);
        fullDto.setInitiator(getUserOrThrow(event.getInitiatorId()));

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(List.of(id));
        fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(id, 0L));

        Map<Long, Double> ratingsMap = getRatingsMap(List.of(id));
        fullDto.setRating(ratingsMap.getOrDefault(id, 0.0));

        log.info("Получено событие с id = {}", id);

        return fullDto;
    }

    @Override
    public List<EventFullDto> searchEventsAdmin(AdminEventSearchFilter filter) {
        log.info("Search events with filters: {}", filter);

        if (filter.rangeStart() != null && filter.rangeEnd() != null &&
                filter.rangeStart().isAfter(filter.rangeEnd())) {
            throw new ValidationException("rangeEnd не может быть раньше rangeStart");
        }

        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        if (filter.users() != null && !filter.users().isEmpty()) {
            predicate.and(event.initiatorId.in(filter.users()));
        }

        if (filter.states() != null && !filter.states().isEmpty()) {
            predicate.and(event.state.in(filter.states()));
        }

        if (filter.categories() != null && !filter.categories().isEmpty()) {
            predicate.and(event.category.id.in(filter.categories()));
        }

        if (filter.rangeStart() != null) {
            predicate.and(event.eventDate.goe(filter.rangeStart()));
        }

        if (filter.rangeEnd() != null) {
            predicate.and(event.eventDate.loe(filter.rangeEnd()));
        }

        Pageable pageable = PageRequest.of(filter.from() / filter.size(), filter.size());

        List<Event> events = eventRepository.findAll(predicate, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);
        Map<Long, Double> ratingsMap = getRatingsMap(eventIds);

        return events.stream()
                .map(eventMapper::toFullDto)
                .peek(fullDto -> {
                    fullDto.setInitiator(initiatorsMap.get(fullDto.getId()));
                    fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(fullDto.getId(), 0L));
                    fullDto.setRating(ratingsMap.getOrDefault(fullDto.getId(), 0.0));
                })
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto) {
        log.info("Обновление события с ID: {}", eventId);

        Event event = existsEvent(eventId);

        if (dto.eventDate() != null && dto.eventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Дата события должна быть не раньше, чем через час");
        }

        updateField(dto.annotation(), event::setAnnotation);
        updateField(dto.description(), event::setDescription);
        updateField(dto.eventDate(), event::setEventDate);
        updateField(dto.paid(), event::setPaid);
        updateField(dto.participantLimit(), event::setParticipantLimit);
        updateField(dto.requestModeration(), event::setRequestModeration);
        updateField(dto.title(), event::setTitle);

        if (dto.location() != null) {
            event.setLocation(new Location(dto.location().getLat(), dto.location().getLon()));
        }

        if (dto.category() != null) {
            Category category = categoryRepository.findById(dto.category())
                    .orElseThrow(() -> new NotFoundException(
                            "Category with id= " + dto.category() + " was not found"));
            event.setCategory(category);
        }

        if (dto.stateAction() != null) {
            switch (dto.stateAction()) {
                case PUBLISH_EVENT -> {
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException(
                                "An event cannot be published unless it is in the required status (PENDING): "
                                        + event.getState());
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    log.info("Event с id={} успешно опубликовано", eventId);
                }
                case REJECT_EVENT -> {
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot publish the event because " +
                                "it's not in the right state: PUBLISHED");
                    }
                    event.setState(EventState.CANCELED);
                    log.info("Event с id={} отклонено", eventId);
                }
            }
        }

        Event updated = eventRepository.save(event);
        log.info("Event c id={} успешно обновлено", updated.getId());

        EventFullDto fullDto = eventMapper.toFullDto(updated);
        fullDto.setInitiator(getUserOrThrow(updated.getInitiatorId()));

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(List.of(eventId));
        fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(eventId, 0L));
        return fullDto;
    }

    private <T> void updateField(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    @Override
    public EventInfoDto getEventInfo(Long eventId) {
        Event event = existsEvent(eventId);
        return new EventInfoDto(
                event.getId(),
                event.getInitiatorId(),
                event.getState(),
                event.getParticipantLimit(),
                event.getRequestModeration()
        );
    }

    @Override
    public List<EventPreviewDto> getEventPreviews(List<Long> eventIds) {
        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);

        return events.stream()
                .map(event -> new EventPreviewDto(
                        event.getId(),
                        event.getAnnotation(),
                        event.getCategory(),
                        event.getEventDate(),
                        initiatorsMap.get(event.getId()),
                        event.getTitle()
                ))
                .toList();
    }

    @Override
    public Event existsEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private UserShortDto getUserOrThrow(Long userId) {
        List<UserShortDto> users = userClient.findAll(List.of(userId), 0, 1);
        if (users.isEmpty()) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        return users.get(0);
    }

    private Map<Long, UserShortDto> getInitiatorsMap(List<Event> events) {
        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();

        Map<Long, UserShortDto> usersById = userClient.findAll(initiatorIds, 0, initiatorIds.size()).stream()
                .collect(Collectors.toMap(UserShortDto::id, u -> u));

        return events.stream()
                .collect(Collectors.toMap(Event::getId, e -> usersById.get(e.getInitiatorId())));
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        return requestClient.getConfirmedCounts(eventIds).stream()
                .collect(Collectors.toMap(ConfirmedRequestCount::eventId, ConfirmedRequestCount::count));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    @Override
    public Map<Long, Double> getRatingsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        try {
            return analyzerClient.getInteractionsCount(eventIds)
                    .collect(Collectors.toMap(
                            RecommendedEventProto::getEventId,
                            RecommendedEventProto::getScore));
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги мероприятий: {}", e.getMessage());
            return Map.of();
        }
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, Integer maxResults) {
        log.info("Получение рекомендаций для пользователя id={}", userId);

        Map<Long, Double> scores = analyzerClient.getRecommendationsForUser(userId, maxResults)
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore));

        if (scores.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllById(scores.keySet());
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);

        List<EventShortDto> result = new ArrayList<>(events.stream()
                .map(eventMapper::toShortDto)
                .peek(shortDto -> {
                    shortDto.setInitiator(initiatorsMap.get(shortDto.getId()));
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
                    shortDto.setRating(scores.getOrDefault(shortDto.getId(), 0.0));
                })
                .toList());

        result.sort(Comparator.comparing(EventShortDto::getRating).reversed());
        return result;
    }

    @Override
    public void likeEvent(Long userId, Long eventId) {
        log.info("Пользователь id={} лайкает мероприятие id={}", userId, eventId);

        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event must be published");
        }

        boolean participated = requestClient.getUserRequests(userId).stream()
                .anyMatch(request -> eventId.equals(request.event())
                        && "CONFIRMED".equals(request.status()));

        if (!participated) {
            throw new ValidationException("Пользователь может лайкать только посещённые мероприятия");
        }

        collectorClient.sendLike(userId, eventId);
    }

}