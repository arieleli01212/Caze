package com.hit.client.view;

import javafx.animation.AnimationTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JavaFX route walker animator.
 * <p>
 * Uses {@link AnimationTimer} (called on every frame by the JavaFX pulse
 * scheduler) to advance a marker along a polyline of (x, y) waypoints at a
 * configurable speed. When the marker reaches the end it loops back to the start.
 * <p>
 * Framework-agnostic coordinate space — the caller decides whether the
 * waypoints are screen pixels or world (lat/lon) units.
 */
public class FxRouteAnimator {

    private static final double DEFAULT_SPEED = 120.0; // units per second

    private final Runnable onTick;

    private List<double[]> waypoints       = Collections.emptyList();
    private double[]       cumLengths      = new double[0];
    private double         totalLength     = 0.0;
    private double         distTravelled   = 0.0;
    private double         speed           = DEFAULT_SPEED;
    private boolean        looping         = true;
    private long           lastNanos       = -1;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastNanos < 0) { lastNanos = now; return; }
            double dt = (now - lastNanos) / 1_000_000_000.0;
            lastNanos = now;
            tick(dt);
        }
    };

    public FxRouteAnimator(Runnable onTick) {
        this.onTick = onTick;
    }

    /** Sets the polyline the marker should walk. Stops and resets on path change. */
    public void setPath(List<double[]> points) {
        stop();
        if (points == null || points.size() < 2) {
            waypoints   = Collections.emptyList();
            cumLengths  = new double[0];
            totalLength = 0.0;
            distTravelled = 0.0;
            return;
        }
        waypoints  = new ArrayList<>(points);
        cumLengths = new double[points.size() - 1];
        double acc = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            double dx = points.get(i + 1)[0] - points.get(i)[0];
            double dy = points.get(i + 1)[1] - points.get(i)[1];
            acc += Math.sqrt(dx * dx + dy * dy);
            cumLengths[i] = acc;
        }
        totalLength   = acc;
        distTravelled = 0.0;
    }

    public void setSpeed(double unitsPerSec) { this.speed = Math.max(1e-9, unitsPerSec); }
    public void setLooping(boolean looping)  { this.looping = looping; }

    public void start() {
        if (waypoints.size() >= 2) { lastNanos = -1; timer.start(); }
    }

    public void stop() { timer.stop(); lastNanos = -1; }
    public boolean isRunning() { return waypoints.size() >= 2 && timer != null; }

    /**
     * @return current [x, y] of the marker, or {@code null} if no path is set
     */
    public double[] currentPosition() {
        if (waypoints.size() < 2 || totalLength <= 0) return null;
        double d   = distTravelled;
        int    seg = 0;
        while (seg < cumLengths.length && cumLengths[seg] < d) seg++;
        if (seg >= cumLengths.length) {
            double[] last = waypoints.get(waypoints.size() - 1);
            return new double[]{last[0], last[1]};
        }
        double segStart = (seg == 0) ? 0.0 : cumLengths[seg - 1];
        double segLen   = Math.max(1e-12, cumLengths[seg] - segStart);
        double t        = (d - segStart) / segLen;
        double[] a = waypoints.get(seg);
        double[] b = waypoints.get(seg + 1);
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
    }

    private void tick(double dt) {
        distTravelled += speed * dt;
        if (distTravelled >= totalLength) {
            if (looping) distTravelled %= Math.max(1e-12, totalLength);
            else         { distTravelled = totalLength; stop(); }
        }
        if (onTick != null) onTick.run();
    }
}
