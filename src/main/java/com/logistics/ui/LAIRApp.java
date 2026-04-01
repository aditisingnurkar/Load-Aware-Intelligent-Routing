package com.logistics.ui;

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

import java.util.*;

public class LAIRApp extends Application {

    // ── canvas size ──────────────────────────────────────────
    private static final double CW = 860;
    private static final double CH = 520;

    // ── hub positions (fixed layout matching our network) ────
    private static final Map<String, double[]> HUB_POS = new LinkedHashMap<>();
    static {
        HUB_POS.put("W1",  new double[]{80,  260});
        HUB_POS.put("S1",  new double[]{240, 140});
        HUB_POS.put("S2",  new double[]{240, 380});
        HUB_POS.put("R1",  new double[]{420, 140});
        HUB_POS.put("R2",  new double[]{420, 380});
        HUB_POS.put("D1",  new double[]{580, 200});
        HUB_POS.put("D2",  new double[]{580, 320});
        HUB_POS.put("P1",  new double[]{760, 100});
        HUB_POS.put("P2",  new double[]{760, 260});
        HUB_POS.put("P3",  new double[]{760, 420});
    }

    // ── hub type colors ──────────────────────────────────────
    private static final Map<String, Color> TYPE_COLOR = new HashMap<>();
    static {
        TYPE_COLOR.put("WAREHOUSE",    Color.web("#FFD700"));
        TYPE_COLOR.put("SORTING",      Color.web("#4FC3F7"));
        TYPE_COLOR.put("REGIONAL",     Color.web("#81C784"));
        TYPE_COLOR.put("DISTRIBUTION", Color.web("#FF8A65"));
        TYPE_COLOR.put("DELIVERY",     Color.web("#CE93D8"));
    }

    // ── shipment colors ──────────────────────────────────────
    private static final Color[] SHIP_COLORS = {
            Color.web("#FF6B6B"),
            Color.web("#FFE66D"),
            Color.web("#4ECDC4")
    };

    // ── simulation state ─────────────────────────────────────
    private LogisticsGraph graph;
    private ShipmentManager manager;
    private SimulationEngine engine;
    private InputParser parser;

    private List<Hub>     hubs;
    private List<Edge>    edges;
    private List<Shipment> shipments;

    private int step = 0; // 0=network, 1=shipments, 2=isolation, 3=reroute

    // ── UI nodes ─────────────────────────────────────────────
    private Canvas canvas;
    private Label  stepLabel;
    private VBox   infoBox;
    private Button nextBtn;

    @Override
    public void start(Stage stage) throws Exception {

        // parse input
        parser = new InputParser();
        parser.parse("input/sample_network.txt");
        hubs      = parser.getHubs();
        edges     = parser.getEdges();
        shipments = parser.getShipments();

        // init simulation
        graph   = new LogisticsGraph();
        manager = new ShipmentManager();
        engine  = new SimulationEngine(graph, manager);

        // ── header ───────────────────────────────────────────
        Label title = new Label("LAIR  —  Load-Aware Intelligent Rerouting");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(12));
        header.setStyle("-fx-background-color: #1a1a2e;");

        // ── canvas ───────────────────────────────────────────
        canvas = new Canvas(CW, CH);
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setStyle("-fx-background-color: #0d0d1a; " +
                "-fx-border-color: #2a2a4a; " +
                "-fx-border-width: 1;");

        // ── step label ───────────────────────────────────────
        stepLabel = new Label("Step 1 of 4  —  Network Overview");
        stepLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        stepLabel.setTextFill(Color.web("#aaaacc"));

        // ── next button ──────────────────────────────────────
        nextBtn = new Button("Next  →");
        nextBtn.setPrefWidth(120);
        nextBtn.setPrefHeight(36);
        nextBtn.setStyle("""
            -fx-background-color: #0f3460;
            -fx-text-fill: white;
            -fx-font-size: 13px;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            """);
        nextBtn.setOnAction(e -> advance());

        HBox btnBar = new HBox(12, stepLabel, nextBtn);
        btnBar.setAlignment(Pos.CENTER_LEFT);
        btnBar.setPadding(new Insets(10, 16, 10, 16));
        btnBar.setStyle("-fx-background-color: #12122a;");

        // ── info panel (right side) ──────────────────────────
        infoBox = new VBox(10);
        infoBox.setPadding(new Insets(14));
        infoBox.setPrefWidth(300);
        infoBox.setStyle("-fx-background-color: #0f0f23;");

