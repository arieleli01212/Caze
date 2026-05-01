package com.hit.client.view;

import com.hit.client.controller.ClientController;
import com.hit.dm.Building;
import com.hit.dm.Campus;
import com.hit.protocol.Mode;
import com.hit.protocol.RouteResponse;
import com.hit.protocol.Status;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/**
 * Main Swing window for the HIT Campus Navigation client.
 * <p>
 * The center area is a {@link CardLayout} swapping between two views:
 * <ul>
 *   <li><b>Map</b> — {@link MapPanel}, real OpenStreetMap or Esri satellite
 *       imagery centred on the HIT campus, with buildings as markers and
 *       the route drawn on top.</li>
 *   <li><b>3D</b> — {@link CampusPanel}, the hand-rolled isometric view
 *       with extruded buildings and a tilt slider.</li>
 * </ul>
 * Both views share the same source/destination state and route highlight,
 * so switching modes keeps the user's selection in place.
 */
public class ClientView extends JFrame implements ClientController.Listener {

    private static final long serialVersionUID = 1L;

    private static final String CARD_MAP = "MAP";
    private static final String CARD_3D  = "3D";

    private final ClientController controller;
    private final Campus           campus;

    /* shared toolbar */
    private final JComboBox<String> fromCombo;
    private final JComboBox<String> toCombo;
    private final JRadioButton      fastestBtn;
    private final JRadioButton      fewestBtn;
    private final JButton           findBtn;
    private final JButton           resetBtn;
    private final JLabel            statusLabel;

    /* center cards */
    private final CardLayout  centerCards = new CardLayout();
    private final JPanel      centerHost  = new JPanel(centerCards);
    private final MapPanel    mapPanel;
    private final CampusPanel campusPanel;

    /* sidebar */
    private final JRadioButton  modeMapBtn;
    private final JRadioButton  mode3DBtn;
    private final JRadioButton  styleStreetsBtn;
    private final JRadioButton  styleSatelliteBtn;
    private final JButton       recenterBtn;
    private final JSlider       tiltSlider;
    private final JButton       zoomInBtn;
    private final JButton       zoomOutBtn;
    private final JButton       fitBtn;
    private final JToggleButton animateBtn;

    private enum NextSlot { SOURCE, DESTINATION }
    private NextSlot nextSlot = NextSlot.SOURCE;

    public ClientView(Campus campus, ClientController controller) {
        super("HIT Campus Navigation");
        this.campus     = campus;
        this.controller = controller;
        this.controller.setListener(this);

        List<String> names = campus.getBuildingNames();
        this.fromCombo = new JComboBox<>(names.toArray(new String[0]));
        this.toCombo   = new JComboBox<>(names.toArray(new String[0]));
        if (names.size() > 1) toCombo.setSelectedIndex(1);

        this.fastestBtn = new JRadioButton("Fastest (Dijkstra)", true);
        this.fewestBtn  = new JRadioButton("Fewest segments (BFS)");
        ButtonGroup modes = new ButtonGroup();
        modes.add(fastestBtn);
        modes.add(fewestBtn);

        this.findBtn     = new JButton("Find Route");
        this.resetBtn    = new JButton("Reset");
        this.statusLabel = new JLabel(" ");

        this.mapPanel    = new MapPanel(campus);
        this.campusPanel = new CampusPanel(campus);

        this.modeMapBtn        = new JRadioButton("Map (real)", true);
        this.mode3DBtn         = new JRadioButton("3D schematic");
        this.styleStreetsBtn   = new JRadioButton("Streets");
        this.styleSatelliteBtn = new JRadioButton("Satellite", true);
        this.recenterBtn       = new JButton("Recenter");
        this.tiltSlider        = new JSlider(0, 100, 100);
        this.zoomInBtn         = new JButton("Zoom +");
        this.zoomOutBtn        = new JButton("Zoom −");
        this.fitBtn            = new JButton("Fit");
        this.animateBtn        = new JToggleButton("Animate", true);

        Building initialFrom = campus.getBuilding((String) fromCombo.getSelectedItem());
        Building initialTo   = campus.getBuilding((String) toCombo.getSelectedItem());
        mapPanel.setSource(initialFrom);
        mapPanel.setDestination(initialTo);
        campusPanel.setSource(initialFrom);
        campusPanel.setDestination(initialTo);

        buildLayout();
        wireEvents();

        setStatus("Click two buildings on the map to find a route — or use the dropdowns.");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    /* ---------- layout ---------- */

    private void buildLayout() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildToolbar(), BorderLayout.NORTH);

