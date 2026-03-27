package com.logistics.model;

public class DelayEvent {
    private final String hubId;
    private final int initialDelay;

    public DelayEvent(String hubId, int initialDelay) {
        this.hubId = hubId;
        this.initialDelay = initialDelay;
    }

    public String getHubId()      { return hubId; }
    public int getInitialDelay()  { return initialDelay; }

    @Override
    public String toString() {
        return "DelayEvent{hub='" + hubId + "', delay=" + initialDelay + "}";
    }
}
