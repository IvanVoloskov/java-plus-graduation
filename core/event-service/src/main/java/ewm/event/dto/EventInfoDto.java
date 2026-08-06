package ewm.event.dto;

import ewm.event.model.EventState;

public record EventInfoDto(
        Long id,
        Long initiatorId,
        EventState state,
        Integer participantLimit,
        Boolean requestModeration
) {
}