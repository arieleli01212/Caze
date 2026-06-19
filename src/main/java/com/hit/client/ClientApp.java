package com.hit.client;

import com.hit.client.controller.ClientController;
import com.hit.client.network.NavigationClient;
import com.hit.client.view.ClientViewController;
import com.hit.dao.FileCampusDAO;
import com.hit.dm.Campus;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * JavaFX entry point for the HIT Campus Navigation client (Part D).
 * <p>
 * Loads the campus model, wires the MVC components together, and
 * shows the main window. Command-line args (optional):
 * <pre>
 *   arg[0] = server host (default: localhost)
 *   arg[1] = server port (default: 8080)
 *   arg[2] = campus JSON path (default: classpath resource)
 * </pre>
 * Run with:
 * <pre>
 *   mvn javafx:run
 *   mvn javafx:run -Djavafx.args="localhost 8080"
 * </pre>
 */
public class ClientApp extends Application {

    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 8080;

    @Override
    public void start(Stage primaryStage) throws Exception {
        List<String> params = getParameters().getRaw();
        String host = params.size() >= 1 ? params.get(0)                  : DEFAULT_HOST;
        int    port = params.size() >= 2 ? Integer.parseInt(params.get(1)) : DEFAULT_PORT;

        Campus campus;
        try {
            campus = (params.size() >= 3)
                    ? new FileCampusDAO(Paths.get(params.get(2))).load()
                    : new FileCampusDAO().load();
        } catch (IOException ioe) {
            System.err.println("Failed to load campus: " + ioe.getMessage());
            System.exit(1);
            return;
        }

        NavigationClient     networkClient = new NavigationClient(host, port);
        ClientController     controller    = new ClientController(networkClient);
        ClientViewController viewCtrl      = new ClientViewController(campus, controller);
        viewCtrl.buildScene(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
