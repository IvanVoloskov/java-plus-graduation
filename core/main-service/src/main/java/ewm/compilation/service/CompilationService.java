package ewm.compilation.service;

import ewm.compilation.dto.CompilationDto;
import ewm.compilation.dto.NewCompilationDto;
import ewm.compilation.dto.UpdateCompilationDto;

import java.util.List;

public interface CompilationService {

    CompilationDto create(NewCompilationDto newCompilationDto);

    void delete(Long compilationId);

    CompilationDto update(UpdateCompilationDto newCompilationDto, Long compilationId);

    List<CompilationDto> getCompilations(boolean pinned, Integer from, Integer size);

    CompilationDto getCompilation(Long compId);
}
