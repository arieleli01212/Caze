package com.hit.dm;

/**
 * Domain object representing a walkway connecting two buildings.
 * The {@code distance} field is in arbitrary units (e.g. metres or
 * walking-time seconds) — the algorithm doesn't care, only relative
 * magnitudes matter.
 */
public class Walkway {

    private final int from;
    private final int to;
    private final double distance;

    public Walkway(int from, int to, double distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("Distance must be non-negative: " + distance);
        }
        this.from = from;
        this.to = to;
        this.distance = distance;
    }

    public int getFrom()        { return from; }
    public int getTo()          { return to; }
    public double getDistance() { return distance; }

    @Override
    public String toString() {
        return from + "->" + to + " (" + distance + ")";
    }
}
