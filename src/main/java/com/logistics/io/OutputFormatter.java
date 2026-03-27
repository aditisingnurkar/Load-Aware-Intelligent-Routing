package com.logistics.io;


import com.logistics.model.*;

import java.util.*;

public class OutputFormatter {

    // Prints original vs new route for each shipment
    public static void printRerouteResults(List<RerouteResult> results) {
        System.out.println("==================== REROUTE RESULTS ====================");
        for (RerouteResult r : results) {
            System.out.println("Shipment : " + r.getShipmentId());
            System.out.println("New Path : " + r.getNewPath());
            System.out.printf ("Old Cost : %.1f%n", r.getOldCost());
            System.out.printf ("New Cost : %.1f%n", r.getNewCost());
            System.out.printf ("Delay Reduction : %.1f%%%n", r.getDelayReductionPercent());
            System.out.println("----------------------------------------------------------");
        }
    }

    // Prints how many shipments are at each hub right now
    public static void printLoadMap(Map<String, Integer> loadMap) {
        System.out.println("==================== HUB LOAD MAP ====================");
        for (Map.Entry<String, Integer> entry : loadMap.entrySet()) {
            System.out.println("Hub " + entry.getKey() + " → load: " + entry.getValue());
        }
    }

    // Prints each shipment's final status
    public static void printShipmentStatuses(List<Shipment> shipments) {
        System.out.println("==================== SHIPMENT STATUS ====================");
        for (Shipment s : shipments) {
            System.out.println("Shipment " + s.getId()
                    + " | Status: " + s.getStatus()
                    + " | Path: " + s.getPath());
        }
    }

    // Quick test with fake data to make sure printing works
    public static void testWithMockData() {
        System.out.println("--- Mock Output Test ---");

        // Fake reroute result
        List<String> newPath = Arrays.asList("W1", "S2", "R3", "D1", "DEL2");
        RerouteResult mockResult = new RerouteResult("SH2", newPath, 18.0, 14.0);
        printRerouteResults(Collections.singletonList(mockResult));

        // Fake load map
        Map<String, Integer> mockLoad = new LinkedHashMap<>();
        mockLoad.put("W1", 2);
        mockLoad.put("S1", 1);
        mockLoad.put("D1", 3);
        printLoadMap(mockLoad);
    }
}
