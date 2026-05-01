package com.hit.dm;

import java.util.Objects;

/**
 * Domain object representing a single building on campus.
 * <p>
 * Carries an integer id (used by the algorithm module), a human-readable
 * name, schematic (x, y) coordinates plus an extrusion {@code height} used
 * by the 3D isometric renderer, and optionally real GPS (lat, lon) used by
 * the OpenStreetMap / satellite map view.
 * <p>
 * The constructors are layered so older callers (and unit tests) only need
 * the fields they care about — the rest get sensible defaults.
 */
public class Building {

    /** Default extrusion height when none is supplied. */
    public static final int DEFAULT_HEIGHT = 40;

    /** Sentinel meaning "no GPS coordinate". */
    public static final double NO_COORD = Double.NaN;

    private final int    id;
    private final String name;
    private final int    x;
    private final int    y;
    private final int    height;
    private final double lat;
    private final double lon;

    /** Convenience ctor with default height — used by tests and simple cases. */
    public Building(int id, String name, int x, int y) {
        this(id, name, x, y, DEFAULT_HEIGHT, NO_COORD, NO_COORD);
    }

    /** Schematic-only ctor. */
    public Building(int id, String name, int x, int y, int height) {
        this(id, name, x, y, height, NO_COORD, NO_COORD);
    }

    /** Full ctor with GPS for the real-map view. */
    public Building(int id, String name, int x, int y, int height, double lat, double lon) {
        this.id     = id;
        this.name   = Objects.requireNonNull(name, "name");
        this.x      = x;
        this.y      = y;
        this.height = height;
        this.lat    = lat;
        this.lon    = lon;
    }

    public int    getId()     { return id; }
    public String getName()   { return name; }
    public int    getX()      { return x; }
    public int    getY()      { return y; }
    public int    getHeight() { return height; }
    public double getLat()    { return lat; }
    public double getLon()    { return lon; }

    /** @return {@code true} if both lat and lon are present (non-NaN). */
    public boolean hasGeoLocation() {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Building)) return false;
        Building b = (Building) o;
        return id == b.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
