package com.hit.client.controller;

import com.hit.client.network.NavigationClient;
import com.hit.protocol.HistoryEntry;
import com.hit.protocol.Mode;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller (the C in MVC) — the only class that bridges the view and the
 * network layer. The view fires user actions to it; the controller builds
 * requests, calls {@link NavigationClient}, and pushes results back through
 * callback interfaces.
 * <p>
 * Intentionally framework-agnostic: no Swing, no JavaFX imports here.
 * The view is responsible for dispatching callbacks onto the correct UI thread.
 */
public class ClientController {

    private static final Logger LOG = Logger.getLogger(ClientController.class.getName());

    private final NavigationClient client;
    private       RouteListener    routeListener;

    public ClientController(NavigationClient client) {
        this.client = client;
    }

    public void setListener(RouteListener listener) {
        this.routeListener = listener;
    }

    /**
     * Sends a route request on a background thread.
     * Result is delivered to the registered {@link RouteListener}.
     */
    public void requestRoute(String fromBuilding, String toBuilding, Mode mode) {
        new Thread(() -> {
            try {
                RouteResponse response = client.send(new RouteRequest(fromBuilding, toBuilding, mode));
                if (routeListener != null) routeListener.onResponse(response);
            } catch (IOException io) {
                LOG.log(Level.WARNING, "Network call failed", io);
                if (routeListener != null) routeListener.onError(io.getMessage());
            }
        }, "navigation-client-call").start();
    }

    /**
     * Fetches the history list on a background thread.
     *
     * @param listener callback invoked with the result or error
     */
    public void requestHistory(HistoryListener listener) {
        new Thread(() -> {
            try {
                List<HistoryEntry> history = client.getHistory();
                if (listener != null) listener.onHistory(history);
            } catch (IOException io) {
                LOG.log(Level.WARNING, "History fetch failed", io);
                if (listener != null) listener.onHistoryError(io.getMessage());
            }
        }, "history-fetch").start();
    }

    /**
     * Clears history on a background thread, then runs {@code onDone}.
     */
    public void clearHistory(Runnable onDone) {
        new Thread(() -> {
            try {
                client.clearHistory();
            } catch (IOException io) {
                LOG.log(Level.WARNING, "History clear failed", io);
            } finally {
                if (onDone != null) onDone.run();
            }
        }, "history-clear").start();
    }

    // ---- callback interfaces ----

    /** Callback for route request results. */
    public interface RouteListener {
        void onResponse(RouteResponse response);
        void onError(String message);
    }

    /**
     * Backward-compatible alias for {@link RouteListener}.
     * Keeps any existing code that used the old {@code Listener} name working.
     */
    public interface Listener extends RouteListener {}

    /** Callback for history fetch results. */
    public interface HistoryListener {
        void onHistory(List<HistoryEntry> history);
        void onHistoryError(String message);
    }
}
