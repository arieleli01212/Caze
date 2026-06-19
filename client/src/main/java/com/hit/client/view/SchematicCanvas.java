package com.hit.client.view;

import com.hit.dm.Building;
import com.hit.dm.Campus;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * JavaFX Canvas that renders the HIT campus in an isometric "2.5D" view.
 * <p>
 * Mirrors the logic of the former Swing {@code CampusPanel} but uses the
 * JavaFX {@link GraphicsContext} API. The projection math is delegated to
 * {@link IsometricProjection} (unchanged — pure Java, no UI deps).
 * <p>
 * User interactions:
 * <ul>
 *   <li>Left-click on a building → fires the registered building click listener</li>
 *   <li>Left-drag on empty space → pans the camera</li>
 *   <li>Scroll → zoom in/out</li>
 * </ul>
 */
public class SchematicCanvas extends Canvas {

    private static final int    BW            = 50;   // building footprint width
    private static final int    BD            = 50;   // building footprint depth
    private static final double WALKER_HEIGHT = 30.0;
    private static final double LABEL_LIFT    = 12.0;

    // Colour palette
    private static final Color C_GROUND_TOP      = Color.web("#eef0e8");
    private static final Color C_GROUND_BOTTOM   = Color.web("#d6dced");
    private static final Color C_WALKWAY         = Color.web("#aaaaaa");
    private static final Color C_HIGHLIGHT       = Color.web("#dc4632");
    private static final Color C_BLDG_TOP        = Color.web("#aabcd7");
    private static final Color C_BLDG_SOUTH      = Color.web("#788aaa");
    private static final Color C_BLDG_EAST       = Color.web("#5a6c8c");
    private static final Color C_BLDG_OUTLINE    = Color.web("#32385f");
    private static final Color C_SOURCE          = Color.web("#46af5f");
    private static final Color C_DEST            = Color.web("#dc5544");
    private static final Color C_PATH            = Color.web("#f5c33c");
    private static final Color C_WALKER          = Color.web("#dc2323");

    private final Campus              campus;
    private final IsometricProjection proj     = new IsometricProjection();
    private final FxRouteAnimator     animator;

    private List<String>  highlightedPath = Collections.emptyList();
    private final Set<String> pathSet     = new HashSet<>();
    private Building source;
    private Building destination;

    private Consumer<Building> clickListener;

    private double dragStartX, dragStartY, dragStartPanX, dragStartPanY;
    private boolean dragging;
    private boolean fitted = false;

    public SchematicCanvas(Campus campus) {
        this.campus   = campus;
        this.animator = new FxRouteAnimator(this::redraw);
        animator.setSpeed(120.0);

        setWidth(900);
        setHeight(620);

        widthProperty().addListener(o  -> { fitted = false; redraw(); });
        heightProperty().addListener(o -> { fitted = false; redraw(); });

        wireMouse();
        redraw();
    }

    // ---- public API ----

    public void setTilt(double tilt)   { proj.setTilt(tilt); redraw(); }
    public void zoomBy(double factor)  { proj.setScale(proj.getScale() * factor); redraw(); }

    public void setHighlightedPath(List<String> path) {
        highlightedPath = (path == null) ? Collections.emptyList() : path;
        pathSet.clear();
        pathSet.addAll(highlightedPath);
        List<double[]> waypoints = new ArrayList<>();
        for (String name : highlightedPath) {
            Building b = campus.getBuilding(name);
            if (b != null) waypoints.add(new double[]{b.getX(), b.getY()});
        }
        animator.setPath(waypoints);
        animator.start();
        redraw();
    }

    public void clearHighlight() {
        highlightedPath = Collections.emptyList();
        pathSet.clear();
        animator.stop();
        animator.setPath(Collections.emptyList());
        redraw();
    }

    public void setSource(Building b)      { source      = b; redraw(); }
    public void setDestination(Building b) { destination = b; redraw(); }

    public void setClickListener(Consumer<Building> listener) { clickListener = listener; }

    public void resetView()       { fitted = false; redraw(); }
    public void startAnimation()  { animator.start(); }
    public void stopAnimation()   { animator.stop(); }
    public boolean isAnimating()  { return animator.isRunning(); }

    // ---- mouse ----

