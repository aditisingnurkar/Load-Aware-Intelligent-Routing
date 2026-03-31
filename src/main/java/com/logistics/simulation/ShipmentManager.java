package com.logistics.simulation;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Shipment;
import com.logistics.model.ShipmentStatus;

import java.util.ArrayList;
import java.util.List;

public class ShipmentManager {

    private final List<Shipment> shipments;

    public ShipmentManager() {
        this.shipments = new ArrayList<>();
    }

    public void addShipment(Shipment shipment) {
        shipments.add(shipment);
    }

    public boolean validatePath(LogisticsGraph graph, List<String> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to   = path.get(i + 1);
            boolean edgeExists = graph.getNeighbours(from)
                    .stream()
                    .anyMatch(e -> e.getTo().equals(to));
            if (!edgeExists) return false;
        }
        return true;
    }

    public void applyInitialLoads(LogisticsGraph graph) {
        for (Shipment shipment : shipments) {
            for (String hubId : shipment.getPath()) {
                graph.incrementLoad(hubId);
            }
        }
    }

    public List<Shipment> getAffected(String isolatedHubId) {
        List<Shipment> affected = new ArrayList<>();
        for (Shipment s : shipments) {
            if (s.getStatus() == ShipmentStatus.ACTIVE &&
                    s.getPath().contains(isolatedHubId)) {
                affected.add(s);
            }
        }
        return affected;
    }

    // returns index of isolated hub in shipment's path
    public int getIsolatedIndex(Shipment s, String isolatedHubId) {
        return s.indexOf(isolatedHubId);
    }

    // releases loads from a specific index onward
    public void releaseLoadsFrom(LogisticsGraph graph, Shipment shipment, int fromIndex) {
        List<String> path = shipment.getPath();
        for (int i = fromIndex; i < path.size(); i++) {
            graph.decrementLoad(path.get(i));
        }
    }

    // kept for compatibility — releases from failIndex onward
    public void releaseLoads(LogisticsGraph graph, Shipment shipment) {
        releaseLoadsFrom(graph, shipment, shipment.getFailIndex());
    }

    // commits loads for a full new path
    public void commitLoads(LogisticsGraph graph, Shipment shipment, List<String> newPath) {
        for (String hubId : newPath) {
            graph.incrementLoad(hubId);
        }
    }

    public List<Shipment> getAll() {
        return shipments;
    }
}