package com.logistics.io;

import com.logistics.model.*;
import com.logistics.graph.LogisticsGraph;

import java.io.*;
import java.util.*;

public class InputParser {

    // This is the main method - give it the file path, it builds the entire graph
    public static LogisticsGraph parse(String filePath) throws IOException {
        LogisticsGraph graph = new LogisticsGraph();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\\s+");

            if (parts[0].equals("HUB")) {
                // HUB W1 WAREHOUSE → create a Hub object
                String id = parts[1];
                HubType type = HubType.valueOf(parts[2]);
                graph.addHub(new Hub(id, type));

            } else if (parts[0].equals("ROUTE")) {
                // ROUTE W1 S1 4 → create an Edge object
                String from = parts[1];
                String to = parts[2];
                int travelTime = Integer.parseInt(parts[3]);
                graph.addEdge(new Edge(from, to, travelTime));

            } else if (parts[0].equals("SHIPMENT")) {
                // SHIPMENT SH1 W1 S1 R1 D1 DEL1 → create a Shipment object
                String id = parts[1];
                List<String> path = new ArrayList<>();
                for (int i = 2; i < parts.length; i++) {
                    path.add(parts[i]);
                }
                graph.addShipment(new Shipment(id, path));

            } else if (parts[0].equals("DELAY")) {
                // DELAY R2 10 → create a DelayEvent object
                String hubId = parts[1];
                int delay = Integer.parseInt(parts[2]);
                graph.setDelayEvent(new DelayEvent(hubId, delay));
            }
        }

        reader.close();
        return graph;

    }
}