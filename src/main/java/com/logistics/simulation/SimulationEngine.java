package com.logistics.simulation;

import com.logistics.algorithm.BFSDelayPropagator;
import com.logistics.algorithm.BottleneckDetector;
import com.logistics.algorithm.LoadAwareDijkstra;
import com.logistics.graph.LogisticsGraph;
import com.logistics.io.InputParser;
import com.logistics.io.OutputFormatter;
import com.logistics.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulationEngine {

    private final LogisticsGraph graph;
    private final ShipmentManager manager;
    private final List<RerouteResult> results;

    private DelayEvent delayEvent;
    private String bottleneckId;
    private double bottleneckScore;

    // alpha = travel weight, beta = load weight in Dijkstra cost formula
    private static final double ALPHA = 1.0;
    private static final double BETA  = 0.5;

    public SimulationEngine(LogisticsGraph graph, ShipmentManager manager) {
        this.graph   = graph;
        this.manager = manager;
        this.results = new ArrayList<>();
    }

    // ─── PHASE 1 ─────────────────────────────────────────────────────────────
    // Build graph from parsed data + apply initial loads from all shipment paths

    public void runPhase1(List<Hub> hubs, List<Edge> routes, List<Shipment> shipments) {
        // Register all hubs
        for (Hub hub : hubs) {
            graph.addHub(hub);
        }

        // Register all routes
        for (Edge edge : routes) {
            graph.addEdge(edge);
        }

        // Register all shipments in manager
        for (Shipment shipment : shipments) {
            manager.addShipment(shipment);
        }

        // Stamp initial loads onto graph from every shipment path
        manager.applyInitialLoads(graph);

        System.out.println("[Phase 1] Graph built: "
                + hubs.size() + " hubs, "
                + routes.size() + " routes, "
                + shipments.size() + " shipments loaded.");
    }

    // ─── PHASE 2 ─────────────────────────────────────────────────────────────
    // Propagate delay via BFS, detect bottleneck, isolate it

    public void runPhase2(DelayEvent event) {
        this.delayEvent = event;

        // BFS delay propagation
        Map<String, Integer> delayMap = new BFSDelayPropagator().propagate(graph, event);

        System.out.println("[Phase 2] Delay propagated from hub: " + event.getHubId()
                + " (initial delay=" + event.getDelay() + ")");

        // Detect bottleneck hub
        bottleneckId = new BottleneckDetector().detect(graph, delayMap, manager.getAll());

        // Use delay at bottleneck as its score for output
        bottleneckScore = delayMap.getOrDefault(bottleneckId, 0);

        System.out.println("[Phase 2] Bottleneck detected: " + bottleneckId
                + " (score=" + bottleneckScore + ")");

        // Isolate bottleneck — removes all edges to/from it
        graph.isolateHub(bottleneckId);

        System.out.println("[Phase 2] Hub isolated: " + bottleneckId);
    }

    // ─── PHASE 3 ─────────────────────────────────────────────────────────────
    // Reroute all shipments affected by the isolated bottleneck

    public void runPhase3(double alpha, double beta) {
        LoadAwareDijkstra dijkstra = new LoadAwareDijkstra();

        List<Shipment> affected = manager.getAffected(bottleneckId);

        System.out.println("[Phase 3] Rerouting " + affected.size()
                + " affected shipment(s) around: " + bottleneckId);

        for (Shipment shipment : affected) {
            String currentHub = shipment.getCurrentHub();
            String destination = shipment.getPath().get(shipment.getPath().size() - 1);

            // Compute old cost from currentHub to destination along existing path
            double oldCost = computePathCost(shipment.getPath(),
                    shipment.getFailIndex(), alpha, beta);

            // Release loads for the segment we're replacing
            manager.releaseLoads(graph, shipment);

            // Find new path from current position to destination
            List<String> newSegment = dijkstra.reroute(graph, currentHub, destination,
                    alpha, beta);

            if (newSegment.isEmpty()) {
                // No path found — mark shipment as failed
                shipment.setStatus(ShipmentStatus.FAILED);
                System.out.println("[Phase 3] FAILED to reroute: " + shipment.getId());
                continue;
            }

            // Commit new loads for the new segment
            manager.commitLoads(graph, shipment, newSegment);

            // Update shipment path
            shipment.updatePath(newSegment);

            double newCost = computePathCost(newSegment, 0, alpha, beta);

            results.add(new RerouteResult(shipment.getId(), newSegment, oldCost, newCost));

            System.out.println("[Phase 3] Rerouted " + shipment.getId()
                    + ": " + String.join(" -> ", newSegment)
                    + " (old=" + String.format("%.1f", oldCost)
                    + ", new=" + String.format("%.1f", newCost) + ")");
        }
    }

    // ─── RUN ─────────────────────────────────────────────────────────────────
    // Full pipeline: parse input → phase1 → phase2 → phase3 → print output

    public void run(String inputFile) throws IOException {
        OutputFormatter formatter = new OutputFormatter();
        formatter.printHeader();

        // Parse input file
        InputParser parser = new InputParser();
        parser.parse(inputFile);

        // Phase 1 — build graph + load shipments
        runPhase1(parser.getHubs(), parser.getEdges(), parser.getShipments());

        // Phase 2 — propagate delay + detect + isolate bottleneck
        if (parser.getDelayEvent() != null) {
            runPhase2(parser.getDelayEvent());
        } else {
            System.out.println("[Phase 2] No delay event found in input.");
            return;
        }

        // Phase 3 — reroute affected shipments
        runPhase3(ALPHA, BETA);

        // Output
        formatter.printBottleneck(bottleneckId, bottleneckScore);
        formatter.printRouteComparison(results);

        // Compute total delay before/after across all results
        double totalBefore = results.stream().mapToDouble(RerouteResult::getOldCost).sum();
        double totalAfter  = results.stream().mapToDouble(RerouteResult::getNewCost).sum();
        formatter.printDelayReduction(totalBefore, totalAfter);

        formatter.printLoadMap(graph);
    }

    // ─── RESULTS ─────────────────────────────────────────────────────────────

    public List<RerouteResult> getResults() {
        return results;
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    // Computes composite cost for a path segment starting at fromIndex
    private double computePathCost(List<String> path, int fromIndex,
                                   double alpha, double beta) {
        double cost = 0.0;
        for (int i = fromIndex; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to   = path.get(i + 1);

            // Find the edge travel time
            int travelTime = graph.getNeighbours(from).stream()
                    .filter(e -> e.getTo().equals(to))
                    .mapToInt(e -> e.getWeight())
                    .findFirst()
                    .orElse(0);

            cost += alpha * travelTime + beta * graph.getLoad(to);
        }
        return cost;
    }
}