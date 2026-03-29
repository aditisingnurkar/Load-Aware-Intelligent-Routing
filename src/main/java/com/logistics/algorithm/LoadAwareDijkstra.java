package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Edge;
import com.logistics.model.HeapEntry;

import java.util.*;

public class LoadAwareDijkstra {

    // Composite cost = alpha * travelTime + beta * load
    public List<String> reroute(LogisticsGraph graph, String src, String dest,
                                double alpha, double beta) {

        // Guard: isolated source or destination
        if (graph.isIsolated(src) || graph.isIsolated(dest)) return Collections.emptyList();

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();

        // Init all known hubs to infinity
        for (String hubId : graph.getAllHubIds()) {
            dist.put(hubId, Double.MAX_VALUE);
        }
        dist.put(src, 0.0);
        pq.offer(new HeapEntry(src, 0.0));

        while (!pq.isEmpty()) {
            HeapEntry current = pq.poll();
            String currId = current.getHubId();

            // Cycle guard: skip already-settled nodes
            if (visited.contains(currId)) continue;
            visited.add(currId);

            if (currId.equals(dest)) break;

            for (Edge edge : graph.getNeighbours(currId)) {
                String neighbour = edge.getTo();

                // Skip isolated neighbours
                if (graph.isIsolated(neighbour)) continue;

                // Cycle guard: skip already-settled
                if (visited.contains(neighbour)) continue;

                double edgeCost = alpha * edge.getWeight()
                        + beta  * graph.getLoad(neighbour);

                double newDist = dist.getOrDefault(currId, Double.MAX_VALUE) + edgeCost;

                if (newDist < dist.getOrDefault(neighbour, Double.MAX_VALUE)) {
                    dist.put(neighbour, newDist);
                    prev.put(neighbour, currId);
                    pq.offer(new HeapEntry(neighbour, newDist));
                }
            }
        }

        return reconstructPath(prev, src, dest);
    }

    private List<String> reconstructPath(Map<String, String> prev, String src, String dest) {
        List<String> path = new ArrayList<>();
        String cur = dest;

        // dest unreachable
        if (!prev.containsKey(dest) && !dest.equals(src)) return Collections.emptyList();

        while (cur != null) {
            path.add(0, cur);
            cur = prev.get(cur);
        }

        // Sanity check: path must start from src
        if (path.isEmpty() || !path.get(0).equals(src)) return Collections.emptyList();
        return path;
    }

    // Initialises a fresh heap with just the source (used by SimulationEngine if needed)
    public PriorityQueue<HeapEntry> initHeap(String sourceId) {
        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();
        pq.offer(new HeapEntry(sourceId, 0.0));
        return pq;
    }

    // Edge cost helper — exposed so SimulationEngine / tests can reuse formula
    public double computeCost(Edge edge, LogisticsGraph graph, double alpha, double beta) {
        return alpha * edge.getWeight() + beta * graph.getLoad(edge.getTo());
    }

    // BFS-style reachability check (used as guard before full Dijkstra)
    public boolean findNearestReachable(LogisticsGraph graph, String src) {
        if (graph.isIsolated(src)) return false;
        Queue<String> queue = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        queue.offer(src);
        seen.add(src);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (Edge e : graph.getNeighbours(cur)) {
                String nb = e.getTo();
                if (!graph.isIsolated(nb) && !seen.contains(nb)) {
                    seen.add(nb);
                    queue.offer(nb);
                }
            }
        }
        return seen.size() > 1; // reachable means at least one other hub exists
    }
}
