package com.hit.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Simple weighted graph using an adjacency-list representation.
 * Supports both directed and undirected edges.
 * <p>
 * The graph is intentionally generic: nodes are integer ids, and any
 * domain-level meaning (a building name, for instance) is mapped externally.
 * That keeps this class fully reusable as a standalone algorithmic module.
 */
public class Graph {

    private final Map<Integer, List<Edge>> adjacencyList;
    private final boolean directed;

    /**
     * @param directed {@code true} for a directed graph, {@code false} for undirected
     */
    public Graph(boolean directed) {
        this.adjacencyList = new HashMap<>();
        this.directed = directed;
    }

    /** Adds an isolated node (no-op if it already exists). */
    public void addNode(int node) {
        adjacencyList.putIfAbsent(node, new ArrayList<>());
    }

    /**
     * Adds a weighted edge. For undirected graphs both directions are stored.
     *
     * @param from   source node id
     * @param to     target node id
     * @param weight non-negative edge weight
     * @throws IllegalArgumentException if {@code weight} is negative
     */
    public void addEdge(int from, int to, double weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Edge weight must be non-negative: " + weight);
        }
        addNode(from);
        addNode(to);
        adjacencyList.get(from).add(new Edge(to, weight));
        if (!directed) {
            adjacencyList.get(to).add(new Edge(from, weight));
        }
    }

    /** Returns an unmodifiable view of the neighbours of {@code node}. */
    public List<Edge> getNeighbors(int node) {
        List<Edge> list = adjacencyList.get(node);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    /** Returns the set of node ids in this graph. */
    public Set<Integer> getNodes() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    /** @return {@code true} if {@code node} exists in the graph */
    public boolean containsNode(int node) {
        return adjacencyList.containsKey(node);
    }

    public boolean isDirected() {
        return directed;
    }

    /** Immutable directed weighted edge. */
    public static final class Edge {
        private final int target;
        private final double weight;

        public Edge(int target, double weight) {
            this.target = target;
            this.weight = weight;
        }

        public int getTarget() { return target; }
        public double getWeight() { return weight; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge)) return false;
            Edge edge = (Edge) o;
            return target == edge.target && Double.compare(edge.weight, weight) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(target, weight);
        }

        @Override
        public String toString() {
            return "Edge{->" + target + ", w=" + weight + "}";
        }
    }
}
