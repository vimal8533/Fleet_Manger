package com.example.fleetmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import com.example.fleetmanager.model.WarehouseGraph;
import com.example.fleetmanager.service.RouterService;

@SpringBootTest
class RouterServiceTests {

    @Autowired
    private RouterService routerService;

    @Autowired
    private WarehouseGraph warehouseGraph;

    @Test
    void testDirectRoute() {
        List<String> path = routerService.findShortestPath("A", "B");
        assertEquals(List.of("A", "B"), path);
        assertEquals(1, routerService.calculatePathDistance(path));
    }

    @Test
    void testMultiHopDijkstraRoute() {
        List<String> path = routerService.findShortestPath("A", "D");
        assertNotNull(path);
        assertEquals("A", path.get(0));
        assertEquals("D", path.get(path.size() - 1));
        int dist = routerService.calculatePathDistance(path);
        assertEquals(2, dist); // A -> E -> D (1+1=2) or A -> C -> D (2+1) or A -> B -> D (1+2)
    }

    @Test
    void testSameSourceAndDestination() {
        List<String> path = routerService.findShortestPath("C", "C");
        assertEquals(List.of("C"), path);
        assertEquals(0, routerService.calculatePathDistance(path));
    }

    @Test
    void testUnknownNodeThrowsException() {
        assertThrows(ResponseStatusException.class, () -> {
            routerService.findShortestPath("A", "Z");
        });
    }

    @Test
    void testGraphTopologyLoaded() {
        assertTrue(warehouseGraph.hasNode("A"));
        assertTrue(warehouseGraph.hasNode("B"));
        assertTrue(warehouseGraph.hasNode("C"));
        assertTrue(warehouseGraph.hasNode("D"));
        assertTrue(warehouseGraph.hasNode("E"));
    }
}
