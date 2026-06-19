package com.hit.client.view;

import com.hit.dm.Building;
import com.hit.dm.Campus;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Real-map view using a JavaFX {@link WebView} embedding Leaflet.js.
 * <p>
 * Tiles are loaded from OpenStreetMap CDN directly in the browser engine,
 * bypassing any JVM TLS or tile-proxy issues. Building markers and route
 * overlays are controlled via JavaScript bridge calls after the map loads.
 */
public class LeafletMapView extends StackPane {

    private final WebView    webView;
    private final WebEngine  engine;
    private final Campus     campus;

    private boolean mapLoaded = false;

    public LeafletMapView(Campus campus) {
        this.campus  = campus;
        this.webView = new WebView();
        this.engine  = webView.getEngine();

        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                mapLoaded = true;
            }
        });

        getChildren().add(webView);
        engine.loadContent(buildHtml());
    }

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

    // ---- HTML generation ----

    private String buildHtml() {
        // Build JSON array of buildings that have GPS coordinates.
        String buildings = campus.getBuildings().values().stream()
                .filter(Building::hasGeoLocation)
                .map(b -> String.format(
                        "{\"name\":\"%s\",\"lat\":%.6f,\"lon\":%.6f}",
                        escape(b.getName()), b.getLat(), b.getLon()))
                .collect(Collectors.joining(",", "[", "]"));

        return "<!DOCTYPE html><html><head>"
                + "<meta charset='utf-8'/>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>body{margin:0;padding:0;}#map{width:100%;height:100vh;}</style>"
                + "</head><body><div id='map'></div><script>"
                + "var map=L.map('map').setView([32.0177,34.8925],16);"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{"
                + "attribution:'&copy; OpenStreetMap contributors',maxZoom:19}).addTo(map);"
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
                + "  var ll=path.map(function(n){var m=markers[n];"
                + "    return m?[m.building.lat,m.building.lon]:null;"
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

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'");
    }
}
