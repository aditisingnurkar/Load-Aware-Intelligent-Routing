package com.logistics.model;

public class DelayEvent {
    private final String hubId;
    private final int delay;

    public DelayEvent(String hubId, int delay) {
        this.hubId = hubId;
        this.delay = delay;
    }

    public String getHubId() { return hubId; }
    public int getDelay()    { return delay; }

    @Override
    public String toString() {
        return "DelayEvent{hubId='" + hubId + "', delay=" + delay + "}";
    }
}