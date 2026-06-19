package com.hit.protocol;

import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Wire-format envelope for every response sent from server to client.
 * <p>
 * Format (JSON):
 * <pre>
 * {
 *   "headers": { "status": "OK" },
 *   "body":    { ... action-specific result ... }
 * }
 * </pre>
 */
public class ServerResponse {

    private Map<String, String> headers;
    private JsonElement         body;

    public ServerResponse() {}

    public ServerResponse(String status, JsonElement body) {
        this.headers = new HashMap<>();
        this.headers.put("status", status);
        this.body = body;
    }

    public static ServerResponse ok(JsonElement body) {
        return new ServerResponse("OK", body);
    }

    public static ServerResponse error(String reason) {
        return new ServerResponse("ERROR", null);
    }

    public String getStatus()          { return headers != null ? headers.get("status") : null; }
    public JsonElement getBody()       { return body; }
    public Map<String, String> getHeaders() { return headers; }
}
