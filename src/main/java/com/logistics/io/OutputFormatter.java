package com.logistics.io;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.RerouteResult;

import java.util.List;

public class OutputFormatter {

    // ─── HEADER ──────────────────────────────────────────────────────────────

    public void printHeader() {
        System.out.println("=".repeat(60));
        System.out.println("       SMART LOGISTICS NETWORK SIMULATOR");
        System.out.println("=".repeat(60));
    }

    // ─── BOTTLENECK ──────────────────────────────────────────────────────────

    public void printBottleneck(String hubId, double score) {
        System.out.println("\n[BOTTLENECK DETECTED]");
        System.out.println("  Hub     : " + hubId);
        System.out.printf ("  Score   : %.2f%n", score);
        System.out.println("  Status  : ISOLATED");
    }

    // ─── ROUTE COMPARISON ────────────────────────────────────────────────────

    public void printRouteComparison(List<RerouteResult> results) {
        System.out.println("\n[ROUTE COMPARISON]");
        System.out.println("-".repeat(60));

        for (RerouteResult r : results) {
            System.out.println("  Shipment : " + r.getId());
            System.out.println("  New Path : " + String.join(" -> ", r.getPath()));
            System.out.printf ("  Old Cost : %.1f%n", r.getOldCost());
            System.out.printf ("  New Cost : %.1f%n", r.getNewCost());
            System.out.printf ("  Reduction: %.1f%%%n", r.getDelayReductionPercent());
            System.out.println("-".repeat(60));
        }
    }

    // ─── DELAY REDUCTION SUMMARY ─────────────────────────────────────────────

    public void printDelayReduction(double before, double after) {
        System.out.println("\n[TOTAL DELAY REDUCTION]");
        System.out.printf ("  Before Reroute : %.1f%n", before);
        System.out.printf ("  After Reroute  : %.1f%n", after);
        if (before > 0) {
            double percent = ((before - after) / before) * 100;
            System.out.printf("  Reduction      : %.1f%%%n", percent);
        }
    }

    // ─── LOAD MAP ────────────────────────────────────────────────────────────

    public void printLoadMap(LogisticsGraph graph) {
        System.out.println("\n[FINAL HUB LOAD MAP]");
        System.out.println("-".repeat(30));
        for (String hubId : graph.getAllHubIds()) {
            System.out.printf("  %-10s : %d shipments%n", hubId, graph.getLoad(hubId));
        }
        System.out.println("=".repeat(60));
    }
}