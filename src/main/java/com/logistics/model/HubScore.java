package com.logistics.model;

public class HubScore implements Comparable<HubScore> {
    private final String hubId;
    private final double score;

    public HubScore(String hubId, double score) {
        this.hubId = hubId;
        this.score = score;
    }

    public String getHubId()  { return hubId; }
    public double getScore()  { return score; }

    @Override
    public int compareTo(HubScore other) {
        return Double.compare(other.score, this.score); // max-heap: higher score = higher priority
    }

    @Override
    public String toString() {
        return "HubScore{hub='" + hubId + "', score=" + score + "}";
    }
}