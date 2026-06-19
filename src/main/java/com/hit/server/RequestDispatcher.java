package com.hit.server;

import com.hit.protocol.ProtocolCodec;
import com.hit.protocol.RouteResponse;
import com.hit.protocol.ServerRequest;
import com.hit.server.controller.ControllerFactory;
import com.hit.server.controller.IController;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decodes the incoming {@link ServerRequest} envelope, reads the {@code action}
 * header, and delegates to the matching {@link IController} looked up from the
 * {@link ControllerFactory}.
 * <p>
 * This class does not know about any specific action — it only owns the
 * lookup-and-dispatch loop (Strategy + Factory working together).
 */
public class RequestDispatcher {

    private static final Logger LOG = Logger.getLogger(RequestDispatcher.class.getName());

    private final ControllerFactory factory;

    public RequestDispatcher(ControllerFactory factory) {
        this.factory = factory;
    }

    /**
     * Decodes the request envelope, finds the right controller via the factory,
     * calls it, and returns the encoded response line.
     */
    public String dispatch(String rawJson) {
        try {
            ServerRequest envelope = ProtocolCodec.decode(rawJson, ServerRequest.class);
            if (envelope == null || envelope.getAction() == null) {
                return ProtocolCodec.encode(RouteResponse.badRequest("Missing action header"));
            }

            IController controller = factory.getController(envelope.getAction());
            if (controller == null) {
                return ProtocolCodec.encode(
                        RouteResponse.badRequest("Unknown action: " + envelope.getAction()));
            }

            return controller.handle(envelope.getBody());

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Dispatch failed for: " + rawJson, e);
            return ProtocolCodec.encode(
                    RouteResponse.badRequest("Malformed request: " + e.getMessage()));
        }
    }
}
