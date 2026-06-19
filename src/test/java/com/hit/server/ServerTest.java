package com.hit.server;

import com.hit.dao.FileCampusDAO;
import com.hit.dm.Campus;
import com.hit.protocol.Mode;
import com.hit.protocol.ProtocolCodec;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;
import com.hit.protocol.Status;
import com.hit.service.NavigationService;
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

import static org.junit.Assert.*;

/**
 * Integration tests for {@link Server}.
 * Verifies: implements Runnable, binds a port, handles a real request round-trip,
 * and shuts down cleanly.
 */
public class ServerTest {

    private Server server;
    private int    port;

    @Before
    public void setUp() throws Exception {
        // Pick a free port dynamically to avoid conflicts.
        try (ServerSocket tmp = new ServerSocket(0)) {
            port = tmp.getLocalPort();
        }

        Campus campus = new FileCampusDAO().load();
        NavigationService service    = new NavigationService(campus, null);
        RequestDispatcher dispatcher = new RequestDispatcher(service);

        server = new Server(port, dispatcher);
        new Thread(server, "test-server").start();
        Thread.sleep(100); // Give the socket a moment to bind.
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
        RouteResponse response = sendRequest(
                new RouteRequest("Main Gate (Canada Gate)", "Elyachar Central Library", Mode.FASTEST));
        assertNotNull(response);
        assertEquals(Status.OK, response.getStatus());
        assertFalse(response.getPath().isEmpty());
    }

    @Test
    public void badBuildingNameReturnsBadRequest() throws IOException {
        RouteResponse response = sendRequest(
                new RouteRequest("NoSuchBuilding", "Elyachar Central Library", Mode.FASTEST));
        assertEquals(Status.BAD_REQUEST, response.getStatus());
    }

    @Test
    public void fewestSegmentsModeWorks() throws IOException {
        RouteResponse response = sendRequest(
                new RouteRequest("Main Gate (Canada Gate)", "Elyachar Central Library", Mode.FEWEST_SEGMENTS));
        assertEquals(Status.OK, response.getStatus());
        assertTrue(response.getCost() >= 1.0);
    }

    // --- helper ---

    private RouteResponse sendRequest(RouteRequest request) throws IOException {
        try (Socket socket = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), false, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            out.println(ProtocolCodec.encode(request));
            out.flush();
            String line = in.readLine();
            return ProtocolCodec.decode(line, RouteResponse.class);
        }
    }
}
