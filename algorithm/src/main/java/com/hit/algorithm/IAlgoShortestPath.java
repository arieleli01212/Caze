package com.hit.algorithm;

import java.util.List;

/**
 * Defines the public API for the Shortest Path algorithm family.
 * <p>
 * Any class implementing this interface provides a way to find the shortest
 * path between two nodes in a {@link Graph}. Following the Strategy Pattern,
 * clients depend only on this interface — never on a specific implementation
 * — so swapping algorithms (e.g. Dijkstra ↔ BFS) is a one-line change.
 *
 * @author HIT Campus Navigation team
 */
public interface IAlgoShortestPath {

    /**
     * Finds the shortest path from {@code source} to {@code destination}.
     *
     * @param graph       the graph to search
     * @param source      the starting node id
     * @param destination the target node id
     * @return ordered list of node ids from source to destination,
     *         or an empty list if no path exists
     * @throws IllegalArgumentException if {@code graph} is {@code null}
     *         or either node is not in the graph
     */
    List<Integer> findShortestPath(Graph graph, int source, int destination);

    /**
     * Returns the total cost (weight/distance) of the shortest path.
     * <p>
     * Implementations may compute this by walking the path returned from
     * {@link #findShortestPath} or by using algorithm-specific shortcuts.
     *
     * @param graph       the graph to search
     * @param source      the starting node id
     * @param destination the target node id
     * @return the path cost, or {@link Double#POSITIVE_INFINITY} if no path exists
     */
    double getPathCost(Graph graph, int source, int destination);
}
