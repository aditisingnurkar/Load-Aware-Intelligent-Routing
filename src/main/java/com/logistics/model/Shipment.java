package com.logistics.model;

import java.util.ArrayList;
import java.util.List;

public class Shipment {
    private final String id;
    private List<String> path;
    private int currentIndex;
    private ShipmentStatus status;

    public Shipment(String id, List<String> path) {
        this.id = id;
        this.path = new ArrayList<>(path);
        this.currentIndex = 0;
        this.status = ShipmentStatus.ACTIVE;
    }

    // Returns the hub the shipment is currently sitting at
    public String getCurrentHub() {
        if (currentIndex < path.size()) return path.get(currentIndex);
        return null;
    }

    // Moves shipment forward by one hub
    public void advance() {
        if (currentIndex < path.size() - 1) {
            currentIndex++;
        } else {
            this.status = ShipmentStatus.DELIVERED;
        }
    }

    public void setStatus(ShipmentStatus status) { this.status = status; }
    public ShipmentStatus getStatus()            { return status; }
    public List<String> getPath()                { return path; }
    public String getId()                        { return id; }
    public int getFailIndex()                    { return currentIndex; }

    // Keeps already-travelled hubs, replaces everything from currentIndex onward
    public void updatePath(List<String> newSegment) {
        List<String> updated = new ArrayList<>(path.subList(0, currentIndex));
        updated.addAll(newSegment);
        this.path = updated;
    }

    @Override
    public String toString() {
        return "Shipment{id='" + id + "', status=" + status +
                ", currentIndex=" + currentIndex + ", path=" + path + "}";
    }
}
