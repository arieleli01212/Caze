package com.hit.dao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hit.protocol.HistoryEntry;
import com.hit.protocol.ProtocolCodec;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * File-backed implementation of {@link IHistoryDAO}.
 * <p>
 * Stores the entire history as a single JSON array file (pretty-printed for
 * easy hand-inspection during the demo). Reads and writes are guarded by
 * an instance lock so concurrent server threads don't corrupt the file.
 * <p>
 * For higher-throughput needs we'd switch to NDJSON append-only — for a
 * student project's traffic, rewriting the array on each entry is fine.
 */
public class FileHistoryDAO implements IHistoryDAO {

    private static final Type LIST_TYPE = new TypeToken<List<HistoryEntry>>() {}.getType();

    private final Path filePath;
    private final Object lock = new Object();

    public FileHistoryDAO(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void append(HistoryEntry entry) throws IOException {
        synchronized (lock) {
            List<HistoryEntry> all = readAllLocked();
            all.add(entry);
            ensureParentExists();
            Files.writeString(filePath, ProtocolCodec.encodePretty(all), StandardCharsets.UTF_8);
        }
    }

    @Override
    public List<HistoryEntry> readAll() throws IOException {
        synchronized (lock) {
            return Collections.unmodifiableList(readAllLocked());
        }
    }

    private List<HistoryEntry> readAllLocked() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        String json = Files.readString(filePath, StandardCharsets.UTF_8).trim();
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        List<HistoryEntry> parsed = new Gson().fromJson(json, LIST_TYPE);
        return parsed == null ? new ArrayList<>() : new ArrayList<>(parsed);
    }

    @Override
    public void clear() throws IOException {
        synchronized (lock) {
            ensureParentExists();
            Files.writeString(filePath, "[]", StandardCharsets.UTF_8);
        }
    }

    private void ensureParentExists() throws IOException {
        Path parent = filePath.toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}
