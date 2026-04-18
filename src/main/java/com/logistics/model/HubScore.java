package com.logistics.model;

/**
 * Represents a hub along with its bottleneck score.
 * Used to rank hubs during bottleneck detection.
 */
public class HubScore implements Comparable<HubScore> {

    private final String hubId; // hub identifier
    private final int score;    // computed bottleneck score

    public HubScore(String hubId, int score) {
        this.hubId = hubId;
        this.score = score;
    }

    public String getHubId() {
        return hubId;
    }

    public int getScore() {
        return score;
    }

    // Natural ordering: lower score first (used with reverseOrder for max-heap)
    @Override
    public int compareTo(HubScore o) {
        return Integer.compare(this.score, o.score);
    }

    @Override
    public String toString() {
        return "HubScore{hub='" + hubId + "', score=" + score + "}";
    }
}