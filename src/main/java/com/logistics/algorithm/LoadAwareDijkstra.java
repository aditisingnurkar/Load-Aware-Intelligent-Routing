package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Edge;
import com.logistics.model.HeapEntry;
import com.logistics.model.Shipment;

import java.util.*;

/**
 * Load-aware Dijkstra implementation.
 * Cost = alpha * travelTime + beta * hub load.
 */
public class LoadAwareDijkstra {

    // Shortest path using load-aware cost
    public List<String> dijkstra(LogisticsGraph graph,
                                 String src,
                                 String dest,
                                 double alpha,
                                 double beta) {

        if (graph.isIsolated(src) || graph.isIsolated(dest)) {
            return Collections.emptyList();
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();

        for (String hub : graph.getAllHubIds()) {
            dist.put(hub, Double.MAX_VALUE);
        }

        dist.put(src, 0.0);
        pq.offer(new HeapEntry(src, 0.0));

        while (!pq.isEmpty()) {
            HeapEntry entry = pq.poll();
            String current = entry.getHubId();

            if (visited.contains(current)) continue;
            visited.add(current);

            if (current.equals(dest)) break;

            for (Edge edge : graph.getNeighbours(current)) {
                String next = edge.getTo();

                if (visited.contains(next) || graph.isIsolated(next)) continue;

                double cost = alpha * edge.getWeight()
                        + beta * graph.getLoad(next);

                double newDist = dist.get(current) + cost;

                if (newDist < dist.get(next)) {
                    dist.put(next, newDist);
                    prev.put(next, current);
                    pq.offer(new HeapEntry(next, newDist));
                }
            }
        }

        return reconstructPath(prev, src, dest);
    }

    // Main optimal path (origin → destination)
    public List<String> computeMainPath(LogisticsGraph graph,
                                        String origin,
                                        String destination,
                                        double alpha,
                                        double beta) {
        return dijkstra(graph, origin, destination, alpha, beta);
    }

    // Compute main paths for multiple destinations
    public Map<String, List<String>> computeAllMainPaths(
            LogisticsGraph graph,
            String origin,
            List<String> destinations,
            double alpha,
            double beta) {

        Map<String, List<String>> result = new HashMap<>();

        for (String dest : destinations) {
            List<String> path = dijkstra(graph, origin, dest, alpha, beta);
            if (!path.isEmpty()) {
                result.put(dest, path);
            }
        }

        return result;
    }

    // Find shortest path to any node in main path
    private List<String> findJoinPath(LogisticsGraph graph,
                                      String currentHub,
                                      List<String> mainPath,
                                      double alpha,
                                      double beta) {

        if (graph.isIsolated(currentHub)) {
            return Collections.emptyList();
        }

        Set<String> mainSet = new HashSet<>(mainPath);

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();

        for (String hub : graph.getAllHubIds()) {
            dist.put(hub, Double.MAX_VALUE);
        }

        dist.put(currentHub, 0.0);
        pq.offer(new HeapEntry(currentHub, 0.0));

        String joinNode = null;

        while (!pq.isEmpty()) {
            HeapEntry entry = pq.poll();
            String current = entry.getHubId();

            if (visited.contains(current)) continue;
            visited.add(current);

            // Stop when we hit main path (excluding start)
            if (mainSet.contains(current) && !current.equals(currentHub)) {
                joinNode = current;
                break;
            }

            for (Edge edge : graph.getNeighbours(current)) {
                String next = edge.getTo();

                if (visited.contains(next) || graph.isIsolated(next)) continue;

                double cost = alpha * edge.getWeight()
                        + beta * graph.getLoad(next);

                double newDist = dist.get(current) + cost;

                if (newDist < dist.get(next)) {
                    dist.put(next, newDist);
                    prev.put(next, current);
                    pq.offer(new HeapEntry(next, newDist));
                }
            }
        }

        if (joinNode == null) return Collections.emptyList();

        return reconstructPath(prev, currentHub, joinNode);
    }

    // Merge current position with main path
    public List<String> mergeShipmentToMainPath(
            LogisticsGraph graph,
            String currentHub,
            String destination,
            List<String> mainPath,
            double alpha,
            double beta) {

        // Case 1: already on main path
        int index = mainPath.indexOf(currentHub);
        if (index != -1) {
            return new ArrayList<>(mainPath.subList(index, mainPath.size()));
        }

        // Case 2: try joining main path
        List<String> joinPath = findJoinPath(graph, currentHub, mainPath, alpha, beta);

        if (!joinPath.isEmpty()) {
            String joinNode = joinPath.get(joinPath.size() - 1);
            int joinIndex = mainPath.indexOf(joinNode);

            List<String> result = new ArrayList<>(joinPath);
            result.addAll(mainPath.subList(joinIndex + 1, mainPath.size()));

            return result;
        }

        // Case 3: fallback direct route
        return computeMainPath(graph, currentHub, destination, alpha, beta);
    }

    // Reroute shipment after disruption
    public List<String> reroute(LogisticsGraph graph,
                                Shipment shipment,
                                double alpha,
                                double beta) {

        List<String> originalPath = shipment.getPath();
        String origin = originalPath.get(0);
        String destination = originalPath.get(originalPath.size() - 1);

        List<String> mainPath = computeMainPath(graph, origin, destination, alpha, beta);

        if (mainPath.isEmpty()) {
            return dijkstra(graph,
                    shipment.getCurrentHub(),
                    destination,
                    alpha,
                    beta);
        }

        return mergeShipmentToMainPath(
                graph,
                shipment.getCurrentHub(),
                destination,
                mainPath,
                alpha,
                beta
        );
    }

    // Reconstruct path from parent map
    private List<String> reconstructPath(Map<String, String> prev,
                                         String src,
                                         String dest) {

        List<String> path = new ArrayList<>();
        String current = dest;

        if (!prev.containsKey(dest) && !dest.equals(src)) {
            return Collections.emptyList();
        }

        while (current != null) {
            path.add(0, current);
            current = prev.get(current);
        }

        if (path.isEmpty() || !path.get(0).equals(src)) {
            return Collections.emptyList();
        }

        return path;
    }
}