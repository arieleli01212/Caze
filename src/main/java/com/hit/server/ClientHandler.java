package com.hit.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker that handles a single client connection on its own thread.
 * <p>
 * Protocol: one request line of JSON in, one response line of JSON out, then
 * close. Keeping it one-shot keeps state management trivial; if we wanted
 * persistent connections later, we'd just loop on {@code readLine}.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket            socket;
    private final RequestDispatcher dispatcher;

    public ClientHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket     = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress().toString();
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     s.getOutputStream(), false, StandardCharsets.UTF_8)) {

            String requestLine = in.readLine();
            if (requestLine == null) {
                LOG.fine(() -> "Client " + remote + " disconnected before sending");
                return;
            }
            LOG.fine(() -> "<- " + remote + " " + requestLine);

            String responseLine = dispatcher.dispatch(requestLine);
            out.println(responseLine);
            out.flush();
            LOG.fine(() -> "-> " + remote + " " + responseLine);

        } catch (IOException io) {
            LOG.log(Level.WARNING, "I/O error talking to " + remote, io);
        }
    }
}
