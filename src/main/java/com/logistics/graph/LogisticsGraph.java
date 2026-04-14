package com.logistics.graph;

import com.logistics.model.Edge;
import com.logistics.model.Hub;

import java.util.*;

public class LogisticsGraph {

    private final Map<String, Hub>        hubs;         // hubId -> Hub
    private final Map<String, List<Edge>> adjList;      // hubId -> outgoing edges
    private final Map<String, Integer>    hubLoadMap;   // hubId -> active shipment count
    private final Set<String>             isolatedSet;  // hubs removed from routing

    public LogisticsGraph() {
        this.hubs        = new HashMap<>();
        this.adjList     = new HashMap<>();
        this.hubLoadMap  = new HashMap<>();
        this.isolatedSet = new HashSet<>();
    }

    // ─── STRUCTURAL METHODS ───────────────────────────────────────────────────

    public void addHub(Hub hub) {
        hubs.put(hub.getId(), hub);
        adjList.putIfAbsent(hub.getId(), new ArrayList<>());
        hubLoadMap.putIfAbsent(hub.getId(), 0);
    }

    public void addEdge(Edge edge) {
        adjList.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>()).add(edge);
    }

    /**
     * FIX (Bug #1 / #3 support): Returns true only if the hub was registered
     * via addHub().  Hubs referenced in shipment paths that were never declared
     * in the HUB section return false, letting callers skip phantom entries
     * instead of silently inserting them into hubLoadMap.
     */
    public boolean hubExists(String hubId) {
        return hubs.containsKey(hubId);
    }

    // Removes all incoming and outgoing edges for a hub — called during isolation
    public void removeEdgesOf(String hubId) {
        adjList.put(hubId, new ArrayList<>());
        for (List<Edge> edges : adjList.values()) {
            edges.removeIf(e -> e.getTo().equals(hubId));
        }
    }

    public List<Edge> getNeighbours(String hubId) {
        return adjList.getOrDefault(hubId, new ArrayList<>());
    }

    // Isolates hub: removes all edges + adds to isolated set
    public void isolateHub(String hubId) {
        removeEdgesOf(hubId);
        isolatedSet.add(hubId);
    }

    public boolean isIsolated(String hubId) {
        return isolatedSet.contains(hubId);
    }

    public Set<String> getAllHubIds() {
        return hubs.keySet();
    }

    // ─── LOAD METHODS ─────────────────────────────────────────────────────────

    public void incrementLoad(String hubId) {
        hubLoadMap.merge(hubId, 1, Integer::sum);
    }

    public void decrementLoad(String hubId) {
        hubLoadMap.merge(hubId, -1, Integer::sum);
        // Guard against negative load
        if (hubLoadMap.get(hubId) < 0) hubLoadMap.put(hubId, 0);
    }

    public int getLoad(String hubId) {
        return hubLoadMap.getOrDefault(hubId, 0);
    }

    // ─── UTILITY ──────────────────────────────────────────────────────────────

    public Map<String, List<Edge>> getAdjList() {
        return adjList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LogisticsGraph:\n");
        for (String hubId : adjList.keySet()) {
            sb.append("  ").append(hubId)
                    .append(" -> ").append(adjList.get(hubId)).append("\n");
        }
        return sb.toString();
    }
}