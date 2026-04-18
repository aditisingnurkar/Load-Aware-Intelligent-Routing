package com.logistics.test;

import com.logistics.graph.LogisticsGraph;
import com.logistics.io.InputParser;
import com.logistics.model.*;
import com.logistics.simulation.ShipmentManager;
import com.logistics.simulation.SimulationEngine;

import java.util.List;
import java.util.Map;

public class LAIRTest {

    public static void main(String[] args) throws Exception {

        System.out.println("========== LAIR FULL SYSTEM TEST ==========\n");

        InputParser parser = new InputParser();
        parser.parse("sample_network.txt");

        System.out.println("[CHECK] Input Parsing");
        System.out.println("Hubs       : " + parser.getHubs().size());
        System.out.println("Edges      : " + parser.getEdges().size());
        System.out.println("Shipments  : " + parser.getShipments().size());

        if (parser.getDelayEvent() == null) {
            System.out.println("Delay Event: NONE");
            return;
        }

        System.out.println("Delay Event: " + parser.getDelayEvent().getHubId()
                + " (" + parser.getDelayEvent().getDelay() + ")\n");

        LogisticsGraph graph = new LogisticsGraph();
        ShipmentManager manager = new ShipmentManager();
        SimulationEngine engine = new SimulationEngine(graph, manager);

        // Phase 1
        engine.runPhase1(
                parser.getHubs(),
                parser.getEdges(),
                parser.getShipments()
        );

        System.out.println("[CHECK] Phase 1");
        for (String hubId : graph.getAllHubIds()) {
            System.out.println("Load at " + hubId + " = " + graph.getLoad(hubId));
        }

        // Phase 2
        engine.runPhase2(parser.getDelayEvent());

        System.out.println("\n[CHECK] Phase 2");
        System.out.println("Bottleneck: " + engine.getBottleneckId());

        // Phase 3
        engine.runPhase3(1.0, 0.5);

        System.out.println("\n[CHECK] Phase 3");

        List<RerouteResult> results = engine.getResults();

        for (RerouteResult r : results) {

            System.out.println("\nShipment: " + r.getId());
            System.out.println("Old: " + r.getOldCost());
            System.out.println("New: " + r.getNewCost());

            if (r.getPath().contains(engine.getBottleneckId())) {
                throw new RuntimeException(
                        "Reroute FAILED: still contains bottleneck"
                );
            }
        }

        // Shipment validation
        System.out.println("\n[CHECK] Shipment Status");

        for (Shipment s : manager.getAll()) {

            System.out.println(s.getId() + " → " + s.getStatus());

            if (s.getCurrentHub() != null &&
                    s.getCurrentHub().equals(s.getDestination()) &&
                    s.getStatus() != ShipmentStatus.DELIVERED) {

                throw new RuntimeException(
                        "Shipment not marked DELIVERED correctly: " + s.getId()
                );
            }
        }

        // Main paths
        System.out.println("\n[CHECK] Main Paths");

        for (Map.Entry<String, List<String>> e : engine.getMainPaths().entrySet()) {
            System.out.println(e.getKey() + " → " + String.join(" -> ", e.getValue()));
        }

        // Load map
        System.out.println("\n[FINAL LOAD MAP]");
        for (String hubId : graph.getAllHubIds()) {
            System.out.println(hubId + " → " + graph.getLoad(hubId));
        }

        System.out.println("\n========== TEST COMPLETE ==========");
    }
}