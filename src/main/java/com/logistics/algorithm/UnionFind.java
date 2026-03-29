package com.logistics.algorithm;

import com.logistics.graph.LogisticsGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UnionFind {

    private final Map<String, String> parent;
    private final Map<String, Integer> rank;

    public UnionFind() {
        this.parent = new HashMap<>();
        this.rank = new HashMap<>();
    }

    // Builds UnionFind from all hub IDs in the graph
    public void build(LogisticsGraph graph) {
        parent.clear();
        rank.clear();
        Set<String> allHubIds = graph.getAllHubIds();
        for (String hubId : allHubIds) {
            parent.put(hubId, hubId);
            rank.put(hubId, 0);
        }
    }

    // Path-compressed find
    public String find(String hubId) {
        if (!parent.containsKey(hubId)) return null;
        if (!parent.get(hubId).equals(hubId)) {
            parent.put(hubId, find(parent.get(hubId))); // path compression
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

    public boolean connected(String a, String b) {
        String rootA = find(a);
        String rootB = find(b);
        if (rootA == null || rootB == null) return false;
        return rootA.equals(rootB);
    }
}
 