package com.hit.service;

import com.hit.algorithm.IAlgoShortestPath;
import com.hit.dao.IHistoryDAO;
import com.hit.dm.Campus;
import com.hit.protocol.HistoryEntry;
import com.hit.protocol.Mode;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Domain-level service that orchestrates a route lookup.
 * <p>
 * Responsibilities:
 * <ol>
 *   <li>Validate the request against the loaded {@link Campus}.</li>
 *   <li>Pick the right algorithm via {@link AlgorithmFactory}.</li>
 *   <li>Run it on the campus graph, translating names ↔ ids on the way in
 *       and out.</li>
 *   <li>Persist the lookup to the history file (best-effort — a failure here
 *       must not break the user-facing response).</li>
 * </ol>
 */
public class NavigationService {

    private static final Logger LOG = Logger.getLogger(NavigationService.class.getName());

    private final Campus      campus;
    private final IHistoryDAO historyDAO;

    public NavigationService(Campus campus, IHistoryDAO historyDAO) {
        this.campus     = campus;
        this.historyDAO = historyDAO;
    }

    /**
     * Handles one request and produces a wire-format response.
     * Never throws — domain errors are encoded as {@link RouteResponse}.
     */
    public RouteResponse findRoute(RouteRequest request) {
        if (request == null) {
            return RouteResponse.badRequest("Request payload missing");
        }
        if (request.getMode() == null) {
            return RouteResponse.badRequest("Mode is required");
        }

        Integer fromId = campus.getBuildingId(request.getFromBuilding());
        Integer toId   = campus.getBuildingId(request.getToBuilding());
        if (fromId == null) {
            return RouteResponse.badRequest("Unknown source building: " + request.getFromBuilding());
        }
        if (toId == null) {
            return RouteResponse.badRequest("Unknown destination building: " + request.getToBuilding());
        }

        try {
            IAlgoShortestPath algo = AlgorithmFactory.create(request.getMode());
            List<Integer> idPath = algo.findShortestPath(campus.getGraph(), fromId, toId);

            if (idPath.isEmpty()) {
                return RouteResponse.noPath();
            }

            List<String> namedPath = campus.idsToNames(idPath);
            double cost = (request.getMode() == Mode.FEWEST_SEGMENTS)
                    // BFS treats edges as unweighted — return number of segments.
                    ? idPath.size() - 1
                    : algo.getPathCost(campus.getGraph(), fromId, toId);

            recordHistory(request, namedPath, cost);
            return RouteResponse.ok(namedPath, cost);

        } catch (IllegalArgumentException badInput) {
            return RouteResponse.badRequest(badInput.getMessage());
        } catch (RuntimeException unexpected) {
            LOG.log(Level.SEVERE, "Route computation failed", unexpected);
            return RouteResponse.serverError(unexpected.getMessage());
        }
    }

    private void recordHistory(RouteRequest request, List<String> namedPath, double cost) {
        if (historyDAO == null) return;
        try {
            historyDAO.append(new HistoryEntry(
                    System.currentTimeMillis(),
                    request.getFromBuilding(),
                    request.getToBuilding(),
                    request.getMode(),
                    namedPath,
                    cost));
        } catch (Exception persistenceFailure) {
            // History is best-effort. Log and move on; never fail the user request.
            LOG.log(Level.WARNING, "Failed to persist history entry", persistenceFailure);
        }
    }
}
