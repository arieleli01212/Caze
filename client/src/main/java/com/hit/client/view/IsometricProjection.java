package com.hit.client.view;

import java.awt.geom.Point2D;

/**
 * Pure-math projection from world (x, y, z) coordinates to 2D screen
 * coordinates. Handles the 2D-to-3D tilt animation, plus pan and zoom.
 * <p>
 * Camera model:
 * <ul>
 *   <li>{@code tilt = 0} → camera looks straight down (alpha = 90°),
 *       Z disappears — pure top-down 2D map.</li>
 *   <li>{@code tilt = 1} → camera tilted to alpha = 30° — classic
 *       isometric view, building heights become visible.</li>
 * </ul>
 * The class is intentionally framework-free so it can be unit tested
 * without spinning up Swing.
 */
public final class IsometricProjection {

    private double tilt        = 1.0;   // [0, 1]
    private double scale       = 1.0;   // pixels per world unit
    private double panX        = 0.0;   // post-projection translation
    private double panY        = 0.0;

    /* ---------- camera setters ---------- */

    public void setTilt(double tilt) {
        this.tilt = clamp(tilt, 0.0, 1.0);
    }

    public double getTilt() {
        return tilt;
    }

    public void setScale(double scale) {
        this.scale = Math.max(0.1, scale);
    }

    public double getScale() {
        return scale;
    }

    public void setPan(double panX, double panY) {
        this.panX = panX;
        this.panY = panY;
    }

    public void translate(double dx, double dy) {
        this.panX += dx;
        this.panY += dy;
    }

    public double getPanX() { return panX; }
    public double getPanY() { return panY; }

    /* ---------- projection math ---------- */

    /**
     * Projects a 3D point in world space to 2D screen space.
     * <p>
     * The current tilt is mapped to a camera angle alpha:
     * {@code alpha = 90° - 60° × tilt} (so tilt 0..1 → alpha 90°..30°).
     * Then:
     * <pre>
     *   screenX = x
     *   screenY = y · sin(alpha) − z · cos(alpha)
     * </pre>
     * No perspective distortion — orthographic projection only, which
     * is what gives the classic "video-game isometric" look.
     */
    public Point2D project(double worldX, double worldY, double worldZ) {
        double alpha = Math.toRadians(90.0 - 60.0 * tilt);
        double sx = worldX;
        double sy = worldY * Math.sin(alpha) - worldZ * Math.cos(alpha);
        return new Point2D.Double(panX + sx * scale, panY + sy * scale);
    }

    /** Convenience overload for ground-plane points (z = 0). */
    public Point2D project(double worldX, double worldY) {
        return project(worldX, worldY, 0.0);
    }

    /**
     * Inverse projection of a screen point onto the ground plane (z = 0).
     * Used for hit-testing — turning a mouse click back into world (x, y).
     */
    public Point2D unprojectGround(double screenX, double screenY) {
        double alpha = Math.toRadians(90.0 - 60.0 * tilt);
        double sx = (screenX - panX) / scale;
        double sy = (screenY - panY) / scale;
        // Inverse of: sy = wy · sin(alpha) − 0
        double wy = sy / Math.sin(alpha);
        return new Point2D.Double(sx, wy);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