        ScrollPane infoScroll = new ScrollPane(infoBox);
        infoScroll.setFitToWidth(true);
        infoScroll.setPrefWidth(310);
        infoScroll.setStyle("-fx-background-color: #0f0f23;");

        // ── legend ───────────────────────────────────────────
        VBox legend = buildLegend();

        // ── layout ───────────────────────────────────────────
        HBox body = new HBox(8,
                new VBox(canvasPane, btnBar),
                new VBox(8, legend, infoScroll));
        VBox.setVgrow(infoScroll, Priority.ALWAYS);
        body.setPadding(new Insets(12));
        body.setStyle("-fx-background-color: #0a0a1a;");

        VBox root = new VBox(header, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        Scene scene = new Scene(root, 1200, 660);
        stage.setTitle("LAIR — Smart Logistics Network Simulator");
        stage.setScene(scene);
        stage.show();

        // draw initial state
        drawStep0();
    }

    // ── STEP ADVANCE ─────────────────────────────────────────

    private void advance() {
        step++;
        switch (step) {
            case 1 -> { drawStep1(); stepLabel.setText(
                    "Step 2 of 4  —  Initial Shipment Routes"); }
            case 2 -> { runPhase1and2(); drawStep2(); stepLabel.setText(
                    "Step 3 of 4  —  Delay Detected & Bottleneck Isolated"); }
            case 3 -> { runPhase3(); drawStep3(); stepLabel.setText(
                    "Step 4 of 4  —  Rerouted Paths"); nextBtn.setDisable(true); }
        }
    }

    // ── SIMULATION RUNNERS ────────────────────────────────────

    private void runPhase1and2() {
        try {
            engine.runPhase1(hubs, edges, shipments);
            engine.runPhase2(parser.getDelayEvent());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void runPhase3() {
        try { engine.runPhase3(1.0, 0.5); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ── DRAW STEP 0 — plain network ──────────────────────────

    private void drawStep0() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        clearCanvas(gc);
        drawAllEdges(gc, Color.web("#2a4a6a"), 1.5);
        drawAllHubs(gc, new HashSet<>(), null);
        updateInfo("Network Overview",
                "All hubs and possible routes in the logistics network.\n\n" +
                        hubs.size() + " hubs\n" +
                        edges.size() + " routes\n" +
                        shipments.size() + " shipments registered");
    }

    // ── DRAW STEP 1 — shipment paths ─────────────────────────

    private void drawStep1() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        clearCanvas(gc);
        drawAllEdges(gc, Color.web("#1a2a3a"), 1.0);

        // draw each shipment path
        for (int i = 0; i < shipments.size(); i++) {
            Color c = SHIP_COLORS[i % SHIP_COLORS.length];
            drawPath(gc, shipments.get(i).getPath(), c, 3.0, false);
        }

        drawAllHubs(gc, new HashSet<>(), null);

        // info panel
        StringBuilder sb = new StringBuilder("Initial Shipment Routes\n\n");
        for (int i = 0; i < shipments.size(); i++) {
            Shipment s = shipments.get(i);
            sb.append("● ").append(s.getId()).append("\n  ")
                    .append(String.join(" → ", s.getPath()))
                    .append("\n\n");
        }
        updateInfo("Initial Routes", sb.toString());
    }

    // ── DRAW STEP 2 — isolation ───────────────────────────────

    private void drawStep2() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        clearCanvas(gc);

        String bottleneck = engine.getBottleneckId();
        String delayHub   = parser.getDelayEvent().getHubId();

        // draw remaining edges (isolated hub's edges are gone)
        drawAllEdges(gc, Color.web("#2a4a6a"), 1.5);

        // draw original paths faded
        for (int i = 0; i < shipments.size(); i++) {
            Color c = SHIP_COLORS[i % SHIP_COLORS.length].deriveColor(
                    0, 1, 1, 0.25);
            drawPath(gc, shipments.get(i).getPath(), c, 2.0, false);
        }

        drawAllHubs(gc, Set.of(bottleneck), delayHub);

        // info
        updateInfo("Bottleneck Detected",
                "Delay event at hub: " + delayHub +
                        "\nInitial delay: " + parser.getDelayEvent().getDelay() + " units" +
                        "\n\nBottleneck detected: " + bottleneck +
                        "\nScore: " + engine.getBottleneckScore() +
                        "\n\nHub " + bottleneck + " has been ISOLATED." +
                        "\nAll edges removed." +
                        "\nUnion-Find rebuilt.");
    }

    // ── DRAW STEP 3 — reroutes ───────────────────────────────

    private void drawStep3() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        clearCanvas(gc);

        String bottleneck = engine.getBottleneckId();

        // faded original paths
        for (int i = 0; i < shipments.size(); i++) {
            Color c = SHIP_COLORS[i % SHIP_COLORS.length].deriveColor(
                    0, 1, 1, 0.2);
            drawPath(gc, shipments.get(i).getPath(), c, 1.5, false);
        }

        // bright new rerouted paths
        List<RerouteResult> results = engine.getResults();
        for (int i = 0; i < results.size(); i++) {
            Color c = SHIP_COLORS[i % SHIP_COLORS.length];
            drawPath(gc, results.get(i).getPath(), c, 3.5, true);
        }

        drawAllHubs(gc, Set.of(bottleneck), null);

        // info table
        StringBuilder sb = new StringBuilder("Rerouted Shipments\n\n");
        for (RerouteResult r : results) {
            double diff = r.getNewCost() - r.getOldCost();
            String arrow = diff <= 0 ? "▼ improved" : "▲ longer";
            sb.append(r.getId()).append("\n")
                    .append("  Before: ")
                    .append(String.join("→", r.getOriginalPath())).append("\n")
                    .append("  After:  ")
                    .append(String.join("→", r.getPath())).append("\n")
                    .append(String.format("  Cost: %.0f → %.0f  %s%n",
                            r.getOldCost(), r.getNewCost(), arrow))
                    .append("\n");
        }

        // load map
        sb.append("─────────────────\n");
        sb.append("Final Hub Loads\n\n");
        for (String hubId : HUB_POS.keySet()) {
            sb.append(String.format("  %-4s : %d%n",
                    hubId, graph.getLoad(hubId)));
        }

        updateInfo("Reroute Results", sb.toString());
    }

