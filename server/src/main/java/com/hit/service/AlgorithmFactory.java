package com.hit.service;

import com.hit.algorithm.BFSAlgoShortestPathImpl;
import com.hit.algorithm.DijkstraAlgoShortestPathImpl;
import com.hit.algorithm.IAlgoShortestPath;
import com.hit.protocol.Mode;

/**
 * Factory that maps a routing {@link Mode} onto the appropriate
 * {@link IAlgoShortestPath} implementation.
 * <p>
 * Adding a third algorithm later (e.g. A*) means: a new {@code Mode} value,
 * a new {@code case} in {@link #create}, and a new impl class. Nothing else
 * changes.
 */
public final class AlgorithmFactory {

    private AlgorithmFactory() { /* static utility */ }

    public static IAlgoShortestPath create(Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }
        return switch (mode) {
            case FASTEST          -> new DijkstraAlgoShortestPathImpl();
            case FEWEST_SEGMENTS  -> new BFSAlgoShortestPathImpl();
        };
    }
}
