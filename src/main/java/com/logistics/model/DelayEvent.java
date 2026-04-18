package com.logistics.model;

/**
 * Represents a delay occurring at a specific hub.
 * Used as the starting point for delay propagation.
 */
public class DelayEvent {

    private final String hubId; // hub where delay occurs
    private final int delay;    // delay value (time units)

    public DelayEvent(String hubId, int delay) {
        this.hubId = hubId;
        this.delay = delay;
    }

    public String getHubId() {
        return hubId;
    }

    public int getDelay() {
        return delay;
    }

    @Override
    public String toString() {
        return "DelayEvent{hubId='" + hubId + "', delay=" + delay + "}";
    }
}