        centerHost.add(mapPanel,    CARD_MAP);
        centerHost.add(campusPanel, CARD_3D);
        getContentPane().add(centerHost, BorderLayout.CENTER);

        getContentPane().add(buildSidebar(),   BorderLayout.EAST);
        getContentPane().add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        toolbar.add(label("From:"));
        toolbar.add(fromCombo);
        toolbar.add(label("To:"));
        toolbar.add(toCombo);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(fastestBtn);
        toolbar.add(fewestBtn);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(findBtn);
        toolbar.add(resetBtn);
        return toolbar;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        side.setPreferredSize(new Dimension(200, 0));

        section(side, "View");
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(modeMapBtn);
        viewGroup.add(mode3DBtn);
        side.add(modeMapBtn);
        side.add(mode3DBtn);

        section(side, "Map style");
        ButtonGroup styleGroup = new ButtonGroup();
        styleGroup.add(styleStreetsBtn);
        styleGroup.add(styleSatelliteBtn);
        side.add(styleStreetsBtn);
        side.add(styleSatelliteBtn);
        sidebarButton(side, recenterBtn);

        section(side, "Map zoom");
        sidebarButton(side, zoomInBtn);
        sidebarButton(side, zoomOutBtn);

        section(side, "3D tilt (2D ↔ 3D)");
        tiltSlider.setMajorTickSpacing(50);
        tiltSlider.setPaintTicks(true);
        tiltSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(tiltSlider);
        sidebarButton(side, fitBtn);

        section(side, "Animation");
        sidebarButton(side, animateBtn);

