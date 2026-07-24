package ewm.compilation.dto;

import ewm.event.dto.EventShortDto;

import java.util.List;

public record CompilationDto(
        List<EventShortDto> events,
        Long id,
        Boolean pinned,
        String title) {
}
