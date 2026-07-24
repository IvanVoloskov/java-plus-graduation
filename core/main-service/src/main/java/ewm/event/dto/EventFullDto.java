package ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ewm.category.model.Category;
import ewm.event.model.Location;
import ewm.user.dto.UserShortDto;
import ewm.event.model.EventState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventFullDto {
        private Long id;
        private String annotation;
        private Category category;
        private Long confirmedRequests;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdOn;

        private String description;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime eventDate;

        private UserShortDto initiator;
        private Location location;
        private Boolean paid;
        private Integer participantLimit;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime publishedOn;

        private Boolean requestModeration;
        private EventState state;
        private String title;
        private Long views;
}
