package com.hit.protocol;

import java.util.Collections;
import java.util.List;

/**
 * Wire-format response sent back from the server.
 */
public class RouteResponse {

    private Status       status;
    private List<String> path;
    private double       cost;
    private String       errorMessage;

    /** No-args ctor required by Gson. */
    public RouteResponse() {}

    private RouteResponse(Status status, List<String> path, double cost, String errorMessage) {
        this.status       = status;
        this.path         = path;
        this.cost         = cost;
        this.errorMessage = errorMessage;
    }

    public static RouteResponse ok(List<String> path, double cost) {
        return new RouteResponse(Status.OK, path, cost, null);
    }

    public static RouteResponse noPath() {
        return new RouteResponse(
                Status.NO_PATH, Collections.emptyList(), Double.POSITIVE_INFINITY, null);
    }

    public static RouteResponse badRequest(String reason) {
        return new RouteResponse(
                Status.BAD_REQUEST, Collections.emptyList(), Double.POSITIVE_INFINITY, reason);
    }

    public static RouteResponse serverError(String reason) {
        return new RouteResponse(
                Status.SERVER_ERROR, Collections.emptyList(), Double.POSITIVE_INFINITY, reason);
    }

    public Status       getStatus()       { return status; }
    public List<String> getPath()         { return path; }
    public double       getCost()         { return cost; }
    public String       getErrorMessage() { return errorMessage; }

    public void setStatus(Status status)              { this.status = status; }
    public void setPath(List<String> path)            { this.path = path; }
    public void setCost(double cost)                  { this.cost = cost; }
    public void setErrorMessage(String errorMessage)  { this.errorMessage = errorMessage; }
}
