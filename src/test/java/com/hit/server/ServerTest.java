package com.hit.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hit.dao.FileCampusDAO;
import com.hit.dm.Campus;
import com.hit.protocol.Mode;
import com.hit.protocol.ProtocolCodec;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;
import com.hit.protocol.ServerRequest;
import com.hit.protocol.Status;
import com.hit.dao.IHistoryDAO;
import com.hit.protocol.HistoryEntry;
import com.hit.server.controller.ControllerFactory;
import com.hit.server.controller.HistoryClearController;
import com.hit.server.controller.HistoryGetController;
import com.hit.server.controller.RouteController;
import com.hit.service.HistoryService;
import com.hit.service.NavigationService;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link Server}.
 */
public class ServerTest {

    private static final Gson GSON = new Gson();

    private Server server;
    private int    port;

    @Before
    public void setUp() throws Exception {
        try (ServerSocket tmp = new ServerSocket(0)) {
            port = tmp.getLocalPort();
        }

        Campus campus = new FileCampusDAO().load();
        InMemoryHistoryDAO historyDAO = new InMemoryHistoryDAO();
        NavigationService service    = new NavigationService(campus, historyDAO);
        HistoryService    histSvc    = new HistoryService(historyDAO);
        ControllerFactory factory    = new ControllerFactory();
        factory.register("route/find",    new RouteController(service));
        factory.register("history/get",   new HistoryGetController(histSvc));
        factory.register("history/clear", new HistoryClearController(histSvc));
        RequestDispatcher dispatcher = new RequestDispatcher(factory);

        server = new Server(port, dispatcher);
        new Thread(server, "test-server").start();
        Thread.sleep(100);
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void serverImplementsRunnable() {
        assertTrue("Server must implement Runnable", server instanceof Runnable);
    }

    @Test
    public void routeRequestReturnsOkResponse() throws IOException {
        RouteResponse response = sendRoute("Main Gate (Canada Gate)", "Elyachar Central Library", Mode.FASTEST);
        assertNotNull(response);
        assertEquals(Status.OK, response.getStatus());
        assertFalse(response.getPath().isEmpty());
    }

    @Test
    public void badBuildingNameReturnsBadRequest() throws IOException {
        RouteResponse response = sendRoute("NoSuchBuilding", "Elyachar Central Library", Mode.FASTEST);
        assertEquals(Status.BAD_REQUEST, response.getStatus());
    }

    @Test
    public void fewestSegmentsModeWorks() throws IOException {
        RouteResponse response = sendRoute("Main Gate (Canada Gate)", "Elyachar Central Library", Mode.FEWEST_SEGMENTS);
        assertEquals(Status.OK, response.getStatus());
        assertTrue(response.getCost() >= 1.0);
    }

    @Test
    public void unknownActionReturnsBadRequest() throws IOException {
        ServerRequest envelope = new ServerRequest("unknown/action", new JsonObject());
        String raw = sendRaw(ProtocolCodec.encode(envelope));
        RouteResponse response = ProtocolCodec.decode(raw, RouteResponse.class);
        assertEquals(Status.BAD_REQUEST, response.getStatus());
    }

    @Test
    public void missingActionReturnsBadRequest() throws IOException {
        String raw = sendRaw("{}");
        RouteResponse response = ProtocolCodec.decode(raw, RouteResponse.class);
        assertEquals(Status.BAD_REQUEST, response.getStatus());
    }

    @Test
    public void historyGetReturnsListAfterRoute() throws IOException {
        sendRoute("Main Gate (Canada Gate)", "Elyachar Central Library", Mode.FASTEST);
        Type listType = new TypeToken<List<HistoryEntry>>() {}.getType();
        ServerRequest env = new ServerRequest("history/get", new JsonObject());
        String raw = sendRaw(ProtocolCodec.encode(env));
        List<HistoryEntry> history = GSON.fromJson(raw, listType);
        assertNotNull(history);
        assertFalse("History should have entries after a route", history.isEmpty());
    }

    @Test
    public void historyClearEmptiesHistory() throws IOException {
        sendRoute("Main Gate (Canada Gate)", "Elyachar Central Library", Mode.FASTEST);
        ServerRequest clear = new ServerRequest("history/clear", new JsonObject());
        sendRaw(ProtocolCodec.encode(clear));

        Type listType = new TypeToken<List<HistoryEntry>>() {}.getType();
        ServerRequest get = new ServerRequest("history/get", new JsonObject());
        String raw = sendRaw(ProtocolCodec.encode(get));
        List<HistoryEntry> history = GSON.fromJson(raw, listType);
        assertTrue("History should be empty after clear", history == null || history.isEmpty());
    }

    // --- helpers ---

    private RouteResponse sendRoute(String from, String to, Mode mode) throws IOException {
        RouteRequest req = new RouteRequest(from, to, mode);
        JsonObject body  = GSON.toJsonTree(req).getAsJsonObject();
        ServerRequest env = new ServerRequest("route/find", body);
        String raw = sendRaw(ProtocolCodec.encode(env));
        return ProtocolCodec.decode(raw, RouteResponse.class);
    }

    private String sendRaw(String line) throws IOException {
        try (Socket socket = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), false, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            out.println(line);
            out.flush();
            return in.readLine();
        }
    }

    /** Thread-safe in-memory DAO for test isolation (no disk I/O). */
    private static class InMemoryHistoryDAO implements IHistoryDAO {
        private final List<HistoryEntry> entries = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override public void append(HistoryEntry e) { entries.add(e); }
        @Override public List<HistoryEntry> readAll() { return new ArrayList<>(entries); }
        @Override public void clear() { entries.clear(); }
    }
}
