# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & run

```bash
# Compile all modules and run all tests
mvn clean test

# Run a single test class (from server module)
mvn test -pl server -Dtest=IAlgoShortestPathTest

# Start the server (port 8080) — from project root
mvn exec:java@server -pl server

# Start the JavaFX client — from project root
mvn javafx:run -pl client

# Build standalone server fat-jar
mvn package -pl algorithm,server
java -jar server/target/server-1.0-SNAPSHOT.jar
```

Server accepts optional args: `-Dexec.args="<port> <campus.json> <history.json>"`.

## Module structure

Maven multi-module project — each sub-directory is a self-contained Maven module:

```
campus-navigation-parent  (root pom.xml — aggregator, packaging=pom)
├── algorithm/            — standalone algorithm JAR (no external deps except JUnit for tests)
├── server/               — depends on algorithm; runs the TCP server
└── client/               — depends on server (for shared protocol); runs the JavaFX UI
```

## Architecture

Two separate JVM processes communicate over a single-shot TCP connection (one JSON line each way):

**Server** (`com.hit.server`): `ServerMain` → `Server implements Runnable` (thread pool) → `ClientHandler` (one per connection) → `RequestDispatcher` → `ControllerFactory` → `IController` implementation

Controllers registered in `ServerMain`:
- `"route/find"` → `RouteController` → `NavigationService`
- `"history/get"` → `HistoryGetController` → `HistoryService`
- `"history/clear"` → `HistoryClearController` → `HistoryService`

**Wire protocol envelope** (`com.hit.protocol`):
```json
Request:  {"headers":{"action":"route/find"}, "body":{...RouteRequest...}}
Response: {"status":"OK","path":[...],"cost":123}
```
Serialized with Gson via `ProtocolCodec`.

**Client** (`com.hit.client`): `ClientApp` (JavaFX `Application`) → `ClientViewController` (MVC View) → `ClientController` (MVC Controller) → `NavigationClient` (socket wrapper sending the envelope)

**Algorithm module** (`com.hit.algorithm`): `IAlgoShortestPath` interface with two Strategy implementations — `DijkstraAlgoShortestPathImpl` (weighted, FASTEST) and `BFSAlgoShortestPathImpl` (unweighted, FEWEST_SEGMENTS). Both extend `AbstractAlgoShortestPath` (Template Method). `AlgorithmFactory` in `com.hit.service` maps `Mode` → implementation.

**Domain model** (`com.hit.dm`): `Campus` contains a list of `Building`s and `Walkway`s. Each `Building` has `(x,y,height)` for the isometric view and `(lat,lon)` for the map view.

**Persistence** (`com.hit.dao`): `FileCampusDAO` reads `server/src/main/resources/campus.json`. `FileHistoryDAO` appends to `data/history.json` (server-side only, created on first route request).

**Client views**: `SchematicCanvas` — JavaFX Canvas isometric renderer with `IsometricProjection` math and `FxRouteAnimator` (AnimationTimer). `LeafletMapView` — JavaFX WebView wrapping Leaflet.js for real OpenStreetMap tiles. Both tabs live in a `TabPane` inside `ClientViewController`. History is shown in a `TableView`.

## Key data file

`server/src/main/resources/campus.json` — 13 HIT buildings with schematic coordinates, heights, GPS coordinates, and weighted walkway connections.

## Dependencies

- **Gson 2.11** — JSON serialization (wire protocol + persistence)
- **JUnit 4.13.2** — test framework (HIT course default)
- **JavaFX 21.0.4** — GUI framework (`javafx-controls`, `javafx-fxml`, `javafx-web`)
- Java 17 target bytecode; project compiled with Java 25
