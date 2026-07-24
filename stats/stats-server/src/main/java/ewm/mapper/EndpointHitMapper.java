package ewm.mapper;

import ewm.HitDto;
import ewm.model.EndpointHit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EndpointHitMapper {
    EndpointHit mapToEndpointHit(HitDto hitDto);
}
