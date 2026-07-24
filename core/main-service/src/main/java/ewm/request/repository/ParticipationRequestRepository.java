package ewm.request.repository;

import ewm.request.model.ConfirmedRequestCount;
import ewm.event.model.Event;
import ewm.request.model.ParticipationRequest;
import ewm.request.model.ParticipationStatus;
import ewm.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    // Список своих заявок на участия в событиях
    List<ParticipationRequest> findByRequester(User requester);

    // Проверяем наличие такого запроса
    boolean existsByRequesterAndEvent(User requester, Event event);

    // Количество заявок на событие
    Long countByEventAndStatus(Event event, ParticipationStatus status);

    // Количество заявок для событий
    @Query("""
            SELECT new ewm.request.model.ConfirmedRequestCount(r.event.id, COUNT(r.id))
            FROM ParticipationRequest AS r
            WHERE r.event.id IN :eventIds AND r.status = 'CONFIRMED'
            GROUP BY r.event.id
            """)
    List<ConfirmedRequestCount> findAllConfirmedRequests(List<Long> eventIds);

    List<ParticipationRequest> findByEvent(Event event);
}
