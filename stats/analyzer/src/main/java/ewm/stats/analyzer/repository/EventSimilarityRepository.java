package ewm.stats.analyzer.repository;

import ewm.stats.analyzer.model.EventSimilarity;
import ewm.stats.analyzer.model.EventSimilarityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {

    // все пары, где участвует указанное мероприятие
    @Query("""
            select s from EventSimilarity s
            where s.id.eventA = :eventId or s.id.eventB = :eventId
            """)
    List<EventSimilarity> findByEvent(@Param("eventId") long eventId);

    // все пары, где участвует любое из указанных мероприятий
    @Query("""
            select s from EventSimilarity s
            where s.id.eventA in :eventIds or s.id.eventB in :eventIds
            """)
    List<EventSimilarity> findByEvents(@Param("eventIds") Collection<Long> eventIds);
}