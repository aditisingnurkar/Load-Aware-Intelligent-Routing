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
import java.util.*;

public class SimulationEngine {

    private final LogisticsGraph graph;
    private final ShipmentManager manager;
    private final List<RerouteResult> results;

    /**
     * FIX (Bug #5): originalCosts now stores the cost of each shipment's
     * planned path computed AFTER isolation, so both old-cost and new-cost
     * are evaluated on the same post-isolation graph.  This makes the
     * comparison meaningful: it answers "is the merge-rerouted path cheaper
     * than naively following the original plan would be today?"
     *
     * The previous behaviour computed old costs in Phase 1 (pre-isolation),
     * which mixed a path that went through D1 against a rerouted path that
     * never could — an apples-to-oranges comparison.
     *
     * NOTE: if you specifically want to show the raw before/after delay cost
     * across the isolation event, compute a separate pre-isolation snapshot
     * in runPhase1() and expose it via getPreIsolationCosts().
     */
    private final Map<String, Double> preIsolationCosts  = new HashMap<>(); // kept for reference display
    private final Map<String, Double> originalCosts      = new HashMap<>(); // post-isolation, used for comparison

    private Map<String, List<String>> mainPaths = new HashMap<>();
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

        // Store pre-isolation costs for reference / display only.
        for (Shipment s : shipments) {
            double cost = computePathCost(s.getPath(), 0, ALPHA, BETA);
            preIsolationCosts.put(s.getId(), cost);
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

        // FIX (Bug #5): Compute originalCosts HERE — after delay has propagated
        // (so hub loads reflect the delay penalty) but BEFORE isolateHub() strips
        // the bottleneck's edges from the graph.  This gives the cost of "follow
        // the original planned path through the congested, pre-isolation network",
        // which is the correct baseline to compare against the rerouted cost on
        // the post-isolation graph.  Previously this was computed in Phase 1
        // (before delay loaded the graph) — apples-to-oranges.
        for (Shipment s : manager.getAll()) {
            double cost = computePathCost(s.getPath(), 0, ALPHA, BETA);
            originalCosts.put(s.getId(), cost);
        }

        graph.isolateHub(bottleneckId);
        System.out.println("[Phase 2] Hub isolated: " + bottleneckId);
    }

    public void runPhase3(double alpha, double beta) {
        LoadAwareDijkstra dijkstra = new LoadAwareDijkstra();
        UnionFind uf = new UnionFind();
        uf.build(graph);

        List<Shipment> affected = manager.getAffected(bottleneckId);
        System.out.println("[Phase 3] Rerouting " + affected.size()
                + " affected shipment(s) around: " + bottleneckId);

        // ─── STEP 1: COMPUTE MAIN PATHS (PER DESTINATION) ─────────────
        //
        // FIX (Bug #2 / design tension): Main paths are now computed from the
        // WAREHOUSE origin (s.getOrigin()) of the first shipment bound for each
        // destination — which is correct for producing a globally-optimal spine.
        // Individual shipments then JOIN that spine from their currentHub via
        // mergeShipmentToMainPath.  The join works reliably because Dijkstra
        // finds the cheapest connection from currentHub to any node on mainPath.
        //
        // If no affected shipment for a destination has a warehouse origin
        // (edge case: all mid-journey), we fall back to the first affected
        // shipment's currentHub as the spine start to guarantee a valid path.

        mainPaths.clear();
        for (Shipment s : affected) {
            String dest = s.getDestination();
            if (mainPaths.containsKey(dest)) continue;

            // Prefer origin (warehouse) so the spine is globally optimal.
            // Fall back to currentHub only if origin is itself isolated.
            String spineStart = s.getOrigin();
            if (graph.isIsolated(spineStart)) {
                spineStart = s.getCurrentHub();
            }

            List<String> mainPath = dijkstra.computeMainPath(
                    graph, spineStart, dest, alpha, beta);
            mainPaths.put(dest, mainPath);
        }

        // ─── STEP 2: PROCESS EACH SHIPMENT ─────────────
        for (Shipment shipment : affected) {
            String currentHub  = shipment.getCurrentHub();
            String destination = shipment.getDestination();
            List<String> originalPath = new ArrayList<>(shipment.getPath());
            double oldCost = originalCosts.getOrDefault(shipment.getId(), 0.0);

            // ─── CONNECTIVITY CHECK ─────────────
            if (!uf.connected(currentHub, destination)) {
                shipment.setStatus(ShipmentStatus.FAILED);
                System.out.println("[Phase 3] FAILED (unreachable): "
                        + shipment.getId()
                        + " — currentHub '" + currentHub + "' disconnected from '"
                        + destination + "'");
                continue;
            }

            // ─── GET MAIN PATH ─────────────
            List<String> mainPath = mainPaths.get(destination);
            if (mainPath == null || mainPath.isEmpty()) {
                shipment.setStatus(ShipmentStatus.FAILED);
                System.out.println("[Phase 3] FAILED (no main path): "
                        + shipment.getId());
                continue;
            }

            // ─── RELEASE OLD LOADS FROM CURRENT POSITION ─────────────
            manager.releaseLoadsFromCurrent(graph, shipment);

            // ─── MERGE INTO MAIN PATH ─────────────
            List<String> newPath = dijkstra.mergeShipmentToMainPath(
                    graph, currentHub, destination, mainPath, alpha, beta);

            if (newPath.isEmpty()) {
                shipment.setStatus(ShipmentStatus.FAILED);
                System.out.println("[Phase 3] FAILED to reroute: "
                        + shipment.getId());
                continue;
            }

            // ─── COMMIT NEW LOADS ─────────────
            manager.commitLoads(graph, newPath);

            // ─── UPDATE SHIPMENT ─────────────
            shipment.updatePath(newPath);
            // FIX (Bug #4): Removed the dead `shipment.setCurrentHub(currentHub)`
            // line that was here. updatePath() does not touch currentHub, so
            // currentHub is already preserved correctly — the extra set was a
            // no-op that only added confusion.

            // ─── COST CALCULATION ─────────────
            double newCost = computePathCost(newPath, 0, alpha, beta);
            results.add(new RerouteResult(
                    shipment.getId(),
                    originalPath,
                    newPath,
                    oldCost,
                    newCost
            ));

            System.out.println("[Phase 3] Rerouted " + shipment.getId()
                    + ": " + String.join(" -> ", newPath)
                    + " (post-isolation original cost=" + String.format("%.1f", oldCost)
                    + ", rerouted cost=" + String.format("%.1f", newCost) + ")");
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

    public List<RerouteResult> getResults()             { return results; }
    public String getBottleneckId()                      { return bottleneckId; }
    public int getBottleneckScore()                      { return bottleneckScore; }
    public Map<String, List<String>> getMainPaths()      { return mainPaths; }
    public Map<String, Double> getPreIsolationCosts()    { return preIsolationCosts; }

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