package com.hit.dao;

import com.hit.protocol.HistoryEntry;

import java.io.IOException;
import java.util.List;

/**
 * Persistence interface for the route-lookup history that the server records
 * after every successful request.
 */
public interface IHistoryDAO {

    /** Appends a single entry to the history store. */
    void append(HistoryEntry entry) throws IOException;

    /** Reads the full history (oldest first). Returns empty list if none. */
    List<HistoryEntry> readAll() throws IOException;
}