        side.add(Box.createVerticalGlue());
        return side;
    }

    private static void section(JPanel side, String title) {
        side.add(Box.createVerticalStrut(10));
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11.5f));
        l.setForeground(new Color(70, 80, 100));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(l);
        side.add(Box.createVerticalStrut(4));
    }

    private static void sidebarButton(JPanel side, javax.swing.AbstractButton b) {
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(170, 28));
        side.add(b);
        side.add(Box.createVerticalStrut(2));
    }

    private JPanel buildStatusBar() {
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        statusLabel.setForeground(new Color(60, 70, 90));
        JPanel bar = new JPanel(new BorderLayout());
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /* ---------- event wiring ---------- */

    private void wireEvents() {
        findBtn.addActionListener(e -> sendCurrentRoute());
        resetBtn.addActionListener(e -> {
            mapPanel.clearHighlight();
            campusPanel.clearHighlight();
            mapPanel.setSource(null);
            mapPanel.setDestination(null);
            campusPanel.setSource(null);
            campusPanel.setDestination(null);
            nextSlot = NextSlot.SOURCE;
            setStatus("Selection cleared. Click a building to set the source.");
        });

        fromCombo.addActionListener(e -> {
            Building b = campus.getBuilding((String) fromCombo.getSelectedItem());
            mapPanel.setSource(b);
            campusPanel.setSource(b);
        });
        toCombo.addActionListener(e -> {
            Building b = campus.getBuilding((String) toCombo.getSelectedItem());
            mapPanel.setDestination(b);
            campusPanel.setDestination(b);
        });

        modeMapBtn.addActionListener(e -> centerCards.show(centerHost, CARD_MAP));
        mode3DBtn.addActionListener(e -> centerCards.show(centerHost, CARD_3D));

        styleStreetsBtn.addActionListener(e   -> mapPanel.setMapStyle(MapPanel.TileStyle.STREETS));
        styleSatelliteBtn.addActionListener(e -> mapPanel.setMapStyle(MapPanel.TileStyle.SATELLITE));
        recenterBtn.addActionListener(e       -> mapPanel.recenterOnCampus());

        zoomInBtn.addActionListener(e -> {
            if (modeMapBtn.isSelected()) mapPanel.zoomIn();
            else                          campusPanel.zoomBy(1.2);
        });
        zoomOutBtn.addActionListener(e -> {
            if (modeMapBtn.isSelected()) mapPanel.zoomOut();
            else                          campusPanel.zoomBy(1 / 1.2);
        });
        fitBtn.addActionListener(e -> {
            campusPanel.resetView();
            mapPanel.recenterOnCampus();
        });

        tiltSlider.addChangeListener(e -> campusPanel.setTilt(tiltSlider.getValue() / 100.0));

        animateBtn.addActionListener(e -> {
            if (animateBtn.isSelected()) {
                mapPanel.startAnimation();
                campusPanel.startAnimation();
            } else {
                mapPanel.stopAnimation();
                campusPanel.stopAnimation();
            }
        });

        // Click-to-select on either view.
        mapPanel.setClickListener(this::onMapClick);
        campusPanel.setClickListener(this::onMapClick);
    }

    private void onMapClick(Building b) {
        if (b == null) return;
        switch (nextSlot) {
            case SOURCE -> {
                fromCombo.setSelectedItem(b.getName());
                mapPanel.setSource(b);
                campusPanel.setSource(b);
                mapPanel.clearHighlight();
                campusPanel.clearHighlight();
                nextSlot = NextSlot.DESTINATION;
                setStatus("Source: " + b.getName() + ". Now click your destination.");
            }
            case DESTINATION -> {
                toCombo.setSelectedItem(b.getName());
                mapPanel.setDestination(b);
                campusPanel.setDestination(b);
                nextSlot = NextSlot.SOURCE;
                sendCurrentRoute();
            }
        }
    }

    private void sendCurrentRoute() {
        String from = (String) fromCombo.getSelectedItem();
        String to   = (String) toCombo.getSelectedItem();
        if (from == null || to == null) return;
        if (from.equals(to)) {
            setStatus("Source and destination are the same — pick a different building.");
            return;
        }
        Mode mode = fastestBtn.isSelected() ? Mode.FASTEST : Mode.FEWEST_SEGMENTS;
        setStatus("Querying server for route " + from + " → " + to + " …");
        findBtn.setEnabled(false);
        controller.requestRoute(from, to, mode);
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    /* ---------- ClientController.Listener ---------- */

    @Override
    public void onResponse(RouteResponse response) {
        SwingUtilities.invokeLater(() -> {
            findBtn.setEnabled(true);
            if (response == null) {
                setStatus("Empty response from server.");
                return;
            }
            if (response.getStatus() == Status.OK) {
                mapPanel.setHighlightedPath(response.getPath());
                campusPanel.setHighlightedPath(response.getPath());
                if (animateBtn.isSelected()) {
                    mapPanel.startAnimation();
                    campusPanel.startAnimation();
                }
                String costLabel = (fastestBtn.isSelected())
                        ? String.format("walking distance %.0f", response.getCost())
                        : String.format("%d segments", (int) response.getCost());
                setStatus("Route: " + String.join(" → ", response.getPath())
                        + "    [" + costLabel + "]");
            } else if (response.getStatus() == Status.NO_PATH) {
                mapPanel.clearHighlight();
                campusPanel.clearHighlight();
                setStatus("No path between the selected buildings.");
            } else {
                mapPanel.clearHighlight();
                campusPanel.clearHighlight();
                setStatus("Server error: " + response.getErrorMessage());
            }
        });
    }

    @Override
    public void onError(String message) {
        SwingUtilities.invokeLater(() -> {
            findBtn.setEnabled(true);
            mapPanel.clearHighlight();
            campusPanel.clearHighlight();
            setStatus("Network error: " + message
                    + "    (is the server running on the right port?)");
        });
    }
}
