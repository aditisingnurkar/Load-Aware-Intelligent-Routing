//package com.logistics;
//
//import com.logistics.algorithm.LoadAwareDijkstra;
//import com.logistics.graph.LogisticsGraph;
//import com.logistics.model.Shipment;
//import com.logistics.model.ShipmentStatus;
//import com.logistics.simulation.SimulationEngine;
//
//import java.util.Arrays;
//
//public class LAIRTest {
//
//    public static void main(String[] args) {
//
//        // ─── BUILD GRAPH ─────────────────────────────
//
//        LogisticsGraph graph = new LogisticsGraph();
//
//        graph.addHub("W1");
//        graph.addHub("S1");
//        graph.addHub("S2");
//        graph.addHub("R1");
//        graph.addHub("R2");
//        graph.addHub("D1");
//        graph.addHub("D2");
//        graph.addHub("P1");
//        graph.addHub("P2");
//
//        graph.addEdge("W1", "S1", 5);
//        graph.addEdge("W1", "S2", 6);
//        graph.addEdge("S1", "R1", 4);
//        graph.addEdge("S2", "R2", 3);
//        graph.addEdge("R1", "D1", 5);
//        graph.addEdge("R2", "D1", 6);
//        graph.addEdge("R1", "D2", 2);
//        graph.addEdge("D2", "P1", 3);
//        graph.addEdge("D1", "P2", 4);
//
//        // ─── CREATE SHIPMENTS (POSITION-AWARE) ─────────────────────────────
//
//        Shipment sh1 = new Shipment("SH1",
//                Arrays.asList("W1", "S1", "R1", "D1", "P1"));
//        sh1.setCurrentHub("W1");
//
//        Shipment sh2 = new Shipment("SH2",
//                Arrays.asList("W1", "S2", "R2", "D1", "P2"));
//        sh2.setCurrentHub("R2"); // off main path
//
//        Shipment sh3 = new Shipment("SH3",
//                Arrays.asList("W1", "S1", "R1", "D1", "P1"));
//        sh3.setCurrentHub("S1"); // mid-route
//
//        Shipment sh4 = new Shipment("SH4",
//                Arrays.asList("W1", "S1", "R1", "D1", "P1"));
//        sh4.setCurrentHub("D1"); // at bottleneck
//
//        Shipment sh5 = new Shipment("SH5",
//                Arrays.asList("W1", "S1", "R1", "D1", "P1"));
//        sh5.setCurrentHub("X1"); // disconnected node
//
//        // ─── SIMULATION ENGINE ─────────────────────────────
//
//        SimulationEngine engine = new SimulationEngine(graph);
//
//        engine.addShipment(sh1);
//        engine.addShipment(sh2);
//        engine.addShipment(sh3);
//        engine.addShipment(sh4);
//        engine.addShipment(sh5);
//
//        // ─── INITIAL LOAD ─────────────────────────────
//
//        engine.applyInitialLoads();
//
//        // ─── PHASE 1: DELAY ─────────────────────────────
//
//        engine.runPhase1("D1", 10);
//
//        // ─── PHASE 2: BOTTLENECK ─────────────────────────────
//
//        engine.runPhase2();
//
//        // ─── PHASE 3: REROUTE ─────────────────────────────
//
//        engine.runPhase3(1.0, 0.5);
//
//        // ─── VERIFY RESULTS ─────────────────────────────
//
//        System.out.println("\n--- FINAL STATES ---");
//
//        for (Shipment s : engine.getShipments()) {
//            System.out.println(s);
//        }
//    }
//}