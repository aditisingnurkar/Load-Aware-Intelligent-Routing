package com.logistics.io;

import com.logistics.model.*;
import com.logistics.graph.LogisticsGraph;

import java.io.*;
import java.util.*;

public class InputParser {

    private List<Hub>      hubs;
    private List<Edge>     edges;
    private List<Shipment> shipments;
    private DelayEvent     delayEvent;

    public InputParser() {
        this.hubs      = new ArrayList<>();
        this.edges     = new ArrayList<>();
        this.shipments = new ArrayList<>();
    }

    // Master parse method — reads the full file and delegates to section parsers
    public void parse(String filePath) throws IOException {
        List<String> hubLines      = new ArrayList<>();
        List<String> routeLines    = new ArrayList<>();
        List<String> shipmentLines = new ArrayList<>();
        List<String> delayLines    = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if      (line.startsWith("HUB"))      hubLines.add(line);
                else if (line.startsWith("ROUTE"))     routeLines.add(line);
                else if (line.startsWith("SHIPMENT"))  shipmentLines.add(line);
                else if (line.startsWith("DELAY"))     delayLines.add(line);
            }
        }

        parseHubs(hubLines);
        parseRoutes(routeLines);
        parseShipments(shipmentLines);  // hubs must be parsed first for validation
        parseDelayEvent(delayLines);
    }

    // format: HUB <id> <type>
    public void parseHubs(List<String> lines) {
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            String id      = parts[1];
            HubType type   = HubType.valueOf(parts[2]);
            hubs.add(new Hub(id, type));
        }
    }

    // format: ROUTE <from> <to> <travelTime>
    public void parseRoutes(List<String> lines) {
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            String from    = parts[1];
            String to      = parts[2];
            int time       = Integer.parseInt(parts[3]);
            edges.add(new Edge(from, to, time));
        }
    }

    /**
     * FIX (Bug #1 — parse-time): Build a set of declared hub IDs so we can
     * warn immediately when a shipment references a hub that doesn't exist.
     * This surfaces the SH4 (S3/R3) and SH5 (X1) problems at parse time with
     * a clear message, rather than having them fail silently later.
     *
     * The shipment is still added (with a warning) so the rest of the pipeline
     * can reach its own failure path (FAILED status) with an accurate reason.
     *
     * format: SHIPMENT <id> <hub1> <hub2> ... <hubN> [| CURRENT <hubId>]
     */
    public void parseShipments(List<String> lines) {
        Set<String> knownHubs = new HashSet<>();
        for (Hub hub : hubs) knownHubs.add(hub.getId());

        for (String line : lines) {
            String[] parts = line.split("\\|");
            String[] left  = parts[0].trim().split("\\s+");

            String id             = left[1];
            List<String> path     = new ArrayList<>(
                    Arrays.asList(left).subList(2, left.length));

            // Validate path hubs against declared hub set
            for (String hubId : path) {
                if (!knownHubs.contains(hubId)) {
                    System.out.println("[WARN] InputParser: shipment " + id
                            + " references undeclared hub '" + hubId
                            + "' in its path. It will be skipped during load "
                            + "accounting and will likely result in FAILED status.");
                }
            }

            Shipment shipment = new Shipment(id, path);

            // optional CURRENT hub
            if (parts.length > 1) {
                String[] right = parts[1].trim().split("\\s+");
                if (right.length == 2 && right[0].equals("CURRENT")) {
                    String currentHubId = right[1];
                    if (!knownHubs.contains(currentHubId)) {
                        System.out.println("[WARN] InputParser: shipment " + id
                                + " has CURRENT hub '" + currentHubId
                                + "' which is not a declared hub. "
                                + "UnionFind will not find it; shipment will "
                                + "be marked FAILED (disconnected).");
                    }
                    shipment.setCurrentHub(currentHubId);
                }
            }

            shipments.add(shipment);
        }
    }

    // format: DELAY <hubId> <initialDelay>
    public void parseDelayEvent(List<String> lines) {
        if (!lines.isEmpty()) {
            String[] parts = lines.get(0).split("\\s+");
            delayEvent = new DelayEvent(parts[1], Integer.parseInt(parts[2]));
        }
    }

    // Getters for SimulationEngine to consume
    public List<Hub>      getHubs()       { return hubs; }
    public List<Edge>     getEdges()      { return edges; }
    public List<Shipment> getShipments()  { return shipments; }
    public DelayEvent     getDelayEvent() { return delayEvent; }
}