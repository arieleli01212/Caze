package com.hit.server.controller;

import com.google.gson.JsonObject;

/**
 * Strategy interface for server-side request handlers.
 * <p>
 * Each implementation handles one {@code action} value from the request's
 * {@code headers.action} field and returns a JSON-encoded response string.
 * <p>
 * Clients depend only on this interface — adding a new action means adding
 * a new implementation and registering it in {@link ControllerFactory}
 * without touching any existing code (Open/Closed Principle).
 */
public interface IController {

    /**
     * Handles one request.
     *
     * @param body the parsed request body from the {@link com.hit.protocol.ServerRequest}
     * @return JSON-encoded response string (sent back to the client as-is)
     */
    String handle(JsonObject body);
}
