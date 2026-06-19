package com.hit.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The TCP server: owns the {@link ServerSocket} and a thread pool, then
 * accepts incoming connections in a loop and hands each one to a
 * {@link ClientHandler} worker.
 * <p>
 * Implements {@link Runnable} so callers can do {@code new Thread(server).start()},
 * matching the course-required pattern from the assignment spec.
 * The pool size is fixed (10) — plenty for a student-load demo and
 * deterministic for the defense.
 */
public class Server implements Runnable {

    private static final Logger LOG       = Logger.getLogger(Server.class.getName());
    private static final int    POOL_SIZE = 10;

    private final int               port;
    private final RequestDispatcher dispatcher;
    private final ExecutorService   pool;

    private ServerSocket serverSocket;
    private volatile boolean running;

    public Server(int port, RequestDispatcher dispatcher) {
        this.port       = port;
        this.dispatcher = dispatcher;
        this.pool       = Executors.newFixedThreadPool(POOL_SIZE);
    }

    /**
     * Binds the server socket and runs the accept loop until {@link #stop()}.
     * Required by {@link Runnable}; IO errors are caught and logged here
     * since {@code run()} cannot declare checked exceptions.
     */
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "Cannot bind to port " + port, ioe);
            return;
        }
        running = true;
        LOG.info("Campus navigation server listening on port " + port);

        while (running) {
            try {
                Socket client = serverSocket.accept();
                pool.submit(new ClientHandler(client, dispatcher));
            } catch (IOException ioe) {
                if (running) {
                    LOG.log(Level.WARNING, "accept() failed", ioe);
                }
                // If running == false, the socket was closed by stop() — exit cleanly.
            }
        }
    }

    /** Stops accepting new connections and shuts down the worker pool. */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ioe) {
            LOG.log(Level.WARNING, "Error closing server socket", ioe);
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("Server stopped");
    }
}
