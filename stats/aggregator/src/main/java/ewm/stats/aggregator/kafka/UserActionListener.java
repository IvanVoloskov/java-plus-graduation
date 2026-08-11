package ewm.stats.aggregator.kafka;

import ewm.stats.aggregator.service.EventSimilarityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener {

    private final EventSimilarityService similarityService;
    private final KafkaTemplate<Long, SpecificRecordBase> kafkaTemplate;

    @Value("${aggregator.kafka.topics.events-similarity}")
    private String similarityTopic;

    @KafkaListener(topics = "${aggregator.kafka.topics.user-actions}")
    public void handle(ConsumerRecord<Long, UserActionAvro> record) {
        UserActionAvro action = record.value();
        log.info("Запись: key={}, partition={}, offset={}, value={}",
                record.key(), record.partition(), record.offset(), action);

        if (action == null) {
            log.warn("Пустое значение, пропускаем");
            return;
        }

        log.info("Получено действие: userId={}, eventId={}, type={}",
                action.getUserId(), action.getEventId(), action.getActionType());

        List<EventSimilarityAvro> similarities = similarityService.process(action);
        for (EventSimilarityAvro similarity : similarities) {
            log.info("Сходство мероприятий {} и {} = {}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore());
            kafkaTemplate.send(similarityTopic, similarity.getEventA(), similarity);
        }
    }
}