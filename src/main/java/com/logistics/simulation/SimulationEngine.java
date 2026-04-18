package com.logistics.simulation;

import com.logistics.algorithm.*;
import com.logistics.graph.LogisticsGraph;
import com.logistics.io.InputParser;
import com.logistics.io.OutputFormatter;
import com.logistics.model.*;

import java.io.IOException;
import java.util.*;

public class SimulationEngine {

    private final LogisticsGraph graph;
    private final ShipmentManager manager;
    private final List<RerouteResult> results;

    private final Map<String, Double> preIsolationCosts = new HashMap<>();
    private final Map<String, Double> originalCosts     = new HashMap<>();

    private Map<String, List<String>> mainPaths = new HashMap<>();

    private DelayEvent delayEvent;
    private String bottleneckId;
    private int bottleneckScore;

    private static final double ALPHA = 1.0;
    private static final double BETA  = 0.5;

    public SimulationEngine(LogisticsGraph graph, ShipmentManager manager) {
        this.graph = graph;
        this.manager = manager;
        this.results = new ArrayList<>();
    }

    // Phase 1: build graph and apply loads
    public void runPhase1(List<Hub> hubs, List<Edge> routes, List<Shipment> shipments) {

        for (Hub hub : hubs) graph.addHub(hub);
        for (Edge edge : routes) graph.addEdge(edge);
        for (Shipment s : shipments) manager.addShipment(s);

        manager.applyInitialLoads(graph);

        System.out.println("[Phase 1] Graph built: "
                + hubs.size() + " hubs, "
                + routes.size() + " routes, "
                + shipments.size() + " shipments loaded.");

        for (Shipment s : shipments) {
            double cost = computePathCost(s.getPath(), 0, ALPHA, BETA);
            preIsolationCosts.put(s.getId(), cost);
        }
    }

    // Phase 2: delay propagation and bottleneck isolation
    public void runPhase2(DelayEvent event) {

        this.delayEvent = event;

        Map<String, Integer> delayMap =
                new BFSDelayPropagator().propagate(graph, event);

        System.out.println("[Phase 2] Delay propagated from hub: "
                + event.getHubId() + " (delay=" + event.getDelay() + ")");

        BottleneckDetector detector = new BottleneckDetector();

        bottleneckId = detector.detect(graph, delayMap, manager.getAll());
        bottleneckScore = detector.getBottleneckScore();

        System.out.println("[Phase 2] Bottleneck detected: "
                + bottleneckId + " (score=" + bottleneckScore + ")");

        // compute baseline costs before isolation
        for (Shipment s : manager.getAll()) {
            double cost = computePathCost(s.getPath(), 0, ALPHA, BETA);
            originalCosts.put(s.getId(), cost);
        }

        graph.isolateHub(bottleneckId);
        System.out.println("[Phase 2] Hub isolated: " + bottleneckId);
    }

    // Phase 3: reroute shipments
    public void runPhase3(double alpha, double beta) {

        results.clear();
        mainPaths.clear();

        LoadAwareDijkstra dijkstra = new LoadAwareDijkstra();
        UnionFind uf = new UnionFind();
        uf.build(graph);

        List<Shipment> affected = manager.getAffected(bottleneckId);

        System.out.println("[Phase 3] Rerouting " + affected.size()
                + " shipment(s)");

        // compute main paths per destination
        for (Shipment s : affected) {

            String dest = s.getDestination();
            if (mainPaths.containsKey(dest)) continue;

            String start = s.getOrigin();
            if (graph.isIsolated(start)) {
                start = s.getCurrentHub();
            }

            List<String> mainPath =
                    dijkstra.computeMainPath(graph, start, dest, alpha, beta);

            mainPaths.put(dest, mainPath);
        }

        // process shipments
        for (Shipment s : affected) {

            String current = s.getCurrentHub();
            String dest = s.getDestination();

            if (current.equals(dest)) {
                s.setStatus(ShipmentStatus.DELIVERED);
                continue;
            }

            if (!uf.connected(current, dest)) {
                s.setStatus(ShipmentStatus.FAILED);
                continue;
            }

            List<String> mainPath = mainPaths.get(dest);

            if (mainPath == null || mainPath.isEmpty()) {
                s.setStatus(ShipmentStatus.FAILED);
                continue;
            }

            List<String> originalPath = new ArrayList<>(s.getPath());
            double oldCost = originalCosts.getOrDefault(s.getId(), 0.0);

            manager.releaseLoadsFromCurrent(graph, s);

            List<String> newPath =
                    dijkstra.mergeShipmentToMainPath(graph, current, dest, mainPath, alpha, beta);

            if (newPath.isEmpty()) {
                s.setStatus(ShipmentStatus.FAILED);
                continue;
            }

            manager.commitLoads(graph, newPath);
            s.updatePath(newPath);

            double newCost = computePathCost(newPath, 0, alpha, beta);

            results.add(new RerouteResult(
                    s.getId(),
                    originalPath,
                    newPath,
                    oldCost,
                    newCost
            ));

            System.out.println("[Phase 3] Rerouted " + s.getId()
                    + " → " + String.join(" -> ", newPath));
        }
    }

    // Full pipeline
    public void run(String inputFile) throws IOException {

        OutputFormatter formatter = new OutputFormatter();
        formatter.printHeader();

        InputParser parser = new InputParser();
        parser.parse(inputFile);

        runPhase1(parser.getHubs(), parser.getEdges(), parser.getShipments());

        if (parser.getDelayEvent() == null) {
            System.out.println("[Phase 2] No delay event.");
            return;
        }

        runPhase2(parser.getDelayEvent());
        runPhase3(ALPHA, BETA);

        formatter.printBottleneck(bottleneckId, bottleneckScore);
        formatter.printRouteComparison(results);

        double before = results.stream().mapToDouble(RerouteResult::getOldCost).sum();
        double after  = results.stream().mapToDouble(RerouteResult::getNewCost).sum();

        formatter.printDelayReduction(before, after);
        formatter.printLoadMap(graph);
    }

    public List<RerouteResult> getResults() {
        return results;
    }

    public String getBottleneckId() {
        return bottleneckId;
    }

    public int getBottleneckScore() {
        return bottleneckScore;
    }

    public Map<String, List<String>> getMainPaths() {
        return mainPaths;
    }

    public Map<String, Double> getPreIsolationCosts() {
        return preIsolationCosts;
    }

    // compute cost of path
    private double computePathCost(List<String> path, int fromIndex,
                                   double alpha, double beta) {

        double cost = 0.0;

        for (int i = fromIndex; i < path.size() - 1; i++) {

            String from = path.get(i);
            String to = path.get(i + 1);

            int travel = graph.getNeighbours(from).stream()
                    .filter(e -> e.getTo().equals(to))
                    .mapToInt(Edge::getWeight)
                    .findFirst()
                    .orElse(0);

            cost += alpha * travel + beta * graph.getLoad(to);
        }

        return cost;
    }
}