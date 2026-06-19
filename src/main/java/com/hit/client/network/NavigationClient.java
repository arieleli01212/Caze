package com.hit.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hit.protocol.ProtocolCodec;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;
import com.hit.protocol.ServerRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Thin client-side wrapper around a TCP socket conversation with the server.
 * Each call opens a new socket, writes one {@link ServerRequest} envelope line,
 * reads one response line, and closes — matching the server's one-shot model.
 */
public class NavigationClient {

    private static final Gson GSON = new Gson();

    private final String host;
    private final int    port;

    public NavigationClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Sends a {@link RouteRequest} wrapped in a {@code route/find} envelope
     * and blocks for the response.
     *
     * @throws IOException if the connection cannot be established or fails midway
     */
    public RouteResponse send(RouteRequest request) throws IOException {
        JsonObject body = GSON.toJsonTree(request).getAsJsonObject();
        ServerRequest envelope = new ServerRequest("route/find", body);
        String responseLine = sendRaw(ProtocolCodec.encode(envelope));
        return ProtocolCodec.decode(responseLine, RouteResponse.class);
    }

    /**
     * Low-level send: opens socket, writes one line, reads one line, closes.
     */
    String sendRaw(String requestLine) throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), false, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            out.println(requestLine);
            out.flush();

            String response = in.readLine();
            if (response == null) {
                throw new IOException("Server closed connection before responding");
            }
            return response;
        }
    }
}
