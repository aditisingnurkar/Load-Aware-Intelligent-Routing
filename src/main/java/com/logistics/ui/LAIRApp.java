package com.logistics.ui;

import com.logistics.graph.LogisticsGraph;
import com.logistics.io.InputParser;
import com.logistics.model.*;
import com.logistics.simulation.ShipmentManager;
import com.logistics.simulation.SimulationEngine;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
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

    // Backend objects
    private LogisticsGraph   graph;
    private ShipmentManager  manager;
    private SimulationEngine engine;
    private InputParser      parser;

    private String              bottleneckId   = null;
    private String              delaySourceId  = null;
    private List<RerouteResult> rerouteResults = new ArrayList<>();

    // Snapshot of every shipment's planned path taken right after Phase 1,
    // before Phase 3 overwrites them via rerouting.
    private final Map<String, List<String>> originalPaths = new LinkedHashMap<>();

    private int step = 0;
    private static final int MAX_STEP = 4;

    // Shipment colours
    private static final Color[] PALETTE = {
            Color.web("#00BFFF"),
            Color.web("#FFD700"),
            Color.web("#FF6B6B"),
            Color.web("#ADFF2F"),
            Color.web("#FF69B4"),
            Color.web("#FFA500"),
            Color.web("#DA70D6"),
            Color.web("#40E0D0"),
    };

    // Stable index and colour assigned once per shipment ID
    private final Map<String, Integer> sIdx = new LinkedHashMap<>();
    private final Map<String, Color>   sCol = new LinkedHashMap<>();

    // UI nodes
    private Canvas          canvas;
    private Pane            canvasPane;
    private TextArea        logArea;
    private ListView<Label> shipmentListView;
    private VBox            resultsPanel;
    private VBox            legendPanel;
    private Button          nextStepBtn;
    private Label           stepLabel;

    // Hub positions stored as normalised [0..1] coordinates, scaled at draw time
    private final Map<String, double[]> hubNorm = new LinkedHashMap<>();

    private String inputFile = "sample_network.txt";

    @Override
    public void start(Stage stage) {
        for (String p : getParameters().getRaw()) {
            if (p.startsWith("--input=")) inputFile = p.substring(8);
            else if (!p.startsWith("--"))  inputFile = p;
        }
        buildLayout(stage);
        initSimulation();
        renderStep();
    }

    //Layout

    private void buildLayout(Stage stage) {
        canvas = new Canvas(800, 580);
        canvasPane = new Pane(canvas);
        canvasPane.setStyle("-fx-background-color:#000000;");

        // Resize canvas whenever the pane is resized
        ChangeListener<Number> onResize = (obs, o, n) -> {
            canvas.setWidth(canvasPane.getWidth());
            canvas.setHeight(canvasPane.getHeight());
            renderStep();
        };
        canvasPane.widthProperty().addListener(onResize);
        canvasPane.heightProperty().addListener(onResize);

        VBox right  = buildRightPanel();
        right.setPrefWidth(320);
        right.setMinWidth(260);

        HBox bottom = buildBottomBar();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0a0a;");
        root.setCenter(canvasPane);
        root.setRight(right);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 1150, 670);
        stage.setTitle("LAIR — Smart Logistics Network Simulator");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.show();
    }

    private VBox buildRightPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color:#111111;");

        legendPanel = new VBox(5);
        legendPanel.setPadding(new Insets(6));
        legendPanel.setStyle("-fx-background-color:#1a1a1a;-fx-border-color:#333;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(175);
        logArea.setStyle(
                "-fx-control-inner-background:#1a1a1a;" +
                        "-fx-text-fill:#cccccc;" +
                        "-fx-font-family:Monospaced;" +
                        "-fx-font-size:11;"
        );

        shipmentListView = new ListView<>();
        shipmentListView.setPrefHeight(150);
        shipmentListView.setStyle(
                "-fx-background-color:#1a1a1a;" +
                        "-fx-control-inner-background:#1a1a1a;" +
                        "-fx-border-color:#333;"
        );

        resultsPanel = new VBox(4);
        resultsPanel.setPadding(new Insets(6));
        resultsPanel.setStyle("-fx-background-color:#1a1a1a;-fx-border-color:#333;");

        ScrollPane resScroll = new ScrollPane(resultsPanel);
        resScroll.setFitToWidth(true);
        resScroll.setPrefHeight(135);
        resScroll.setStyle("-fx-background-color:#1a1a1a;-fx-background:#1a1a1a;");

        panel.getChildren().addAll(
                sec("▣  LEGEND"),         legendPanel,
                sec("▣  SIMULATION LOG"), logArea,
                sec("▣  SHIPMENTS"),      shipmentListView,
                sec("▣  RESULTS"),        resScroll
        );
        VBox.setVgrow(resScroll, Priority.ALWAYS);
        return panel;
    }

    private HBox buildBottomBar() {
        nextStepBtn = new Button("▶  NEXT STEP");
        nextStepBtn.setStyle(
                "-fx-background-color:#1e90ff;-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-font-size:13;" +
                        "-fx-padding:6 20 6 20;-fx-cursor:hand;"
        );
        nextStepBtn.setOnAction(e -> advanceStep());

        stepLabel = new Label("Step 0 / 4 — Initial Network");
        stepLabel.setStyle("-fx-text-fill:#888;-fx-font-size:12;");

        HBox bar = new HBox(16, nextStepBtn, stepLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.setStyle("-fx-background-color:#0d0d0d;-fx-border-color:#222;-fx-border-width:1 0 0 0;");
        return bar;
    }

    private Label sec(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#888;-fx-font-size:10;-fx-font-weight:bold;-fx-padding:4 0 2 0;");
        return l;
    }

    //Simulation init

    private void initSimulation() {
        graph   = new LogisticsGraph();
        manager = new ShipmentManager();
        engine  = new SimulationEngine(graph, manager);
        parser  = new InputParser();

        try {
            parser.parse(inputFile);
        } catch (IOException ex) {
            logArea.appendText("[ERROR] Cannot read: " + inputFile + "\n" + ex.getMessage() + "\n");
            nextStepBtn.setDisable(true);
            return;
        }

        engine.runPhase1(parser.getHubs(), parser.getEdges(), parser.getShipments());

        // Snapshot paths now - Phase 3 will overwrite them later
        for (Shipment s : manager.getAll()) {
            originalPaths.put(s.getId(), new ArrayList<>(s.getPath()));
        }

        if (parser.getDelayEvent() != null) {
            delaySourceId = parser.getDelayEvent().getHubId();
        }

        // Assign each shipment a stable colour index - used everywhere
        int ci = 0;
        for (Shipment s : manager.getAll()) {
            sIdx.put(s.getId(), ci);
            sCol.put(s.getId(), PALETTE[ci % PALETTE.length]);
            ci++;
        }

        buildHubNorm();
        rebuildLegend();

        logArea.appendText("[Phase 1] Network loaded.\n");
        logArea.appendText("  Hubs     : " + parser.getHubs().size() + "\n");
        logArea.appendText("  Routes   : " + parser.getEdges().size() + "\n");
        logArea.appendText("  Shipments: " + parser.getShipments().size() + "\n");

        refreshShipmentList();
        step = 0;
        updateStepLabel();
    }

    // step logic
    private void advanceStep() {
        step++;
        switch (step) {
            case 1 -> {
                // Just reveal the delay origin - no simulation work needed
                log("[Step 1] Delay origin: " + delaySourceId
                        + "  (initial delay=" + (parser.getDelayEvent() != null
                        ? parser.getDelayEvent().getDelay() : "?") + ")");
            }
            case 2 -> doPhase2();
            case 3 -> {
                log("[Step 3] Hub " + bottleneckId + " ISOLATED — all edges removed.");
            }
            case 4 -> doPhase3();
            default -> { step = MAX_STEP; nextStepBtn.setDisable(true); }
        }
        renderStep();
        refreshShipmentList();
        updateStepLabel();
        rebuildLegend();
    }

    private void doPhase2() {
        if (parser.getDelayEvent() == null) { log("[Phase 2] No delay event."); return; }
        engine.runPhase2(parser.getDelayEvent());
        bottleneckId = engine.getBottleneckId();
        log("[Phase 2] Bottleneck detected: " + bottleneckId);
        log("  Score: " + engine.getBottleneckScore());
    }

    private void doPhase3() {
        engine.runPhase3(1.0, 0.5);
        rerouteResults = engine.getResults();
        log("[Phase 3] Rerouting complete — " + rerouteResults.size() + " shipment(s) affected.");
        for (RerouteResult r : rerouteResults) {
            log("  " + r.getId() + ": "
                    + String.join(" → ", r.getOriginalPath())
                    + "  ⟹  "
                    + String.join(" → ", r.getPath()));
            log(String.format("    Cost %.1f → %.1f", r.getOldCost(), r.getNewCost()));
        }
        refreshResultsPanel();
    }

    // Render Dispatcher

    private void renderStep() {
        double W = canvas.getWidth();
        double H = canvas.getHeight();
        if (W < 1 || H < 1) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, W, H);

        drawEdges(gc, W, H);

        switch (step) {
            case 0 -> {
                // Initial: network + all active shipment routes, no markers
                drawAllOriginalPaths(gc, W, H);
            }
            case 1 -> {
                // Same routes, but now highlight where the delay started
                drawAllOriginalPaths(gc, W, H);
                drawDelayHub(gc, W, H);
            }
            case 2 -> {
                // Bottleneck found - show routes + bottleneck ring
                drawAllOriginalPaths(gc, W, H);
                drawBottleneckHighlight(gc, W, H);
            }
            case 3 -> {
                // Hub isolated - bottleneck replaced by isolated overlay
                drawIsolatedOverlay(gc, W, H);
            }
            case 4 -> {
                // Rerouting done - show old/new paths
                drawReroutes(gc, W, H);
                drawIsolatedOverlay(gc, W, H);
            }
        }

        drawHubs(gc, W, H);
        drawShipmentDots(gc, W, H);
        drawStepBadge(gc);
    }

    // hub positions

    // Normalised grid coordinates for the known hub IDs in sample_network.txt.
    // Unknown hubs are placed along the bottom row as a fallback.
    private void buildHubNorm() {
        hubNorm.clear();
        double[] TX = { 0.07, 0.26, 0.47, 0.67, 0.87 };
        double[][] TY = {
                { 0.46 },
                { 0.26, 0.66 },
                { 0.21, 0.69 },
                { 0.26, 0.66 },
                { 0.17, 0.46, 0.75 },
        };
        Map<String, int[]> known = new LinkedHashMap<>();
        known.put("W1", new int[]{0,0});
        known.put("S1", new int[]{1,0}); known.put("S2", new int[]{1,1});
        known.put("R1", new int[]{2,0}); known.put("R2", new int[]{2,1});
        known.put("D1", new int[]{3,0}); known.put("D2", new int[]{3,1});
        known.put("P1", new int[]{4,0}); known.put("P2", new int[]{4,1}); known.put("P3", new int[]{4,2});

        for (String id : graph.getAllHubIds()) {
            if (known.containsKey(id)) {
                int t = known.get(id)[0], r = known.get(id)[1];
                hubNorm.put(id, new double[]{ TX[t], TY[t][r] });
            }
        }
        int fb = 0;
        for (String id : graph.getAllHubIds()) {
            if (!hubNorm.containsKey(id)) {
                hubNorm.put(id, new double[]{ 0.05 + fb * 0.1, 0.92 });
                fb++;
            }
        }
    }

    // Convert normalised coordinate to actual canvas pixel
    private double[] hp(String id, double W, double H) {
        double[] n = hubNorm.get(id);
        return n == null ? null : new double[]{ n[0] * W, n[1] * H };
    }

    // Draw base edges

    private void drawEdges(GraphicsContext gc, double W, double H) {
        // Track labelled pairs so bidirectional edges don't get two overlapping weight labels
        Set<String> labelled = new HashSet<>();

        gc.setLineDashes(null);
        Map<String, List<Edge>> adj = graph.getAdjList();

        for (Map.Entry<String, List<Edge>> entry : adj.entrySet()) {
            String   from = entry.getKey();
            double[] fp   = hp(from, W, H);
            if (fp == null) continue;

            for (Edge e : entry.getValue()) {
                double[] tp = hp(e.getTo(), W, H);
                if (tp == null) continue;

                gc.setStroke(Color.web("#2a2a2a"));
                gc.setLineWidth(1.5);
                gc.strokeLine(fp[0], fp[1], tp[0], tp[1]);

                // Only label one direction per pair
                String rev = e.getTo() + ">" + from;
                String fwd = from + ">" + e.getTo();
                if (!labelled.contains(rev)) {
                    labelled.add(fwd);
                    double mx  = (fp[0] + tp[0]) / 2;
                    double my  = (fp[1] + tp[1]) / 2;
                    double dx  = tp[0] - fp[0];
                    double dy  = tp[1] - fp[1];
                    double len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                    // Offset perpendicular to the edge so the label doesn't sit on the line
                    double ox  = -dy / len * 10;
                    double oy  =  dx / len * 10;
                    gc.setFill(Color.web("#505050"));
                    gc.setFont(Font.font("SansSerif", 9));
                    gc.fillText(String.valueOf(e.getWeight()), mx + ox, my + oy);
                }
            }
        }
    }

    // Draw hubs

    private void drawHubs(GraphicsContext gc, double W, double H) {
        double r = Math.min(W, H) * 0.033;

        for (String id : graph.getAllHubIds()) {
            double[] p = hp(id, W, H);
            if (p == null) continue;

            boolean isolated = graph.isIsolated(id);
            Hub     hub      = getHub(id);
            int     load     = graph.getLoad(id);
            Color   fill     = hubFill(hub, isolated);

            // Faint glow to hint at how loaded a hub is
            if (load > 0 && !isolated) {
                double gr = r + 4 + load * 1.5;
                gc.setFill(Color.color(1, 0.6, 0, 0.07));
                gc.fillOval(p[0] - gr, p[1] - gr, gr * 2, gr * 2);
            }

            gc.setFill(fill);
            gc.fillOval(p[0] - r, p[1] - r, r * 2, r * 2);

            gc.setLineDashes(null);
            gc.setStroke(isolated ? Color.web("#444") : Color.web("#cccccc"));
            gc.setLineWidth(1.5);
            gc.strokeOval(p[0] - r, p[1] - r, r * 2, r * 2);

            gc.setFill(isolated ? Color.web("#555") : Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, Math.max(9, r * 0.5)));
            double tw = id.length() * r * 0.30;
            gc.fillText(id, p[0] - tw, p[1] + r * 0.18);

            // Small badge showing shipment count on this hub
            if (load > 0 && !isolated) {
                double bx = p[0] + r - 3, by = p[1] - r + 3;
                gc.setFill(Color.web("#FF4500"));
                gc.fillOval(bx - 7, by - 7, 14, 14);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 8));
                gc.fillText(String.valueOf(load), bx - (load > 9 ? 5 : 3), by + 3);
            }

            if (hub != null) {
                String t = hub.getType().name();
                gc.setFill(Color.web("#4a4a4a"));
                gc.setFont(Font.font("SansSerif", 8));
                gc.fillText(t.substring(0, Math.min(4, t.length())),
                        p[0] - r * 0.38, p[1] + r + 11);
            }
        }
    }

    // Draw: delay origin marker

    private void drawDelayHub(GraphicsContext gc, double W, double H) {
        if (delaySourceId == null) return;
        double[] p = hp(delaySourceId, W, H);
        if (p == null) return;
        double r = Math.min(W, H) * 0.04;

        // Fading concentric rings to suggest a ripple / pulse effect
        for (int i = 3; i >= 1; i--) {
            double ri = r + i * 7;
            gc.setStroke(Color.color(1.0, 0.65, 0.0, 0.11 * i));
            gc.setLineWidth(2.5);
            gc.setLineDashes(null);
            gc.strokeOval(p[0] - ri, p[1] - ri, ri * 2, ri * 2);
        }
        gc.setStroke(Color.web("#FFA500"));
        gc.setLineWidth(2.5);
        gc.strokeOval(p[0] - r, p[1] - r, r * 2, r * 2);

        gc.setFill(Color.web("#FFA500"));
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 10));
        // Place label above the hub, padded well clear of the rings
        gc.fillText("DELAY ORIGIN", p[0] - 36, p[1] - r - 10);
    }

    //  Draw: bottleneck ring

    private void drawBottleneckHighlight(GraphicsContext gc, double W, double H) {
        if (bottleneckId == null) return;
        double[] p = hp(bottleneckId, W, H);
        if (p == null) return;
        double r = Math.min(W, H) * 0.046;

        for (int i = 3; i >= 1; i--) {
            double ri = r + i * 8;
            gc.setStroke(Color.color(1, 0, 0, 0.13 * i));
            gc.setLineWidth(3);
            gc.setLineDashes(null);
            gc.strokeOval(p[0] - ri, p[1] - ri, ri * 2, ri * 2);
        }
        gc.setStroke(Color.RED);
        gc.setLineWidth(4);
        gc.strokeOval(p[0] - r, p[1] - r, r * 2, r * 2);

        gc.setFill(Color.RED);
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
        gc.fillText("BOTTLENECK", p[0] - 38, p[1] - r - 10);
    }

    // Draw: isolated overlay

    private void drawIsolatedOverlay(GraphicsContext gc, double W, double H) {
        if (bottleneckId == null) return;
        double[] p = hp(bottleneckId, W, H);
        if (p == null) return;
        double r = Math.min(W, H) * 0.044;

        gc.setStroke(Color.web("#FF4500"));
        gc.setLineWidth(3.5);
        gc.setLineDashes(8, 5);
        gc.strokeOval(p[0] - r, p[1] - r, r * 2, r * 2);
        gc.setLineDashes(null);

        double x = r * 0.56;
        gc.setStroke(Color.web("#FF4500", 0.85));
        gc.setLineWidth(2.5);
        gc.strokeLine(p[0] - x, p[1] - x, p[0] + x, p[1] + x);
        gc.strokeLine(p[0] + x, p[1] - x, p[0] - x, p[1] + x);

        gc.setFill(Color.web("#FF4500"));
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
        gc.fillText("ISOLATED", p[0] - 28, p[1] - r - 10);
    }

    // Draw: original paths

    // Draws every shipment's planned path. Failed shipments are skipped entirely.
    private void drawAllOriginalPaths(GraphicsContext gc, double W, double H) {
        for (Shipment s : manager.getAll()) {
            // Never draw paths for failed shipments
            if (s.getStatus() == ShipmentStatus.FAILED) continue;

            List<String> path = originalPaths.getOrDefault(s.getId(), s.getPath());
            int   idx = sIdx.getOrDefault(s.getId(), 0);
            Color col = sCol.getOrDefault(s.getId(), Color.WHITE);
            // Uniform thickness for all original routes
            drawPath(gc, path, col, 2.0, false, idx, W, H, true);
        }
    }

    //  Draw: rerouted paths

    private void drawReroutes(GraphicsContext gc, double W, double H) {
        // Collect which shipments were actually rerouted by Phase 3
        Set<String> rerouted = new HashSet<>();
        for (RerouteResult r : rerouteResults) rerouted.add(r.getId());

        // Draw old (grey dashed) paths for rerouted shipments first, so new paths render on top
        for (RerouteResult r : rerouteResults) {
            int idx = sIdx.getOrDefault(r.getId(), 0);
            drawPath(gc, r.getOriginalPath(),
                    Color.web("#505050"), 1.5, true, idx, W, H, false);
        }

        // Draw paths for shipments that were NOT rerouted and NOT failed
        for (Shipment s : manager.getAll()) {
            if (rerouted.contains(s.getId())) continue;
            if (s.getStatus() == ShipmentStatus.FAILED) continue;

            List<String> path = originalPaths.getOrDefault(s.getId(), s.getPath());
            if (path == null || path.size() < 2) continue;

            int   idx    = sIdx.getOrDefault(s.getId(), 0);
            Color col    = sCol.getOrDefault(s.getId(), Color.WHITE);
            Color dimmed = Color.color(col.getRed() * 0.7, col.getGreen() * 0.7, col.getBlue() * 0.7, 0.85);
            drawPath(gc, path, dimmed, 2.0, false, idx, W, H, true);
        }

        // Draw new (thick solid coloured) paths for rerouted shipments on top
        for (RerouteResult r : rerouteResults) {
            // Skip if the shipment ended up failed despite being in rerouteResults
            Shipment s = getShipment(r.getId());
            if (s != null && s.getStatus() == ShipmentStatus.FAILED) continue;

            int   idx = sIdx.getOrDefault(r.getId(), 0);
            Color col = sCol.getOrDefault(r.getId(), Color.WHITE);
            drawPath(gc, r.getPath(), col, 3.8, false, idx, W, H, true);
        }
    }
