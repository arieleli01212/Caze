package com.hit.server.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory that maps action strings to their {@link IController} handlers.
 * <p>
 * Populated once at application startup — controllers are pre-registered
 * with their action keys so that runtime dispatch is a simple map lookup.
 * Adding a new action never requires changing existing controllers (Open/Closed).
 * <p>
 * Usage:
 * <pre>
 *   ControllerFactory factory = new ControllerFactory();
 *   factory.register("route/find",   new RouteController(navService));
 *   factory.register("history/get",  new HistoryGetController(historyService));
 *
 *   IController ctrl = factory.getController("route/find"); // Strategy lookup
 * </pre>
 */
public class ControllerFactory {

    private final Map<String, IController> registry;

    public ControllerFactory() {
        this.registry = new HashMap<>();
    }

    /** Registers an action-to-controller mapping. */
    public void register(String action, IController controller) {
        registry.put(action, controller);
    }

    /**
     * Returns the controller for the given action, or {@code null} if none
     * has been registered.
     */
    public IController getController(String action) {
        return registry.get(action);
    }

    /** Returns an unmodifiable view of all registered actions. */
    public Map<String, IController> getAll() {
        return Collections.unmodifiableMap(registry);
    }
}
