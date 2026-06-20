# HIT Campus Navigation

> Course project for **Advanced Programming in Java (PITI)** at HIT — Holon Institute of Technology.
> Implements all four parts (A–D) of the third mission assignment.

A multi-module client/server application that finds shortest routes across the HIT campus.
The server exposes a JSON-over-TCP API backed by two services; the JavaFX client lets the
user pick buildings, choose an algorithm, and watch an animated walker trace the route on
either an isometric 3D canvas or a live OpenStreetMap view.

---

## Module structure

```
campus-navigation-parent   (root pom.xml — aggregator, packaging=pom)
├── algorithm/   standalone JAR: IAlgoShortestPath, Dijkstra, BFS, Graph
├── server/      TCP server + NavigationService + HistoryService + DAO layer
└── client/      JavaFX MVC client (depends on server for shared protocol/domain)
```

---

## Quick start

```bash
# Build everything and run all tests (39 tests across all modules)
mvn clean test

# Terminal 1 — start the server on port 8080
mvn exec:java@server -pl server

# Terminal 2 — launch the JavaFX client
mvn javafx:run -pl client
```

Optional server args: `-Dexec.args="<port> <campus.json> <history.json>"`

Build a standalone server fat-jar:
```bash
mvn package -pl algorithm,server
java -jar server/target/server-1.0-SNAPSHOT.jar
```

---

## Architecture

Two separate JVM processes communicate over a single-shot TCP connection — one JSON line each way.

### Wire protocol

```json
Request:  {"headers":{"action":"route/find"}, "body":{"fromBuilding":"...","toBuilding":"...","mode":"FASTEST"}}
Response: {"status":"OK","path":["A","B","C"],"cost":123.0}
```

### Server (`com.hit.server`)

```
ServerMain → Server implements Runnable  (thread pool via ExecutorService)
           → ClientHandler               (one Runnable per TCP connection)
           → RequestDispatcher → ControllerFactory (Factory Pattern)
                                       → IController implementations

Registered actions:
  route/find      → RouteController      → NavigationService
  history/get     → HistoryGetController  → HistoryService
  history/clear   → HistoryClearController → HistoryService
```

I/O streams wrapped with `Scanner` / `PrintWriter` decorators (Decorator Pattern per spec).

### Algorithm module (`com.hit.algorithm`)

| Class | Pattern role | Routing mode |
|---|---|---|
| `IAlgoShortestPath` | Strategy interface | — |
| `AbstractAlgoShortestPath` | Template Method — shared validation | — |
| `DijkstraAlgoShortestPathImpl` | Concrete Strategy | `FASTEST` (weighted) |
| `BFSAlgoShortestPathImpl` | Concrete Strategy | `FEWEST_SEGMENTS` (unweighted) |

`AlgorithmFactory` (in `com.hit.service`) maps `Mode` → implementation.

### Client (`com.hit.client`)

```
ClientApp  (JavaFX Application)
  → ClientViewController  (View — all JavaFX nodes; no service/DAO imports)
  → ClientController      (Controller — framework-agnostic, no JavaFX imports)
  → NavigationClient      (Socket wrapper; sends envelope, reads response)
```

**Views (in a TabPane):**
- `SchematicCanvas` — JavaFX Canvas isometric renderer; tilt slider; `FxRouteAnimator` (AnimationTimer walker)
- `LeafletMapView` — JavaFX WebView wrapping Leaflet.js on real OpenStreetMap tiles; GPS-accurate overlays
- History `TableView` — browse and clear past route queries

Click-to-select and drag-to-pan work on the canvas; the map tab mirrors every route change via `engine.executeScript()`.

### Design patterns

| Pattern | Where |
|---|---|
| Strategy | `IAlgoShortestPath` — two interchangeable algorithm implementations |
| Template Method | `AbstractAlgoShortestPath` — shared validation + cost skeleton |
| Factory | `ControllerFactory` maps action strings → `IController`; `AlgorithmFactory` maps `Mode` → algorithm |
| Decorator | `Scanner(InputStreamReader(...))` / `PrintWriter(OutputStreamWriter(...))` I/O wrappers |
| MVC | `ClientViewController` (View) ↔ `ClientController` (Controller) ↔ `NavigationClient` (Model proxy) |
| DAO | `ICampusDAO` / `FileCampusDAO`, `IHistoryDAO` / `FileHistoryDAO` |

---

## Key files

| Path | Description |
|---|---|
| `server/src/main/resources/campus.json` | 13 HIT buildings — schematic coords, GPS coords, walkway weights |
| `data/history.json` | Route history written at runtime (gitignored, auto-created) |
| `submission/slides.pptx` | 3-slide submission deck (functional description, HL arch, challenges) |
| `submission/personal_details.txt` | Student details — **fill in your ID before submitting** |
| `submission/campus-navigation-submission.zip` | Ready-to-submit archive |

---

## Dependencies

| Library | Version | Use |
|---|---|---|
| Gson | 2.11.0 | JSON wire protocol + persistence |
| JUnit | 4.13.2 | Test framework (HIT course default) |
| JavaFX | 21.0.4 | GUI (`javafx-controls`, `javafx-fxml`, `javafx-web`) |

Java 17 target bytecode; compiled with Java 25.
