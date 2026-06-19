package com.hit.protocol;

/**
 * Wire-format request a client sends to ask for a route between two buildings.
 * Plain mutable fields by design — Gson serializes/deserializes via reflection.
 */
public class RouteRequest {

    /** Discriminator for future request types. Always {@code "ROUTE"} for now. */
    private String type = "ROUTE";

    private String fromBuilding;
    private String toBuilding;
    private Mode   mode;

    /** No-args ctor required by Gson. */
    public RouteRequest() {}

    public RouteRequest(String fromBuilding, String toBuilding, Mode mode) {
        this.fromBuilding = fromBuilding;
        this.toBuilding   = toBuilding;
        this.mode         = mode;
    }

    public String getType()         { return type; }
    public String getFromBuilding() { return fromBuilding; }
    public String getToBuilding()   { return toBuilding; }
    public Mode   getMode()         { return mode; }

    public void setType(String type)                 { this.type = type; }
    public void setFromBuilding(String fromBuilding) { this.fromBuilding = fromBuilding; }
    public void setToBuilding(String toBuilding)     { this.toBuilding = toBuilding; }
    public void setMode(Mode mode)                   { this.mode = mode; }
}
