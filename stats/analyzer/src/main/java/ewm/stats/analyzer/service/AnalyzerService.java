package ewm.stats.analyzer.service;

import ewm.stats.analyzer.model.EventSimilarity;
import ewm.stats.analyzer.model.EventSimilarityId;
import ewm.stats.analyzer.model.UserAction;
import ewm.stats.analyzer.model.UserActionId;
import ewm.stats.analyzer.repository.EventSimilarityRepository;
import ewm.stats.analyzer.repository.UserActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository similarityRepository;

    private static final int NEIGHBOURS_COUNT = 3;

    @Value("${analyzer.weights.view}")
    private double viewWeight;

    @Value("${analyzer.weights.register}")
    private double registerWeight;

    @Value("${analyzer.weights.like}")
    private double likeWeight;

    @Transactional
    public void saveUserAction(UserActionAvro avro) {
        UserActionId id = new UserActionId(avro.getUserId(), avro.getEventId());
        double newWeight = actionWeight(avro.getActionType());

        Optional<UserAction> existing = userActionRepository.findById(id);

        if (existing.isPresent()) {
            UserAction action = existing.get();
            // учитываем только максимальный вес
            if (newWeight <= action.getWeight()) {
                return;
            }
            action.setWeight(newWeight);
            action.setActionDate(avro.getTimestamp());
            userActionRepository.save(action);
        } else {
            userActionRepository.save(new UserAction(id, newWeight, avro.getTimestamp()));
        }
    }

    @Transactional
    public void saveEventSimilarity(EventSimilarityAvro avro) {
        EventSimilarityId id = new EventSimilarityId(avro.getEventA(), avro.getEventB());
        similarityRepository.save(new EventSimilarity(id, avro.getScore(), avro.getTimestamp()));
    }

    private double actionWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> viewWeight;
            case REGISTER -> registerWeight;
            case LIKE -> likeWeight;
        };
    }

    @Transactional(readOnly = true)
    public List<RecommendedEvent> getInteractionsCount(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return List.of();
        }

        List<RecommendedEvent> result = new ArrayList<>();
        for (Object[] row : userActionRepository.sumWeightsByEvents(eventIds)) {
            long eventId = ((Number) row[0]).longValue();
            double sum = ((Number) row[1]).doubleValue();
            result.add(new RecommendedEvent(eventId, sum));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        // 1. все пары, где участвует указанное мероприятие
        List<EventSimilarity> similarities = similarityRepository.findByEvent(eventId);

        // 2. мероприятия, с которыми пользователь уже взаимодействовал
        Set<Long> interacted = new HashSet<>(userActionRepository.findEventIdsByUser(userId));

        // 3. отбираем непросмотренные, сортируем по убыванию сходства, берём N
        List<RecommendedEvent> result = new ArrayList<>();
        for (EventSimilarity s : similarities) {
            long other = s.getId().getEventA() == eventId
                    ? s.getId().getEventB()
                    : s.getId().getEventA();

            if (interacted.contains(other)) {
                continue;
            }
            result.add(new RecommendedEvent(other, s.getScore()));
        }

        result.sort(Comparator.comparingDouble(RecommendedEvent::score).reversed());
        return result.size() > maxResults ? result.subList(0, maxResults) : result;
    }

    @Transactional(readOnly = true)
    public List<RecommendedEvent> getRecommendationsForUser(long userId, int maxResults) {
        // подбор мероприятий, с которыми пользователь ещё не взаимодействовал ---

        // недавние мероприятия пользователя, от новых к старым
        List<Long> recentEvents = userActionRepository.findRecentEventIds(
                userId, PageRequest.of(0, maxResults));

        if (recentEvents.isEmpty()) {
            return List.of();
        }

        Set<Long> interacted = new HashSet<>(userActionRepository.findEventIdsByUser(userId));

        // соседи недавних мероприятий, которых пользователь ещё не видел
        Map<Long, Double> candidates = new HashMap<>();
        for (EventSimilarity s : similarityRepository.findByEvents(recentEvents)) {
            long a = s.getId().getEventA();
            long b = s.getId().getEventB();

            long candidate;
            if (recentEvents.contains(a) && !interacted.contains(b)) {
                candidate = b;
            } else if (recentEvents.contains(b) && !interacted.contains(a)) {
                candidate = a;
            } else {
                continue;
            }

            // у кандидата может быть несколько связей — оставляем максимальную
            candidates.merge(candidate, s.getScore(), Math::max);
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        // топ N кандидатов по коэффициенту подобия
        List<Long> topCandidates = candidates.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();

        // вычисление оценки для каждого кандидата

        List<RecommendedEvent> result = new ArrayList<>();
        for (long candidate : topCandidates) {
            double score = predictScore(candidate, userId, interacted);
            result.add(new RecommendedEvent(candidate, score));
        }

        result.sort(Comparator.comparingDouble(RecommendedEvent::score).reversed());
        return result;
    }

    private double predictScore(long eventId, long userId, Set<Long> interacted) {
        // N ближайших соседей среди мероприятий, которые пользователь уже оценил
        List<EventSimilarity> neighbours = similarityRepository.findByEvent(eventId).stream()
                .filter(s -> {
                    long other = s.getId().getEventA() == eventId
                            ? s.getId().getEventB()
                            : s.getId().getEventA();
                    return interacted.contains(other);
                })
                .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
                .limit(NEIGHBOURS_COUNT)
                .toList();

        if (neighbours.isEmpty()) {
            return 0.0;
        }

        // оценки пользователя по этим мероприятиям
        List<Long> neighbourIds = neighbours.stream()
                .map(s -> s.getId().getEventA() == eventId
                        ? s.getId().getEventB()
                        : s.getId().getEventA())
                .toList();

        Map<Long, Double> userWeights = new HashMap<>();
        for (UserAction action : userActionRepository.findByUserAndEvents(userId, neighbourIds)) {
            userWeights.put(action.getId().getEventId(), action.getWeight());
        }

        // сумма взвешенных оценок и сумма коэффициентов
        double weightedSum = 0.0;
        double similaritySum = 0.0;
        for (EventSimilarity s : neighbours) {
            long other = s.getId().getEventA() == eventId
                    ? s.getId().getEventB()
                    : s.getId().getEventA();

            Double userWeight = userWeights.get(other);
            if (userWeight == null) {
                continue;
            }
            weightedSum += s.getScore() * userWeight;
            similaritySum += s.getScore();
        }

        return similaritySum == 0.0 ? 0.0 : weightedSum / similaritySum;
    }
}