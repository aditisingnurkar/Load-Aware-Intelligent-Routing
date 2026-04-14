package com.logistics.model;

public class Hub {
    private final String id;
    private final HubType type;


    public Hub(String id, HubType type) {
        this.id = id;
        this.type = type;
    }

    public String getId()  { return id; }
    public HubType getType() { return type; }

    @Override
    public String toString() {
        return "Hub{id='" + id + "', type=" + type + "}";
    }
}
