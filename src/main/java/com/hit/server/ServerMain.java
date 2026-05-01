package com.hit.server;

import com.hit.dao.FileCampusDAO;
import com.hit.dao.FileHistoryDAO;
import com.hit.dao.ICampusDAO;
import com.hit.dao.IHistoryDAO;
import com.hit.dm.Campus;
import com.hit.service.NavigationService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the server JVM.
 * <p>
 * Wires together the DAO, service, dispatcher, and {@link Server},
 * then starts the accept loop. Loaded campus comes from the bundled
 * resource by default; pass a path argument to load from disk instead.
 *
 * <pre>
 *   mvn exec:java -Dexec.mainClass=com.hit.server.ServerMain
 *   mvn exec:java -Dexec.mainClass=com.hit.server.ServerMain -Dexec.args="8080 ./my-campus.json"
 * </pre>
 */
public final class ServerMain {

    private static final Logger LOG          = Logger.getLogger(ServerMain.class.getName());
    private static final int    DEFAULT_PORT = 8080;
    private static final Path   DEFAULT_HISTORY_PATH = Paths.get("data", "history.json");

    private ServerMain() { /* main only */ }

    public static void main(String[] args) {
        int  port        = (args.length >= 1) ? parsePort(args[0])      : DEFAULT_PORT;
        Path campusPath  = (args.length >= 2) ? Paths.get(args[1])      : null;
        Path historyPath = (args.length >= 3) ? Paths.get(args[2])      : DEFAULT_HISTORY_PATH;

        try {
            ICampusDAO campusDAO = (campusPath == null)
                    ? new FileCampusDAO()
                    : new FileCampusDAO(campusPath);
            Campus campus = campusDAO.load();
            LOG.info("Loaded campus with " + campus.getBuildings().size() + " buildings");

            IHistoryDAO historyDAO = new FileHistoryDAO(historyPath);
            NavigationService service = new NavigationService(campus, historyDAO);
            RequestDispatcher dispatcher = new RequestDispatcher(service);

            Server server = new Server(port, dispatcher);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown-hook"));
            server.start();

        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "Server failed to start", ioe);
            System.exit(1);
        }
    }

    private static int parsePort(String raw) {
        try {
            int p = Integer.parseInt(raw);
            if (p < 1 || p > 65535) throw new NumberFormatException();
            return p;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid port: " + raw);
        }
    }
}
