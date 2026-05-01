package com.hit.protocol;

import java.util.List;

/**
 * One persisted record of a route lookup, written by the server to history.json
 * after every successful request. Used both as the on-disk shape and (later)
 * as a possible response item if the client requests history.
 */
public class HistoryEntry {

    private long         timestampMillis;
    private String       fromBuilding;
    private String       toBuilding;
    private Mode         mode;
    private List<String> path;
    private double       cost;

    /** No-args ctor required by Gson. */
    public HistoryEntry() {}

    public HistoryEntry(long timestampMillis,
                        String fromBuilding,
                        String toBuilding,
                        Mode mode,
                        List<String> path,
                        double cost) {
        this.timestampMillis = timestampMillis;
        this.fromBuilding    = fromBuilding;
        this.toBuilding      = toBuilding;
        this.mode            = mode;
        this.path            = path;
        this.cost            = cost;
    }

    public long         getTimestampMillis() { return timestampMillis; }
    public String       getFromBuilding()    { return fromBuilding; }
    public String       getToBuilding()      { return toBuilding; }
    public Mode         getMode()            { return mode; }
    public List<String> getPath()            { return path; }
    public double       getCost()            { return cost; }

    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
    public void setFromBuilding(String fromBuilding)     { this.fromBuilding = fromBuilding; }
    public void setToBuilding(String toBuilding)         { this.toBuilding = toBuilding; }
    public void setMode(Mode mode)                       { this.mode = mode; }
    public void setPath(List<String> path)               { this.path = path; }
    public void setCost(double cost)                     { this.cost = cost; }
}
