package com.hit.service;

import com.hit.dao.IHistoryDAO;
import com.hit.protocol.HistoryEntry;
import com.hit.protocol.Mode;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link HistoryService}.
 */
public class HistoryServiceTest {

    private StubHistoryDAO dao;
    private HistoryService service;

    @Before
    public void setUp() {
        dao     = new StubHistoryDAO();
        service = new HistoryService(dao);
    }

    @Test
    public void getHistoryReturnsAllEntries() {
        dao.entries.add(entry("A", "B"));
        dao.entries.add(entry("C", "D"));
        List<HistoryEntry> history = service.getHistory();
        assertEquals(2, history.size());
        assertEquals("A", history.get(0).getFromBuilding());
    }

    @Test
    public void getHistoryReturnsEmptyListWhenNone() {
        assertTrue(service.getHistory().isEmpty());
    }

    @Test
    public void getHistoryReturnsEmptyListOnIoError() {
        dao.throwOnRead = true;
        List<HistoryEntry> result = service.getHistory();
        assertTrue("Should return empty list on I/O error", result.isEmpty());
    }

    @Test
    public void clearHistoryReturnsTrueOnSuccess() {
        dao.entries.add(entry("A", "B"));
        assertTrue(service.clearHistory());
        assertTrue(dao.entries.isEmpty());
    }

    @Test
    public void clearHistoryReturnsFalseOnIoError() {
        dao.throwOnClear = true;
        assertFalse(service.clearHistory());
    }

    @Test
    public void clearHistoryThenGetReturnsEmpty() {
        dao.entries.add(entry("X", "Y"));
        service.clearHistory();
        assertTrue(service.getHistory().isEmpty());
    }

    // --- stub ---

    private static HistoryEntry entry(String from, String to) {
        return new HistoryEntry(System.currentTimeMillis(), from, to,
                Mode.FASTEST, Arrays.asList(from, to), 10.0);
    }

    private static class StubHistoryDAO implements IHistoryDAO {
        final List<HistoryEntry> entries = new ArrayList<>();
        boolean throwOnRead  = false;
        boolean throwOnClear = false;

        @Override
        public void append(HistoryEntry e) { entries.add(e); }

        @Override
        public List<HistoryEntry> readAll() throws IOException {
            if (throwOnRead) throw new IOException("simulated read error");
            return new ArrayList<>(entries);
        }

        @Override
        public void clear() throws IOException {
            if (throwOnClear) throw new IOException("simulated clear error");
            entries.clear();
        }
    }
}
