package com.example.fleetmanager.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fleetmanager.model.WarehouseGraph;
import com.example.fleetmanager.service.RouterService;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    private final WarehouseGraph warehouseGraph;
    private final RouterService routerService;
    private final com.example.fleetmanager.service.ResourceSchedulerService resourceSchedulerService;

    public WarehouseController(WarehouseGraph warehouseGraph,
                               RouterService routerService,
                               com.example.fleetmanager.service.ResourceSchedulerService resourceSchedulerService) {
        this.warehouseGraph = warehouseGraph;
        this.routerService = routerService;
        this.resourceSchedulerService = resourceSchedulerService;
    }

    public record RouteResponse(String from, String to, List<String> path, int totalDistance) {}

    public record GraphResponse(
            Map<String, WarehouseGraph.NodeCoordinate> nodes,
            Map<String, List<WarehouseGraph.Edge>> adjacencyList
    ) {}

    public record ReservationsResponse(
            Map<String, String> nodes,
            Map<String, String> edges
    ) {}

    @GetMapping("/graph")
    public GraphResponse getGraph() {
        return new GraphResponse(
                warehouseGraph.getNodeCoordinates(),
                warehouseGraph.getAdjacencyList()
        );
    }

    @GetMapping("/reservations")
    public ReservationsResponse getReservations() {
        return new ReservationsResponse(
                resourceSchedulerService.getNodeReservations(),
                resourceSchedulerService.getEdgeReservations()
        );
    }

    @GetMapping("/route")
    public RouteResponse getRoute(
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        List<String> path = routerService.findShortestPath(from, to);
        int distance = routerService.calculatePathDistance(path);
        return new RouteResponse(from.toUpperCase(), to.toUpperCase(), path, distance);
    }
}
