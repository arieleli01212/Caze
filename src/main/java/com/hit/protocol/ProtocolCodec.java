package com.hit.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Thin wrapper around {@link Gson} so the rest of the codebase doesn't need
 * to know about the JSON library directly. Centralising it here means the
 * choice of serializer can change without rippling through every caller.
 */
public final class ProtocolCodec {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .serializeSpecialFloatingPointValues()
            .create();

    private ProtocolCodec() { /* static utility */ }

    /** Serialise any object to a single line of JSON (no embedded newlines). */
    public static String encode(Object obj) {
        return GSON.toJson(obj);
    }

    /** Parse a JSON line back into an instance of {@code type}. */
    public static <T> T decode(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    /** Pretty-printing variant used by persistence files for human readability. */
    public static String encodePretty(Object obj) {
        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .serializeSpecialFloatingPointValues()
                .create()
                .toJson(obj);
    }
}
