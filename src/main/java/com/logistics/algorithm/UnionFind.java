package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Edge;

import java.util.HashMap;
import java.util.Map;

/**
 * Union-Find structure to track connectivity between hubs.
 * Used to check if two hubs are still reachable after isolation.
 */
public class UnionFind {

    private final Map<String, String> parent;
    private final Map<String, Integer> rank;

    public UnionFind() {
        this.parent = new HashMap<>();
        this.rank = new HashMap<>();
    }

    // Build connected components from graph
    public void build(LogisticsGraph graph) {
        parent.clear();
        rank.clear();

        // Initialize each hub as its own parent
        for (String hubId : graph.getAllHubIds()) {
            parent.put(hubId, hubId);
            rank.put(hubId, 0);
        }

        // Union all non-isolated edges
        for (String hubId : graph.getAllHubIds()) {
            if (graph.isIsolated(hubId)) continue;

            for (Edge edge : graph.getNeighbours(hubId)) {
                if (!graph.isIsolated(edge.getTo())) {
                    union(hubId, edge.getTo());
                }
            }
        }
    }

    // Find root with path compression
    public String find(String hubId) {
        if (!parent.containsKey(hubId)) return null;

        if (!parent.get(hubId).equals(hubId)) {
            parent.put(hubId, find(parent.get(hubId)));
        }

        return parent.get(hubId);
    }

    // Union by rank
    public void union(String a, String b) {
        String rootA = find(a);
        String rootB = find(b);

        if (rootA == null || rootB == null || rootA.equals(rootB)) return;

        int rankA = rank.get(rootA);
        int rankB = rank.get(rootB);

        if (rankA < rankB) {
            parent.put(rootA, rootB);
        } else if (rankA > rankB) {
            parent.put(rootB, rootA);
        } else {
            parent.put(rootB, rootA);
            rank.put(rootA, rankA + 1);
        }
    }

    // Check if two hubs are in same component
    public boolean connected(String a, String b) {
        String rootA = find(a);
        String rootB = find(b);

        if (rootA == null || rootB == null) return false;

        return rootA.equals(rootB);
    }
}