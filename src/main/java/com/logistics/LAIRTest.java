package com.logistics;

import com.logistics.algorithm.BFSDelayPropagator;
import com.logistics.algorithm.BottleneckDetector;
import com.logistics.algorithm.LoadAwareDijkstra;
import com.logistics.algorithm.UnionFind;
import com.logistics.graph.LogisticsGraph;
import com.logistics.model.*;
import com.logistics.simulation.ShipmentManager;
import com.logistics.simulation.SimulationEngine;

import java.util.*;

public class LAIRTest {

    static LogisticsGraph g;
    static List<Shipment> shipments;
    static ShipmentManager sm;

    public static void main(String[] args) throws Exception {
        setup(); testHubLoad();
        setup(); testNeighbours();
        setup(); testIsolation();
        setup(); testBFS();
        setup(); testBottleneck();
        setup(); testValidatePath();
        setup(); testInitialLoads();
        setup(); testAffectedShipments();
        setup(); testUnionFind();
        setup(); testDijkstraCase1();
        setup(); testDijkstraCase2();
        setup(); testFullPipeline();
    }

    static void setup() {
        g = new LogisticsGraph();
        g.addHub(new Hub("W1", HubType.WAREHOUSE));
        g.addHub(new Hub("S1", HubType.SORTING));
        g.addHub(new Hub("S2", HubType.SORTING));
        g.addHub(new Hub("R1", HubType.REGIONAL));
        g.addHub(new Hub("R2", HubType.REGIONAL));
        g.addHub(new Hub("D1", HubType.DISTRIBUTION));
        g.addHub(new Hub("D2", HubType.DISTRIBUTION));
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
        g.addEdge(new Edge("R1", "D2", 10));
        g.addEdge(new Edge("R2", "D2", 14));
        g.addEdge(new Edge("D1", "P1", 5));
        g.addEdge(new Edge("D1", "P2", 7));
        g.addEdge(new Edge("D1", "P3", 9));
        g.addEdge(new Edge("D2", "P1", 6));
        g.addEdge(new Edge("D2", "P2", 8));
        g.addEdge(new Edge("D2", "P3", 11));

        shipments = new ArrayList<>(List.of(
                new Shipment("SH1", List.of("W1","S1","R1","D1","P1")),
                new Shipment("SH2", List.of("W1","S2","R2","D1","P2")),
                new Shipment("SH3", List.of("W1","S1","R2","D1","P3"))
        ));

        sm = new ShipmentManager();
        sm.addShipment(new Shipment("SH1", List.of("W1","S1","R1","D1","P1")));
        sm.addShipment(new Shipment("SH2", List.of("W1","S2","R2","D1","P2")));
        sm.addShipment(new Shipment("SH3", List.of("W1","S1","R2","D1","P3")));
    }

    // ─── DAY 1-2 TESTS ───────────────────────────────────────────────────────

    static void testHubLoad() {
        g.incrementLoad("R1");
        g.incrementLoad("R1");
        g.decrementLoad("R1");
        System.out.println("=== Hub Load ===");
        System.out.println("R1 load (expect 1): " + g.getLoad("R1"));
    }

    static void testNeighbours() {
        System.out.println("\n=== Neighbours ===");
        System.out.println("W1 neighbours (expect [S1, S2]):   " +
                g.getNeighbours("W1").stream().map(Edge::getTo).toList());
        System.out.println("S1 neighbours (expect [R1, R2]):   " +
                g.getNeighbours("S1").stream().map(Edge::getTo).toList());
        System.out.println("R1 neighbours (expect [D1, D2]):   " +
                g.getNeighbours("R1").stream().map(Edge::getTo).toList());
        System.out.println("P1 neighbours (expect []):          " +
                g.getNeighbours("P1").stream().map(Edge::getTo).toList());
    }

    static void testIsolation() {
        System.out.println("\n=== Isolation ===");
        g.isolateHub("D1");
        System.out.println("D1 isolated (expect true):              " + g.isIsolated("D1"));
        System.out.println("D1 outgoing (expect 0):                 " + g.getNeighbours("D1").size());
        System.out.println("R1 can reach D1 (expect false):         " +
                g.getNeighbours("R1").stream().anyMatch(e -> e.getTo().equals("D1")));
        System.out.println("R2 can reach D1 (expect false):         " +
                g.getNeighbours("R2").stream().anyMatch(e -> e.getTo().equals("D1")));
        System.out.println("D2 still reachable from R1 (expect true): " +
                g.getNeighbours("R1").stream().anyMatch(e -> e.getTo().equals("D2")));
    }

    // ─── DAY 3 TESTS ─────────────────────────────────────────────────────────

    static void testBFS() {
        System.out.println("\n=== BFS Delay Propagation (delay at D1 = 20) ===");
        Map<String, Integer> delays = new BFSDelayPropagator()
                .propagate(g, new DelayEvent("D1", 20));
        System.out.println("D1 (expect 20): " + delays.getOrDefault("D1", 0));
        System.out.println("P1 (expect 25): " + delays.getOrDefault("P1", 0));
        System.out.println("P2 (expect 27): " + delays.getOrDefault("P2", 0));
        System.out.println("P3 (expect 29): " + delays.getOrDefault("P3", 0));
        System.out.println("W1 (expect 0):  " + delays.getOrDefault("W1", 0));
        System.out.println("R1 (expect 0):  " + delays.getOrDefault("R1", 0));
        System.out.println("D2 (expect 0):  " + delays.getOrDefault("D2", 0));
    }

