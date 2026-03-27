package com.logistics.graph;

import com.logistics.model.*;

import java.util.*;

public class LogisticsGraph {

    // The map — stores all roads between hubs
    private final Map<String, List<Edge>> adjList = new HashMap<>();

    // Stores all hub objects
    private final Map<String, Hub> hubs = new HashMap<>();

    // Hubs that have been isolated (removed from routing)
    private final Set<String> isolatedSet = new HashSet<>();

    // All shipments in the system
    private final List<Shipment> shipments = new ArrayList<>();

    // The delay event (only one per simulation)
    private DelayEvent delayEvent;

    // ─────────────────────────────────────────
    // PERSON A's methods — structural
    // ─────────────────────────────────────────

    public void addHub(Hub hub) {
        hubs.put(hub.getId(), hub);
        adjList.putIfAbsent(hub.getId(), new ArrayList<>());
    }

    public void addEdge(Edge edge) {
        adjList.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>()).add(edge);
    }

    public void addShipment(Shipment shipment) {
        shipments.add(shipment);
    }

    public void setDelayEvent(DelayEvent event) {
        this.delayEvent = event;
    }

    public void isolateHub(String hubId) {
        isolatedSet.add(hubId);
        adjList.put(hubId, new ArrayList<>());
        for (List<Edge> edges : adjList.values()) {
            edges.removeIf(e -> e.getTo().equals(hubId));
        }
    }

    public List<Edge> getNeighbours(String hubId) {
        return adjList.getOrDefault(hubId, new ArrayList<>());
    }

    public boolean isIsolated(String hubId) {
        return isolatedSet.contains(hubId);
    }

    public Set<String> getIsolatedSet()       { return isolatedSet; }
    public Map<String, Hub> getHubs()         { return hubs; }
    public List<Shipment> getShipments()      { return shipments; }
    public DelayEvent getDelayEvent()         { return delayEvent; }
    public Map<String, List<Edge>> getAdjList() { return adjList; }

    // ─────────────────────────────────────────
    // PERSON B's methods — load management
    // ─────────────────────────────────────────

    // Add 1 shipment to a hub's load count
    public void incrementLoad(String hubId) {
        if (hubs.containsKey(hubId)) {
            hubs.get(hubId).incrementLoad();
        }
    }

    // Remove 1 shipment from a hub's load count
    public void decrementLoad(String hubId) {
        if (hubs.containsKey(hubId)) {
            hubs.get(hubId).decrementLoad();
        }
    }

    // Get how many shipments are currently at a hub
    public int getLoad(String hubId) {
        if (hubs.containsKey(hubId)) {
            return hubs.get(hubId).getLoad();
        }
        return 0;
    }

    // Get load map for all hubs (used by OutputFormatter)
    public Map<String, Integer> getLoadMap() {
        Map<String, Integer> loadMap = new HashMap<>();
        for (String id : hubs.keySet()) {
            loadMap.put(id, hubs.get(id).getLoad());
        }
        return loadMap;
    }
}
