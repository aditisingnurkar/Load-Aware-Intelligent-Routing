package com.logistics.simulation;

import com.logistics.algorithm.BFSDelayPropagator;
import com.logistics.algorithm.BottleneckDetector;
import com.logistics.algorithm.LoadAwareDijkstra;
import com.logistics.algorithm.UnionFind;
import com.logistics.graph.LogisticsGraph;
import com.logistics.io.InputParser;
import com.logistics.io.OutputFormatter;
import com.logistics.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationEngine {

    private final LogisticsGraph graph;
    private final ShipmentManager manager;
    private final List<RerouteResult> results;
    private final Map<String, Double> originalCosts = new HashMap<>();

    private DelayEvent delayEvent;
    private String bottleneckId;
    private int bottleneckScore;

    private static final double ALPHA = 1.0;
    private static final double BETA  = 0.5;

    public SimulationEngine(LogisticsGraph graph, ShipmentManager manager) {
        this.graph   = graph;
        this.manager = manager;
        this.results = new ArrayList<>();
    }

    // Phase 1 — build graph + apply initial loads
    public void runPhase1(List<Hub> hubs, List<Edge> routes, List<Shipment> shipments) {
        for (Hub hub : hubs)         graph.addHub(hub);
        for (Edge edge : routes)     graph.addEdge(edge);
        for (Shipment s : shipments) manager.addShipment(s);
        manager.applyInitialLoads(graph);

        System.out.println("[Phase 1] Graph built: "
                + hubs.size() + " hubs, "
                + routes.size() + " routes, "
                + shipments.size() + " shipments loaded.");

        for (Shipment s : shipments) {
            double cost = computePathCost(s.getPath(), 0, ALPHA, 0.0);
            originalCosts.put(s.getId(), cost);
        }
    }

    // Phase 2 — propagate delay, detect bottleneck, isolate it
    public void runPhase2(DelayEvent event) {
        this.delayEvent = event;

        Map<String, Integer> delayMap =
                new BFSDelayPropagator().propagate(graph, event);

        System.out.println("[Phase 2] Delay propagated from hub: "
                + event.getHubId() + " (initial delay=" + event.getDelay() + ")");

        BottleneckDetector detector = new BottleneckDetector();
        bottleneckId    = detector.detect(graph, delayMap, manager.getAll());
        bottleneckScore = detector.getBottleneckScore();

        System.out.println("[Phase 2] Bottleneck detected: "
                + bottleneckId + " (score=" + bottleneckScore + ")");

        graph.isolateHub(bottleneckId);

        System.out.println("[Phase 2] Hub isolated: " + bottleneckId);
    }

    // Phase 3 — reroute all affected shipments
    public void runPhase3(double alpha, double beta) {
        LoadAwareDijkstra dijkstra = new LoadAwareDijkstra();
        UnionFind uf = new UnionFind();
        uf.build(graph);

        List<Shipment> affected = manager.getAffected(bottleneckId);

        System.out.println("[Phase 3] Rerouting " + affected.size()
                + " affected shipment(s) around: " + bottleneckId);

        for (Shipment shipment : affected) {
            List<String> originalPath = new ArrayList<>(shipment.getPath());
            String destination = originalPath.get(originalPath.size() - 1);
            int isolatedIndex = manager.getIsolatedIndex(shipment, bottleneckId);

            // get pre-computed original cost (computed before any isolation)
            double oldCost = originalCosts.getOrDefault(shipment.getId(), 0.0);

            // determine source and release loads based on case
            String source;
            if (shipment.getFailIndex() < isolatedIndex) {
                // Case 1 — hasn't reached isolated hub, reroute from origin
                // release entire original path since we're replacing it fully
                source = originalPath.get(0);
                manager.releaseLoadsFrom(graph, shipment, 0);
            } else {
                // Case 2 — at or past isolated hub, emergency reroute
                // release only from isolated hub onward
                source = isolatedIndex > 0
                        ? originalPath.get(isolatedIndex - 1)
                        : originalPath.get(0);
                manager.releaseLoadsFrom(graph, shipment, isolatedIndex);
            }

            // pre-flight reachability check via Union-Find
            if (!uf.connected(source, destination)) {
                shipment.setStatus(ShipmentStatus.FAILED);
                System.out.println("[Phase 3] FAILED (unreachable): "
                        + shipment.getId());
                continue;
            }

            // reroute
            List<String> newPath = dijkstra.reroute(
                    graph, shipment, bottleneckId, alpha, beta);

            if (newPath.isEmpty()) {
                shipment.setStatus(ShipmentStatus.FAILED);
                System.out.println("[Phase 3] FAILED to reroute: "
                        + shipment.getId());
                continue;
            }

            // commit loads for full new path (old loads fully released above)
            manager.commitLoads(graph, shipment, newPath);

            shipment.updatePath(newPath);

            double newCost = computePathCost(newPath, 0, alpha, 0.0);
            results.add(new RerouteResult(
                    shipment.getId(), originalPath, newPath, oldCost, newCost));

            System.out.println("[Phase 3] Rerouted " + shipment.getId()
                    + ": " + String.join(" -> ", newPath)
                    + " (old=" + String.format("%.1f", oldCost)
                    + ", new=" + String.format("%.1f", newCost) + ")");
        }
    }

    // Full pipeline
    public void run(String inputFile) throws IOException {
        OutputFormatter formatter = new OutputFormatter();
        formatter.printHeader();

        InputParser parser = new InputParser();
        parser.parse(inputFile);

        runPhase1(parser.getHubs(), parser.getEdges(), parser.getShipments());

        if (parser.getDelayEvent() != null) {
            runPhase2(parser.getDelayEvent());
        } else {
            System.out.println("[Phase 2] No delay event found.");
            return;
        }

        runPhase3(ALPHA, BETA);

        formatter.printBottleneck(bottleneckId, bottleneckScore);
        formatter.printRouteComparison(results);

        double totalBefore = results.stream().mapToDouble(RerouteResult::getOldCost).sum();
        double totalAfter  = results.stream().mapToDouble(RerouteResult::getNewCost).sum();
        formatter.printDelayReduction(totalBefore, totalAfter);

        formatter.printLoadMap(graph);
    }

    public List<RerouteResult> getResults() { return results; }

    private double computePathCost(List<String> path, int fromIndex,
                                   double alpha, double beta) {
        double cost = 0.0;
        for (int i = fromIndex; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to   = path.get(i + 1);
            int travelTime = graph.getNeighbours(from).stream()
                    .filter(e -> e.getTo().equals(to))
                    .mapToInt(Edge::getWeight)
                    .findFirst()
                    .orElse(0);
            cost += alpha * travelTime + beta * graph.getLoad(to);
        }
        return cost;
    }
}