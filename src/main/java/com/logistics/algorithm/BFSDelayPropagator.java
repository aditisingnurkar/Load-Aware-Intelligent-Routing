package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.DelayEvent;
import com.logistics.model.Edge;

import java.util.*;

/**
 * Propagates delay through the network using BFS-style traversal.
 * Delay accumulates along edges and keeps the maximum impact.
 */
public class BFSDelayPropagator {

    private Map<String, Integer> delayMap;

    public BFSDelayPropagator() {
        this.delayMap = new HashMap<>();
    }

    // Run delay propagation starting from the event source
    public Map<String, Integer> propagate(LogisticsGraph graph, DelayEvent event) {
        delayMap.clear();
        propagateFromSource(graph, event.getHubId(), event.getDelay());
        return delayMap;
    }

    // BFS traversal with delay accumulation
    private void propagateFromSource(LogisticsGraph graph, String sourceId, int initialDelay) {
        Queue<String> queue = new ArrayDeque<>();

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

    // Update delay only if we found a worse (higher) delay
    private void relaxNeighbour(Queue<String> queue, String next,
                                int currentDelay, int travelTime) {

        int newDelay = currentDelay + travelTime;
        int existingDelay = delayMap.getOrDefault(next, -1);

        if (newDelay > existingDelay) {
            delayMap.put(next, newDelay);
            queue.offer(next);
        }
    }

    public Map<String, Integer> getDelayMap() {
        return Collections.unmodifiableMap(delayMap);
    }
}