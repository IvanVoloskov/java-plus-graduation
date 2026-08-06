package ewm.event.service;

import client.StatClient;
import com.querydsl.core.BooleanBuilder;
import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import ewm.category.model.Category;
import ewm.category.repository.CategoryRepository;
import ewm.client.RequestClient;
import ewm.client.UserClient;
import ewm.client.dto.ConfirmedRequestCount;
import ewm.client.dto.UserShortDto;
import ewm.event.dto.*;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CategoryRepository categoryRepository;
    private final StatClient statClient;
    private final UserClient userClient;
    private final RequestClient requestClient;

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
        Map<Long, Long> viewsMap = getViewsMap(events, false);

        return events.stream()
                .map(eventMapper::toShortDto)
                .peek(shortDto -> {
                    shortDto.setInitiator(user);
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
                    shortDto.setViews(viewsMap.getOrDefault(shortDto.getId(), 0L));
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

        LocalDateTime start = event.getPublishedOn() != null ? event.getPublishedOn() : event.getCreatedOn();
        ParamDto paramDto = new ParamDto(start, LocalDateTime.now(), List.of(url), false);

        EventFullDto fullDto = eventMapper.toFullDto(event);
        fullDto.setInitiator(user);

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(List.of(eventId));
        fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(eventId, 0L));
        fullDto.setViews(getViews(paramDto));

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

        saveHit(request);

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
        if (eventParamDto.sort() != null && eventParamDto.sort().equalsIgnoreCase("EVENT_DATE")) {
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
        Map<Long, Long> viewsMap = getViewsMap(events, true);

        List<EventShortDto> shortsDto = events.stream()
                .map(eventMapper::toShortDto)
                .peek(shortDto -> {
                    shortDto.setInitiator(initiatorsMap.get(shortDto.getId()));
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
                    shortDto.setViews(viewsMap.getOrDefault(shortDto.getId(), 0L));
                })
                .toList();

        if (eventParamDto.sort() != null && eventParamDto.sort().equalsIgnoreCase("VIEWS")) {
            shortsDto.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        log.info("Получен список запросов по указанным фильтрам");

        return shortsDto;
    }

    @Override
    public EventFullDto getEventByIdPublic(Long id, HttpServletRequest request) {
        saveHit(request);

        Event event = getEventOrThrow(id);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event must be published");
        }

        ParamDto paramDto = new ParamDto(event.getPublishedOn(),
                LocalDateTime.now(),
                List.of(request.getRequestURI()),
                true);

        EventFullDto fullDto = eventMapper.toFullDto(event);
        fullDto.setInitiator(getUserOrThrow(event.getInitiatorId()));

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(List.of(id));
        fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(id, 0L));
        fullDto.setViews(getViews(paramDto));

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
        Map<Long, Long> viewsMap = getViewsMap(events, false);

        return events.stream()
                .map(eventMapper::toFullDto)
                .peek(fullDto -> {
                    fullDto.setInitiator(initiatorsMap.get(fullDto.getId()));
                    fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(fullDto.getId(), 0L));
                    fullDto.setViews(viewsMap.getOrDefault(fullDto.getId(), 0L));
                })
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto) {
        log.info("Update event with ID: {}", eventId);

        Event event = existsEvent(eventId);

        if (dto.eventDate() != null && dto.eventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Дата события должна быть не раньше, чем через час");
        }

        if (dto.annotation() != null) {
            event.setAnnotation(dto.annotation());
        }
        if (dto.description() != null) {
            event.setDescription(dto.description());
        }
        if (dto.eventDate() != null) {
            event.setEventDate(dto.eventDate());
        }
        if (dto.paid() != null) {
            event.setPaid(dto.paid());
        }
        if (dto.participantLimit() != null) {
            event.setParticipantLimit(dto.participantLimit());
        }
        if (dto.requestModeration() != null) {
            event.setRequestModeration(dto.requestModeration());
        }
        if (dto.title() != null) {
            event.setTitle(dto.title());
        }
        if (dto.location() != null) {
            event.setLocation(new Location(dto.location().getLat(), dto.location().getLon()));
        }
        if (dto.category() != null) {
            Category category = categoryRepository.findById(dto.category())
                    .orElseThrow(() -> new NotFoundException("Category with id= " + dto.category() + " was not found"));
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

    private void saveHit(HttpServletRequest request) {
        HitDto hitDto = new HitDto(
                "ewm-main-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now());
        try {
            statClient.hit(hitDto);
            log.info("Статистика сохранена для URI: {}", request.getRequestURI());
        } catch (Exception e) {
            log.warn("Не удалось сохранить статистику: {}", e.getMessage());
        }
    }

    private Long getViews(ParamDto paramDto) {
        List<StatsDto> views = statClient.get(paramDto);
        return views.isEmpty() ? 0L : views.getFirst().hits();
    }

    public Map<Long, Long> getViewsMap(List<Event> events, boolean unique) {
        try {
            String url = "/events/";
            List<String> uris = events.stream()
                    .map(event -> url + event.getId())
                    .toList();
            LocalDateTime start = events.stream()
                    .map(Event::getPublishedOn)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            List<StatsDto> stats = statClient.get(new ParamDto(start, LocalDateTime.now(), uris, unique));

            return stats.stream()
                    .filter(statsDto -> {
                        String lastPart = statsDto.uri().substring(statsDto.uri().lastIndexOf("/") + 1);
                        try {
                            Long.parseLong(lastPart);
                            return true;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toMap(
                            statsDto -> Long.parseLong(statsDto.uri().substring(statsDto.uri().lastIndexOf("/") + 1)),
                            StatsDto::hits
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить статистику просмотров: {}", e.getMessage());
            return Map.of();
        }
    }
}