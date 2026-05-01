package com.hit.client;

import com.hit.client.controller.ClientController;
import com.hit.client.network.NavigationClient;
import com.hit.client.view.ClientView;
import com.hit.dao.FileCampusDAO;
import com.hit.dm.Campus;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry point for the Swing client JVM.
 * <p>
 * The client loads its own copy of the campus (just for drawing the map).
 * For routing it always asks the server — the server stays the source of truth.
 * <p>
 * Usage:
 * <pre>
 *   mvn exec:java -Dexec.mainClass=com.hit.client.ClientMain
 *   mvn exec:java -Dexec.mainClass=com.hit.client.ClientMain -Dexec.args="localhost 8080 ./campus.json"
 * </pre>
 */
public final class ClientMain {

    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 8080;

    private ClientMain() { /* main only */ }

    public static void main(String[] args) {
        String host = (args.length >= 1) ? args[0]                         : DEFAULT_HOST;
        int    port = (args.length >= 2) ? Integer.parseInt(args[1])       : DEFAULT_PORT;
        Path  cpath = (args.length >= 3) ? Paths.get(args[2])              : null;

        Campus campus;
        try {
            campus = (cpath == null) ? new FileCampusDAO().load()
                                     : new FileCampusDAO(cpath).load();
        } catch (IOException ioe) {
            System.err.println("Failed to load campus map: " + ioe.getMessage());
            System.exit(1);
            return;
        }

        NavigationClient client     = new NavigationClient(host, port);
        ClientController controller = new ClientController(client);

        SwingUtilities.invokeLater(() -> new ClientView(campus, controller).setVisible(true));
    }
}
