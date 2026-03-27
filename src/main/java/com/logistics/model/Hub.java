package com.logistics.model;

public class Hub {
    private final String id;
    private final HubType type;
    private int load;

    public Hub(String id, HubType type) {
        this.id = id;
        this.type = type;
        this.load = 0;
    }

    public void incrementLoad() { this.load++; }
    public void decrementLoad() { if (this.load > 0) this.load--; }

    public int getLoad()   { return load; }
    public String getId()  { return id; }
    public HubType getType() { return type; }

    @Override
    public String toString() {
        return "Hub{id='" + id + "', type=" + type + ", load=" + load + "}";
    }
}
