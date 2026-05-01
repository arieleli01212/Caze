package com.hit.client.view;

import javax.swing.Timer;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Coordinate-agnostic walker animator.
 * <p>
 * Owns a single Swing {@link Timer} firing on the EDT; on every tick it
 * advances {@link #currentPosition} along a polyline of {@link Point2D}
 * waypoints at a configurable speed. The "units" of x, y, and speed are
 * up to the caller — pixel coordinates for the 3D schematic view, lat/lon
 * for the real-map view.
 */
public class RouteAnimator {

    /** Milliseconds between animation frames (~33 fps). */
    private static final int FRAME_MILLIS = 30;

    private final Runnable onTick;
    private final Timer    timer;

    private List<Point2D> waypoints       = Collections.emptyList();
    private double[]      cumulativeLengths = new double[0];
    private double        totalLength       = 0.0;
    private double        distanceTravelled = 0.0;
    private double        speedUnitsPerSecond = 120.0;
    private boolean       looping           = true;

    public RouteAnimator(Runnable onTick) {
        this.onTick = onTick;
        this.timer  = new Timer(FRAME_MILLIS, e -> tick());
    }

    /** Sets the polyline the marker should walk. {@code null} or short paths stop the animation. */
    public void setPath(List<Point2D> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            stop();
            this.waypoints       = Collections.emptyList();
            cumulativeLengths    = new double[0];
            totalLength          = 0.0;
            distanceTravelled    = 0.0;
            return;
        }

        this.waypoints = new ArrayList<>(waypoints);
        cumulativeLengths = new double[waypoints.size() - 1];
        double acc = 0.0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Point2D a = waypoints.get(i);
            Point2D b = waypoints.get(i + 1);
            acc += a.distance(b);
            cumulativeLengths[i] = acc;
        }
        totalLength = acc;
        distanceTravelled = 0.0;
    }

    /**
     * Sets walking speed in the same units as the path coordinates.
     * Roughly: 120 for screen pixels per second, ~0.00015 for degrees of
     * latitude per second on a campus-sized OSM map.
     */
    public void setSpeed(double unitsPerSecond) {
        this.speedUnitsPerSecond = Math.max(1e-9, unitsPerSecond);
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void start() {
        if (waypoints.size() >= 2 && !timer.isRunning()) {
            timer.start();
        }
    }

    public void stop() {
        if (timer.isRunning()) timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    /**
     * @return the marker's current position, or {@code null} if there is
     *         no path or the animation finished and isn't looping
     */
    public Point2D currentPosition() {
        if (waypoints.size() < 2 || totalLength <= 0.0) return null;

        double d = distanceTravelled;
        int seg = 0;
        while (seg < cumulativeLengths.length && cumulativeLengths[seg] < d) {
            seg++;
        }
        if (seg >= cumulativeLengths.length) {
            return waypoints.get(waypoints.size() - 1);
        }

        double segStart = (seg == 0) ? 0.0 : cumulativeLengths[seg - 1];
        double segEnd   = cumulativeLengths[seg];
        double segLen   = Math.max(1e-12, segEnd - segStart);
        double t        = (d - segStart) / segLen;

        Point2D a = waypoints.get(seg);
        Point2D b = waypoints.get(seg + 1);
        return new Point2D.Double(
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t);
    }

    private void tick() {
        distanceTravelled += speedUnitsPerSecond * (FRAME_MILLIS / 1000.0);
        if (distanceTravelled >= totalLength) {
            if (looping) {
                distanceTravelled %= Math.max(1.0e-12, totalLength);
            } else {
                distanceTravelled = totalLength;
                stop();
            }
        }
        if (onTick != null) onTick.run();
    }
}
