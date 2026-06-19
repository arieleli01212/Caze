package com.hit.server.controller;

import com.google.gson.JsonObject;
import com.hit.protocol.HistoryEntry;
import com.hit.protocol.ProtocolCodec;
import com.hit.service.HistoryService;

import java.util.List;

/**
 * Controller for {@code history/get} requests.
 * Returns the full list of persisted route-history entries as JSON.
 */
public class HistoryGetController implements IController {

    private final HistoryService historyService;

    public HistoryGetController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public String handle(JsonObject body) {
        List<HistoryEntry> history = historyService.getHistory();
        return ProtocolCodec.encode(history);
    }
}
