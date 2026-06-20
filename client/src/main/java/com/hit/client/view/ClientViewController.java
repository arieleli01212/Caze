package com.hit.client.view;

import com.hit.client.controller.ClientController;
import com.hit.dm.Building;
import com.hit.dm.Campus;
import com.hit.protocol.HistoryEntry;
import com.hit.protocol.Mode;
import com.hit.protocol.RouteResponse;
import com.hit.protocol.Status;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main JavaFX view for the HIT Campus Navigation client (Part D).
 * <p>
 * Implements the MVC pattern per the assignment spec:
 * <ul>
 *   <li><b>Model</b>: {@link Campus}, {@link com.hit.protocol.RouteResponse}</li>
 *   <li><b>View</b>: this class — all JavaFX nodes live here</li>
 *   <li><b>Controller</b>: {@link ClientController} — all business logic delegated there</li>
 * </ul>
 * The view is <em>loosely coupled</em> from the controller: it only calls
 * methods on the controller and never accesses network or service classes directly.
 */
public class ClientViewController implements ClientController.RouteListener {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final Campus           campus;
    private final ClientController controller;

    /* toolbar */
    private ComboBox<String> fromCombo;
    private ComboBox<String> toCombo;
    private RadioButton      fastestRadio;
    private RadioButton      fewestRadio;
    private Button           findBtn;
    private Button           resetBtn;

    /* center views */
    private SchematicCanvas schematicCanvas;
    private LeafletMapView  leafletMapView;
    private TabPane         tabPane;

    /* 3D controls */
    private Slider   tiltSlider;
    private CheckBox animateCheck;

    /* history tab */
    private TableView<HistoryEntry>          historyTable;
    private ObservableList<HistoryEntry>     historyData;

    /* status bar */
    private Label statusLabel;

    public ClientViewController(Campus campus, ClientController controller) {
        this.campus     = campus;
        this.controller = controller;
        controller.setListener(this);
    }

    /** Builds the complete JavaFX scene and attaches it to the Stage. */
    public void buildScene(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1200, 780);
        applyStyle(scene);

