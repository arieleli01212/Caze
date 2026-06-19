package com.hit.dao;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hit.dm.Building;
import com.hit.dm.Campus;
import com.hit.dm.Walkway;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link ICampusDAO} implementation that reads a JSON description from disk
 * (or, as a fallback, from the bundled classpath resource).
 * <p>
 * Expected JSON shape — see {@code src/main/resources/campus.json} for a
 * worked example:
 * <pre>
 * {
 *   "directed": false,
 *   "buildings": [{ "id": 1, "name": "...", "x": 0, "y": 0 }, ...],
 *   "walkways":  [{ "from": 1, "to": 2, "distance": 100 }, ...]
 * }
 * </pre>
 */
public class FileCampusDAO implements ICampusDAO {

    /** Optional override path. If null, the bundled resource is used. */
    private final Path filePath;

    /**
     * Loads from {@code src/main/resources/campus.json} bundled in the JAR.
     */
    public FileCampusDAO() {
        this.filePath = null;
    }

    /**
     * Loads from a specific file on disk (useful for tests or deployment).
     */
    public FileCampusDAO(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
    }

    @Override
    public Campus load() throws IOException {
        try (Reader reader = openReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            boolean directed = root.has("directed") && root.get("directed").getAsBoolean();

            Gson gson = new Gson();

            List<Building> buildings = new ArrayList<>();
            for (JsonElement el : root.getAsJsonArray("buildings")) {
                JsonObject obj = el.getAsJsonObject();
                int height = obj.has("height")
                        ? obj.get("height").getAsInt()
                        : Building.DEFAULT_HEIGHT;
                double lat = obj.has("lat")
                        ? obj.get("lat").getAsDouble()
                        : Building.NO_COORD;
                double lon = obj.has("lon")
                        ? obj.get("lon").getAsDouble()
                        : Building.NO_COORD;
                buildings.add(new Building(
                        obj.get("id").getAsInt(),
                        obj.get("name").getAsString(),
                        obj.get("x").getAsInt(),
                        obj.get("y").getAsInt(),
                        height,
                        lat,
                        lon));
            }

            List<Walkway> walkways = new ArrayList<>();
            JsonArray walkwayArray = root.getAsJsonArray("walkways");
            for (JsonElement el : walkwayArray) {
                JsonObject obj = el.getAsJsonObject();
                walkways.add(new Walkway(
                        obj.get("from").getAsInt(),
                        obj.get("to").getAsInt(),
                        obj.get("distance").getAsDouble()));
            }

            // gson reference silences the "unused import" warning while keeping
            // the import available if a future schema needs full deserialisation.
            Objects.requireNonNull(gson);

            return Campus.build(buildings, walkways, directed);
        }
    }

    private Reader openReader() throws IOException {
        if (filePath != null) {
            return Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
        }
        InputStream in = getClass().getClassLoader().getResourceAsStream("campus.json");
        if (in == null) {
            throw new IOException("campus.json not found on classpath");
        }
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }
}
