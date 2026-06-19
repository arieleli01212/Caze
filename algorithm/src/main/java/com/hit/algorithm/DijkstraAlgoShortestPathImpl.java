package com.hit.algorithm;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Dijkstra's shortest-path algorithm.
 * <p>
 * Works on graphs with non-negative edge weights. Uses a min-heap (PriorityQueue)
 * keyed on tentative distance, giving an O((V + E) log V) running time.
 */
public class DijkstraAlgoShortestPathImpl extends AbstractAlgoShortestPath {

    @Override
    public List<Integer> findShortestPath(Graph graph, int source, int destination) {
        validateInput(graph, source, destination);

        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Integer> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue =
                new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));

        for (int node : graph.getNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(source, 0.0);
        queue.add(new NodeDistance(source, 0.0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();

            // Early exit once we have settled the destination.
            if (current.node() == destination) break;

            // Skip stale entries (we may have pushed several copies of the same node).
            if (current.distance() > distances.get(current.node())) continue;

            for (Graph.Edge edge : graph.getNeighbors(current.node())) {
                double newDist = current.distance() + edge.getWeight();
                if (newDist < distances.get(edge.getTarget())) {
                    distances.put(edge.getTarget(), newDist);
                    previous.put(edge.getTarget(), current.node());
                    queue.add(new NodeDistance(edge.getTarget(), newDist));
                }
            }
        }

        return reconstructPath(previous, source, destination);
    }

    /**
     * Override of the default cost computation: Dijkstra already computes
     * distances as a side effect, so re-walking the path is wasted work.
     * We just rerun the search and read the distance map.
     */
    @Override
    public double getPathCost(Graph graph, int source, int destination) {
        validateInput(graph, source, destination);
        if (source == destination) return 0.0;

        Map<Integer, Double> distances = new HashMap<>();
        PriorityQueue<NodeDistance> queue =
                new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));

        for (int node : graph.getNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(source, 0.0);
        queue.add(new NodeDistance(source, 0.0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            if (current.node() == destination) return current.distance();
            if (current.distance() > distances.get(current.node())) continue;

            for (Graph.Edge edge : graph.getNeighbors(current.node())) {
                double newDist = current.distance() + edge.getWeight();
                if (newDist < distances.get(edge.getTarget())) {
                    distances.put(edge.getTarget(), newDist);
                    queue.add(new NodeDistance(edge.getTarget(), newDist));
                }
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    private List<Integer> reconstructPath(Map<Integer, Integer> previous,
                                          int source, int destination) {
        if (source == destination) {
            return Collections.singletonList(source);
        }
        if (!previous.containsKey(destination)) {
            return Collections.emptyList();
        }
        LinkedList<Integer> path = new LinkedList<>();
        Integer current = destination;
        while (current != null) {
            path.addFirst(current);
            current = previous.get(current);
        }
        return path.getFirst() == source ? path : Collections.emptyList();
    }

    /** Internal record bundling a node id with its tentative distance. */
    private record NodeDistance(int node, double distance) {}
}