        stage.setTitle("HIT Campus Navigation");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> Platform.exit());
        stage.show();

        // Set initial selection to first two buildings.
        List<String> names = campus.getBuildingNames();
        if (names.size() >= 2) {
            String f = names.get(0), t = names.get(1);
            fromCombo.setValue(f);
            toCombo.setValue(t);
            onFromChanged(f);
            onToChanged(t);
        }
        setStatus("Select buildings above and click Find Route.");
    }

    // ---- toolbar ----

    private HBox buildToolbar() {
        fromCombo = new ComboBox<>(FXCollections.observableList(campus.getBuildingNames()));
        toCombo   = new ComboBox<>(FXCollections.observableList(campus.getBuildingNames()));
        fromCombo.setPrefWidth(210); toCombo.setPrefWidth(210);

        ToggleGroup modeGroup = new ToggleGroup();
        fastestRadio = new RadioButton("Fastest (Dijkstra)");
        fewestRadio  = new RadioButton("Fewest stops (BFS)");
        fastestRadio.setToggleGroup(modeGroup);
        fewestRadio.setToggleGroup(modeGroup);
        fastestRadio.setSelected(true);

        findBtn  = new Button("Find Route");
        resetBtn = new Button("Reset");
        findBtn.getStyleClass().add("primary-btn");

        fromCombo.setOnAction(e -> onFromChanged(fromCombo.getValue()));
        toCombo.setOnAction(e   -> onToChanged(toCombo.getValue()));
        findBtn.setOnAction(e   -> sendCurrentRoute());
        resetBtn.setOnAction(e  -> resetAll());

        HBox bar = new HBox(8,
                label("From:"),  fromCombo,
                label("To:"),    toCombo,
                new Separator(Orientation.VERTICAL),
                fastestRadio,    fewestRadio,
                new Separator(Orientation.VERTICAL),
                findBtn,         resetBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.setStyle("-fx-background-color: #f0f2f5;-fx-border-color: #d0d3da;-fx-border-width: 0 0 1 0;");
        return bar;
    }

    // ---- center ----

    private TabPane buildCenter() {
        schematicCanvas = new SchematicCanvas(campus);
        schematicCanvas.setClickListener(this::onBuildingClick);

        leafletMapView = new LeafletMapView(campus);

        StackPane canvasWrapper = new StackPane(schematicCanvas);
        schematicCanvas.widthProperty().bind(canvasWrapper.widthProperty());
        schematicCanvas.heightProperty().bind(canvasWrapper.heightProperty());

        VBox tab3dContent = new VBox(canvasWrapper, build3dControls());
        VBox.setVgrow(canvasWrapper, Priority.ALWAYS);

        Tab tab3d  = new Tab("Campus 3D",  tab3dContent);
        Tab tabMap = new Tab("Real Map",   leafletMapView);
        Tab tabHist = new Tab("History",   buildHistoryPanel());
        tab3d.setClosable(false);
        tabMap.setClosable(false);
        tabHist.setClosable(false);

        // Leaflet measures the container at init time; by the time the user
        // clicks "Real Map" the WebView has been fully laid out, so we tell
        // Leaflet to re-measure and fill the available space.
        tabMap.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) leafletMapView.invalidateSize();
        });

        tabPane = new TabPane(tab3d, tabMap, tabHist);
        return tabPane;
    }

    private HBox build3dControls() {
        tiltSlider  = new Slider(0, 100, 100);
        animateCheck = new CheckBox("Animate walker");
        animateCheck.setSelected(true);

        Button zoomIn  = new Button("Zoom +");
        Button zoomOut = new Button("Zoom −");
        Button fit     = new Button("Fit");

        tiltSlider.setPrefWidth(160);
        tiltSlider.valueProperty().addListener((obs, ov, nv) ->
                schematicCanvas.setTilt(nv.doubleValue() / 100.0));

        zoomIn.setOnAction(e  -> schematicCanvas.zoomBy(1.2));
        zoomOut.setOnAction(e -> schematicCanvas.zoomBy(1.0 / 1.2));
        fit.setOnAction(e     -> schematicCanvas.resetView());
        animateCheck.setOnAction(e -> {
            if (animateCheck.isSelected()) schematicCanvas.startAnimation();
            else                            schematicCanvas.stopAnimation();
        });

        HBox row = new HBox(8,
                label("Tilt:"), tiltSlider,
                new Separator(Orientation.VERTICAL),
                zoomIn, zoomOut, fit,
                new Separator(Orientation.VERTICAL),
                animateCheck);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 12, 6, 12));
        row.setStyle("-fx-background-color: #f8f9fb;-fx-border-color: #d0d3da;-fx-border-width: 1 0 0 0;");
        return row;
    }

    private VBox buildHistoryPanel() {
        historyData  = FXCollections.observableArrayList();
        historyTable = new TableView<>(historyData);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<HistoryEntry, String> colTime = new TableColumn<>("Time");
        colTime.setCellValueFactory(cd -> new SimpleStringProperty(
                TS_FMT.format(Instant.ofEpochMilli(cd.getValue().getTimestampMillis()))));
        colTime.setPrefWidth(140);

        TableColumn<HistoryEntry, String> colFrom = new TableColumn<>("From");
        colFrom.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFromBuilding()));

        TableColumn<HistoryEntry, String> colTo = new TableColumn<>("To");
        colTo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getToBuilding()));

        TableColumn<HistoryEntry, String> colMode = new TableColumn<>("Mode");
        colMode.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getMode() != null ? cd.getValue().getMode().name() : ""));
        colMode.setPrefWidth(130);

        TableColumn<HistoryEntry, String> colCost = new TableColumn<>("Cost");
        colCost.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("%.0f", cd.getValue().getCost())));
        colCost.setPrefWidth(70);

        TableColumn<HistoryEntry, String> colPath = new TableColumn<>("Path");
        colPath.setCellValueFactory(cd -> {
            List<String> p = cd.getValue().getPath();
            String s = (p != null) ? String.join(" → ", p) : "";
            return new SimpleStringProperty(s);
        });

        historyTable.getColumns().addAll(colTime, colFrom, colTo, colMode, colCost, colPath);

        Button refreshBtn = new Button("Refresh");
        Button clearBtn   = new Button("Clear All");
        clearBtn.getStyleClass().add("danger-btn");

        refreshBtn.setOnAction(e -> loadHistory());
        clearBtn.setOnAction(e -> {
            controller.clearHistory(() -> Platform.runLater(this::loadHistory));
        });

        HBox btnRow = new HBox(8, refreshBtn, clearBtn);
        btnRow.setPadding(new Insets(8, 12, 8, 12));
        btnRow.setStyle("-fx-background-color: #f0f2f5;");

        VBox panel = new VBox(btnRow, historyTable);
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        return panel;
    }

    // ---- status bar ----

    private HBox buildStatusBar() {
        statusLabel = new Label("Ready.");
        statusLabel.setFont(Font.font("System", 13));
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.setStyle("-fx-background-color: #e8eaed;-fx-border-color: #d0d3da;-fx-border-width: 1 0 0 0;");
        return bar;
    }

    // ---- event handlers ----

    private void onFromChanged(String name) {
        Building b = campus.getBuilding(name);
        schematicCanvas.setSource(b);
        leafletMapView.setSource(b);
    }

    private void onToChanged(String name) {
        Building b = campus.getBuilding(name);
        schematicCanvas.setDestination(b);
        leafletMapView.setDestination(b);
    }

    private void onBuildingClick(Building b) {
        if (fromCombo.getValue() == null) {
            fromCombo.setValue(b.getName());
            onFromChanged(b.getName());
            setStatus("From: " + b.getName() + ". Now click or select your destination.");
        } else {
            toCombo.setValue(b.getName());
            onToChanged(b.getName());
            sendCurrentRoute();
        }
    }

    private void sendCurrentRoute() {
        String from = fromCombo.getValue();
        String to   = toCombo.getValue();
        if (from == null || to == null) return;
        if (from.equals(to)) {
            setStatus("Source and destination must be different buildings.");
            return;
        }
        Mode mode = fastestRadio.isSelected() ? Mode.FASTEST : Mode.FEWEST_SEGMENTS;
        setStatus("Querying server: " + from + " → " + to + " …");
        findBtn.setDisable(true);
        controller.requestRoute(from, to, mode);
    }

    private void resetAll() {
        schematicCanvas.clearHighlight();
        leafletMapView.clearSelection();
        fromCombo.setValue(null);
        toCombo.setValue(null);
        setStatus("Selection cleared. Choose buildings above.");
    }

    private void loadHistory() {
        controller.requestHistory(new ClientController.HistoryListener() {
            @Override
            public void onHistory(List<HistoryEntry> history) {
                Platform.runLater(() -> {
                    historyData.setAll(history);
                    setStatus("History loaded: " + history.size() + " entries.");
                });
            }
            @Override
            public void onHistoryError(String message) {
                Platform.runLater(() -> setStatus("Could not load history: " + message));
            }
        });
    }

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    // ---- ClientController.RouteListener ----

    @Override
    public void onResponse(RouteResponse response) {
        Platform.runLater(() -> {
            findBtn.setDisable(false);
            if (response == null) { setStatus("Empty response from server."); return; }
            if (response.getStatus() == Status.OK) {
                schematicCanvas.setHighlightedPath(response.getPath());
                leafletMapView.setHighlightedPath(response.getPath());
                String costLabel = fastestRadio.isSelected()
                        ? String.format("%.0f m", response.getCost())
                        : String.format("%d stops", (int) response.getCost());
                setStatus("Route: " + String.join(" → ", response.getPath())
                        + "    [" + costLabel + "]");
            } else if (response.getStatus() == Status.NO_PATH) {
                schematicCanvas.clearHighlight();
                leafletMapView.clearHighlight();
                setStatus("No path found between the selected buildings.");
            } else {
                schematicCanvas.clearHighlight();
                leafletMapView.clearHighlight();
                setStatus("Error: " + response.getErrorMessage());
            }
        });
    }

    @Override
    public void onError(String message) {
        Platform.runLater(() -> {
            findBtn.setDisable(false);
            schematicCanvas.clearHighlight();
            leafletMapView.clearHighlight();
            setStatus("Network error: " + message
                    + " — is the server running?");
        });
    }

    // ---- helpers ----

    private static Label label(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.NORMAL, 13));
        return l;
    }

    private static void applyStyle(Scene scene) {
        scene.getStylesheets().add("data:text/css,"
                + ".primary-btn{-fx-background-color:%234477cc;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-padding:5 14;-fx-background-radius:4;}"
                + ".primary-btn:hover{-fx-background-color:%235588dd;}"
                + ".danger-btn{-fx-background-color:%23cc4433;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-padding:5 14;-fx-background-radius:4;}"
                + ".danger-btn:hover{-fx-background-color:%23dd5544;}");
    }
}
