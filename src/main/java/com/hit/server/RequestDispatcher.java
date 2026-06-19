package com.hit.server;

import com.google.gson.Gson;
import com.hit.protocol.ProtocolCodec;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;
import com.hit.protocol.ServerRequest;
import com.hit.service.NavigationService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decodes the incoming {@link ServerRequest} envelope, reads the {@code action}
 * header, and routes to the appropriate service method.
 * <p>
 * Supported actions:
 * <ul>
 *   <li>{@code "route/find"} — find a shortest-path route on campus</li>
 * </ul>
 * Responds with a JSON-encoded {@link RouteResponse} in all cases.
 */
public class RequestDispatcher {

    private static final Logger LOG  = Logger.getLogger(RequestDispatcher.class.getName());
    private static final Gson   GSON = new Gson();

    private final NavigationService navigationService;

    public RequestDispatcher(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    /**
     * Decodes the request envelope, dispatches by action, and returns the
     * encoded response line.
     */
    public String dispatch(String rawJson) {
        try {
            ServerRequest envelope = ProtocolCodec.decode(rawJson, ServerRequest.class);
            if (envelope == null || envelope.getAction() == null) {
                return ProtocolCodec.encode(RouteResponse.badRequest("Missing action header"));
            }

            String action = envelope.getAction();
            return switch (action) {
                case "route/find" -> handleRouteFind(envelope);
                default -> ProtocolCodec.encode(
                        RouteResponse.badRequest("Unknown action: " + action));
            };

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Dispatch failed for: " + rawJson, e);
            return ProtocolCodec.encode(
                    RouteResponse.badRequest("Malformed request: " + e.getMessage()));
        }
    }

    private String handleRouteFind(ServerRequest envelope) {
        if (envelope.getBody() == null) {
            return ProtocolCodec.encode(RouteResponse.badRequest("Missing request body"));
        }
        RouteRequest request = GSON.fromJson(envelope.getBody(), RouteRequest.class);
        return ProtocolCodec.encode(navigationService.findRoute(request));
    }
}
