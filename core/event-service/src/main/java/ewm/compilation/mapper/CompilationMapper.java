package ewm.compilation.mapper;

import ewm.compilation.dto.CompilationDto;
import ewm.compilation.dto.NewCompilationDto;
import ewm.compilation.dto.UpdateCompilationDto;
import ewm.compilation.model.Compilation;
import ewm.event.mapper.EventMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {EventMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CompilationMapper {

    CompilationDto compilationToDto(Compilation compilation);

    @Mapping(target = "pinned", defaultExpression  = "java(false)")
    @Mapping(target = "events", ignore = true)
    Compilation postDtoToCompilation(NewCompilationDto newCompilationDto);

    @Mapping(target = "events", ignore = true)
    void updateDtoToCompilation(@MappingTarget Compilation compilation, UpdateCompilationDto dto);
}
