package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.DelayEvent;
import com.logistics.model.Edge;

import java.util.*;

public class BFSDelayPropagator {

    private Map<String, Integer> delayMap;

    public BFSDelayPropagator() {
        this.delayMap = new HashMap<>();
    }

    // Master method — runs full BFS propagation and returns the delay map
    public Map<String, Integer> propagate(LogisticsGraph graph, DelayEvent event) {
        delayMap.clear();
        initQueue(graph, event.getHubId(), event.getDelay());
        return delayMap;
    }

    // Seeds the BFS queue with the source hub and fans out downstream
    private void initQueue(LogisticsGraph graph, String sourceId, int initialDelay) {
        Queue<String> queue = new LinkedList<>();

        delayMap.put(sourceId, initialDelay);
        queue.offer(sourceId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDelay = delayMap.get(current);

            for (Edge edge : graph.getNeighbours(current)) {
                String next = edge.getTo();

                // Skip isolated hubs
                if (graph.isIsolated(next)) continue;

                relaxNeighbour(queue, next, currentDelay, edge.getWeight());
            }
        }
    }

    // Max-delay rule: only update and re-queue if we found a worse delay
    public void relaxNeighbour(Queue<String> queue, String next, int currentDelay, int travelTime) {
        int newDelay = currentDelay + travelTime;
        int existingDelay = delayMap.getOrDefault(next, -1);

        if (newDelay > existingDelay) {
            delayMap.put(next, newDelay);
            queue.offer(next); // re-queue because delay increased
        }
    }

    public Map<String, Integer> getDelayMap() {
        return Collections.unmodifiableMap(delayMap);
    }
}