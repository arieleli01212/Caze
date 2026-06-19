package com.hit.dm;

import com.hit.algorithm.Graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregate root for the campus map.
 * <p>
 * Wraps:
 * <ul>
 *   <li>a {@link Graph} of integer node ids used by the algorithm module,</li>
 *   <li>a name ↔ id map so the rest of the system can talk in human terms,</li>
 *   <li>the original {@link Building} objects (for UI rendering).</li>
 * </ul>
 * The class is immutable from the outside: build it via the {@link #build(List, List, boolean)}
 * factory, then read from it.
 */
public class Campus {

    private final Graph graph;
    private final Map<Integer, Building> buildingsById;
    private final Map<String, Integer> idsByName;

    private Campus(Graph graph,
                   Map<Integer, Building> buildingsById,
                   Map<String, Integer> idsByName) {
        this.graph = graph;
        this.buildingsById = buildingsById;
        this.idsByName = idsByName;
    }

    /**
     * Builds a {@code Campus} from raw lists of buildings and walkways.
     *
     * @param buildings buildings on campus
     * @param walkways  edges between buildings (must reference valid building ids)
     * @param directed  whether walkways are one-way
     */
    public static Campus build(List<Building> buildings, List<Walkway> walkways, boolean directed) {
        Objects.requireNonNull(buildings, "buildings");
        Objects.requireNonNull(walkways,  "walkways");

        Map<Integer, Building> byId = new LinkedHashMap<>();
        Map<String, Integer> byName = new HashMap<>();
        for (Building b : buildings) {
            if (byId.containsKey(b.getId())) {
                throw new IllegalArgumentException("Duplicate building id: " + b.getId());
            }
            if (byName.containsKey(b.getName())) {
                throw new IllegalArgumentException("Duplicate building name: " + b.getName());
            }
            byId.put(b.getId(), b);
            byName.put(b.getName(), b.getId());
        }

        Graph graph = new Graph(directed);
        for (Building b : buildings) {
            graph.addNode(b.getId());
        }
        for (Walkway w : walkways) {
            if (!byId.containsKey(w.getFrom()) || !byId.containsKey(w.getTo())) {
                throw new IllegalArgumentException(
                        "Walkway references unknown building: " + w);
            }
            graph.addEdge(w.getFrom(), w.getTo(), w.getDistance());
        }
        return new Campus(graph, byId, byName);
    }

    /** @return the underlying graph (unmodifiable view from the caller's perspective) */
    public Graph getGraph() { return graph; }

    /** @return building by id, or {@code null} if absent */
    public Building getBuilding(int id) { return buildingsById.get(id); }

    /** @return building by name, or {@code null} if absent */
    public Building getBuilding(String name) {
        Integer id = idsByName.get(name);
        return id == null ? null : buildingsById.get(id);
    }

    /** @return id of a building by name, or {@code null} if absent */
    public Integer getBuildingId(String name) { return idsByName.get(name); }

    /** @return alphabetically-sortable list of all building names */
    public List<String> getBuildingNames() {
        return buildingsById.values().stream()
                .map(Building::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /** @return all buildings (unmodifiable view) */
    public Map<Integer, Building> getBuildings() {
        return Collections.unmodifiableMap(buildingsById);
    }

    /** Translates a list of node ids back into building names. */
    public List<String> idsToNames(List<Integer> ids) {
        return ids.stream()
                .map(buildingsById::get)
                .filter(Objects::nonNull)
                .map(Building::getName)
                .collect(Collectors.toList());
    }
}
