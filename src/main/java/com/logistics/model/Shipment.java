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

    public String getCurrentHub() {
        if (currentIndex < path.size()) return path.get(currentIndex);
        return null;
    }

    public void advance() {
        if (currentIndex < path.size() - 1) {
            currentIndex++;
        } else {
            this.status = ShipmentStatus.DELIVERED;
        }
    }

    // returns index of a hub in the path, -1 if not found
    public int indexOf(String hubId) {
        return path.indexOf(hubId);
    }

    // advances currentIndex to a specific position
    public void advanceTo(int index) {
        if (index >= 0 && index < path.size())
            this.currentIndex = index;
    }

    public void setStatus(ShipmentStatus status) { this.status = status; }
    public ShipmentStatus getStatus()            { return status; }
    public List<String> getPath()                { return path; }
    public String getId()                        { return id; }
    public int getFailIndex()                    { return currentIndex; }

    // replaces entire path with the new full path, resets index to 0
    public void updatePath(List<String> fullNewPath) {
        this.path = new ArrayList<>(fullNewPath);
        this.currentIndex = 0;
    }

    @Override
    public String toString() {
        return "Shipment{id='" + id + "', status=" + status +
                ", currentIndex=" + currentIndex + ", path=" + path + "}";
    }
}