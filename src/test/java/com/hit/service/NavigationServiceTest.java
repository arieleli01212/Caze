package com.hit.service;

import com.hit.dao.IHistoryDAO;
import com.hit.dm.Building;
import com.hit.dm.Campus;
import com.hit.dm.Walkway;
import com.hit.protocol.HistoryEntry;
import com.hit.protocol.Mode;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;
import com.hit.protocol.Status;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Service-layer tests with an in-memory {@link IHistoryDAO} so no disk I/O happens.
 */
public class NavigationServiceTest {

    private NavigationService service;
    private RecordingHistoryDAO history;

    @Before
    public void setUp() {
        Campus campus = Campus.build(
                Arrays.asList(
                        new Building(1, "MainGate",    50, 50),
                        new Building(2, "Library",    100, 50),
                        new Building(3, "Engineering", 150, 50),
                        new Building(4, "Cafeteria",  100, 100)),
                Arrays.asList(
                        new Walkway(1, 2, 10.0),
                        new Walkway(2, 3, 20.0),
                        new Walkway(1, 4, 5.0)),
                false);
        history = new RecordingHistoryDAO();
        service = new NavigationService(campus, history);
    }

    @Test
    public void testFastestRouteReturnsExpectedPath() {
        RouteResponse response = service.findRoute(
                new RouteRequest("MainGate", "Engineering", Mode.FASTEST));
        assertEquals(Status.OK, response.getStatus());
        assertEquals(Arrays.asList("MainGate", "Library", "Engineering"), response.getPath());
        assertEquals(30.0, response.getCost(), 0.001);
        assertEquals(1, history.entries.size());
    }

    @Test
    public void testFewestSegmentsReturnsSegmentCount() {
        RouteResponse response = service.findRoute(
                new RouteRequest("MainGate", "Engineering", Mode.FEWEST_SEGMENTS));
        assertEquals(Status.OK, response.getStatus());
        assertEquals(2.0, response.getCost(), 0.0); // two hops, BFS view
    }

    @Test
    public void testNoPathReturnsNoPathStatus() {
        Campus disconnected = Campus.build(
                Arrays.asList(
                        new Building(1, "A", 0, 0),
                        new Building(2, "B", 0, 0)),
                Collections.emptyList(),
                false);
        NavigationService svc = new NavigationService(disconnected, history);
        RouteResponse response = svc.findRoute(new RouteRequest("A", "B", Mode.FASTEST));
        assertEquals(Status.NO_PATH, response.getStatus());
    }

    @Test
    public void testUnknownBuildingReturnsBadRequest() {
        RouteResponse response = service.findRoute(
                new RouteRequest("MainGate", "DoesNotExist", Mode.FASTEST));
        assertEquals(Status.BAD_REQUEST, response.getStatus());
        assertTrue(history.entries.isEmpty());
    }

    @Test
    public void testNullModeReturnsBadRequest() {
        RouteResponse response = service.findRoute(
                new RouteRequest("MainGate", "Library", null));
        assertEquals(Status.BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testHistoryRecordedOnSuccessOnly() {
        service.findRoute(new RouteRequest("MainGate", "Library", Mode.FASTEST));
        service.findRoute(new RouteRequest("MainGate", "Nope",    Mode.FASTEST));
        assertEquals(1, history.entries.size());
        assertEquals("Library", history.entries.get(0).getToBuilding());
        assertFalse(history.entries.get(0).getPath().isEmpty());
    }

    /** Lightweight test double for the persistence layer. */
    private static class RecordingHistoryDAO implements IHistoryDAO {
        final List<HistoryEntry> entries = new ArrayList<>();
        @Override public void append(HistoryEntry entry) { entries.add(entry); }
        @Override public List<HistoryEntry> readAll()    { return entries; }
    }
}
