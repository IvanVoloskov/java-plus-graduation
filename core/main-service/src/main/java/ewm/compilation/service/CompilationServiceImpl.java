package ewm.compilation.service;

import ewm.compilation.dto.CompilationDto;
import ewm.compilation.dto.NewCompilationDto;
import ewm.compilation.dto.UpdateCompilationDto;
import ewm.compilation.mapper.CompilationMapper;
import ewm.compilation.model.Compilation;
import ewm.compilation.repository.CompilationRepository;
import ewm.event.model.Event;
import ewm.event.repository.EventRepository;
import ewm.event.service.EventService;
import ewm.exception.NotFoundException;
import ewm.request.model.ConfirmedRequestCount;
import ewm.request.repository.ParticipationRequestRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventService eventService;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        Compilation compilation = compilationMapper.postDtoToCompilation(newCompilationDto);

        if (newCompilationDto.events() != null && !newCompilationDto.events().isEmpty()) {
            List<Event> events = eventRepository.findAllById(newCompilationDto.events());
            compilation.setEvents(events);
        } else {
            compilation.setEvents(new ArrayList<>());
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Create new compilation {}", savedCompilation);

        CompilationDto dto = compilationMapper.compilationToDto(savedCompilation);
        saveViewsAndConfirmedRequests(List.of(dto), savedCompilation.getEvents());
        return dto;
    }

    @Override
    @Transactional
    public void delete(Long compilationId) {
        Compilation compilation = existsCompilation(compilationId);
        log.info("Delete compilation with id {}", compilationId);
        compilationRepository.delete(compilation);
    }

    @Override
    @Transactional
    public CompilationDto update(UpdateCompilationDto updCompilationDto, Long compilationId) {
        Compilation compilation = existsCompilation(compilationId);

        if (updCompilationDto.events() != null && !updCompilationDto.events().isEmpty()) {
            List<Event> events = eventRepository.findAllById(updCompilationDto.events());
            compilation.setEvents(events);
        }

        compilationMapper.updateDtoToCompilation(compilation, updCompilationDto);
        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Recreate updated compilation {}", savedCompilation);

        CompilationDto dto = compilationMapper.compilationToDto(savedCompilation);
        saveViewsAndConfirmedRequests(List.of(dto), savedCompilation.getEvents());
        return dto;
    }

    @Override
    public List<CompilationDto> getCompilations(boolean pinned, Integer from, Integer size) {
        log.info("Return compilation with pinned={}, from={}, size={}", pinned, from, size);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations = compilationRepository.findByPinned(pinned, pageable);

        if (compilations.isEmpty()) {
            return List.of();
        }

        List<Event> events = compilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream())
                .distinct()
                .toList();

        List<CompilationDto> dto = compilations.stream()
                .map(compilationMapper::compilationToDto)
                .collect(Collectors.toList());

        saveViewsAndConfirmedRequests(dto, events);
        return dto;
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        log.info("Looking for compilation with id={}", compId);

        Compilation compilation = existsCompilation(compId);
        CompilationDto dto = compilationMapper.compilationToDto(compilation);

        saveViewsAndConfirmedRequests(List.of(dto), compilation.getEvents());
        return dto;
    }

    private Compilation existsCompilation(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id=%d was not found", compId)));
    }

    private void saveViewsAndConfirmedRequests(List<CompilationDto> dto, List<Event> events) {
        if (dto.isEmpty() || events.isEmpty()) {
            return;
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequestsMap = requestRepository.findAllConfirmedRequests(eventIds).stream()
                .collect(Collectors.toMap(ConfirmedRequestCount::eventId, ConfirmedRequestCount::count));

        Map<Long, Long> viewsMap = eventService.getViewsMap(events, false);

        dto.forEach(comDto -> {
            if (comDto.events() != null) {
                comDto.events().forEach(shortDto -> {
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
                    shortDto.setViews(viewsMap.getOrDefault(shortDto.getId(), 0L));
                });
            }
        });
    }
}
