package com.logistics.ui;

import com.logistics.algorithm.*;
import com.logistics.graph.LogisticsGraph;
import com.logistics.io.InputParser;
import com.logistics.model.*;
import com.logistics.simulation.ShipmentManager;
import com.logistics.simulation.SimulationEngine;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class LAIRApp extends Application {

    private SimulationEngine engine;
    private LogisticsGraph graph;
    private ShipmentManager manager;
    private InputParser parser;

    private Canvas canvas;
    private TextArea logArea;
    private VBox statsPanel;
    private Button nextStepBtn;

    private int currentPhase = 0;
    private final Map<String, double[]> hubPositions = new HashMap<>();
    private final Map<String, Color> shipmentColors = new HashMap<>();
    private final Random random = new Random();

    @Override
    public void start(Stage primaryStage) {
        initializeBackend();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-base-color: #2c3e50; -fx-background-color: #ecf0f1;");

        // --- HEADER ---
        Label header = new Label("SMART LOGISTICS NETWORK SIMULATOR");
        header.setFont(Font.font("System", FontWeight.BOLD, 24));
        header.setTextFill(Color.web("#2c3e50"));
        header.setAlignment(Pos.CENTER);
        root.setTop(header);

        // --- CENTER: CANVAS ---
        canvas = new Canvas(800, 600);
        StackPane canvasHolder = new StackPane(canvas);
        canvasHolder.setStyle("-fx-background-color: black; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        root.setCenter(canvasHolder);

        // --- RIGHT: INFO PANEL ---
        statsPanel = new VBox(15);
        statsPanel.setPrefWidth(350);
        statsPanel.setPadding(new Insets(0, 0, 0, 15));

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(300);
        logArea.setWrapText(true);
        logArea.setText("Ready to start Phase 1: Network Construction...");

        VBox controls = new VBox(10);
        nextStepBtn = new Button("Run Phase 1: Build Network");
        nextStepBtn.setMaxWidth(Double.MAX_VALUE);
        nextStepBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        nextStepBtn.setOnAction(e -> handleNextPhase());

        statsPanel.getChildren().addAll(new Label("Simulation Logs"), logArea, nextStepBtn);
        root.setRight(statsPanel);

        Scene scene = new Scene(root, 1200, 750);
        primaryStage.setTitle("LAIR - Load-Aware-Intelligent-Rerouter");
        primaryStage.setScene(scene);
        primaryStage.show();

        drawNetwork();
    }

    private void initializeBackend() {
        graph = new LogisticsGraph();
        manager = new ShipmentManager();
        engine = new SimulationEngine(graph, manager);
        parser = new InputParser();
        try {
            // Assuming the file is in the project root
            parser.parse("sample_network.txt");
            assignHubCoordinates();
        } catch (IOException e) {
            System.err.println("Error loading input file: " + e.getMessage());
        }
    }

    private void assignHubCoordinates() {
        // Simple heuristic layout based on Hub Type hierarchy
        double width = 800;
        double height = 600;

        hubPositions.put("W1", new double[]{50, height/2});
        hubPositions.put("S1", new double[]{200, height/3});
        hubPositions.put("S2", new double[]{200, 2*height/3});
        hubPositions.put("R1", new double[]{400, height/3});
        hubPositions.put("R2", new double[]{400, 2*height/3});
        hubPositions.put("D1", new double[]{600, height/3});
        hubPositions.put("D2", new double[]{600, 2*height/3});
        hubPositions.put("P1", new double[]{750, height/4});
        hubPositions.put("P2", new double[]{750, height/2});
        hubPositions.put("P3", new double[]{750, 3*height/4});

        // Ensure colors for shipments
        List<Color> palette = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE);
        int colorIdx = 0;
        for (Shipment s : parser.getShipments()) {
            shipmentColors.put(s.getId(), palette.get(colorIdx % palette.size()));
            colorIdx++;
        }
    }

    private void handleNextPhase() {
        currentPhase++;
        switch (currentPhase) {
            case 1 -> {
                engine.runPhase1(parser.getHubs(), parser.getEdges(), parser.getShipments());
                logArea.appendText("\n[Phase 1 Complete] Graph constructed and shipments loaded.");
                nextStepBtn.setText("Run Phase 2: Detect Bottleneck");
            }
            case 2 -> {
                engine.runPhase2(parser.getDelayEvent());
                logArea.appendText("\n[Phase 2 Complete] Bottleneck Hub: " + engine.getBottleneckId());
                logArea.appendText("\nImpact Score: " + engine.getBottleneckScore());
                logArea.appendText("\nHub " + engine.getBottleneckId() + " has been ISOLATED.");
                nextStepBtn.setText("Run Phase 3: Reroute Shipments");
            }
            case 3 -> {
                engine.runPhase3(1.0, 0.5); // ALPHA and BETA constants
                logArea.appendText("\n[Phase 3 Complete] Rerouting analysis finished.");
                displayRerouteResults();
                nextStepBtn.setDisable(true);
            }
        }
        drawNetwork();
    }

    private void displayRerouteResults() {
        statsPanel.getChildren().add(new Separator());
        Label resultsTitle = new Label("REROUTE IMPACT SCORES");
        resultsTitle.setStyle("-fx-font-weight: bold;");
        statsPanel.getChildren().add(resultsTitle);

        for (RerouteResult res : engine.getResults()) {
            double diff = res.getDelayReductionPercent();
            String impact = String.format("%s: %.1f%% %s",
                    res.getId(), Math.abs(diff), (diff >= 0 ? "Improvement" : "Increase"));
            Label l = new Label(impact);
            l.setTextFill(diff >= 0 ? Color.DARKGREEN : Color.DARKRED);
            statsPanel.getChildren().add(l);
        }
    }

    private void drawNetwork() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 1. Draw Edges (Routes)
        gc.setLineWidth(2);
        for (Edge edge : parser.getEdges()) {
            double[] start = hubPositions.get(edge.getFrom());
            double[] end = hubPositions.get(edge.getTo());
            if (start == null || end == null) continue;

            // Faded if hub is isolated
            if (graph.isIsolated(edge.getFrom()) || graph.isIsolated(edge.getTo())) {
                gc.setStroke(Color.LIGHTGRAY);
                gc.setLineDashes(5);
            } else {
                gc.setStroke(Color.DARKGRAY);
                gc.setLineDashes(0);
            }
            gc.strokeLine(start[0], start[1], end[0], end[1]);
        }

        // 2. Draw Shipment Paths (if Phase 3 or Phase 1)
        gc.setLineDashes(0);
        for (Shipment s : manager.getAll()) {
            if (s.getStatus() == ShipmentStatus.FAILED) continue;

            gc.setStroke(shipmentColors.get(s.getId()));
            gc.setLineWidth(3);
            List<String> path = s.getPath();
            for (int i = 0; i < path.size() - 1; i++) {
                double[] p1 = hubPositions.get(path.get(i));
                double[] p2 = hubPositions.get(path.get(i+1));
                if (p1 != null && p2 != null) {
                    // Offset paths slightly so they don't overlap perfectly
                    double offset = (random.nextDouble() - 0.5) * 10;
                    gc.strokeLine(p1[0]+offset, p1[1]+offset, p2[0]+offset, p2[1]+offset);
                }
            }
        }

        // 3. Draw Hubs (Nodes)
        for (Map.Entry<String, double[]> entry : hubPositions.entrySet()) {
            String id = entry.getKey();
            double[] pos = entry.getValue();

            // Background Circle
            if (id.equals(engine.getBottleneckId())) {
                gc.setFill(Color.RED); // Isolated Bottleneck
            } else if (graph.isIsolated(id)) {
                gc.setFill(Color.LIGHTGRAY);
            } else {
                gc.setFill(Color.web("#34495e"));
            }

            gc.fillOval(pos[0] - 15, pos[1] - 15, 30, 30);
            gc.setStroke(Color.WHITE);
            gc.strokeOval(pos[0] - 15, pos[1] - 15, 30, 30);

            // Label
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 12));
            gc.fillText(id, pos[0] - 10, pos[1] - 20);

            // Load Info
            if (currentPhase >= 1) {
                gc.setFont(Font.font(10));
                gc.fillText("L:" + graph.getLoad(id), pos[0] - 10, pos[1] + 28);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}