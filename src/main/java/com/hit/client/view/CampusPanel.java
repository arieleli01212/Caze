package com.hit.client.view;

import com.hit.dm.Building;
import com.hit.dm.Campus;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Isometric "2.5D" renderer for the campus.
 * <p>
 * The panel projects the world through {@link IsometricProjection}, so
 * tilting the projection 0..1 smoothly morphs the view from a flat
 * top-down map (tilt 0) to a classic isometric 3D look (tilt 1).
 * <p>
 * Buildings are extruded into boxes with three visible faces (top, south,
 * east) shaded by orientation. Walkways are drawn as lines on the ground
 * plane. The highlighted route is a thick stroke; an animated "walker"
 * marker travels along it courtesy of {@link RouteAnimator}.
 * <p>
 * User interactions handled here:
 * <ul>
 *   <li>Left click on a building → fires {@link BuildingClickListener}</li>
 *   <li>Left drag on empty space → pans the camera</li>
 *   <li>Mouse wheel → zoom in/out</li>
 * </ul>
 */
public class CampusPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /* Visual constants — tweakable in one place. */
    private static final int    BUILDING_WIDTH        = 50;
    private static final int    BUILDING_DEPTH        = 50;
    private static final double WALKER_HEIGHT         = 30.0;
    private static final double LABEL_LIFT            = 12.0;
    private static final Color  COLOR_GROUND_TOP      = new Color(238, 240, 232);
    private static final Color  COLOR_GROUND_BOTTOM   = new Color(214, 220, 205);
    private static final Color  COLOR_WALKWAY         = new Color(170, 170, 165);
    private static final Color  COLOR_HIGHLIGHT       = new Color(220,  70,  50);
    private static final Color  COLOR_BUILDING_TOP    = new Color(170, 188, 215);
    private static final Color  COLOR_BUILDING_SOUTH  = new Color(120, 138, 170);
    private static final Color  COLOR_BUILDING_EAST   = new Color( 90, 108, 140);
    private static final Color  COLOR_BUILDING_OUTLINE = new Color( 50,  60,  85);
    private static final Color  COLOR_SOURCE          = new Color( 70, 175,  95);
    private static final Color  COLOR_DEST            = new Color(220,  85,  60);
    private static final Color  COLOR_PATH            = new Color(245, 195,  60);
    private static final Color  COLOR_WALKER          = new Color(220,  35,  35);

    private final Campus               campus;
    private final IsometricProjection  projection = new IsometricProjection();
    private final RouteAnimator        animator;

    private List<String> highlightedPath = Collections.emptyList();
    private final Set<String> pathSet    = new HashSet<>();
    private Building source;
    private Building destination;

    private BuildingClickListener clickListener;

    private int    dragStartX, dragStartY;
    private double dragStartPanX, dragStartPanY;
    private boolean dragging;

    public CampusPanel(Campus campus) {
        this.campus  = campus;
        this.animator = new RouteAnimator(this::repaint);
        this.animator.setSpeed(120.0); // pixels per second — schematic units

        setPreferredSize(new Dimension(900, 620));
        setBackground(COLOR_GROUND_BOTTOM);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        wireMouse();
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { fitToWindow(); }
        });
    }

    /* ---------- public API used by ClientView ---------- */

    /** Smoothly tilt the camera. 0 = top-down, 1 = isometric. */
    public void setTilt(double tilt) {
        projection.setTilt(tilt);
        repaint();
    }

    public double getTilt() { return projection.getTilt(); }

    /** Multiplies the current zoom by {@code factor}. */
    public void zoomBy(double factor) {
        projection.setScale(projection.getScale() * factor);
        repaint();
    }

    /** Highlights a route returned from the server. */
    public void setHighlightedPath(List<String> path) {
        this.highlightedPath = (path == null) ? Collections.emptyList() : path;
        this.pathSet.clear();
        this.pathSet.addAll(this.highlightedPath);

        // Convert building names to schematic (x, y) waypoints for the animator.
        List<Point2D> waypoints = new ArrayList<>();
        for (String name : this.highlightedPath) {
            Building b = campus.getBuilding(name);
            if (b != null) waypoints.add(new Point2D.Double(b.getX(), b.getY()));
        }
        animator.setPath(waypoints);
        animator.start();
        repaint();
    }

    public void clearHighlight() {
        this.highlightedPath = Collections.emptyList();
        this.pathSet.clear();
        animator.stop();
        animator.setPath(Collections.emptyList());
        repaint();
    }

    public void setSource(Building source) {
        this.source = source;
        repaint();
    }

    public void setDestination(Building destination) {
        this.destination = destination;
        repaint();
    }

    public Building getSource()      { return source; }
    public Building getDestination() { return destination; }

    public void setClickListener(BuildingClickListener listener) {
        this.clickListener = listener;
    }

    /** Reset pan/zoom/animation. Useful when switching campus. */
    public void resetView() {
        animator.stop();
        fitToWindow();
        repaint();
    }

    public void setAnimationLooping(boolean looping) {
        animator.setLooping(looping);
    }

    public boolean isAnimating() { return animator.isRunning(); }

    public void startAnimation()  { animator.start(); }
    public void stopAnimation()   { animator.stop(); }

    /* ---------- mouse wiring ---------- */

    private void wireMouse() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragStartX    = e.getX();
                dragStartY    = e.getY();
                dragStartPanX = projection.getPanX();
                dragStartPanY = projection.getPanY();
                dragging = false;
            }

            @Override public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - dragStartX;
                int dy = e.getY() - dragStartY;
                if (Math.abs(dx) + Math.abs(dy) > 4) {
                    dragging = true;
                    projection.setPan(dragStartPanX + dx, dragStartPanY + dy);
                    repaint();
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (dragging) return;
                Building hit = pickBuildingAt(e.getX(), e.getY());
                if (hit != null && clickListener != null) {
                    clickListener.onBuildingClicked(hit);
                }
            }

            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                double factor = (e.getWheelRotation() < 0) ? 1.1 : 1.0 / 1.1;
                projection.setScale(projection.getScale() * factor);
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    /** Adjusts pan + scale so the whole campus fits the panel with margin. */
    public void fitToWindow() {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        if (campus.getBuildings().isEmpty()) return;

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Building b : campus.getBuildings().values()) {
            minX = Math.min(minX, b.getX());
            maxX = Math.max(maxX, b.getX());
            minY = Math.min(minY, b.getY());
            maxY = Math.max(maxY, b.getY());
        }
        double worldW = Math.max(1, maxX - minX);
        double worldH = Math.max(1, maxY - minY);

        // Use an iso-friendly scale: treat as orthographic for fitting, then add 25% margin.
        double margin = 0.85;
        double sx = (getWidth()  * margin) / worldW;
        double sy = (getHeight() * margin) / worldH;
        double scale = Math.min(sx, sy);
        projection.setScale(scale);

        // Reset pan first, project the world centre, then translate so it sits
        // at the panel's centre.
        projection.setPan(0, 0);
        Point2D centreProj = projection.project((minX + maxX) / 2.0,
                                                (minY + maxY) / 2.0,
                                                0.0);
        projection.setPan(getWidth() / 2.0 - centreProj.getX(),
                          getHeight() / 2.0 - centreProj.getY());
    }

    /* ---------- painting ---------- */

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (projection.getScale() == 1.0 && projection.getPanX() == 0
                && projection.getPanY() == 0) {
            // First paint — fit the campus to the panel.
            fitToWindow();
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHints(quality());
            paintGround(g2);
            paintWalkways(g2);
            paintRoute(g2);
            paintBuildings(g2);
            paintWalker(g2);
            paintHud(g2);
        } finally {
            g2.dispose();
        }
    }

    private RenderingHints quality() {
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        rh.put(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
        return rh;
    }

    private void paintGround(Graphics2D g2) {
        // Vertical gradient backdrop suggests sky / horizon when tilted.
        g2.setPaint(new GradientPaint(
                0, 0, COLOR_GROUND_TOP,
                0, getHeight(), COLOR_GROUND_BOTTOM));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private void paintWalkways(Graphics2D g2) {
        g2.setColor(COLOR_WALKWAY);
        g2.setStroke(new BasicStroke(2.2f));
        for (Building from : campus.getBuildings().values()) {
            for (var edge : campus.getGraph().getNeighbors(from.getId())) {
                Building to = campus.getBuilding(edge.getTarget());
                if (to == null) continue;
                if (from.getId() < to.getId()) {
                    Point2D a = projection.project(from.getX(), from.getY(), 0);
                    Point2D b = projection.project(to.getX(),   to.getY(),   0);
                    g2.drawLine((int) a.getX(), (int) a.getY(),
                                (int) b.getX(), (int) b.getY());
                }
            }
        }
    }

    private void paintRoute(Graphics2D g2) {
        if (highlightedPath.size() < 2) return;
        g2.setColor(COLOR_HIGHLIGHT);
        Stroke prev = g2.getStroke();
        g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < highlightedPath.size(); i++) {
            Building b = campus.getBuilding(highlightedPath.get(i));
            if (b == null) continue;
            Point2D p = projection.project(b.getX(), b.getY(), 0);
            if (i == 0) path.moveTo(p.getX(), p.getY());
            else        path.lineTo(p.getX(), p.getY());
        }
        g2.draw(path);
        g2.setStroke(prev);
    }

    private void paintBuildings(Graphics2D g2) {
        // Painter's algorithm — back to front.
        List<Building> sorted = new ArrayList<>(campus.getBuildings().values());
        sorted.sort(Comparator.comparingInt(Building::getY).reversed());

        for (Building b : sorted) {
            paintBox(g2, b);
        }
    }

    private void paintBox(Graphics2D g2, Building b) {
        double cx = b.getX();
        double cy = b.getY();
        double h  = b.getHeight();
        double hw = BUILDING_WIDTH  / 2.0;
        double hd = BUILDING_DEPTH  / 2.0;

        // 8 corners of the box (in world coords).
        Point2D bottomNW = projection.project(cx - hw, cy - hd, 0);
        Point2D bottomNE = projection.project(cx + hw, cy - hd, 0);
        Point2D bottomSE = projection.project(cx + hw, cy + hd, 0);
        Point2D bottomSW = projection.project(cx - hw, cy + hd, 0);
        Point2D topNW    = projection.project(cx - hw, cy - hd, h);
        Point2D topNE    = projection.project(cx + hw, cy - hd, h);
        Point2D topSE    = projection.project(cx + hw, cy + hd, h);
        Point2D topSW    = projection.project(cx - hw, cy + hd, h);

        ColorScheme scheme = colorSchemeFor(b);

        // South face (front, between bottomSW-bottomSE-topSE-topSW).
        Polygon south = poly(bottomSW, bottomSE, topSE, topSW);
        g2.setColor(scheme.south);
        g2.fillPolygon(south);
        g2.setColor(COLOR_BUILDING_OUTLINE);
        g2.drawPolygon(south);

        // East face (right, between bottomSE-bottomNE-topNE-topSE).
        Polygon east = poly(bottomSE, bottomNE, topNE, topSE);
        g2.setColor(scheme.east);
        g2.fillPolygon(east);
        g2.setColor(COLOR_BUILDING_OUTLINE);
        g2.drawPolygon(east);

        // Top face (always visible at any tilt).
        Polygon top = poly(topNW, topNE, topSE, topSW);
        g2.setColor(scheme.top);
        g2.fillPolygon(top);
        g2.setColor(COLOR_BUILDING_OUTLINE);
        g2.drawPolygon(top);

        // Building name label, lifted slightly above the roof so it's readable.
        Point2D label = projection.project(cx, cy, h + LABEL_LIFT);
        Font prevFont = g2.getFont();
        g2.setFont(prevFont.deriveFont(Font.BOLD, 12f));
        int textW = g2.getFontMetrics().stringWidth(b.getName());
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRoundRect((int) (label.getX() - textW / 2.0 - 4),
                         (int) (label.getY() - 14),
                         textW + 8, 18, 8, 8);
        g2.setColor(new Color(40, 50, 70));
        g2.drawString(b.getName(),
                      (int) (label.getX() - textW / 2.0),
                      (int) (label.getY()));
        g2.setFont(prevFont);
    }

    private ColorScheme colorSchemeFor(Building b) {
        if (source != null && b.getId() == source.getId()) {
            return new ColorScheme(brighter(COLOR_SOURCE, 1.2),
                                   COLOR_SOURCE,
                                   darker(COLOR_SOURCE, 0.75));
        }
        if (destination != null && b.getId() == destination.getId()) {
            return new ColorScheme(brighter(COLOR_DEST, 1.2),
                                   COLOR_DEST,
                                   darker(COLOR_DEST, 0.75));
        }
        if (pathSet.contains(b.getName())) {
            return new ColorScheme(brighter(COLOR_PATH, 1.15),
                                   COLOR_PATH,
                                   darker(COLOR_PATH, 0.8));
        }
        return new ColorScheme(COLOR_BUILDING_TOP, COLOR_BUILDING_SOUTH, COLOR_BUILDING_EAST);
    }

    private void paintWalker(Graphics2D g2) {
        Point2D pos = animator.currentPosition();
        if (pos == null) return;

        // A short pole + ball so the marker reads as a person, not just a dot.
        Point2D foot = projection.project(pos.getX(), pos.getY(), 0);
        Point2D head = projection.project(pos.getX(), pos.getY(), WALKER_HEIGHT);

        g2.setColor(COLOR_WALKER.darker());
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) foot.getX(), (int) foot.getY(),
                    (int) head.getX(), (int) head.getY());

        int r = 7;
        g2.setColor(COLOR_WALKER);
        g2.fillOval((int) head.getX() - r, (int) head.getY() - r, 2 * r, 2 * r);
        g2.setColor(Color.WHITE);
        g2.drawOval((int) head.getX() - r, (int) head.getY() - r, 2 * r, 2 * r);

        // Soft shadow at the foot.
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval((int) foot.getX() - 8, (int) foot.getY() - 3, 16, 6);
    }

    private void paintHud(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 215));
        g2.fillRoundRect(10, 10, 220, 56, 12, 12);
        g2.setColor(new Color(70, 80, 100));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(10, 10, 220, 56, 12, 12);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString("Click a building to set source, then destination.", 18, 28);
        g2.drawString(String.format("Tilt: %3d%%   Zoom: %3.0f%%",
                Math.round(projection.getTilt() * 100),
                projection.getScale() * 100), 18, 46);
        g2.drawString("Drag to pan · scroll to zoom",  18, 60);
    }

    /* ---------- helpers ---------- */

    private static Polygon poly(Point2D... pts) {
        Polygon p = new Polygon();
        for (Point2D pt : pts) p.addPoint((int) pt.getX(), (int) pt.getY());
        return p;
    }

    private static Color brighter(Color c, double factor) {
        int r = clamp((int) (c.getRed()   * factor));
        int g = clamp((int) (c.getGreen() * factor));
        int b = clamp((int) (c.getBlue()  * factor));
        return new Color(r, g, b);
    }

    private static Color darker(Color c, double factor) {
        return brighter(c, factor);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private Building pickBuildingAt(int sx, int sy) {
        // Test in painter order so a foreground building wins over one behind it.
        List<Building> sorted = new ArrayList<>(campus.getBuildings().values());
        sorted.sort(Comparator.comparingInt(Building::getY)); // closer last → check last → better
        Building best = null;
        for (Building b : sorted) {
            Point2D centre = projection.project(b.getX(), b.getY(), b.getHeight() / 2.0);
            double dx = sx - centre.getX();
            double dy = sy - centre.getY();
            // Forgiving radius — works at any zoom level.
            double radius = Math.max(18, 30 * projection.getScale());
            if (dx * dx + dy * dy <= radius * radius) {
                best = b;
            }
        }
        return best;
    }

    /** Listener interface for building clicks coming from the map. */
    public interface BuildingClickListener {
        void onBuildingClicked(Building building);
    }

    /** Tiny holder bundling the three face colours a building uses. */
    private record ColorScheme(Color top, Color south, Color east) {}
}
