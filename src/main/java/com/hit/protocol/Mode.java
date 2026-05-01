package com.hit.protocol;

/**
 * Routing mode requested by the client. Each mode maps to a specific
 * shortest-path algorithm via {@code com.hit.service.AlgorithmFactory}.
 */
public enum Mode {

    /** Minimise total walking distance. Maps to Dijkstra. */
    FASTEST,

    /** Minimise number of segments (junctions). Maps to BFS. */
    FEWEST_SEGMENTS
}
