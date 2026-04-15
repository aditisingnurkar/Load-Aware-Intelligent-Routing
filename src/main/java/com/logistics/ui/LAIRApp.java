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

    @Override
    public void start(Stage primaryStage) {
        initializeBackend();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-base-color: #2c3e50; -fx-background-color: #ecf0f1;");

        Label header = new Label("SMART LOGISTICS NETWORK SIMULATOR");
        header.setFont(Font.font("System", FontWeight.BOLD, 24));
        header.setTextFill(Color.web("#2c3e50"));
        header.setAlignment(Pos.CENTER);
        root.setTop(header);

        canvas = new Canvas(800, 600);
        StackPane canvasHolder = new StackPane(canvas);
        canvasHolder.setStyle("-fx-background-color: black; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        root.setCenter(canvasHolder);

        statsPanel = new VBox(15);
        statsPanel.setPrefWidth(350);
        statsPanel.setPadding(new Insets(0, 0, 0, 15));

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(400);
        logArea.setWrapText(true);
        // Stylized for a clean "Step Tracker" look
        logArea.setStyle("-fx-control-inner-background: #1e272e; -fx-text-fill: #d2dae2; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");
        logArea.setText("STATUS: Waiting to begin...\n");

        nextStepBtn = new Button("Run Phase 1: Build Network");
        nextStepBtn.setMaxWidth(Double.MAX_VALUE);
        nextStepBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        nextStepBtn.setOnAction(e -> handleNextPhase());

        statsPanel.getChildren().addAll(new Label("Simulation Steps"), logArea, nextStepBtn);
        root.setRight(statsPanel);

        Scene scene = new Scene(root, 1200, 750);
        primaryStage.setTitle("LAIR - Load Aware Intelligent Rerouting");
        primaryStage.setScene(scene);
        primaryStage.show();
        drawNetwork();
    }

    private void updateStatus(String message) {
        logArea.appendText("\n▶ " + message + "\n");
    }

    private void initializeBackend() {
        graph = new LogisticsGraph();
        manager = new ShipmentManager();
        engine = new SimulationEngine(graph, manager);
        parser = new InputParser();
        try {
            parser.parse("sample_network.txt");
            assignHubCoordinates();
        } catch (IOException e) {
            System.err.println("Error loading input file: " + e.getMessage());
        }
    }

    private void assignHubCoordinates() {
        double height = 600;
        hubPositions.put("W1", new double[]{50, height/2});
        hubPositions.put("S1", new double[]{200, height/3});
        hubPositions.put("S2", new double[]{200, 2*height/3});
        hubPositions.put("S3", new double[]{200, height/2});
        hubPositions.put("R1", new double[]{400, height/3});
        hubPositions.put("R2", new double[]{400, 2*height/3});
        hubPositions.put("R3", new double[]{400, height/2});
        hubPositions.put("D1", new double[]{600, height/3});
        hubPositions.put("D2", new double[]{600, 2*height/3});
        hubPositions.put("P1", new double[]{750, height/4});
        hubPositions.put("P2", new double[]{750, height/2});
        hubPositions.put("P3", new double[]{750, 3*height/4});

        List<Color> palette = Arrays.asList(
                Color.DODGERBLUE, Color.CRIMSON, Color.FORESTGREEN,
                Color.web("#FFD700"),
                Color.DARKORCHID, Color.DARKORANGE
        );
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
                updateStatus("STEP 1: Network Infrastructure Built.\nHubs and Routes have been initialized.\nInitial shipment loads applied.");
                nextStepBtn.setText("Run Phase 2: Detect Bottleneck");
            }
            case 2 -> {
                engine.runPhase2(parser.getDelayEvent());
                updateStatus("STEP 2: Bottleneck Identified.\n" +
                        "System detected critical congestion at: " + engine.getBottleneckId() + "\n" +
                        "The hub has been ISOLATED for maintenance.");
                nextStepBtn.setText("Run Phase 3: Reroute Shipments");
            }
            case 3 -> {
                engine.runPhase3(1.0, 0.5);
                updateStatus("STEP 3: Rerouting Complete.\n" +
                        "All affected shipments have been mapped to alternative routes.\n" +
                        "Cost impact analysis finalized.");
                displayRerouteResults();
                nextStepBtn.setDisable(true);
                nextStepBtn.setText("Simulation Finished");
            }
        }
        drawNetwork();
    }

    private void displayRerouteResults() {
        statsPanel.getChildren().add(new Separator());
        Label resultsTitle = new Label("REROUTE IMPACT SUMMARY");
        resultsTitle.setStyle("-fx-font-weight: bold;");
        statsPanel.getChildren().add(resultsTitle);
        for (RerouteResult res : engine.getResults()) {
            double diff = res.getDelayReductionPercent();
            String impact = String.format("%s: %.1f%% %s", res.getId(), Math.abs(diff), (diff >= 0 ? "Improvement" : "Increase"));
            Label l = new Label(impact);
            l.setTextFill(diff >= 0 ? Color.DARKGREEN : Color.DARKRED);
            statsPanel.getChildren().add(l);
        }
    }

    private void drawNetwork() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 1. Draw Routes
        gc.setLineWidth(1.5);
        for (Edge edge : parser.getEdges()) {
            double[] start = hubPositions.get(edge.getFrom());
            double[] end = hubPositions.get(edge.getTo());
            if (start == null || end == null) continue;

            if (graph.isIsolated(edge.getFrom()) || graph.isIsolated(edge.getTo())) {
                gc.setStroke(Color.web("#3d3d3d"));
                gc.setLineDashes(5);
            } else {
                gc.setStroke(Color.web("#636e72"));
                gc.setLineDashes(0);
            }
            gc.strokeLine(start[0], start[1], end[0], end[1]);
        }

        // 2. Draw Shipment Paths with Perpendicular Offsets
        gc.setLineDashes(0);
        List<Shipment> allShipments = manager.getAll();
        for (int i = 0; i < allShipments.size(); i++) {
            Shipment s = allShipments.get(i);
            if (s.getStatus() == ShipmentStatus.FAILED) continue;

            gc.setStroke(shipmentColors.get(s.getId()));
            gc.setLineWidth(3.0);

            double laneOffset = (i - (allShipments.size() / 2.0)) * 6.0;

            List<String> path = s.getPath();
            for (int j = 0; j < path.size() - 1; j++) {
                double[] p1 = hubPositions.get(path.get(j));
                double[] p2 = hubPositions.get(path.get(j+1));

                if (p1 != null && p2 != null) {
                    double dx = p2[0] - p1[0];
                    double dy = p2[1] - p1[1];
                    double length = Math.sqrt(dx * dx + dy * dy);
                    double nx = -dy / length;
                    double ny = dx / length;

                    double offsetX = nx * laneOffset;
                    double offsetY = ny * laneOffset;

                    gc.strokeLine(p1[0] + offsetX, p1[1] + offsetY,
                            p2[0] + offsetX, p2[1] + offsetY);
                }
            }
        }

        // 3. Draw Hubs
        for (Map.Entry<String, double[]> entry : hubPositions.entrySet()) {
            String id = entry.getKey();
            double[] pos = entry.getValue();

            if (id.equals(engine.getBottleneckId())) {
                gc.setFill(Color.web("#e74c3c"));
            } else if (graph.isIsolated(id)) {
                gc.setFill(Color.web("#636e72"));
            } else {
                gc.setFill(Color.web("#0984e3"));
            }

            gc.fillOval(pos[0] - 12, pos[1] - 12, 24, 24);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeOval(pos[0] - 12, pos[1] - 12, 24, 24);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 12));
            gc.fillText(id, pos[0] - 10, pos[1] - 18);
            if (currentPhase >= 1) {
                gc.setFont(Font.font(10));
                gc.fillText("L:" + graph.getLoad(id), pos[0] - 10, pos[1] + 26);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}