package ewm.stats.analyzer.kafka;

import ewm.stats.analyzer.service.AnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerListener {

    private final AnalyzerService analyzerService;

    @KafkaListener(topics = "${analyzer.kafka.topics.user-actions}")
    public void handleUserAction(ConsumerRecord<Long, SpecificRecordBase> record) {
        SpecificRecordBase value = record.value();
        if (!(value instanceof UserActionAvro action)) {
            log.warn("Неожиданный тип в топике действий: {}", value);
            return;
        }
        log.info("Действие: userId={}, eventId={}, type={}",
                action.getUserId(), action.getEventId(), action.getActionType());
        analyzerService.saveUserAction(action);
    }

    @KafkaListener(topics = "${analyzer.kafka.topics.events-similarity}")
    public void handleEventSimilarity(ConsumerRecord<Long, SpecificRecordBase> record) {
        SpecificRecordBase value = record.value();
        if (!(value instanceof EventSimilarityAvro similarity)) {
            log.warn("Неожиданный тип в топике сходств: {}", value);
            return;
        }
        log.info("Сходство: {} и {} = {}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore());
        analyzerService.saveEventSimilarity(similarity);
    }
}