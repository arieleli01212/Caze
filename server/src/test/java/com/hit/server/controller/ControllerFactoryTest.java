package com.hit.server.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hit.dm.Building;
import com.hit.dm.Campus;
import com.hit.dm.Walkway;
import com.hit.protocol.Mode;
import com.hit.protocol.ProtocolCodec;
import com.hit.protocol.RouteResponse;
import com.hit.protocol.Status;
import com.hit.service.NavigationService;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ControllerFactory}, {@link IController}, and {@link RouteController}.
 */
public class ControllerFactoryTest {

    private static final Gson GSON = new Gson();

    private ControllerFactory factory;

    @Before
    public void setUp() {
        Campus campus = Campus.build(
                Arrays.asList(
                        new Building(1, "A", 0, 0),
                        new Building(2, "B", 0, 0)),
                Collections.singletonList(new Walkway(1, 2, 5.0)),
                false);

        NavigationService service = new NavigationService(campus, null);
        factory = new ControllerFactory();
        factory.register("route/find", new RouteController(service));
    }

    @Test
    public void factoryReturnsRouteController() {
        IController ctrl = factory.getController("route/find");
        assertNotNull(ctrl);
        assertNotNull(factory.getAll().get("route/find"));
    }

    @Test
    public void factoryReturnsNullForUnknownAction() {
        assertNull(factory.getController("no/such/action"));
    }

    @Test
    public void routeControllerHandlesValidRequest() {
        JsonObject body = makeBody("A", "B", Mode.FASTEST);
        IController ctrl = factory.getController("route/find");
        String json = ctrl.handle(body);
        RouteResponse resp = ProtocolCodec.decode(json, RouteResponse.class);
        assertEquals(Status.OK, resp.getStatus());
        assertEquals(Arrays.asList("A", "B"), resp.getPath());
    }

    @Test
    public void routeControllerHandlesBadBuilding() {
        JsonObject body = makeBody("A", "NoSuchBuilding", Mode.FASTEST);
        IController ctrl = factory.getController("route/find");
        String json = ctrl.handle(body);
        RouteResponse resp = ProtocolCodec.decode(json, RouteResponse.class);
        assertEquals(Status.BAD_REQUEST, resp.getStatus());
    }

    @Test
    public void routeControllerHandlesNullBody() {
        IController ctrl = factory.getController("route/find");
        String json = ctrl.handle(null);
        RouteResponse resp = ProtocolCodec.decode(json, RouteResponse.class);
        assertEquals(Status.BAD_REQUEST, resp.getStatus());
    }

    @Test
    public void multipleControllersCanBeRegistered() {
        factory.register("other/action", body -> "{\"test\":true}");
        assertNotNull(factory.getController("other/action"));
        assertNotNull(factory.getController("route/find"));
        assertEquals(2, factory.getAll().size());
    }

    // --- helper ---

    private JsonObject makeBody(String from, String to, Mode mode) {
        JsonObject obj = new JsonObject();
        obj.addProperty("fromBuilding", from);
        obj.addProperty("toBuilding",   to);
        obj.addProperty("mode",         mode.name());
        return obj;
    }
}
