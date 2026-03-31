package com.logistics.io;

import com.logistics.graph.LogisticsGraph;
import com.logistics.model.RerouteResult;

import java.util.List;

public class OutputFormatter {

    public void printHeader() {
        System.out.println("=".repeat(60));
        System.out.println("       SMART LOGISTICS NETWORK SIMULATOR");
        System.out.println("=".repeat(60));
    }

    public void printBottleneck(String hubId, int score) {
        System.out.println("\n[BOTTLENECK DETECTED]");
        System.out.println("  Hub     : " + hubId);
        System.out.println("  Score   : " + score);
        System.out.println("  Status  : ISOLATED");
    }

    public void printRouteComparison(List<RerouteResult> results) {
        System.out.println("\n[ROUTE COMPARISON]");
        System.out.println("-".repeat(60));

        if (results.isEmpty()) {
            System.out.println("  No shipments were rerouted.");
            System.out.println("-".repeat(60));
            return;
        }

        for (RerouteResult r : results) {
            System.out.println("  Shipment     : " + r.getId());
            System.out.println("  Original     : " +
                    String.join(" -> ", r.getOriginalPath()));
            System.out.println("  Rerouted     : " +
                    String.join(" -> ", r.getPath()));
            System.out.printf ("  Old Cost     : %.1f%n", r.getOldCost());
            System.out.printf ("  New Cost     : %.1f%n", r.getNewCost());
            if (r.getNewCost() <= r.getOldCost()) {
                System.out.printf("  Improvement  : %.1f%%%n",
                        r.getDelayReductionPercent());
            } else {
                double increase = ((r.getNewCost() - r.getOldCost())
                        / r.getOldCost()) * 100;
                System.out.printf("  Cost increase: %.1f%% (best available)%n",
                        increase);
            }
            System.out.println("-".repeat(60));
        }
    }


    public void printDelayReduction(double before, double after) {
        System.out.println("\n[TOTAL COST SUMMARY]");
        System.out.printf("  Original route cost : %.1f%n", before);
        System.out.printf("  Rerouted cost       : %.1f%n", after);
        if (after <= before) {
            double percent = ((before - after) / before) * 100;
            System.out.printf("  Improvement         : %.1f%%%n", percent);
        } else {
            double percent = ((after - before) / before) * 100;
            System.out.printf("  Cost increase       : %.1f%% " +
                    "(best available reroute around isolated hub)%n", percent);
        }
    }

    public void printLoadMap(LogisticsGraph graph) {
        System.out.println("\n[FINAL HUB LOAD MAP]");
        System.out.println("-".repeat(30));
        for (String hubId : graph.getAllHubIds()) {
            System.out.printf("  %-10s : %d shipments%n",
                    hubId, graph.getLoad(hubId));
        }
        System.out.println("=".repeat(60));
    }
}
