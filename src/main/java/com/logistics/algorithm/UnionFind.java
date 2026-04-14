package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Edge;

import java.util.HashMap;
import java.util.Map;

public class UnionFind {

    private final Map<String, String> parent;
    private final Map<String, Integer> rank;

    public UnionFind() {
        this.parent = new HashMap<>();
        this.rank = new HashMap<>();
    }

    public void build(LogisticsGraph graph) {
        parent.clear();
        rank.clear();

        // initialise every hub as its own component
        for (String hubId : graph.getAllHubIds()) {
            parent.put(hubId, hubId);
            rank.put(hubId, 0);
        }

        // union all edges — this is what was missing
        for (String hubId : graph.getAllHubIds()) {
            if (graph.isIsolated(hubId)) continue;
            for (Edge edge : graph.getNeighbours(hubId)) {
                if (!graph.isIsolated(edge.getTo())) {
                    union(hubId, edge.getTo());
                }
            }
        }
    }

    public String find(String hubId) {
        if (!parent.containsKey(hubId)) return null;
        if (!parent.get(hubId).equals(hubId)) {
            parent.put(hubId, find(parent.get(hubId)));
        }
        return parent.get(hubId);
    }

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

    public boolean connected(String a, String b) {
        String rootA = find(a);
        String rootB = find(b);
        if (rootA == null || rootB == null) return false;
        return rootA.equals(rootB);
    }
}