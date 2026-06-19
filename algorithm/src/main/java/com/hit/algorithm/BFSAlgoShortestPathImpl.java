package com.hit.algorithm;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Breadth-First Search shortest-path algorithm.
 * <p>
 * Treats the graph as <strong>unweighted</strong>: every edge counts as one
 * step, so the result is the path with the fewest segments / fewest turns,
 * regardless of edge weights. Running time is O(V + E).
 * <p>
 * Use this when "fewest stops" matters more than total distance — e.g.
 * a campus user who prefers a route with fewer junctions even if it's longer.
 */
public class BFSAlgoShortestPathImpl extends AbstractAlgoShortestPath {

    @Override
    public List<Integer> findShortestPath(Graph graph, int source, int destination) {
        validateInput(graph, source, destination);

        if (source == destination) {
            return Collections.singletonList(source);
        }

        Map<Integer, Integer> previous = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();

        queue.add(source);
        visited.add(source);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (Graph.Edge edge : graph.getNeighbors(current)) {
                int neighbor = edge.getTarget();
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    previous.put(neighbor, current);
                    if (neighbor == destination) {
                        return reconstructPath(previous, source, destination);
                    }
                    queue.add(neighbor);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<Integer> reconstructPath(Map<Integer, Integer> previous,
                                          int source, int destination) {
        LinkedList<Integer> path = new LinkedList<>();
        Integer current = destination;
        while (current != null) {
            path.addFirst(current);
            current = previous.get(current);
        }
        return path;
    }
}
