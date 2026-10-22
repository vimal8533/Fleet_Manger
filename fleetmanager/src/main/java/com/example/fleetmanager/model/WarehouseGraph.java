package com.example.fleetmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class WarehouseGraph {

    public record Edge(String target, int weight) {}

    public record NodeCoordinate(String id, String name, int x, int y) {}

    private final Map<String, List<Edge>> adjacencyList = new HashMap<>();
    private final Map<String, NodeCoordinate> nodeCoordinates = new HashMap<>();

    public WarehouseGraph() {
        initializeDefaultGraph();
    }

    private void initializeDefaultGraph() {
        // Warehouse Node Coordinates (for 2D map visualization)
        addNode("A", "Charging Station Alpha", 100, 80);
        addNode("B", "Assembly Dock Beta", 350, 80);
        addNode("C", "Sorting Hub Gamma", 450, 240);
        addNode("D", "Storage Bay Delta", 250, 320);
        addNode("E", "Outbound Port Epsilon", 50, 240);

        // Outer Ring Connections
        addBidirectionalEdge("A", "B", 1);
        addBidirectionalEdge("B", "C", 1);
        addBidirectionalEdge("C", "D", 1);
        addBidirectionalEdge("D", "E", 1);
        addBidirectionalEdge("E", "A", 1);

        // Internal Cross-Aisles (Shortcuts)
        addBidirectionalEdge("B", "D", 2);
        addBidirectionalEdge("A", "C", 2);
    }

    public void addNode(String id, String name, int x, int y) {
        String key = id.toUpperCase();
        nodeCoordinates.put(key, new NodeCoordinate(key, name, x, y));
        adjacencyList.putIfAbsent(key, new ArrayList<>());
    }

    public void addBidirectionalEdge(String node1, String node2, int weight) {
        String u = node1.toUpperCase();
        String v = node2.toUpperCase();
        adjacencyList.computeIfAbsent(u, k -> new ArrayList<>()).add(new Edge(v, weight));
        adjacencyList.computeIfAbsent(v, k -> new ArrayList<>()).add(new Edge(u, weight));
    }

    public Set<String> getNodes() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    public List<Edge> getNeighbors(String node) {
        return adjacencyList.getOrDefault(node.toUpperCase(), Collections.emptyList());
    }

    public Map<String, List<Edge>> getAdjacencyList() {
        return Collections.unmodifiableMap(adjacencyList);
    }

    public Map<String, NodeCoordinate> getNodeCoordinates() {
        return Collections.unmodifiableMap(nodeCoordinates);
    }

    public boolean hasNode(String node) {
        return node != null && adjacencyList.containsKey(node.toUpperCase());
    }
}
