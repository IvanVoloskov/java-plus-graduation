package ewm.service;

import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import ewm.exception.ValidationException;
import ewm.mapper.EndpointHitMapper;
import ewm.model.EndpointHit;
import ewm.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsRepository statsRepository;
    private final EndpointHitMapper endpointHitMapper;

    public void createHit(HitDto hitDto) {
        EndpointHit hit = endpointHitMapper.mapToEndpointHit(hitDto);

        EndpointHit saveHit = statsRepository.save(hit);

        log.info("Сохранена запись о посещении - URI: {} (ID: {})", saveHit.getUri(), saveHit.getId());
    }

    public List<StatsDto> getStats(ParamDto paramDto) {
        if (paramDto.start().isAfter(paramDto.end())) {
            throw new ValidationException("Дата и время начала " + paramDto.start() +
                    " не должны быть позже даты и времени конца " + paramDto.end());
        }

        boolean isUnique = paramDto.unique() != null && paramDto.unique();

        if (paramDto.uris() == null || paramDto.uris().isEmpty()) {
            return isUnique
                    ? statsRepository.getStatsUnique(paramDto.start(), paramDto.end())
                    : statsRepository.getStats(paramDto.start(), paramDto.end());
        } else {
            return isUnique
                    ? statsRepository.getStatsByUriUnique(paramDto.start(), paramDto.end(), paramDto.uris())
                    : statsRepository.getStatsByUri(paramDto.start(), paramDto.end(), paramDto.uris());
        }
    }
}
