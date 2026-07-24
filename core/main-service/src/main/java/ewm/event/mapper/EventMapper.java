package ewm.event.mapper;

import ewm.category.model.Category;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;
import ewm.event.dto.NewEventDto;
import ewm.event.dto.UpdateEventUserRequest;
import ewm.event.model.Event;
import ewm.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventShortDto toShortDto(Event event);

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventFullDto toFullDto(Event event);

    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "category", ignore = true)
    Event toEvent(NewEventDto dto);

    @Mapping(target = "category", source = "category", qualifiedByName = "category")
    void updateEventMap(UpdateEventUserRequest request, @MappingTarget Event event);

    @Named("category")
    default Category map(Long id) {
        if (id == null) return null;
        Category category = new Category();
        category.setId(id);
        return category;
    }
}