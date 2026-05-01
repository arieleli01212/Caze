package com.hit.server;

import com.hit.protocol.ProtocolCodec;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;
import com.hit.service.NavigationService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes a raw incoming JSON request to the right service method.
 * <p>
 * Today we only support {@code "ROUTE"} requests. Adding a new endpoint is
 * a one-line {@code if/else} branch here plus a new request DTO under
 * {@code com.hit.protocol}.
 */
public class RequestDispatcher {

    private static final Logger LOG = Logger.getLogger(RequestDispatcher.class.getName());

    private final NavigationService navigationService;

    public RequestDispatcher(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    /** Decodes the request, dispatches it, and returns the encoded response line. */
    public String dispatch(String requestJson) {
        try {
            // Peek at the type field. For now we only have ROUTE, but the field
            // exists so we can grow without breaking on-wire compatibility.
            RouteRequest request = ProtocolCodec.decode(requestJson, RouteRequest.class);
            if (request == null) {
                return ProtocolCodec.encode(RouteResponse.badRequest("Empty request"));
            }
            String type = request.getType() == null ? "ROUTE" : request.getType();
            return switch (type) {
                case "ROUTE" -> ProtocolCodec.encode(navigationService.findRoute(request));
                default      -> ProtocolCodec.encode(
                        RouteResponse.badRequest("Unknown request type: " + type));
            };
        } catch (Exception parseFailure) {
            LOG.log(Level.WARNING, "Failed to dispatch request: " + requestJson, parseFailure);
            return ProtocolCodec.encode(
                    RouteResponse.badRequest("Malformed request: " + parseFailure.getMessage()));
        }
    }
}
