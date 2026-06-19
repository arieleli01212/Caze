package com.hit.protocol;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Wire-format envelope for every request sent from client to server.
 * <p>
 * Format (JSON):
 * <pre>
 * {
 *   "headers": { "action": "route/find" },
 *   "body":    { ... action-specific fields ... }
 * }
 * </pre>
 * The {@code action} header tells the server which controller to invoke.
 * The {@code body} is decoded later by the matching controller.
 */
public class ServerRequest {

    private Map<String, String> headers;
    private JsonObject          body;

    public ServerRequest() {}

    public ServerRequest(String action, JsonObject body) {
        this.headers = new HashMap<>();
        this.headers.put("action", action);
        this.body = body;
    }

    public String getAction() {
        return (headers != null) ? headers.get("action") : null;
    }

    public JsonObject getBody() { return body; }

    public Map<String, String> getHeaders() { return headers; }
}
