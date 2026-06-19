package com.hit.server.controller;

import com.google.gson.JsonObject;
import com.hit.protocol.ProtocolCodec;
import com.hit.service.HistoryService;

import java.util.Map;

/**
 * Controller for {@code history/clear} requests.
 * Clears all persisted history and returns a success/error status.
 */
public class HistoryClearController implements IController {

    private final HistoryService historyService;

    public HistoryClearController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public String handle(JsonObject body) {
        boolean ok = historyService.clearHistory();
        return ProtocolCodec.encode(Map.of("cleared", ok));
    }
}