    // ── DRAW HELPERS ─────────────────────────────────────────

    private void clearCanvas(GraphicsContext gc) {
        gc.setFill(Color.web("#0d0d1a"));
        gc.fillRect(0, 0, CW, CH);
    }

    private void drawAllEdges(GraphicsContext gc, Color color, double width) {
        gc.setStroke(color);
        gc.setLineWidth(width);
        for (Edge e : edges) {
            double[] from = HUB_POS.get(e.getFrom());
            double[] to   = HUB_POS.get(e.getTo());
            if (from == null || to == null) continue;
            drawArrow(gc, from[0], from[1], to[0], to[1], color, width);

            // weight label
            double mx = (from[0] + to[0]) / 2;
            double my = (from[1] + to[1]) / 2;
            gc.setFill(Color.web("#556677"));
            gc.setFont(Font.font("Arial", 10));
            gc.fillText(String.valueOf(e.getWeight()), mx + 4, my - 4);
        }
    }

    private void drawPath(GraphicsContext gc, List<String> path,
                          Color color, double width, boolean reroute) {
        gc.setStroke(color);
        gc.setLineWidth(width);
        if (reroute) gc.setLineDashes(0);
        else gc.setLineDashes(0);

        for (int i = 0; i < path.size() - 1; i++) {
            double[] from = HUB_POS.get(path.get(i));
            double[] to   = HUB_POS.get(path.get(i + 1));
            if (from == null || to == null) continue;
            drawArrow(gc, from[0], from[1], to[0], to[1], color, width);
        }
    }

    private void drawArrow(GraphicsContext gc,
                           double x1, double y1,
                           double x2, double y2,
                           Color color, double width) {
        double hubR = 22;
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        double ux = dx / len;
        double uy = dy / len;

        // start and end adjusted for hub radius
        double sx = x1 + ux * hubR;
        double sy = y1 + uy * hubR;
        double ex = x2 - ux * hubR;
        double ey = y2 - uy * hubR;

        gc.setStroke(color);
        gc.setLineWidth(width);
        gc.strokeLine(sx, sy, ex, ey);

        // arrowhead
        double arrowLen = 10;
        double arrowAngle = 0.45;
        double angle = Math.atan2(ey - sy, ex - sx);
        gc.setFill(color);
        gc.fillPolygon(
                new double[]{
                        ex,
                        ex - arrowLen * Math.cos(angle - arrowAngle),
                        ex - arrowLen * Math.cos(angle + arrowAngle)
                },
                new double[]{
                        ey,
                        ey - arrowLen * Math.sin(angle - arrowAngle),
                        ey - arrowLen * Math.sin(angle + arrowAngle)
                },
                3
        );
    }

