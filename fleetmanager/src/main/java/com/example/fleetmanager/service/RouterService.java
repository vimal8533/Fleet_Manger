package com.example.fleetmanager.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fleetmanager.model.WarehouseGraph;

@Service
public class RouterService {

    private final WarehouseGraph warehouseGraph;

    public RouterService(WarehouseGraph warehouseGraph) {
        this.warehouseGraph = warehouseGraph;
    }

    private record NodeDistance(String node, int distance) {}

    public List<String> findShortestPath(String start, String destination) {
        if (start == null || destination == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and destination nodes cannot be null");
        }

        String source = start.trim().toUpperCase();
        String target = destination.trim().toUpperCase();

        if (!warehouseGraph.hasNode(source)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Start node does not exist in graph: " + source);
        }
        if (!warehouseGraph.hasNode(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination node does not exist in graph: " + target);
        }

        if (source.equals(target)) {
            return List.of(source);
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::distance));

        for (String node : warehouseGraph.getNodes()) {
            distances.put(node, Integer.MAX_VALUE);
        }

        distances.put(source, 0);
        pq.add(new NodeDistance(source, 0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String u = current.node();

            if (current.distance() > distances.get(u)) {
                continue;
            }

            if (u.equals(target)) {
                break;
            }

            for (WarehouseGraph.Edge edge : warehouseGraph.getNeighbors(u)) {
                String v = edge.target();
                int newDist = distances.get(u) + edge.weight();

                if (newDist < distances.getOrDefault(v, Integer.MAX_VALUE)) {
                    distances.put(v, newDist);
                    predecessors.put(v, u);
                    pq.add(new NodeDistance(v, newDist));
                }
            }
        }

        if (!predecessors.containsKey(target)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Destination node " + target + " is unreachable from " + source);
        }

        // Reconstruct path
        List<String> path = new ArrayList<>();
        String curr = target;
        while (curr != null) {
            path.add(curr);
            curr = predecessors.get(curr);
        }
        Collections.reverse(path);
        return path;
    }

    public List<String> findShortestPathAvoidingNodes(String start, String destination, Set<String> blockedNodes) {
        if (start == null || destination == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and destination nodes cannot be null");
        }

        String source = start.trim().toUpperCase();
        String target = destination.trim().toUpperCase();

        if (source.equals(target)) {
            return List.of(source);
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::distance));

        for (String node : warehouseGraph.getNodes()) {
            distances.put(node, Integer.MAX_VALUE);
        }

        distances.put(source, 0);
        pq.add(new NodeDistance(source, 0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String u = current.node();

            if (current.distance() > distances.get(u)) {
                continue;
            }

            if (u.equals(target)) {
                break;
            }

            for (WarehouseGraph.Edge edge : warehouseGraph.getNeighbors(u)) {
                String v = edge.target();

                // If v is blocked by another robot and is not the destination, skip it
                if (blockedNodes != null && blockedNodes.contains(v) && !v.equals(target)) {
                    continue;
                }

                int newDist = distances.get(u) + edge.weight();

                if (newDist < distances.getOrDefault(v, Integer.MAX_VALUE)) {
                    distances.put(v, newDist);
                    predecessors.put(v, u);
                    pq.add(new NodeDistance(v, newDist));
                }
            }
        }

        if (!predecessors.containsKey(target)) {
            // No bypass detour found; fallback to regular path
            return findShortestPath(start, destination);
        }

        List<String> path = new ArrayList<>();
        String curr = target;
        while (curr != null) {
            path.add(curr);
            curr = predecessors.get(curr);
        }
        Collections.reverse(path);
        return path;
    }

    public int calculatePathDistance(List<String> path) {
        if (path == null || path.size() <= 1) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            int weight = warehouseGraph.getNeighbors(from).stream()
                    .filter(e -> e.target().equalsIgnoreCase(to))
                    .mapToInt(WarehouseGraph.Edge::weight)
                    .findFirst()
                    .orElse(1);
            total += weight;
        }
        return total;
    }
}