// Core path drawing
    private void drawPath(GraphicsContext gc, List<String> path, Color color,
                          double lineW, boolean dashed, int idx,
                          double W, double H, boolean arrowHead) {
        if (path == null || path.size() < 2) return;

        gc.setStroke(color);
        gc.setLineWidth(lineW);
        if (dashed) gc.setLineDashes(7, 5);
        else        gc.setLineDashes(null);

        double mag  = 14.0 + idx * 13.0;
        double sign = (idx % 2 == 0) ? 1.0 : -1.0;

        for (int i = 0; i < path.size() - 1; i++) {
            double[] fp = hp(path.get(i),     W, H);
            double[] tp = hp(path.get(i + 1), W, H);
            if (fp == null || tp == null) continue;

            double dx  = tp[0] - fp[0];
            double dy  = tp[1] - fp[1];
            double len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
            // Control point sits perpendicular to the midpoint
            double ox  = -dy / len * mag * sign;
            double oy  =  dx / len * mag * sign;
            double cx  = (fp[0] + tp[0]) / 2.0 + ox;
            double cy  = (fp[1] + tp[1]) / 2.0 + oy;

            gc.beginPath();
            gc.moveTo(fp[0], fp[1]);
            gc.quadraticCurveTo(cx, cy, tp[0], tp[1]);
            gc.stroke();

            if (arrowHead && i == path.size() - 2) {
                drawArrow(gc, cx, cy, tp[0], tp[1], color, lineW);
            }
        }
        gc.setLineDashes(null);
    }

    private void drawArrow(GraphicsContext gc, double cx, double cy,
                           double tx, double ty, Color color, double lw) {
        double dx  = tx - cx, dy = ty - cy;
        double len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        double ux  = dx / len, uy = dy / len;
        double sz  = lw * 3.5;
        gc.setFill(color);
        gc.beginPath();
        gc.moveTo(tx, ty);
        gc.lineTo(tx - ux * sz + uy * sz * 0.5, ty - uy * sz - ux * sz * 0.5);
        gc.lineTo(tx - ux * sz - uy * sz * 0.5, ty - uy * sz + ux * sz * 0.5);
        gc.closePath();
        gc.fill();
    }

    // Draw: shipment position dots

    private void drawShipmentDots(GraphicsContext gc, double W, double H) {
        Map<String, Integer> slotMap = new HashMap<>();
        double r = 6.5;

        for (Shipment s : manager.getAll()) {
            // Don't draw dots for failed shipments
            if (s.getStatus() == ShipmentStatus.FAILED) continue;

            Color    col  = sCol.getOrDefault(s.getId(), Color.WHITE);
            double[] p    = hp(s.getCurrentHub(), W, H);
            if (p == null) continue;

            // Spread multiple dots around the hub in a circle so they don't overlap
            int slot = slotMap.getOrDefault(s.getCurrentHub(), 0);
            slotMap.put(s.getCurrentHub(), slot + 1);
            double angle = slot * (2 * Math.PI / 6) - Math.PI / 2;
            double dist  = 32 + (slot / 6) * 12;
            double ox    = Math.cos(angle) * dist;
            double oy    = Math.sin(angle) * dist;

            gc.setFill(col);
            gc.fillOval(p[0] + ox - r, p[1] + oy - r, r * 2, r * 2);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.2);
            gc.setLineDashes(null);
            gc.strokeOval(p[0] + ox - r, p[1] + oy - r, r * 2, r * 2);

            gc.setFill(col.brighter());
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 8));
            gc.fillText(s.getId(), p[0] + ox - r + 1, p[1] + oy - r - 2);
        }
    }

    // Draw: step label badge (top-left of canvas)

    private void drawStepBadge(GraphicsContext gc) {
        String[] labels = {
                "Step 0 — Initial Network",
                "Step 1 — Delay Origin",
                "Step 2 — Bottleneck Detected",
                "Step 3 — Hub Isolated",
                "Step 4 — Paths Rerouted"
        };
        String txt = step < labels.length ? labels[step] : labels[labels.length - 1];
        gc.setFill(Color.color(0.09, 0.09, 0.09, 0.82));
        gc.fillRoundRect(6, 6, 228, 22, 6, 6);
        gc.setFill(Color.web("#aaaaaa"));
        gc.setFont(Font.font("SansSerif", 11));
        gc.fillText(txt, 12, 21);
    }

    //  Legend panel

    private void rebuildLegend() {
        legendPanel.getChildren().clear();

        // One colour swatch per shipment
        for (Shipment s : manager.getAll()) {
            Color  c   = sCol.getOrDefault(s.getId(), Color.WHITE);
            String hex = toHex(c);
            String statusSuffix = s.getStatus() == ShipmentStatus.FAILED ? " (FAILED)" : "";
            Label  sw  = new Label("■");
            sw.setStyle("-fx-text-fill:" + hex + ";-fx-font-size:14;");
            Label  lbl = new Label(s.getId() + statusSuffix);
            // Dim failed shipments in the legend too
            String labelColor = s.getStatus() == ShipmentStatus.FAILED ? "#555555" : hex;
            lbl.setStyle("-fx-text-fill:" + labelColor + ";-fx-font-weight:bold;-fx-font-size:11;");
            HBox row = new HBox(6, sw, lbl);
            row.setAlignment(Pos.CENTER_LEFT);
            legendPanel.getChildren().add(row);
        }

        if (step == 1) {
            legendPanel.getChildren().add(new Separator());
            legendPanel.getChildren().add(mkLegRow("◎", "#FFA500", "Delay origin"));
        }
        if (step == 2) {
            legendPanel.getChildren().add(new Separator());
            legendPanel.getChildren().add(mkLegRow("◎", "#FF0000", "Bottleneck"));
        }
        if (step >= 3) {
            legendPanel.getChildren().add(new Separator());
            legendPanel.getChildren().add(mkLegRow("✕", "#FF4500", "Isolated hub"));
        }
        if (step == 4) {
            legendPanel.getChildren().add(new Separator());
            legendPanel.getChildren().add(mkLegRow("╌╌", "#505050", "Old path"));
            legendPanel.getChildren().add(mkLegRow("──", "#00BFFF", "New path"));
        }
    }

    private HBox mkLegRow(String sym, String hexCol, String desc) {
        Label s = new Label(sym);
        s.setStyle("-fx-text-fill:" + hexCol + ";-fx-font-size:13;-fx-min-width:20;");
        Label d = new Label(desc);
        d.setStyle("-fx-text-fill:#999;-fx-font-size:11;");
        HBox row = new HBox(6, s, d);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // Right panel updates

    private void refreshShipmentList() {
        shipmentListView.getItems().clear();
        for (Shipment s : manager.getAll()) {
            Color  col = sCol.getOrDefault(s.getId(), Color.WHITE);
            // Grey out failed shipments in the list
            String hex = s.getStatus() == ShipmentStatus.FAILED ? "#555555" : toHex(col);
            Label  lbl = new Label(
                    String.format("%-5s → %-9s (%s)", s.getId(), s.getStatus(), s.getCurrentHub()));
            lbl.setStyle(
                    "-fx-font-weight:bold;" +
                            "-fx-text-fill:" + hex + ";" +
                            "-fx-font-size:11;" +
                            "-fx-font-family:Monospaced;" +
                            "-fx-padding:2 4 2 4;"
            );
            shipmentListView.getItems().add(lbl);
        }
    }

    private void refreshResultsPanel() {
        resultsPanel.getChildren().clear();
        if (rerouteResults.isEmpty()) {
            Label n = new Label("No reroutes performed.");
            n.setStyle("-fx-text-fill:#888;-fx-font-size:11;");
            resultsPanel.getChildren().add(n);
            return;
        }
        double tot0 = 0, tot1 = 0;
        for (RerouteResult r : rerouteResults) {
            Label l = new Label(String.format("%-5s  %.1f → %.1f",
                    r.getId(), r.getOldCost(), r.getNewCost()));
            l.setStyle("-fx-text-fill:#ccc;-fx-font-family:Monospaced;-fx-font-size:11;");
            resultsPanel.getChildren().add(l);
            tot0 += r.getOldCost();
            tot1 += r.getNewCost();
        }
        resultsPanel.getChildren().add(new Separator());
        Label tl = new Label(String.format("Total  %.1f → %.1f", tot0, tot1));
        tl.setStyle("-fx-text-fill:#eee;-fx-font-family:Monospaced;-fx-font-size:11;-fx-font-weight:bold;");
        double pct = tot0 > 0 ? (tot0 - tot1) / tot0 * 100 : 0;
        Label pl = new Label(pct >= 0
                ? String.format("Improvement: %.1f%%", pct)
                : String.format("Cost increase: %.1f%%", -pct));
        pl.setStyle("-fx-text-fill:#aaa;-fx-font-family:Monospaced;-fx-font-size:11;");
        resultsPanel.getChildren().addAll(tl, pl);
    }

    private void updateStepLabel() {
        String[] labels = {
                "Step 0 / 4 — Initial Network",
                "Step 1 / 4 — Delay Origin",
                "Step 2 / 4 — Bottleneck Detected",
                "Step 3 / 4 — Hub Isolated",
                "Step 4 / 4 — Paths Rerouted ✔"
        };
        stepLabel.setText(step < labels.length ? labels[step] : labels[labels.length - 1]);
        if (step >= MAX_STEP) nextStepBtn.setDisable(true);
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    // Helpers

    private Hub getHub(String id) {
        if (parser == null) return null;
        for (Hub h : parser.getHubs()) if (h.getId().equals(id)) return h;
        return null;
    }

    private Shipment getShipment(String id) {
        for (Shipment s : manager.getAll()) if (s.getId().equals(id)) return s;
        return null;
    }

    private Color hubFill(Hub hub, boolean isolated) {
        if (isolated) return Color.web("#161616");
        if (hub == null) return Color.web("#1e3050");
        return switch (hub.getType()) {
            case WAREHOUSE    -> Color.web("#0d2d5a");
            case SORTING      -> Color.web("#0d4030");
            case REGIONAL     -> Color.web("#2d1a50");
            case DISTRIBUTION -> Color.web("#50300d");
            case DELIVERY     -> Color.web("#0a4525");
        };
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed() * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue() * 255));
    }

    public static void main(String[] args) { launch(args); }
}