    private void wireMouse() {
        setOnMousePressed(e -> {
            dragStartX    = e.getX();
            dragStartY    = e.getY();
            dragStartPanX = proj.getPanX();
            dragStartPanY = proj.getPanY();
            dragging = false;
        });
        setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            if (Math.abs(dx) + Math.abs(dy) > 4) {
                dragging = true;
                proj.setPan(dragStartPanX + dx, dragStartPanY + dy);
                redraw();
            }
        });
        setOnMouseReleased(e -> {
            if (dragging) { dragging = false; return; }
            Building hit = pickBuilding(e.getX(), e.getY());
            if (hit != null && clickListener != null) clickListener.accept(hit);
        });
        setOnScroll(e -> {
            double factor = (e.getDeltaY() > 0) ? 1.1 : 1.0 / 1.1;
            proj.setScale(proj.getScale() * factor);
            redraw();
        });
    }

    // ---- fit ----

    private void fitToWindow() {
        if (getWidth() <= 0 || getHeight() <= 0 || campus.getBuildings().isEmpty()) return;
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Building b : campus.getBuildings().values()) {
            minX = Math.min(minX, b.getX()); maxX = Math.max(maxX, b.getX());
            minY = Math.min(minY, b.getY()); maxY = Math.max(maxY, b.getY());
        }
        double worldW = Math.max(1, maxX - minX);
        double worldH = Math.max(1, maxY - minY);
        double scale  = Math.min(getWidth() * 0.85 / worldW, getHeight() * 0.85 / worldH);
        proj.setScale(scale);
        proj.setPan(0, 0);
        Point2D centre = proj.project((minX + maxX) / 2.0, (minY + maxY) / 2.0, 0.0);
        proj.setPan(getWidth() / 2.0 - centre.getX(), getHeight() / 2.0 - centre.getY());
        fitted = true;
    }

    // ---- drawing ----

    private void redraw() {
        if (!fitted) fitToWindow();
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth(), h = getHeight();

        // Background gradient (simulate with two fills + clip)
        gc.clearRect(0, 0, w, h);
        gc.setFill(C_GROUND_BOTTOM);
        gc.fillRect(0, 0, w, h);

        drawWalkways(gc);
        drawRoute(gc);
        drawBuildings(gc);
        drawWalker(gc);
        drawHud(gc);
    }

    private void drawWalkways(GraphicsContext gc) {
        gc.setStroke(C_WALKWAY);
        gc.setLineWidth(2.2);
        for (Building from : campus.getBuildings().values()) {
            for (var edge : campus.getGraph().getNeighbors(from.getId())) {
                Building to = campus.getBuilding(edge.getTarget());
                if (to == null || from.getId() >= to.getId()) continue;
                Point2D a = proj.project(from.getX(), from.getY(), 0);
                Point2D b = proj.project(to.getX(),   to.getY(),   0);
                gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
            }
        }
    }

    private void drawRoute(GraphicsContext gc) {
        if (highlightedPath.size() < 2) return;
        gc.setStroke(C_HIGHLIGHT);
        gc.setLineWidth(5.5);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.beginPath();
        boolean first = true;
        for (String name : highlightedPath) {
            Building b = campus.getBuilding(name);
            if (b == null) continue;
            Point2D p = proj.project(b.getX(), b.getY(), 0);
            if (first) { gc.moveTo(p.getX(), p.getY()); first = false; }
            else          gc.lineTo(p.getX(), p.getY());
        }
        gc.stroke();
    }

    private void drawBuildings(GraphicsContext gc) {
        List<Building> sorted = new ArrayList<>(campus.getBuildings().values());
        sorted.sort(Comparator.comparingInt(Building::getY).reversed());
        for (Building b : sorted) drawBox(gc, b);
    }

    private void drawBox(GraphicsContext gc, Building b) {
        double cx = b.getX(), cy = b.getY(), h = b.getHeight();
        double hw = BW / 2.0, hd = BD / 2.0;

        Point2D bNW = proj.project(cx - hw, cy - hd, 0);
        Point2D bNE = proj.project(cx + hw, cy - hd, 0);
        Point2D bSE = proj.project(cx + hw, cy + hd, 0);
        Point2D bSW = proj.project(cx - hw, cy + hd, 0);
        Point2D tNW = proj.project(cx - hw, cy - hd, h);
        Point2D tNE = proj.project(cx + hw, cy - hd, h);
        Point2D tSE = proj.project(cx + hw, cy + hd, h);
        Point2D tSW = proj.project(cx - hw, cy + hd, h);

        Color[] scheme = colorScheme(b);

        // South face
        fillPoly(gc, scheme[1], bSW, bSE, tSE, tSW);
        strokePoly(gc, bSW, bSE, tSE, tSW);

        // East face
        fillPoly(gc, scheme[2], bSE, bNE, tNE, tSE);
        strokePoly(gc, bSE, bNE, tNE, tSE);

        // Top face
        fillPoly(gc, scheme[0], tNW, tNE, tSE, tSW);
        strokePoly(gc, tNW, tNE, tSE, tSW);

        // Label
        Point2D label = proj.project(cx, cy, h + LABEL_LIFT);
        String  name  = b.getName();
        gc.setFont(Font.font("System", FontWeight.BOLD, 11));
        double tw = computeTextWidth(name, 11);
        gc.setFill(Color.rgb(255, 255, 255, 0.85));
        gc.fillRoundRect(label.getX() - tw / 2 - 4, label.getY() - 14, tw + 8, 18, 8, 8);
        gc.setFill(Color.rgb(40, 50, 70));
        gc.fillText(name, label.getX() - tw / 2, label.getY());
    }

    private Color[] colorScheme(Building b) {
        if (source != null && b.getId() == source.getId())
            return new Color[]{ C_SOURCE.brighter(), C_SOURCE, C_SOURCE.darker() };
        if (destination != null && b.getId() == destination.getId())
            return new Color[]{ C_DEST.brighter(),   C_DEST,   C_DEST.darker()   };
        if (pathSet.contains(b.getName()))
            return new Color[]{ C_PATH.brighter(),   C_PATH,   C_PATH.darker()   };
        return new Color[]{ C_BLDG_TOP, C_BLDG_SOUTH, C_BLDG_EAST };
    }

    private void drawWalker(GraphicsContext gc) {
        double[] pos = animator.currentPosition();
        if (pos == null) return;
        Point2D foot = proj.project(pos[0], pos[1], 0);
        Point2D head = proj.project(pos[0], pos[1], WALKER_HEIGHT);
        gc.setStroke(C_WALKER.darker());
        gc.setLineWidth(3);
        gc.strokeLine(foot.getX(), foot.getY(), head.getX(), head.getY());
        double r = 7;
        gc.setFill(C_WALKER);
        gc.fillOval(head.getX() - r, head.getY() - r, 2 * r, 2 * r);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(head.getX() - r, head.getY() - r, 2 * r, 2 * r);
        gc.setFill(Color.rgb(0, 0, 0, 0.27));
        gc.fillOval(foot.getX() - 8, foot.getY() - 3, 16, 6);
    }

    private void drawHud(GraphicsContext gc) {
        gc.setFill(Color.rgb(255, 255, 255, 0.88));
        gc.fillRoundRect(10, 10, 260, 56, 12, 12);
        gc.setStroke(Color.rgb(70, 80, 100, 0.5));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(10, 10, 260, 56, 12, 12);
        gc.setFill(Color.rgb(50, 60, 80));
        gc.setFont(Font.font("System", 11));
        gc.fillText("Click a building to set source, then destination.", 18, 28);
        gc.fillText(String.format("Tilt: %3d%%   Zoom: %3.0f%%",
                Math.round(proj.getTilt() * 100), proj.getScale() * 100), 18, 44);
        gc.fillText("Drag to pan · scroll to zoom", 18, 60);
    }

    // ---- helpers ----

    private void fillPoly(GraphicsContext gc, Color fill, Point2D... pts) {
        double[] xs = new double[pts.length], ys = new double[pts.length];
        for (int i = 0; i < pts.length; i++) { xs[i] = pts[i].getX(); ys[i] = pts[i].getY(); }
        gc.setFill(fill);
        gc.fillPolygon(xs, ys, pts.length);
    }

    private void strokePoly(GraphicsContext gc, Point2D... pts) {
        double[] xs = new double[pts.length], ys = new double[pts.length];
        for (int i = 0; i < pts.length; i++) { xs[i] = pts[i].getX(); ys[i] = pts[i].getY(); }
        gc.setStroke(C_BLDG_OUTLINE);
        gc.setLineWidth(1.0);
        gc.strokePolygon(xs, ys, pts.length);
    }

    private Building pickBuilding(double sx, double sy) {
        List<Building> sorted = new ArrayList<>(campus.getBuildings().values());
        sorted.sort(Comparator.comparingInt(Building::getY));
        Building best = null;
        for (Building b : sorted) {
            Point2D c = proj.project(b.getX(), b.getY(), b.getHeight() / 2.0);
            double dx = sx - c.getX(), dy = sy - c.getY();
            double r  = Math.max(18, 30 * proj.getScale());
            if (dx * dx + dy * dy <= r * r) best = b;
        }
        return best;
    }

    private static double computeTextWidth(String text, double size) {
        return text.length() * size * 0.58; // approximate
    }
}
