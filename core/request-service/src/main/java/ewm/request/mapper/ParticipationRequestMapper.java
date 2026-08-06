package ewm.request.mapper;

import ewm.request.dto.ParticipationRequestDto;
import ewm.request.model.ParticipationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {
    @Mapping(target = "requester", source = "requesterId")
    @Mapping(target = "event", source = "eventId")
    ParticipationRequestDto mapToRequestDto(ParticipationRequest request);
}
