package com.logistics.simulation;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.Shipment;
import com.logistics.model.ShipmentStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages shipments and updates load on hubs.
 */
public class ShipmentManager {

    private final List<Shipment> shipments;

    public ShipmentManager() {
        this.shipments = new ArrayList<>();
    }

    public void addShipment(Shipment shipment) {
        shipments.add(shipment);
    }

    public List<Shipment> getAll() {
        return Collections.unmodifiableList(shipments);
    }

    // Initialize load for all valid hubs in shipment paths
    public void applyInitialLoads(LogisticsGraph graph) {
        for (Shipment shipment : shipments) {
            for (String hubId : shipment.getPath()) {
                if (!graph.hubExists(hubId)) {
                    System.out.println("[WARN] Unknown hub '" + hubId +
                            "' in shipment " + shipment.getId());
                    continue;
                }
                graph.incrementLoad(hubId);
            }
        }
    }

    // Get shipments affected by isolated hub
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

    // Release loads from current position to end of path
    public void releaseLoadsFromCurrent(LogisticsGraph graph, Shipment shipment) {
        String current = shipment.getCurrentHub();
        List<String> path = shipment.getPath();

        int start = path.indexOf(current);

        if (start < 0) {
            System.out.println("[WARN] Current hub '" + current +
                    "' not found in path for shipment " + shipment.getId());
            return;
        }

        for (int i = start; i < path.size(); i++) {
            String hubId = path.get(i);

            if (!graph.hubExists(hubId)) continue;

            graph.decrementLoad(hubId);
        }
    }

    // Add loads for new rerouted path
    public void commitLoads(LogisticsGraph graph, List<String> newPath) {
        for (String hubId : newPath) {
            if (graph.hubExists(hubId)) {
                graph.incrementLoad(hubId);
            }
        }
    }
}