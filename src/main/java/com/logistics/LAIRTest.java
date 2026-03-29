package com.logistics;

import com.logistics.algorithm.BFSDelayPropagator;
import com.logistics.algorithm.BottleneckDetector;
import com.logistics.graph.LogisticsGraph;
import com.logistics.model.*;
import com.logistics.simulation.ShipmentManager;

import java.util.*;

public class LAIRTest {

    static LogisticsGraph g;
    static List<Shipment> shipments;
    static ShipmentManager sm;

    public static void main(String[] args) {
        setup(); testHubLoad();
        setup(); testNeighbours();
        setup(); testIsolation();
        setup(); testBFS();
        setup(); testBottleneck();
        setup(); testValidatePath();
        setup(); testInitialLoads();
        setup(); testAffectedShipments();
    }

    static void setup() {
        g = new LogisticsGraph();
        g.addHub(new Hub("W1", HubType.WAREHOUSE));
        g.addHub(new Hub("S1", HubType.SORTING));
        g.addHub(new Hub("S2", HubType.SORTING));
        g.addHub(new Hub("R1", HubType.REGIONAL));
        g.addHub(new Hub("R2", HubType.REGIONAL));
        g.addHub(new Hub("D1", HubType.DISTRIBUTION));
        g.addHub(new Hub("P1", HubType.DELIVERY));
        g.addHub(new Hub("P2", HubType.DELIVERY));
        g.addHub(new Hub("P3", HubType.DELIVERY));

        g.addEdge(new Edge("W1", "S1", 10));
        g.addEdge(new Edge("W1", "S2", 15));
        g.addEdge(new Edge("S1", "R1", 12));
        g.addEdge(new Edge("S1", "R2", 20));
        g.addEdge(new Edge("S2", "R1", 18));
        g.addEdge(new Edge("S2", "R2", 10));
        g.addEdge(new Edge("R1", "D1", 8));
        g.addEdge(new Edge("R2", "D1", 12));
        g.addEdge(new Edge("D1", "P1", 5));
        g.addEdge(new Edge("D1", "P2", 7));
        g.addEdge(new Edge("D1", "P3", 9));

        shipments = List.of(
                new Shipment("SH1", List.of("W1","S1","R1","D1","P1")),
                new Shipment("SH2", List.of("W1","S2","R2","D1","P2")),
                new Shipment("SH3", List.of("W1","S1","R2","D1","P3"))
        );

        sm = new ShipmentManager();
        sm.addShipment(new Shipment("SH1", List.of("W1","S1","R1","D1","P1")));
        sm.addShipment(new Shipment("SH2", List.of("W1","S2","R2","D1","P2")));
        sm.addShipment(new Shipment("SH3", List.of("W1","S1","R2","D1","P3")));
    }

    static void testHubLoad() {
        g.incrementLoad("R1");
        g.incrementLoad("R1");
        g.decrementLoad("R1");
        System.out.println("=== Hub Load ===");
        System.out.println("R1 load (expect 1): " + g.getLoad("R1"));
    }

    static void testNeighbours() {
        System.out.println("\n=== Neighbours ===");
        System.out.println("W1 neighbours (expect [S1, S2]): " +
                g.getNeighbours("W1").stream().map(Edge::getTo).toList());
        System.out.println("S1 neighbours (expect [R1, R2]): " +
                g.getNeighbours("S1").stream().map(Edge::getTo).toList());
        System.out.println("P1 neighbours (expect []):        " +
                g.getNeighbours("P1").stream().map(Edge::getTo).toList());
    }

    static void testIsolation() {
        System.out.println("\n=== Isolation ===");
        g.isolateHub("R1");
        System.out.println("R1 isolated (expect true):          " + g.isIsolated("R1"));
        System.out.println("R1 outgoing (expect 0):             " + g.getNeighbours("R1").size());
        System.out.println("S1 can reach R1 (expect false):     " +
                g.getNeighbours("S1").stream().anyMatch(e -> e.getTo().equals("R1")));
        System.out.println("S2 can reach R1 (expect false):     " +
                g.getNeighbours("S2").stream().anyMatch(e -> e.getTo().equals("R1")));
    }

    static void testBFS() {
        System.out.println("\n=== BFS Delay Propagation (delay at R1 = 30) ===");
        Map<String, Integer> delays = new BFSDelayPropagator()
                .propagate(g, new DelayEvent("R1", 30));
        System.out.println("R1 (expect 30): " + delays.getOrDefault("R1", 0));
        System.out.println("D1 (expect 38): " + delays.getOrDefault("D1", 0));
        System.out.println("P1 (expect 43): " + delays.getOrDefault("P1", 0));
        System.out.println("P2 (expect 45): " + delays.getOrDefault("P2", 0));
        System.out.println("P3 (expect 47): " + delays.getOrDefault("P3", 0));
        System.out.println("W1 (expect 0):  " + delays.getOrDefault("W1", 0));
        System.out.println("S1 (expect 0):  " + delays.getOrDefault("S1", 0));
        System.out.println("S2 (expect 0):  " + delays.getOrDefault("S2", 0));
        System.out.println("R2 (expect 0):  " + delays.getOrDefault("R2", 0));
    }

    static void testBottleneck() {
        System.out.println("\n=== Bottleneck Detection ===");
        Map<String, Integer> delays = new BFSDelayPropagator()
                .propagate(g, new DelayEvent("R1", 30));
        String bottleneck = new BottleneckDetector().detect(g, delays, shipments);
        System.out.println("Bottleneck (expect D1): " + bottleneck);
    }

    static void testValidatePath() {
        System.out.println("\n=== Path Validation ===");
        System.out.println("Valid W1->S1->R1->D1->P1 (expect true):  " +
                sm.validatePath(g, List.of("W1","S1","R1","D1","P1")));
        System.out.println("Invalid R1->W1 (expect false):            " +
                sm.validatePath(g, List.of("R1","W1")));
        System.out.println("Unknown hub (expect false):                " +
                sm.validatePath(g, List.of("W1","XX","D1")));
    }

    static void testInitialLoads() {
        System.out.println("\n=== Initial Loads ===");
        sm.applyInitialLoads(g);
        System.out.println("W1 (expect 3): " + g.getLoad("W1"));
        System.out.println("S1 (expect 2): " + g.getLoad("S1"));
        System.out.println("S2 (expect 1): " + g.getLoad("S2"));
        System.out.println("R1 (expect 1): " + g.getLoad("R1"));
        System.out.println("R2 (expect 2): " + g.getLoad("R2"));
        System.out.println("D1 (expect 3): " + g.getLoad("D1"));
        System.out.println("P1 (expect 1): " + g.getLoad("P1"));
        System.out.println("P2 (expect 1): " + g.getLoad("P2"));
        System.out.println("P3 (expect 1): " + g.getLoad("P3"));
    }

    static void testAffectedShipments() {
        System.out.println("\n=== Affected Shipments ===");
        List<Shipment> affectedR1 = sm.getAffected("R1");
        System.out.println("Affected by R1 (expect 1):    " + affectedR1.size());
        System.out.println("Affected ID (expect SH1):     " + affectedR1.get(0).getId());

        List<Shipment> affectedD1 = sm.getAffected("D1");
        System.out.println("Affected by D1 (expect 3):    " + affectedD1.size());

        List<Shipment> affectedS2 = sm.getAffected("S2");
        System.out.println("Affected by S2 (expect 1):    " + affectedS2.size());
        System.out.println("Affected ID (expect SH2):     " + affectedS2.get(0).getId());
    }
}
