package com.logistics.model;

/**
 * Represents an entry in the priority queue for Dijkstra.
 * Stores the current best cost to reach a hub.
 */
public class HeapEntry implements Comparable<HeapEntry> {

    private final String hubId; // current node
    private final double cost;  // accumulated cost to reach this node

    public HeapEntry(String hubId, double cost) {
        this.hubId = hubId;
        this.cost = cost;
    }

    public String getHubId() {
        return hubId;
    }

    public double getCost() {
        return cost;
    }

    // Lower cost gets higher priority (min-heap behavior)
    @Override
    public int compareTo(HeapEntry other) {
        return Double.compare(this.cost, other.cost);
    }

    @Override
    public String toString() {
        return "HeapEntry{hub='" + hubId + "', cost=" + cost + "}";
    }
}