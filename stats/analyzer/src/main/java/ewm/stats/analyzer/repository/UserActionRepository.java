package ewm.stats.analyzer.repository;

import ewm.stats.analyzer.model.UserAction;
import ewm.stats.analyzer.model.UserActionId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, UserActionId> {

    // мероприятия пользователя, от новых к старым; ограничение через Pageable
    @Query("""
            select a.id.eventId from UserAction a
            where a.id.userId = :userId
            order by a.actionDate desc
            """)
    List<Long> findRecentEventIds(@Param("userId") long userId, Pageable pageable);

    // все мероприятия пользователя
    @Query("select a.id.eventId from UserAction a where a.id.userId = :userId")
    List<Long> findEventIdsByUser(@Param("userId") long userId);

    // веса пользователя по конкретным мероприятиям
    @Query("""
            select a from UserAction a
            where a.id.userId = :userId and a.id.eventId in :eventIds
            """)
    List<UserAction> findByUserAndEvents(@Param("userId") long userId,
                                         @Param("eventIds") Collection<Long> eventIds);

    // сумма весов всех пользователей по каждому мероприятию
    @Query("""
            select a.id.eventId, sum(a.weight) from UserAction a
            where a.id.eventId in :eventIds
            group by a.id.eventId
            """)
    List<Object[]> sumWeightsByEvents(@Param("eventIds") Collection<Long> eventIds);
}