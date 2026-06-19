package com.hit.algorithm;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * JUnit 4 tests for both shortest-path implementations.
 * <p>
 * Each algorithm gets focused tests; shared validation is also covered here.
 */
public class IAlgoShortestPathTest {

    private Graph weightedGraph;
    private Graph unweightedGraph;

    @Before
    public void setUp() {
        // Weighted graph used by Dijkstra:
        //   1 --(2)-- 2 --(3)-- 3
        //    \________(10)______/
        weightedGraph = new Graph(false);
        weightedGraph.addEdge(1, 2, 2.0);
        weightedGraph.addEdge(2, 3, 3.0);
        weightedGraph.addEdge(1, 3, 10.0);

        // Unweighted graph used by BFS:
        //   1 - 2 - 3 - 4
        //        \_______/
        unweightedGraph = new Graph(false);
        unweightedGraph.addEdge(1, 2, 1.0);
        unweightedGraph.addEdge(2, 3, 1.0);
        unweightedGraph.addEdge(3, 4, 1.0);
        unweightedGraph.addEdge(2, 4, 1.0);
    }

    /* ---------- Dijkstra ---------- */

    @Test
    public void testDijkstraFindsShortestWeightedPath() {
        IAlgoShortestPath algo = new DijkstraAlgoShortestPathImpl();
        List<Integer> path = algo.findShortestPath(weightedGraph, 1, 3);
        assertEquals(Arrays.asList(1, 2, 3), path);
        assertEquals(5.0, algo.getPathCost(weightedGraph, 1, 3), 0.001);
    }

    @Test
    public void testDijkstraReturnsEmptyForUnreachable() {
        Graph g = new Graph(true);
        g.addEdge(1, 2, 1.0);
        g.addNode(3); // isolated
        IAlgoShortestPath algo = new DijkstraAlgoShortestPathImpl();
        assertTrue(algo.findShortestPath(g, 1, 3).isEmpty());
        assertEquals(Double.POSITIVE_INFINITY, algo.getPathCost(g, 1, 3), 0.0);
    }

    @Test
    public void testDijkstraSourceEqualsDestination() {
        IAlgoShortestPath algo = new DijkstraAlgoShortestPathImpl();
        List<Integer> path = algo.findShortestPath(weightedGraph, 2, 2);
        assertEquals(List.of(2), path);
        assertEquals(0.0, algo.getPathCost(weightedGraph, 2, 2), 0.0);
    }

    /* ---------- BFS ---------- */

    @Test
    public void testBFSFindsFewestEdgesPath() {
        IAlgoShortestPath algo = new BFSAlgoShortestPathImpl();
        // 1-2-4 (2 edges) should win over 1-2-3-4 (3 edges).
        List<Integer> path = algo.findShortestPath(unweightedGraph, 1, 4);
        assertEquals(3, path.size());
        assertEquals(Integer.valueOf(1), path.get(0));
        assertEquals(Integer.valueOf(4), path.get(2));
    }

    @Test
    public void testBFSSourceEqualsDestination() {
        IAlgoShortestPath algo = new BFSAlgoShortestPathImpl();
        assertEquals(List.of(1), algo.findShortestPath(unweightedGraph, 1, 1));
    }

    /* ---------- Validation ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void testDijkstraRejectsNullGraph() {
        new DijkstraAlgoShortestPathImpl().findShortestPath(null, 1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBFSRejectsMissingNode() {
        new BFSAlgoShortestPathImpl().findShortestPath(unweightedGraph, 1, 99);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGraphRejectsNegativeWeight() {
        Graph g = new Graph(false);
        g.addEdge(1, 2, -3.0);
    }
}
