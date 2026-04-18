package com.logistics.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a shipment moving through the network.
 * Keeps track of planned path and current position separately.
 */
public class Shipment {

    private final String id;
    private List<String> path;       // planned or rerouted path
    private String currentHub;       // actual current position
    private ShipmentStatus status;

    public Shipment(String id, List<String> path) {
        this.id = id;
        this.path = new ArrayList<>(path);
        this.currentHub = path.isEmpty() ? null : path.get(0);
        this.status = ShipmentStatus.ACTIVE;
    }

    // Current position of the shipment
    public String getCurrentHub() {
        return currentHub;
    }

    public void setCurrentHub(String hubId) {
        this.currentHub = hubId;
    }

    // Planned path (can change after rerouting)
    public List<String> getPath() {
        return path;
    }

    // Update route without changing current position
    public void updatePath(List<String> newPath) {
        this.path = new ArrayList<>(newPath);
    }

    public String getDestination() {
        if (path.isEmpty()) return null;
        return path.get(path.size() - 1);
    }

    public String getOrigin() {
        if (path.isEmpty()) return null;
        return path.get(0);
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Shipment{id='" + id +
                "', currentHub='" + currentHub +
                "', status=" + status +
                ", path=" + path + "}";
    }
}