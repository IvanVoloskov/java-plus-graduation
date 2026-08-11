package ewm.stats.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EventSimilarityService {

    // eventId -> (userId -> максимальный вес)
    private final Map<Long, Map<Long, Double>> weights = new HashMap<>();

    // eventId -> сумма весов всех пользователей
    private final Map<Long, Double> eventSums = new HashMap<>();

    // пара мероприятий -> сумма минимальных весов
    private final MinWeightSum minSums = new MinWeightSum();

    @Value("${aggregator.weights.view}")
    private double viewWeight;

    @Value("${aggregator.weights.register}")
    private double registerWeight;

    @Value("${aggregator.weights.like}")
    private double likeWeight;

    public List<EventSimilarityAvro> process(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double newWeight = actionWeight(action.getActionType());

        Map<Long, Double> eventWeights = weights.computeIfAbsent(eventId, e -> new HashMap<>());
        double oldWeight = eventWeights.getOrDefault(userId, 0.0);

        // вес не вырос — пересчитывать нечего
        if (newWeight <= oldWeight) {
            return List.of();
        }

        eventWeights.put(userId, newWeight);
        eventSums.merge(eventId, newWeight - oldWeight, Double::sum);

        return recalculate(eventId, userId, oldWeight, newWeight, action.getTimestamp().toEpochMilli());
    }

    private List<EventSimilarityAvro> recalculate(long eventId, long userId,
                                                  double oldWeight, double newWeight,
                                                  long timestamp) {
        List<EventSimilarityAvro> result = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : weights.entrySet()) {
            long otherId = entry.getKey();
            if (otherId == eventId) {
                continue;
            }

            Double otherWeight = entry.getValue().get(userId);
            // если пользователь не взаимодействовал со вторым мероприятием — вклада нет
            if (otherWeight == null) {
                continue;
            }

            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double updatedSum = minSums.get(eventId, otherId) + (newMin - oldMin);
            minSums.put(eventId, otherId, updatedSum);

            double score = similarity(updatedSum, eventId, otherId);
            result.add(buildSimilarity(eventId, otherId, score, timestamp));
        }

        return result;
    }

    private double similarity(double minSum, long eventA, long eventB) {
        double sumA = eventSums.getOrDefault(eventA, 0.0);
        double sumB = eventSums.getOrDefault(eventB, 0.0);

        if (sumA == 0.0 || sumB == 0.0) {
            return 0.0;
        }
        return minSum / (Math.sqrt(sumA) * Math.sqrt(sumB));
    }

    private EventSimilarityAvro buildSimilarity(long eventA, long eventB, double score, long timestamp) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(Math.min(eventA, eventB))
                .setEventB(Math.max(eventA, eventB))
                .setScore(score)
                .setTimestamp(java.time.Instant.ofEpochMilli(timestamp))
                .build();
    }

    private double actionWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> viewWeight;
            case REGISTER -> registerWeight;
            case LIKE -> likeWeight;
        };
    }

}
