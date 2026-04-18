package com.logistics.model;

/**
 * Represents a directed route between two hubs.
 * Travel time is used as the edge weight in path calculations.
 */
public class Edge {

    private final String from;       // source hub
    private final String to;         // destination hub
    private final int travelTime;    // time to travel this route

    public Edge(String from, String to, int travelTime) {
        this.from = from;
        this.to = to;
        this.travelTime = travelTime;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    // Used as weight in Dijkstra
    public int getWeight() {
        return travelTime;
    }

    @Override
    public String toString() {
        return "Edge{" + from + " -> " + to + ", time=" + travelTime + "}";
    }
}