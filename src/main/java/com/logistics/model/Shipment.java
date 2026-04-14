package com.logistics.model;

import java.util.ArrayList;
import java.util.List;

public class Shipment {

    private final String id;
    private List<String> path;          // planned or rerouted path
    private String currentHub;          // REAL position (independent of path)
    private ShipmentStatus status;

    public Shipment(String id, List<String> path) {
        this.id = id;
        this.path = new ArrayList<>(path);
        this.currentHub = path.isEmpty() ? null : path.get(0);
        this.status = ShipmentStatus.ACTIVE;
    }

    // ─── CURRENT POSITION ─────────────────────────────

    public String getCurrentHub() {
        return currentHub;
    }

    public void setCurrentHub(String hubId) {
        this.currentHub = hubId;
    }

    // ─── PATH MANAGEMENT ─────────────────────────────

    public List<String> getPath() {
        return path;
    }

    public void updatePath(List<String> newPath) {
        this.path = new ArrayList<>(newPath);
        // DO NOT reset currentHub
    }

    public String getDestination() {
        if (path == null || path.isEmpty()) return null;
        return path.get(path.size() - 1);
    }

    public String getOrigin() {
        if (path == null || path.isEmpty()) return null;
        return path.get(0);
    }

    // ─── STATUS ─────────────────────────────

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