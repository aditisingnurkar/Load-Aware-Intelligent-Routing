package com.logistics.model;

public class HeapEntry implements Comparable<HeapEntry> {
    private final String hubId;
    private final double cost;

    public HeapEntry(String hubId, double cost) {
        this.hubId = hubId;
        this.cost = cost;
    }

    public String getHubId() { return hubId; }
    public double getCost()  { return cost; }

    @Override
    public int compareTo(HeapEntry other) {
        return Double.compare(this.cost, other.cost); // min-heap: lower cost = higher priority
    }

    @Override
    public String toString() {
        return "HeapEntry{hub='" + hubId + "', cost=" + cost + "}";
    }
}