    static void testBottleneck() {
        System.out.println("\n=== Bottleneck Detection (delay at D1 = 20) ===");
        Map<String, Integer> delays = new BFSDelayPropagator()
                .propagate(g, new DelayEvent("D1", 20));
        String bottleneck = new BottleneckDetector()
                .detect(g, delays, shipments);
        System.out.println("Bottleneck (expect D1): " + bottleneck);
    }

    static void testValidatePath() {
        System.out.println("\n=== Path Validation ===");
        System.out.println("Valid W1->S1->R1->D1->P1 (expect true):   " +
                sm.validatePath(g, List.of("W1","S1","R1","D1","P1")));
        System.out.println("Valid W1->S1->R1->D2->P1 (expect true):   " +
                sm.validatePath(g, List.of("W1","S1","R1","D2","P1")));
        System.out.println("Invalid D1->W1 (expect false):             " +
                sm.validatePath(g, List.of("D1","W1")));
        System.out.println("Unknown hub (expect false):                 " +
                sm.validatePath(g, List.of("W1","XX","P1")));
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
        System.out.println("D2 (expect 0): " + g.getLoad("D2"));
        System.out.println("P1 (expect 1): " + g.getLoad("P1"));
        System.out.println("P2 (expect 1): " + g.getLoad("P2"));
        System.out.println("P3 (expect 1): " + g.getLoad("P3"));
    }

    static void testAffectedShipments() {
        System.out.println("\n=== Affected Shipments ===");
        List<Shipment> affectedD1 = sm.getAffected("D1");
        System.out.println("Affected by D1 (expect 3):    " + affectedD1.size());

        List<Shipment> affectedR1 = sm.getAffected("R1");
        System.out.println("Affected by R1 (expect 1):    " + affectedR1.size());
        System.out.println("Affected ID (expect SH1):     " + affectedR1.get(0).getId());

        List<Shipment> affectedD2 = sm.getAffected("D2");
        System.out.println("Affected by D2 (expect 0):    " + affectedD2.size());
    }

    // ─── DAY 4 TESTS ─────────────────────────────────────────────────────────

    static void testUnionFind() {
        System.out.println("\n=== UnionFind ===");
        UnionFind uf = new UnionFind();
        uf.build(g);
        System.out.println("W1-P1 connected (expect true):  " + uf.connected("W1", "P1"));
        System.out.println("W1-D1 connected (expect true):  " + uf.connected("W1", "D1"));

        // isolate D1 and rebuild
        g.isolateHub("D1");
        uf.build(g);
        System.out.println("After D1 isolated:");
        System.out.println("W1-P1 connected via D2 (expect true):  " + uf.connected("W1", "P1"));
        System.out.println("W1-D1 connected (expect false):        " + uf.connected("W1", "D1"));
    }

    // Case 1 — shipment hasn't reached isolated hub, reroute from origin
    static void testDijkstraCase1() {
        System.out.println("\n=== Dijkstra Case 1 (reroute from origin) ===");
        g.isolateHub("D1");

        LoadAwareDijkstra dijkstra = new LoadAwareDijkstra();
        Shipment sh1 = new Shipment("SH1", List.of("W1","S1","R1","D1","P1"));
        // currentIndex = 0, isolatedIndex = 3 → 0 < 3 → Case 1

        List<String> newPath = dijkstra.reroute(g, sh1, "D1", 1.0, 0.5);
        System.out.println("New full path (expect W1->S1->R1->D2->P1): " + newPath);
        System.out.println("Starts at W1 (expect true):  " +
                (!newPath.isEmpty() && newPath.get(0).equals("W1")));
        System.out.println("Ends at P1 (expect true):    " +
                (!newPath.isEmpty() && newPath.get(newPath.size()-1).equals("P1")));
        System.out.println("Does not contain D1 (expect true): " +
                !newPath.contains("D1"));
    }

    // Case 2 — shipment is past isolated hub, emergency reroute from current position
    static void testDijkstraCase2() {
        System.out.println("\n=== Dijkstra Case 2 (emergency reroute from current pos) ===");
        g.isolateHub("D1");

        LoadAwareDijkstra dijkstra = new LoadAwareDijkstra();
        Shipment sh1 = new Shipment("SH1", List.of("W1","S1","R1","D1","P1"));
        // simulate shipment is currently at D1 (index 3)
        sh1.advanceTo(3);
        // currentIndex = 3, isolatedIndex = 3 → 3 >= 3 → Case 2

        List<String> newPath = dijkstra.reroute(g, sh1, "D1", 1.0, 0.5);
        System.out.println("New full path: " + newPath);
        System.out.println("Contains already-travelled hubs W1,S1,R1 (expect true): " +
                (newPath.contains("W1") && newPath.contains("S1") && newPath.contains("R1")));
        System.out.println("Ends at P1 (expect true):    " +
                (!newPath.isEmpty() && newPath.get(newPath.size()-1).equals("P1")));
        System.out.println("Does not contain D1 (expect true): " +
                !newPath.contains("D1"));
    }

    // ─── DAY 5 FULL PIPELINE TEST ─────────────────────────────────────────────

    static void testFullPipeline() throws Exception {
        System.out.println("\n=== Full Pipeline ===");
        LogisticsGraph pg = new LogisticsGraph();
        ShipmentManager pm = new ShipmentManager();
        SimulationEngine engine = new SimulationEngine(pg, pm);
        engine.run("input/sample_network.txt");
    }
}
