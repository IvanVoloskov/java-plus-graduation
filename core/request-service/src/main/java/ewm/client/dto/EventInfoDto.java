package ewm.client.dto;

public record EventInfoDto(
        Long id,
        Long initiatorId,
        String state,
        Integer participantLimit,
        Boolean requestModeration
) {
}