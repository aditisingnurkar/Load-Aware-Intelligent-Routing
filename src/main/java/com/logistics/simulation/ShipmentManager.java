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

    public List<Shipment> getAll() {
        return shipments;
    }

    // ─── INITIAL LOAD ─────────────────────────────

    /**
     * FIX (Bug #1): Only increment load for hubs that actually exist in the
     * graph's adjacency list. Phantom hubs (e.g. SH4's S3/R3) referenced in
     * shipment paths are skipped with a warning instead of creating ghost
     * entries in hubLoadMap that corrupt Dijkstra neighbour lookups.
     */
    public void applyInitialLoads(LogisticsGraph graph) {
        for (Shipment shipment : shipments) {
            for (String hubId : shipment.getPath()) {
                if (!graph.hubExists(hubId)) {
                    System.out.println("[WARN] applyInitialLoads: hub '"
                            + hubId + "' in shipment " + shipment.getId()
                            + " does not exist in graph — skipped.");
                    continue;
                }
                graph.incrementLoad(hubId);
            }
        }
    }

    // ─── AFFECTED SHIPMENTS ─────────────────────────────

    public List<Shipment> getAffected(String isolatedHubId) {
        List<Shipment> affected = new ArrayList<>();
        for (Shipment s : shipments) {
            if (s.getStatus() == ShipmentStatus.ACTIVE
                    && s.getPath().contains(isolatedHubId)) {
                affected.add(s);
            }
        }
        return affected;
    }

    // ─── LOAD HANDLING (POSITION-AWARE) ─────────────────────────────

    /**
     * FIX (Bug #3): The old code silently fell back to index 0 when
     * currentHub wasn't found in the path, releasing loads for already-
     * traversed hubs and potentially making load counts go negative.
     *
     * New behaviour:
     *   1. If currentHub is not in the path, log a clear warning and release
     *      NOTHING (the load accounting is already wrong; releasing from 0
     *      makes it worse).
     *   2. Also skip any hub in the release range that doesn't exist in the
     *      graph (guards against phantom hubs from Bug #1 leaking through).
     */
    public void releaseLoadsFromCurrent(LogisticsGraph graph, Shipment shipment) {
        String current = shipment.getCurrentHub();
        List<String> path = shipment.getPath();

        int start = path.indexOf(current);
        if (start < 0) {
            System.out.println("[WARN] releaseLoadsFromCurrent: currentHub '"
                    + current + "' not found in path for shipment "
                    + shipment.getId() + " — no loads released to avoid "
                    + "negative accounting.");
            return;   // ← was: start = 0  (the silent bug)
        }

        for (int i = start; i < path.size(); i++) {
            String hubId = path.get(i);
            if (!graph.hubExists(hubId)) {
                // Phantom hub — never had a load added, skip silently.
                continue;
            }
            graph.decrementLoad(hubId);
        }
    }

    public void commitLoads(LogisticsGraph graph, List<String> newPath) {
        for (String hubId : newPath) {
            graph.incrementLoad(hubId);
        }
    }
}