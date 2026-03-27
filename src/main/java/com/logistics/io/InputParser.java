package com.logistics.io;

import com.logistics.model.*;
import com.logistics.graph.LogisticsGraph;

import java.io.*;
import java.util.*;

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

    // Master parse method — reads the full file and delegates to section parsers
    public void parse(String filePath) throws IOException {
        List<String> hubLines      = new ArrayList<>();
        List<String> routeLines    = new ArrayList<>();
        List<String> shipmentLines = new ArrayList<>();
        List<String> delayLines    = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String section = "";

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Detect section by keyword
                if (line.startsWith("HUB"))      hubLines.add(line);
                else if (line.startsWith("ROUTE"))    routeLines.add(line);
                else if (line.startsWith("SHIPMENT")) shipmentLines.add(line);
                else if (line.startsWith("DELAY"))    delayLines.add(line);
            }
        }

        parseHubs(hubLines);
        parseRoutes(routeLines);
        parseShipments(shipmentLines);
        parseDelayEvent(delayLines);
    }

    // format: HUB <id> <type>
    public void parseHubs(List<String> lines) {
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            String id = parts[1];
            HubType type = HubType.valueOf(parts[2]);
            hubs.add(new Hub(id, type));
        }
    }

    // format: ROUTE <from> <to> <travelTime>
    public void parseRoutes(List<String> lines) {
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            String from = parts[1];
            String to   = parts[2];
            int time    = Integer.parseInt(parts[3]);
            edges.add(new Edge(from, to, time));
        }
    }

    // format: SHIPMENT <id> <hub1> <hub2> ... <hubN>
    public void parseShipments(List<String> lines) {
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            String id = parts[1];
            List<String> path = new ArrayList<>(Arrays.asList(parts).subList(2, parts.length));
            shipments.add(new Shipment(id, path));
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
    public List<Hub> getHubs()           { return hubs; }
    public List<Edge> getEdges()         { return edges; }
    public List<Shipment> getShipments() { return shipments; }
    public DelayEvent getDelayEvent()    { return delayEvent; }
}