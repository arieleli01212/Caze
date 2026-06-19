package com.hit.service;

import com.hit.dao.IHistoryDAO;
import com.hit.protocol.HistoryEntry;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Domain service for route-history operations.
 * <p>
 * Provides the business logic layer above {@link IHistoryDAO}: retrieve the
 * full history list and clear it. This is the second required Service in the
 * assignment's Part B specification.
 */
public class HistoryService {

    private static final Logger LOG = Logger.getLogger(HistoryService.class.getName());

    private final IHistoryDAO historyDAO;

    public HistoryService(IHistoryDAO historyDAO) {
        this.historyDAO = historyDAO;
    }

    /**
     * Returns all persisted history entries, oldest first.
     * Returns an empty list on any I/O error (best-effort read).
     */
    public List<HistoryEntry> getHistory() {
        try {
            return historyDAO.readAll();
        } catch (IOException ioe) {
            LOG.log(Level.WARNING, "Failed to read history", ioe);
            return Collections.emptyList();
        }
    }

    /**
     * Clears all history by overwriting the store with an empty list.
     *
     * @return {@code true} if cleared successfully, {@code false} on I/O error
     */
    public boolean clearHistory() {
        try {
            historyDAO.clear();
            return true;
        } catch (IOException ioe) {
            LOG.log(Level.WARNING, "Failed to clear history", ioe);
            return false;
        }
    }
}
