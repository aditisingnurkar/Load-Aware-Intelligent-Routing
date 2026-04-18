package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Edge;
import com.logistics.model.HubScore;
import com.logistics.model.Shipment;

import java.util.*;

/**
 * Identifies the most critical hub (bottleneck) after delay propagation.
 */
public class BottleneckDetector {

    private String bottleneckId;
    private int bottleneckScore;

    public BottleneckDetector() {
        this.bottleneckId = null;
        this.bottleneckScore = -1;
    }

    // Select the hub with highest impact score
    public String detect(LogisticsGraph graph,
                         Map<String, Integer> delayMap,
                         List<Shipment> shipments) {

        PriorityQueue<HubScore> maxHeap =
                buildMaxHeap(graph, delayMap, shipments);

        if (!maxHeap.isEmpty()) {
            HubScore top = maxHeap.poll();
            bottleneckId = top.getHubId();
            bottleneckScore = top.getScore();
        }

        return bottleneckId;
    }

    // Score based on downstream impact and affected shipments
    private int scoreHub(String hubId,
                         LogisticsGraph graph,
                         Map<String, Integer> delayMap,
                         List<Shipment> shipments) {

        int downstreamCount = countDownstream(graph, hubId, delayMap);

        int shipmentCount = 0;
        for (Shipment s : shipments) {
            List<String> path = s.getPath();

            int hubIndex = path.indexOf(hubId);
            int currentIndex = path.indexOf(s.getCurrentHub());

            // Count only if shipment has not passed this hub yet
            if (hubIndex != -1 && currentIndex != -1 && hubIndex >= currentIndex) {
                shipmentCount++;
            }
        }

        return downstreamCount * shipmentCount;
    }

    // Count how many downstream nodes are affected
    private int countDownstream(LogisticsGraph graph,
                                String hubId,
                                Map<String, Integer> delayMap) {

        int count = 0;
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        queue.add(hubId);
        visited.add(hubId);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            for (Edge edge : graph.getNeighbours(current)) {
                String next = edge.getTo();

                if (!visited.contains(next) && delayMap.containsKey(next)) {
                    visited.add(next);
                    queue.add(next);
                    count++;
                }
            }
        }
        return count;
    }

    // Build max-heap of candidate hubs
    private PriorityQueue<HubScore> buildMaxHeap(LogisticsGraph graph,
                                                 Map<String, Integer> delayMap,
                                                 List<Shipment> shipments) {

        PriorityQueue<HubScore> maxHeap =
                new PriorityQueue<>(Collections.reverseOrder());

        for (String hubId : delayMap.keySet()) {

            // Skip isolated hubs
            if (graph.isIsolated(hubId)) continue;

            // Skip sinks (e.g., delivery nodes)
            if (graph.getNeighbours(hubId).isEmpty()) continue;

            int score = scoreHub(hubId, graph, delayMap, shipments);
            maxHeap.offer(new HubScore(hubId, score));
        }

        return maxHeap;
    }

    public String getBottleneckId() {
        return bottleneckId;
    }

    public int getBottleneckScore() {
        return bottleneckScore;
    }
}