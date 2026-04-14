package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Edge;
import com.logistics.model.HeapEntry;
import com.logistics.model.Shipment;

import java.util.*;

public class LoadAwareDijkstra {

    // ─── CORE DIJKSTRA ───────────────────────────────────────────────────────
    // Runs from src to dest on current graph state.
    // Skips all isolated hubs automatically via graph.isIsolated().
    // Cost = alpha * travelTime + beta * hubLoad

    public List<String> dijkstra(LogisticsGraph graph, String src,
                                 String dest, double alpha, double beta) {

        if (graph.isIsolated(src) || graph.isIsolated(dest))
            return Collections.emptyList();

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited      = new HashSet<>();
        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();

        for (String hubId : graph.getAllHubIds())
            dist.put(hubId, Double.MAX_VALUE);

        dist.put(src, 0.0);
        pq.offer(new HeapEntry(src, 0.0));

        while (!pq.isEmpty()) {
            HeapEntry curr = pq.poll();
            String currId  = curr.getHubId();

            if (visited.contains(currId)) continue;
            visited.add(currId);

            if (currId.equals(dest)) break;

            for (Edge edge : graph.getNeighbours(currId)) {
                String nb = edge.getTo();
                if (graph.isIsolated(nb) || visited.contains(nb)) continue;

                double edgeCost = alpha * edge.getWeight()
                        + beta * graph.getLoad(nb);
                double newDist  = dist.get(currId) + edgeCost;

                if (newDist < dist.getOrDefault(nb, Double.MAX_VALUE)) {
                    dist.put(nb, newDist);
                    prev.put(nb, currId);
                    pq.offer(new HeapEntry(nb, newDist));
                }
            }
        }

        return reconstructPath(prev, src, dest);
    }

    // ─── COMPUTE MAIN PATH ───────────────────────────────────────────────────
    // Computes global optimal path: origin → destination
    // Independent of any shipment. Used as the reference path.

    public List<String> computeMainPath(LogisticsGraph graph, String origin,
                                        String destination,
                                        double alpha, double beta) {
        return dijkstra(graph, origin, destination, alpha, beta);
    }

    // ─── COMPUTE ALL MAIN PATHS ──────────────────────────────────────────────
    // Runs single-source Dijkstra from origin to all delivery hubs.
    // Returns Map<destinationId, path>

    public Map<String, List<String>> computeAllMainPaths(
            LogisticsGraph graph, String origin,
            List<String> destinations, double alpha, double beta) {

        Map<String, List<String>> mainPaths = new HashMap<>();
        for (String dest : destinations) {
            List<String> path = dijkstra(graph, origin, dest, alpha, beta);
            if (!path.isEmpty()) mainPaths.put(dest, path);
        }
        return mainPaths;
    }

    // ─── FIND JOIN PATH ──────────────────────────────────────────────────────
    // Emergency reroute: find shortest path from currentHub to any node
    // in MAIN_PATH. Returns the sub-path from currentHub to the join node.
    // If no join node reachable, returns empty list.

    public List<String> findJoinPath(LogisticsGraph graph, String currentHub,
                                     List<String> mainPath,
                                     double alpha, double beta) {

        if (graph.isIsolated(currentHub)) return Collections.emptyList();

        Set<String> mainPathSet = new HashSet<>(mainPath);

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited      = new HashSet<>();
        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();

        for (String hubId : graph.getAllHubIds())
            dist.put(hubId, Double.MAX_VALUE);

        dist.put(currentHub, 0.0);
        pq.offer(new HeapEntry(currentHub, 0.0));

        String joinNode = null;

        while (!pq.isEmpty()) {
            HeapEntry curr = pq.poll();
            String currId  = curr.getHubId();

            if (visited.contains(currId)) continue;
            visited.add(currId);

            // stop as soon as we hit any node in MAIN_PATH
            // (other than the current hub itself)
            if (mainPathSet.contains(currId) && !currId.equals(currentHub)) {
                joinNode = currId;
                break;
            }

            for (Edge edge : graph.getNeighbours(currId)) {
                String nb = edge.getTo();
                if (graph.isIsolated(nb) || visited.contains(nb)) continue;

                double edgeCost = alpha * edge.getWeight()
                        + beta * graph.getLoad(nb);
                double newDist  = dist.get(currId) + edgeCost;

                if (newDist < dist.getOrDefault(nb, Double.MAX_VALUE)) {
                    dist.put(nb, newDist);
                    prev.put(nb, currId);
                    pq.offer(new HeapEntry(nb, newDist));
                }
            }
        }

        if (joinNode == null) return Collections.emptyList();
        return reconstructPath(prev, currentHub, joinNode);
    }

    // ─── MERGE SHIPMENT TO MAIN PATH ─────────────────────────────────────────
    // Position-aware routing. Decision based on currentHub only.
    //
    // Case A: currentHub is in MAIN_PATH
    //   → continue along MAIN_PATH from that point
    //
    // Case B: currentHub is NOT in MAIN_PATH
    //   → find join path from currentHub to any node in MAIN_PATH
    //   → finalPath = joinPath + MAIN_PATH from joinNode
    //   → if no join found, try direct dijkstra to destination
    //   → if still fails, return empty (FAILED)

    public List<String> mergeShipmentToMainPath(
            LogisticsGraph graph,
            String currentHub,
            String destination,
            List<String> mainPath,
            double alpha,
            double beta
    ) {

        // Case 1: already on main path
        int idx = mainPath.indexOf(currentHub);
        if (idx != -1) {
            return new ArrayList<>(mainPath.subList(idx, mainPath.size()));
        }

        // Case 2: try joining main path
        List<String> joinPath = findJoinPath(graph, currentHub, mainPath, alpha, beta);

        if (!joinPath.isEmpty()) {
            String joinNode = joinPath.get(joinPath.size() - 1);

            int joinIdx = mainPath.indexOf(joinNode);

            List<String> finalPath = new ArrayList<>(joinPath);
            finalPath.addAll(mainPath.subList(joinIdx + 1, mainPath.size()));

            return finalPath;
        }

        // Case 3: direct reroute to destination
        return computeMainPath(graph, currentHub, destination, alpha, beta);
    }

    // ─── REROUTE (kept for compatibility with tests) ──────────────────────────
    // Used by existing tests. Internally delegates to mergeShipmentToMainPath
    // if a main path is available, else falls back to direct dijkstra.

    public List<String> reroute(LogisticsGraph graph, Shipment shipment,
                                String isolatedHubId,
                                double alpha, double beta) {

        List<String> originalPath = shipment.getPath();
        String destination = originalPath.get(originalPath.size() - 1);
        String origin      = originalPath.get(0);

        // compute main path (after isolation)
        List<String> mainPath = computeMainPath(
                graph, origin, destination, alpha, beta);

        // if no main path, fallback to direct routing
        if (mainPath.isEmpty()) {
            return dijkstra(graph,
                    shipment.getCurrentHub(),
                    destination,
                    alpha,
                    beta);
        }

        // merge based on current position
        return mergeShipmentToMainPath(
                graph,
                shipment.getCurrentHub(),
                destination,
                mainPath,
                alpha,
                beta
        );
    }

    // ─── PATH RECONSTRUCTION ─────────────────────────────────────────────────

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

    // ─── COST HELPER ─────────────────────────────────────────────────────────

    public double computeCost(Edge edge, LogisticsGraph graph,
                              double alpha, double beta) {
        return alpha * edge.getWeight() + beta * graph.getLoad(edge.getTo());
    }
}