package com.logistics.model;

public class HubScore implements Comparable<HubScore> {
    private final String hubId;
    private final int score;

    public HubScore(String hubId, int score) {
        this.hubId = hubId;
        this.score = score;
    }

    public String getHubId() { return hubId; }
    public int getScore()    { return score; }

    @Override
    public int compareTo(HubScore o) {
        return Integer.compare(this.score, o.score);
    }
}