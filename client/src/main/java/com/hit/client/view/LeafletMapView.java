package com.hit.client.view;

import com.hit.dm.Building;
import com.hit.dm.Campus;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Real-map view using a JavaFX {@link WebView} embedding Leaflet.js.
 *
 * Leaflet JS and CSS are bundled in the classpath (client/src/main/resources/leaflet/)
 * and inlined into the HTML page so that no network access is required at startup.
 * OSM tile images are still fetched from the internet when the tab is shown.
 */
public class LeafletMapView extends StackPane {

    private static final Logger LOG = Logger.getLogger(LeafletMapView.class.getName());

    private final WebView   webView;
    private final WebEngine engine;
    private final Campus    campus;

    private boolean mapLoaded = false;

    public LeafletMapView(Campus campus) {
        this.campus  = campus;
        this.webView = new WebView();
        this.engine  = webView.getEngine();

        engine.setJavaScriptEnabled(true);

        // Log JS errors to the Java console so we can diagnose map issues.
        engine.setOnError(e -> LOG.log(Level.WARNING, "WebView error: " + e.getMessage()));
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            switch (state) {
                case SUCCEEDED -> mapLoaded = true;
                case FAILED    -> LOG.warning("LeafletMapView: page load FAILED — "
                        + engine.getLoadWorker().getException());
                default        -> {}
            }
        });

        getChildren().add(webView);
        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());
        engine.loadContent(buildHtml());
    }

    // ---- public API called by ClientViewController ----

    public void setHighlightedPath(List<String> path) {
        if (!mapLoaded) return;
        String arr = path.stream()
                .map(n -> "\"" + escape(n) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        engine.executeScript("highlightRoute(" + arr + ")");
    }

    public void clearHighlight() {
        if (!mapLoaded) return;
        engine.executeScript("clearRoute()");
    }

    public void setSource(Building b) {
        if (!mapLoaded || b == null) return;
        engine.executeScript("setSource(\"" + escape(b.getName()) + "\")");
    }

    public void setDestination(Building b) {
        if (!mapLoaded || b == null) return;
        engine.executeScript("setDestination(\"" + escape(b.getName()) + "\")");
    }

    public void clearSelection() {
        if (!mapLoaded) return;
        engine.executeScript("clearSelection()");
    }

    /** Call when the containing tab becomes visible so Leaflet re-measures and fills the pane. */
    public void invalidateSize() {
        if (!mapLoaded) return;
        engine.executeScript("map.invalidateSize(true)");
    }

    // ---- HTML generation ----

    private String buildHtml() {
        // Inline Leaflet from classpath — avoids CDN network calls in WebView.
        String leafletCss = loadResource("/leaflet/leaflet.min.css");
        String leafletJs  = loadResource("/leaflet/leaflet.min.js");

        String buildings = campus.getBuildings().values().stream()
                .filter(Building::hasGeoLocation)
                .map(b -> String.format(
                        "{\"name\":\"%s\",\"lat\":%.6f,\"lon\":%.6f}",
                        escape(b.getName()), b.getLat(), b.getLon()))
                .collect(Collectors.joining(",", "[", "]"));

        return "<!DOCTYPE html><html><head>"
                + "<meta charset='utf-8'/>"
                + "<style>" + leafletCss + "</style>"
                + "<style>"
                + "html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;}"
                + "#map{position:absolute;top:0;bottom:0;left:0;right:0;}"
                + "</style>"
                + "<script>" + leafletJs + "</script>"
                + "</head><body><div id='map'></div><script>"
                + "var map=L.map('map').setView([32.0177,34.8925],16);"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{"
                + "  attribution:'\\u00a9 OpenStreetMap contributors',maxZoom:19"
                + "}).addTo(map);"
                + "var buildings=" + buildings + ";"
                + "var markers={};"
                + "var routeLayer=null,srcLayer=null,dstLayer=null;"
                + "buildings.forEach(function(b){"
                + "  var m=L.circleMarker([b.lat,b.lon],{"
                + "    radius:8,fillColor:'#4488cc',color:'#fff',weight:2,"
                + "    opacity:1,fillOpacity:0.85"
                + "  }).addTo(map).bindPopup(b.name);"
                + "  markers[b.name]={marker:m,building:b};"
                + "});"
                + "function highlightRoute(path){"
                + "  if(routeLayer){map.removeLayer(routeLayer);}"
                + "  var ll=path.map(function(n){"
                + "    var m=markers[n];return m?[m.building.lat,m.building.lon]:null;"
                + "  }).filter(Boolean);"
                + "  if(ll.length>1)routeLayer=L.polyline(ll,"
                + "    {color:'#e63333',weight:5,opacity:0.85}).addTo(map);"
                + "}"
                + "function clearRoute(){"
                + "  if(routeLayer){map.removeLayer(routeLayer);routeLayer=null;}"
                + "}"
                + "function setSource(name){"
                + "  if(srcLayer){map.removeLayer(srcLayer);}"
                + "  var m=markers[name];"
                + "  if(m)srcLayer=L.circleMarker([m.building.lat,m.building.lon],"
                + "    {radius:11,fillColor:'#44bb66',color:'#fff',weight:3,"
                + "    opacity:1,fillOpacity:0.9}).addTo(map).bindPopup('From: '+name);"
                + "}"
                + "function setDestination(name){"
                + "  if(dstLayer){map.removeLayer(dstLayer);}"
                + "  var m=markers[name];"
                + "  if(m)dstLayer=L.circleMarker([m.building.lat,m.building.lon],"
                + "    {radius:11,fillColor:'#dd4433',color:'#fff',weight:3,"
                + "    opacity:1,fillOpacity:0.9}).addTo(map).bindPopup('To: '+name);"
                + "}"
                + "function clearSelection(){"
                + "  if(srcLayer){map.removeLayer(srcLayer);srcLayer=null;}"
                + "  if(dstLayer){map.removeLayer(dstLayer);dstLayer=null;}"
                + "  clearRoute();"
                + "}"
                + "</script></body></html>";
    }

    private static String loadResource(String path) {
        try (InputStream is = LeafletMapView.class.getResourceAsStream(path)) {
            if (is == null) {
                LOG.warning("Leaflet resource not found on classpath: " + path);
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load Leaflet resource: " + path, e);
            return "";
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'",  "\\'");
    }
}
