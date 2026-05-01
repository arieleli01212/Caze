package com.hit.client.view;

import com.hit.algorithm.Graph;
import com.hit.dm.Building;
import com.hit.dm.Campus;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Real-map view of the campus, backed by a {@link JXMapViewer} that renders
 * OpenStreetMap or Esri satellite tiles.
 * <p>
 * Building markers, walkways, the highlighted route, and the animated walker
 * are drawn on top of the map via a single overlay painter. Clicking on the
 * map snaps to the nearest building (within ~30 pixels) and fires
 * {@link MapClickListener}.
 * <p>
 * Buildings without GPS coordinates are silently skipped — only the
 * GPS-tagged ones appear in this view. For schematic-only buildings, the
 * 3D {@link CampusPanel} is the right view.
 */
public class MapPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Tile providers we support — toggleable from the UI. */
    public enum TileStyle { STREETS, SATELLITE }

    /** Click listener fired when the user taps near a building. */
    public interface MapClickListener {
        void onBuildingClicked(Building building);
    }

    /* visual constants */
    private static final int    MARKER_RADIUS         = 9;
    private static final int    SOURCE_DEST_RADIUS    = 13;
    private static final int    PICK_RADIUS_PX        = 28;
    private static final Color  COLOR_WALKWAY         = new Color(255, 255, 255, 200);
    private static final Color  COLOR_WALKWAY_OUTLINE = new Color(  0,   0,   0, 110);
    private static final Color  COLOR_HIGHLIGHT       = new Color(225,  60,  45);
    private static final Color  COLOR_HIGHLIGHT_HALO  = new Color(255, 255, 255, 220);
    private static final Color  COLOR_BUILDING        = new Color( 60, 110, 200);
    private static final Color  COLOR_BUILDING_PATH   = new Color(245, 195,  60);
    private static final Color  COLOR_SOURCE          = new Color( 50, 175,  90);
    private static final Color  COLOR_DEST            = new Color(225,  60,  45);
    private static final Color  COLOR_LABEL_BG        = new Color(255, 255, 255, 230);
    private static final Color  COLOR_LABEL_TEXT      = new Color( 30,  40,  60);
    private static final Color  COLOR_WALKER_FILL     = new Color(225,  35,  35);

    private final Campus           campus;
    private final JXMapViewer      mapViewer  = new JXMapViewer();
    private final RouteAnimator    animator;

    private List<String> highlightedPath = Collections.emptyList();
    private final Set<String> pathSet    = new HashSet<>();
    private Building source;
    private Building destination;

    private TileStyle        currentStyle  = TileStyle.STREETS;
    private MapClickListener clickListener;

    public MapPanel(Campus campus) {
        this.campus   = campus;
        this.animator = new RouteAnimator(mapViewer::repaint);
        // Speed tuned so a typical campus path takes ~10 seconds end-to-end.
        this.animator.setSpeed(0.0004);

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(900, 620));
        add(mapViewer, BorderLayout.CENTER);
        mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Default to satellite imagery — the user asked for "real images".
        installTileFactory(TileStyle.SATELLITE);
        installInputHandlers();
        installOverlayPainter();

        // Frame the campus on first display.
        recenterOnCampus();
    }

    /* ---------- public API used by ClientView ---------- */

    public void setMapStyle(TileStyle style) {
        if (style != null && style != currentStyle) {
            installTileFactory(style);
        }
    }

    public TileStyle getMapStyle() {
        return currentStyle;
    }

    public void zoomIn()  { mapViewer.setZoom(Math.max(0, mapViewer.getZoom() - 1)); }
    public void zoomOut() { mapViewer.setZoom(mapViewer.getZoom() + 1); }

    public void recenterOnCampus() {
        GeoPosition centre = computeCampusCenter();
        if (centre != null) {
            mapViewer.setAddressLocation(centre);
            // JXMapViewer "zoom" is inverted: 0 = max detail. 1 ≈ block scale,
            // which fits a small campus nicely on both OSM and Esri tile sets.
            mapViewer.setZoom(1);
        }
    }

    public void setHighlightedPath(List<String> path) {
        this.highlightedPath = (path == null) ? Collections.emptyList() : path;
        this.pathSet.clear();
        this.pathSet.addAll(this.highlightedPath);

        // Animator works in (lon, lat) space — Point2D.Double(x=lon, y=lat).
        List<Point2D> waypoints = new ArrayList<>();
        for (String name : this.highlightedPath) {
            Building b = campus.getBuilding(name);
            if (b != null && b.hasGeoLocation()) {
                waypoints.add(new Point2D.Double(b.getLon(), b.getLat()));
            }
        }
        animator.setPath(waypoints);
        animator.start();
        mapViewer.repaint();
    }

    public void clearHighlight() {
        highlightedPath = Collections.emptyList();
        pathSet.clear();
        animator.stop();
        animator.setPath(Collections.emptyList());
        mapViewer.repaint();
    }

    public void setSource(Building source) {
        this.source = source;
        mapViewer.repaint();
    }

    public void setDestination(Building destination) {
        this.destination = destination;
        mapViewer.repaint();
    }

    public Building getSource()      { return source; }
    public Building getDestination() { return destination; }

    public void setClickListener(MapClickListener listener) {
        this.clickListener = listener;
    }

    public void startAnimation() { animator.start(); }
    public void stopAnimation()  { animator.stop(); }
    public boolean isAnimating() { return animator.isRunning(); }
    public void setAnimationLooping(boolean looping) { animator.setLooping(looping); }

    /* ---------- tile factory + input wiring ---------- */

    private void installTileFactory(TileStyle style) {
        TileFactoryInfo info = switch (style) {
            case STREETS   -> new OSMTileFactoryInfo();
            case SATELLITE -> esriWorldImageryInfo();
        };
        DefaultTileFactory factory = new DefaultTileFactory(info);
        factory.setThreadPoolSize(8);
        mapViewer.setTileFactory(factory);
        currentStyle = style;
        mapViewer.repaint();
    }

    /** Free Esri World Imagery tiles — high-resolution satellite/aerial photos. */
    private static TileFactoryInfo esriWorldImageryInfo() {
        return new TileFactoryInfo(
                /*minLevel*/ 0, /*maxLevel*/ 17, /*totalMapZoom*/ 19,
                /*tileSize*/ 256, /*xr2l*/ true, /*yt2b*/ true,
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile",
                "x", "y", "z") {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                int slippyZoom = getTotalMapZoom() - zoom;
                return this.baseURL + "/" + slippyZoom + "/" + y + "/" + x;
            }
        };
    }

    private void installInputHandlers() {
        // Built-in pan + wheel zoom.
        MouseInputListener pan = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(pan);
        mapViewer.addMouseMotionListener(pan);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));

        // Click-to-pick (only fires when there was no significant drag).
        mapViewer.addMouseListener(new MouseAdapter() {
            int pressX, pressY;
            @Override public void mousePressed(MouseEvent e) {
                pressX = e.getX();
                pressY = e.getY();
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                if (Math.abs(e.getX() - pressX) > 4 || Math.abs(e.getY() - pressY) > 4) {
                    return;
                }
                Building hit = pickNearestBuilding(e.getPoint());
                if (hit != null && clickListener != null) {
                    clickListener.onBuildingClicked(hit);
                }
            }
        });
    }

    /* ---------- overlay painting ---------- */

    private void installOverlayPainter() {
        Painter<JXMapViewer> overlay = (g, map, w, h) -> paintOverlay(g, map);
        mapViewer.setOverlayPainter(overlay);
    }

    private void paintOverlay(Graphics2D g0, JXMapViewer map) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Rectangle vp = map.getViewportBounds();
            g.translate(-vp.x, -vp.y);

            paintWalkways(g, map);
            paintRoute(g, map);
            paintBuildings(g, map);
            paintWalker(g, map);
        } finally {
            g.dispose();
        }
    }

    private void paintWalkways(Graphics2D g, JXMapViewer map) {
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Building from : campus.getBuildings().values()) {
            if (!from.hasGeoLocation()) continue;
            for (Graph.Edge edge : campus.getGraph().getNeighbors(from.getId())) {
                Building to = campus.getBuilding(edge.getTarget());
                if (to == null || !to.hasGeoLocation()) continue;
                if (from.getId() < to.getId()) {
                    Point2D pa = geoToPixel(map, from);
                    Point2D pb = geoToPixel(map, to);
                    g.setColor(COLOR_WALKWAY_OUTLINE);
                    g.drawLine((int) pa.getX(), (int) pa.getY(),
                               (int) pb.getX(), (int) pb.getY());
                }
            }
        }
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(COLOR_WALKWAY);
        for (Building from : campus.getBuildings().values()) {
            if (!from.hasGeoLocation()) continue;
            for (Graph.Edge edge : campus.getGraph().getNeighbors(from.getId())) {
                Building to = campus.getBuilding(edge.getTarget());
                if (to == null || !to.hasGeoLocation()) continue;
                if (from.getId() < to.getId()) {
                    Point2D pa = geoToPixel(map, from);
                    Point2D pb = geoToPixel(map, to);
                    g.drawLine((int) pa.getX(), (int) pa.getY(),
                               (int) pb.getX(), (int) pb.getY());
                }
            }
        }
    }

    private void paintRoute(Graphics2D g, JXMapViewer map) {
        if (highlightedPath.size() < 2) return;

        Path2D.Double path = new Path2D.Double();
        boolean started = false;
        for (String name : highlightedPath) {
            Building b = campus.getBuilding(name);
            if (b == null || !b.hasGeoLocation()) continue;
            Point2D p = geoToPixel(map, b);
            if (!started) { path.moveTo(p.getX(), p.getY()); started = true; }
            else            path.lineTo(p.getX(), p.getY());
        }

        // White halo for legibility on busy basemaps.
        g.setColor(COLOR_HIGHLIGHT_HALO);
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);

        g.setColor(COLOR_HIGHLIGHT);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
    }

    private void paintBuildings(Graphics2D g, JXMapViewer map) {
        Font prevFont = g.getFont();
        g.setFont(prevFont.deriveFont(Font.BOLD, 12f));

        for (Building b : campus.getBuildings().values()) {
            if (!b.hasGeoLocation()) continue;
            Point2D p = geoToPixel(map, b);
            int cx = (int) p.getX();
            int cy = (int) p.getY();

            boolean isSrc  = (source != null      && b.getId() == source.getId());
            boolean isDst  = (destination != null && b.getId() == destination.getId());
            boolean onPath = pathSet.contains(b.getName());

            Color fill;
            int   r;
            if      (isSrc) { fill = COLOR_SOURCE;        r = SOURCE_DEST_RADIUS; }
            else if (isDst) { fill = COLOR_DEST;          r = SOURCE_DEST_RADIUS; }
            else if (onPath){ fill = COLOR_BUILDING_PATH; r = MARKER_RADIUS; }
            else            { fill = COLOR_BUILDING;      r = MARKER_RADIUS; }

            // Drop shadow.
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval(cx - r + 2, cy - r + 3, r * 2, r * 2);
            // Marker.
            g.setColor(fill);
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.2f));
            g.drawOval(cx - r, cy - r, r * 2, r * 2);

            // Pill-shaped label above the marker.
            String label = b.getName();
            int textW = g.getFontMetrics().stringWidth(label);
            int padding = 6;
            int labelX = cx - textW / 2 - padding;
            int labelY = cy - r - 22;
            g.setColor(COLOR_LABEL_BG);
            g.fillRoundRect(labelX, labelY, textW + 2 * padding, 18, 10, 10);
            g.setColor(COLOR_LABEL_TEXT);
            g.drawString(label, cx - textW / 2, labelY + 13);
        }

        g.setFont(prevFont);
    }

    private void paintWalker(Graphics2D g, JXMapViewer map) {
        Point2D pos = animator.currentPosition(); // (lon, lat)
        if (pos == null) return;
        Point2D screen = map.getTileFactory()
                .geoToPixel(new GeoPosition(pos.getY(), pos.getX()), map.getZoom());

        int cx = (int) screen.getX();
        int cy = (int) screen.getY();

        // Pulsating outer halo.
        g.setColor(new Color(225, 35, 35, 70));
        g.fillOval(cx - 18, cy - 18, 36, 36);
        g.setColor(new Color(225, 35, 35, 130));
        g.fillOval(cx - 12, cy - 12, 24, 24);

        // Solid pin.
        g.setColor(COLOR_WALKER_FILL);
        g.fillOval(cx - 8, cy - 8, 16, 16);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(cx - 8, cy - 8, 16, 16);
    }

    /* ---------- helpers ---------- */

    /** Project a building's GPS coordinate to viewer pixel space. */
    private static Point2D geoToPixel(JXMapViewer map, Building b) {
        return map.getTileFactory()
                .geoToPixel(new GeoPosition(b.getLat(), b.getLon()), map.getZoom());
    }

    private GeoPosition computeCampusCenter() {
        double sumLat = 0, sumLon = 0;
        int count = 0;
        for (Building b : campus.getBuildings().values()) {
            if (b.hasGeoLocation()) {
                sumLat += b.getLat();
                sumLon += b.getLon();
                count++;
            }
        }
        if (count == 0) return null;
        return new GeoPosition(sumLat / count, sumLon / count);
    }

    private Building pickNearestBuilding(java.awt.Point clickPx) {
        Rectangle vp = mapViewer.getViewportBounds();
        // Convert click (panel coords) to world tile coords.
        double targetX = clickPx.x + vp.x;
        double targetY = clickPx.y + vp.y;

        Building best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (Building b : campus.getBuildings().values()) {
            if (!b.hasGeoLocation()) continue;
            Point2D p = geoToPixel(mapViewer, b);
            double dx = p.getX() - targetX;
            double dy = p.getY() - targetY;
            double d2 = dx * dx + dy * dy;
            if (d2 < bestDist) {
                bestDist = d2;
                best = b;
            }
        }
        if (best != null && bestDist <= PICK_RADIUS_PX * PICK_RADIUS_PX) {
            return best;
        }
        return null;
    }
}
