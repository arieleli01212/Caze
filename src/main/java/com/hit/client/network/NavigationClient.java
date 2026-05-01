package com.hit.client.network;

import com.hit.protocol.ProtocolCodec;
import com.hit.protocol.RouteRequest;
import com.hit.protocol.RouteResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Thin client-side wrapper around a TCP socket conversation with the server.
 * Each call to {@link #send} opens a new socket, writes one request line,
 * reads one response line, and closes — matching the server's one-shot model.
 * <p>
 * The class knows nothing about Swing or the controller. That isolation
 * keeps it trivially mockable in tests.
 */
public class NavigationClient {

    private final String host;
    private final int    port;

    public NavigationClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Sends a {@link RouteRequest} and blocks for the response.
     *
     * @throws IOException if the connection cannot be established or fails midway
     */
    public RouteResponse send(RouteRequest request) throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), false, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            out.println(ProtocolCodec.encode(request));
            out.flush();

            String responseLine = in.readLine();
            if (responseLine == null) {
                throw new IOException("Server closed connection before responding");
            }
            return ProtocolCodec.decode(responseLine, RouteResponse.class);
        }
    }
}
