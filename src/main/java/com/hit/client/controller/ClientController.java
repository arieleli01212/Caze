package com.hit.client.controller;

import com.hit.client.network.NavigationClient;
import com.hit.protocol.Mode;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller (the C in MVC) — the only class allowed to bridge the view
 * and the network layer. The view fires user actions to it; the controller
 * builds requests, calls {@link NavigationClient}, and pushes results back
 * via the {@link Listener} callback.
 */
public class ClientController {

    private static final Logger LOG = Logger.getLogger(ClientController.class.getName());

    private final NavigationClient client;
    private       Listener         listener;

    public ClientController(NavigationClient client) {
        this.client = client;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Handle a "Find Route" click from the view. Runs the network call on a
     * background thread so the EDT stays responsive while the server thinks.
     */
    public void requestRoute(String fromBuilding, String toBuilding, Mode mode) {
        new Thread(() -> {
            try {
                RouteResponse response = client.send(new RouteRequest(fromBuilding, toBuilding, mode));
                if (listener != null) listener.onResponse(response);
            } catch (IOException io) {
                LOG.log(Level.WARNING, "Network call failed", io);
                if (listener != null) listener.onError(io.getMessage());
            }
        }, "navigation-client-call").start();
    }

    /** Callback the view registers to receive results. */
    public interface Listener {
        void onResponse(RouteResponse response);
        void onError(String message);
    }
}
