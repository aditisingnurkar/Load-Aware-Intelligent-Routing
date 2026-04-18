package com.logistics.io;

import com.logistics.model.*;

import java.io.*;
import java.util.*;

/**
 * Parses input file into hubs, routes, shipments, and delay event.
 */
public class InputParser {

    private List<Hub> hubs;
    private List<Edge> edges;
    private List<Shipment> shipments;
    private DelayEvent delayEvent;

    public InputParser() {
        this.hubs = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.shipments = new ArrayList<>();
    }

    // Read file and split into sections
    public void parse(String filePath) throws IOException {

        List<String> hubLines = new ArrayList<>();
        List<String> routeLines = new ArrayList<>();
        List<String> shipmentLines = new ArrayList<>();
        List<String> delayLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("HUB")) hubLines.add(line);
                else if (line.startsWith("ROUTE")) routeLines.add(line);
                else if (line.startsWith("SHIPMENT")) shipmentLines.add(line);
                else if (line.startsWith("DELAY")) delayLines.add(line);
            }
        }

        parseHubs(hubLines);
        parseRoutes(routeLines);
        parseShipments(shipmentLines);
        parseDelayEvent(delayLines);
    }

    // HUB <id> <type>
    public void parseHubs(List<String> lines) {
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            if (parts.length < 3) continue;

            String id = parts[1];
            HubType type = HubType.valueOf(parts[2]);

            hubs.add(new Hub(id, type));
        }
    }

    // ROUTE <from> <to> <time>
    public void parseRoutes(List<String> lines) {
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            if (parts.length < 4) continue;

            String from = parts[1];
            String to = parts[2];
            int time = Integer.parseInt(parts[3]);

            edges.add(new Edge(from, to, time));
        }
    }

    // SHIPMENT parsing with validation
    public void parseShipments(List<String> lines) {

        Set<String> knownHubs = new HashSet<>();
        for (Hub hub : hubs) {
            knownHubs.add(hub.getId());
        }

        for (String line : lines) {

            String[] parts = line.split("\\|");
            String[] left = parts[0].trim().split("\\s+");

            if (left.length < 3) continue;

            String id = left[1];

            List<String> path = new ArrayList<>(
                    Arrays.asList(left).subList(2, left.length)
            );

            boolean valid = true;

            // validate path hubs
            for (String hubId : path) {
                if (!knownHubs.contains(hubId)) {
                    System.out.println("[ERROR] Shipment " + id +
                            " invalid hub in path: " + hubId);
                    valid = false;
                }
            }

            Shipment shipment = new Shipment(id, path);

            // handle CURRENT override
            if (parts.length > 1) {
                String[] right = parts[1].trim().split("\\s+");

                if (right.length == 2 && right[0].equals("CURRENT")) {
                    String currentHub = right[1];

                    if (!knownHubs.contains(currentHub)) {
                        System.out.println("[ERROR] Shipment " + id +
                                " invalid CURRENT hub: " + currentHub);
                        valid = false;
                    }

                    shipment.setCurrentHub(currentHub);
                }
            }

            if (!valid) {
                shipment.setStatus(ShipmentStatus.FAILED);
            }

            shipments.add(shipment);
        }
    }

    // DELAY <hubId> <value>
    public void parseDelayEvent(List<String> lines) {
        if (!lines.isEmpty()) {
            String[] parts = lines.get(0).split("\\s+");
            if (parts.length >= 3) {
                delayEvent = new DelayEvent(parts[1],
                        Integer.parseInt(parts[2]));
            }
        }
    }

    public List<Hub> getHubs() { return hubs; }
    public List<Edge> getEdges() { return edges; }
    public List<Shipment> getShipments() { return shipments; }
    public DelayEvent getDelayEvent() { return delayEvent; }
}