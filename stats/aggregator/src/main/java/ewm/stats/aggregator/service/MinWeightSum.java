package ewm.stats.aggregator.service;

import java.util.HashMap;
import java.util.Map;

public class MinWeightSum {
    HashMap<Long, Map<Long, Double>> minWeightSums = new HashMap<>();

    public void put(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightSums.computeIfAbsent(first, e -> new HashMap<>()).put(second, sum);
    }

    public double get(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return minWeightSums.computeIfAbsent(first, e -> new HashMap<>()).getOrDefault(second, 0.0);
    }
}
