package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Edge;
import com.logistics.model.HubScore;
import com.logistics.model.Shipment;

import java.util.*;

public class BottleneckDetector {

    private String bottleneckId;
    private int bottleneckScore;

    public BottleneckDetector() {
        this.bottleneckId = null;
        this.bottleneckScore = -1;
    }

    public String detect(LogisticsGraph graph, Map<String, Integer> delayMap, List<Shipment> shipments) {
        PriorityQueue<HubScore> maxHeap = buildMaxHeap(graph, delayMap, shipments);

        if (!maxHeap.isEmpty()) {
            HubScore top = maxHeap.poll();
            bottleneckId = top.getHubId();
            bottleneckScore = top.getScore();
        }

        return bottleneckId;
    }

    public int scoreHub(String hubId, LogisticsGraph graph, Map<String, Integer> delayMap, List<Shipment> shipments) {
        int downstreamCount = countDownstream(graph, hubId, delayMap);

        int shipmentCount = 0;
        for (Shipment s : shipments) {
            if (s.getPath().contains(hubId)) shipmentCount++;
        }

        return downstreamCount * shipmentCount;
    }

    private int countDownstream(LogisticsGraph graph, String hubId, Map<String, Integer> delayMap) {
        int count = 0;
        Queue<String> queue = new LinkedList<>();
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

    public PriorityQueue<HubScore> buildMaxHeap(LogisticsGraph graph, Map<String, Integer> delayMap, List<Shipment> shipments) {
        PriorityQueue<HubScore> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

        for (String hubId : delayMap.keySet()) {
            if (graph.isIsolated(hubId)) continue;
            int score = scoreHub(hubId, graph, delayMap, shipments);
            maxHeap.offer(new HubScore(hubId, score));
        }

        return maxHeap;
    }

    public String getBottleneckId()  { return bottleneckId; }
    public int getBottleneckScore()  { return bottleneckScore; }
}