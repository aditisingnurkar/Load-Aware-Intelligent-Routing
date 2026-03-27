package com.logistics.model;

public class Edge {
    private final String from;
    private final String to;
    private final int travelTime;

    public Edge(String from, String to, int travelTime) {
        this.from = from;
        this.to = to;
        this.travelTime = travelTime;
    }

    public String getFrom()   { return from; }
    public String getTo()     { return to; }
    public int getWeight()    { return travelTime; }

    @Override
    public String toString() {
        return "Edge{" + from + " -> " + to + ", time=" + travelTime + "}";
    }
}