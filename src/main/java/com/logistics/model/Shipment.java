package com.logistics.model;

import java.util.List;

public class Shipment {
    private final String id;
    private List<String> path;
    private int currentIndex;
    private ShipmentStatus status;

    public Shipment(String id, List<String> path) {
        this.id = id;
        this.path = path;
        this.currentIndex = 0;
        this.status = ShipmentStatus.ACTIVE;
    }

    public String getId()               { return id; }
    public List<String> getPath()       { return path; }
    public void setPath(List<String> path) { this.path = path; }
    public int getCurrentIndex()        { return currentIndex; }
    public void setCurrentIndex(int i)  { this.currentIndex = i; }
    public ShipmentStatus getStatus()   { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public String getCurrentHub() {
        return path.get(currentIndex);
    }

    public String getDestination() {
        return path.get(path.size() - 1);
    }

    @Override
    public String toString() {
        return "Shipment{id='" + id + "', status=" + status + ", path=" + path + ", index=" + currentIndex + "}";
    }
}
