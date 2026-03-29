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

    // Adds a shipment after validating its path against the graph
    public void addShipment(Shipment shipment) {
        shipments.add(shipment);
    }

    // Validates that every consecutive hub pair in the path has a real edge in the graph
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

    // Called during Phase 1 — increments load for every hub in every shipment's path
    public void applyInitialLoads(LogisticsGraph graph) {
        for (Shipment shipment : shipments) {
            for (String hubId : shipment.getPath()) {
                graph.incrementLoad(hubId);
            }
        }
    }

    // Returns all shipments whose path contains the isolated hub
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

    // Decrements load for all hubs from failIndex onward — called before rerouting
    public void releaseLoads(LogisticsGraph graph, Shipment shipment) {
        List<String> path = shipment.getPath();
        int failIndex = shipment.getFailIndex();

        for (int i = failIndex; i < path.size(); i++) {
            graph.decrementLoad(path.get(i));
        }
    }

    // Increments load for the new rerouted segment — called after rerouting succeeds
    public void commitLoads(LogisticsGraph graph, Shipment shipment, List<String> newSegment) {
        for (String hubId : newSegment) {
            graph.incrementLoad(hubId);
        }
    }

    public List<Shipment> getAll() {
        return shipments;
    }
}