    private void drawAllHubs(GraphicsContext gc,
                             Set<String> isolated,
                             String delayHub) {
        double r = 22;
        for (Hub hub : hubs) {
            double[] pos = HUB_POS.get(hub.getId());
            if (pos == null) continue;

            double x = pos[0];
            double y = pos[1];

            Color fill;
            if (isolated != null && isolated.contains(hub.getId())) {
                fill = Color.web("#3a0000"); // dark red for isolated
            } else if (hub.getId().equals(delayHub)) {
                fill = Color.web("#8B0000"); // red for delay source
            } else {
                fill = TYPE_COLOR.getOrDefault(
                        hub.getType().name(), Color.GRAY);
            }

            // glow ring for isolated
            if (isolated != null && isolated.contains(hub.getId())) {
                gc.setFill(Color.web("#ff000033"));
                gc.fillOval(x - r - 6, y - r - 6,
                        (r + 6) * 2, (r + 6) * 2);
            }

            // hub circle
            gc.setFill(fill);
            gc.fillOval(x - r, y - r, r * 2, r * 2);

            // border
            gc.setStroke(isolated != null && isolated.contains(hub.getId())
                    ? Color.RED : Color.web("#ffffff44"));
            gc.setLineWidth(isolated != null
                    && isolated.contains(hub.getId()) ? 2.5 : 1.5);
            gc.strokeOval(x - r, y - r, r * 2, r * 2);

            // X mark for isolated
            if (isolated != null && isolated.contains(hub.getId())) {
                gc.setStroke(Color.RED);
                gc.setLineWidth(2);
                gc.strokeLine(x - 10, y - 10, x + 10, y + 10);
                gc.strokeLine(x + 10, y - 10, x - 10, y + 10);
            }

            // label
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            gc.fillText(hub.getId(), x - 10, y + 5);
        }
    }

    // ── INFO PANEL ───────────────────────────────────────────

    private void updateInfo(String title, String content) {
        infoBox.getChildren().clear();

        Label t = new Label(title);
        t.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        t.setTextFill(Color.web("#aaccff"));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a2a4a;");

        Label body = new Label(content);
        body.setFont(Font.font("Courier New", 12));
        body.setTextFill(Color.web("#ccddee"));
        body.setWrapText(true);

        infoBox.getChildren().addAll(t, sep, body);
    }

    // ── LEGEND ───────────────────────────────────────────────

    private VBox buildLegend() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #0f0f23; " +
                "-fx-border-color: #2a2a4a; " +
                "-fx-border-width: 1;");

        Label title = new Label("Hub Types");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#aaaacc"));
        box.getChildren().add(title);

        Map<String, Color> types = new LinkedHashMap<>();
        types.put("Warehouse",    TYPE_COLOR.get("WAREHOUSE"));
        types.put("Sorting",      TYPE_COLOR.get("SORTING"));
        types.put("Regional",     TYPE_COLOR.get("REGIONAL"));
        types.put("Distribution", TYPE_COLOR.get("DISTRIBUTION"));
        types.put("Delivery",     TYPE_COLOR.get("DELIVERY"));

        for (Map.Entry<String, Color> entry : types.entrySet()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.shape.Circle dot =
                    new javafx.scene.shape.Circle(7, entry.getValue());
            Label lbl = new Label(entry.getKey());
            lbl.setTextFill(Color.web("#aaaacc"));
            lbl.setFont(Font.font("Arial", 11));

            row.getChildren().addAll(dot, lbl);
            box.getChildren().add(row);
        }

        // shipment colors
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a2a4a;");
        box.getChildren().add(sep);

        Label sl = new Label("Shipments");
        sl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        sl.setTextFill(Color.web("#aaaacc"));
        box.getChildren().add(sl);

        String[] names = {"SH1", "SH2", "SH3"};
        for (int i = 0; i < names.length; i++) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            javafx.scene.shape.Circle dot =
                    new javafx.scene.shape.Circle(7, SHIP_COLORS[i]);
            Label lbl = new Label(names[i]);
            lbl.setTextFill(Color.web("#aaaacc"));
            lbl.setFont(Font.font("Arial", 11));
            row.getChildren().addAll(dot, lbl);
            box.getChildren().add(row);
        }

        return box;
    }
}