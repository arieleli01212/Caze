package com.hit.server.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hit.protocol.ProtocolCodec;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;
import com.hit.service.NavigationService;

/**
 * Controller for {@code route/find} requests.
 * <p>
 * Parses the body as a {@link RouteRequest}, delegates to
 * {@link NavigationService#findRoute}, and returns the encoded
 * {@link RouteResponse}.
 */
public class RouteController implements IController {

    private static final Gson GSON = new Gson();

    private final NavigationService navigationService;

    public RouteController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @Override
    public String handle(JsonObject body) {
        if (body == null) {
            return ProtocolCodec.encode(RouteResponse.badRequest("Missing request body"));
        }
        RouteRequest request = GSON.fromJson(body, RouteRequest.class);
        return ProtocolCodec.encode(navigationService.findRoute(request));
    }
}
