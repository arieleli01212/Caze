package com.hit.client;

/**
 * Legacy entry point — delegates to {@link ClientApp}.
 * <p>
 * Kept so that any bookmark referencing the old class name still compiles.
 * The canonical launch command is now:
 * <pre>
 *   mvn javafx:run
 * </pre>
 */
public class ClientMain {

    private ClientMain() { /* no instances */ }

    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
