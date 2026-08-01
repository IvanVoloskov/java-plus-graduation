package ewm.user.dto;

import java.util.List;

public record AdminUserParam(
        List<Long> ids,
        Integer from,
        Integer size) {
}
