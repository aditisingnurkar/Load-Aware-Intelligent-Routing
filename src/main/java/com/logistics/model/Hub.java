package com.logistics.model;

/**
 * Represents a hub (node) in the logistics network.
 * Each hub has a unique id and a functional type.
 */
public class Hub {

    private final String id;       // unique hub identifier
    private final HubType type;   // type of hub (warehouse, sorting, etc.)

    public Hub(String id, HubType type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public HubType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Hub{id='" + id + "', type=" + type + "}";
    }
}