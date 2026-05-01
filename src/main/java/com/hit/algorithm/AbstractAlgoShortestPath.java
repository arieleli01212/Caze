package com.hit.algorithm;

import java.util.List;

/**
 * Abstract base class for shortest-path algorithms.
 * <p>
 * Holds shared validation logic and a default implementation of
 * {@link #getPathCost(Graph, int, int)} so that concrete subclasses
 * don't duplicate boilerplate.
 */
public abstract class AbstractAlgoShortestPath implements IAlgoShortestPath {

    /**
     * Validates inputs that are common to every shortest-path algorithm.
     *
     * @throws IllegalArgumentException if any input is invalid
     */
    protected void validateInput(Graph graph, int source, int destination) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        if (!graph.containsNode(source)) {
            throw new IllegalArgumentException("Source node not in graph: " + source);
        }
        if (!graph.containsNode(destination)) {
            throw new IllegalArgumentException("Destination node not in graph: " + destination);
        }
    }

    /**
     * Default cost calculation: derives the cost by summing edge weights along
     * the path returned by {@link #findShortestPath(Graph, int, int)}.
     * <p>
     * Subclasses may override for a more efficient direct calculation
     * (e.g. Dijkstra already computes distances during traversal).
     */
    @Override
    public double getPathCost(Graph graph, int source, int destination) {
        List<Integer> path = findShortestPath(graph, source, destination);
        if (path.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double cost = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);
            for (Graph.Edge edge : graph.getNeighbors(from)) {
                if (edge.getTarget() == to) {
                    cost += edge.getWeight();
                    break;
                }
            }
        }
        return cost;
    }
}
