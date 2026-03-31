package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Edge;
import com.logistics.model.HeapEntry;
import com.logistics.model.Shipment;

import java.util.*;

public class LoadAwareDijkstra {

    // Main reroute method — handles both cases
    public List<String> reroute(LogisticsGraph graph, Shipment shipment,
                                String isolatedHubId, double alpha, double beta) {

        int isolatedIndex = shipment.indexOf(isolatedHubId);
        List<String> originalPath = shipment.getPath();
        String destination = originalPath.get(originalPath.size() - 1);
        String source;

        if (shipment.getFailIndex() < isolatedIndex) {
            // Case 1 — hasn't reached isolated hub, reroute from origin
            source = originalPath.get(0);
        } else {
            // Case 2 — at or past isolated hub
            // source = hub just before the isolated one
            source = isolatedIndex > 0
                    ? originalPath.get(isolatedIndex - 1)
                    : originalPath.get(0);
        }

        if (graph.isIsolated(source) || graph.isIsolated(destination))
            return Collections.emptyList();

        List<String> newPath = dijkstra(graph, source, destination, alpha, beta);

        if (newPath.isEmpty()) return Collections.emptyList();

        if (source.equals(originalPath.get(0))) {
            // Case 1 — new path is the full path
            return newPath;
        } else {
            // Case 2 — prepend already-travelled portion up to (not including) source
            List<String> fullPath = new ArrayList<>(
                    originalPath.subList(0, isolatedIndex - 1)
            );
            fullPath.addAll(newPath);
            return fullPath;
        }
    }

    // Core Dijkstra — runs from src to dest on the current graph state
    public List<String> dijkstra(LogisticsGraph graph, String src, String dest,
                                 double alpha, double beta) {

        if (graph.isIsolated(src) || graph.isIsolated(dest))
            return Collections.emptyList();

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();

        for (String hubId : graph.getAllHubIds()) {
            dist.put(hubId, Double.MAX_VALUE);
        }
        dist.put(src, 0.0);
        pq.offer(new HeapEntry(src, 0.0));

        while (!pq.isEmpty()) {
            HeapEntry current = pq.poll();
            String currId = current.getHubId();

            if (visited.contains(currId)) continue;
            visited.add(currId);

            if (currId.equals(dest)) break;

            for (Edge edge : graph.getNeighbours(currId)) {
                String neighbour = edge.getTo();

                if (graph.isIsolated(neighbour)) continue;
                if (visited.contains(neighbour)) continue;

                double edgeCost = alpha * edge.getWeight()
                        + beta * graph.getLoad(neighbour);

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

    private List<String> reconstructPath(Map<String, String> prev,
                                         String src, String dest) {
        List<String> path = new ArrayList<>();
        String cur = dest;

        if (!prev.containsKey(dest) && !dest.equals(src))
            return Collections.emptyList();

        while (cur != null) {
            path.add(0, cur);
            cur = prev.get(cur);
        }

        if (path.isEmpty() || !path.get(0).equals(src))
            return Collections.emptyList();

        return path;
    }

    public PriorityQueue<HeapEntry> initHeap(String sourceId) {
        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();
        pq.offer(new HeapEntry(sourceId, 0.0));
        return pq;
    }

    public double computeCost(Edge edge, LogisticsGraph graph,
                              double alpha, double beta) {
        return alpha * edge.getWeight() + beta * graph.getLoad(edge.getTo());
    }

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
        return seen.size() > 1;
    }
}