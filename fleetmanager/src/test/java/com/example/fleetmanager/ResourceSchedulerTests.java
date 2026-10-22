package com.example.fleetmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.fleetmanager.service.ResourceSchedulerService;
import com.example.fleetmanager.service.RouterService;

@SpringBootTest
class ResourceSchedulerTests {

    @Autowired
    private ResourceSchedulerService resourceScheduler;

    @Autowired
    private RouterService routerService;

    @BeforeEach
    void setUp() {
        resourceScheduler.releaseAllForRobot("R1");
        resourceScheduler.releaseAllForRobot("R2");
    }

    @Test
    void testExclusiveNodeReservationPreventsCollision() {
        // R1 acquires Node C
        boolean r1Acquired = resourceScheduler.tryAcquireReservation("R1", "A", "C");
        assertTrue(r1Acquired, "R1 should successfully acquire free Node C");

        // R2 attempts to acquire the same Node C -> must be denied!
        boolean r2Acquired = resourceScheduler.tryAcquireReservation("R2", "B", "C");
        assertFalse(r2Acquired, "R2 must be blocked from acquiring occupied Node C");

        // R1 releases Node C
        resourceScheduler.releaseNode("R1", "C");

        // R2 can now acquire Node C
        boolean r2Retry = resourceScheduler.tryAcquireReservation("R2", "B", "C");
        assertTrue(r2Retry, "R2 should acquire Node C after R1 releases it");
    }

    @Test
    void testHeadOnTrackCollisionPrevention() {
        // R1 reserves track A -> B
        boolean r1Track = resourceScheduler.tryAcquireReservation("R1", "A", "B");
        assertTrue(r1Track);

        // R2 attempts opposing direction B -> A on the same single corridor -> must be denied!
        boolean r2Opposing = resourceScheduler.tryAcquireReservation("R2", "B", "A");
        assertFalse(r2Opposing, "Opposing robot R2 must not enter corridor A-B while R1 holds it");

        resourceScheduler.releaseEdge("R1", "A", "B");
        resourceScheduler.releaseNode("R1", "B");

        // R2 can now enter corridor
        boolean r2Track = resourceScheduler.tryAcquireReservation("R2", "B", "A");
        assertTrue(r2Track);
    }

    @Test
    void testDeadlockCounterAndTrigger() {
        // Lock Node D with R1
        resourceScheduler.tryAcquireReservation("R1", "E", "D");

        // R2 fails twice
        resourceScheduler.tryAcquireReservation("R2", "C", "D");
        assertFalse(resourceScheduler.isDeadlockDetected("R2"));

        resourceScheduler.tryAcquireReservation("R2", "C", "D");
        assertTrue(resourceScheduler.isDeadlockDetected("R2"), "Deadlock should be detected after consecutive wait threshold");
    }

    @Test
    void testDynamicDetourRoutingAroundBlockedNode() {
        // Normal shortest route A -> D is A -> E -> D (distance 2)
        List<String> defaultRoute = routerService.findShortestPath("A", "D");
        assertEquals(List.of("A", "E", "D"), defaultRoute);

        // If Station E is blocked by another robot, router dynamically detours around E!
        List<String> detourRoute = routerService.findShortestPathAvoidingNodes("A", "D", Set.of("E"));
        assertNotNull(detourRoute);
        assertFalse(detourRoute.contains("E"), "Detour route must not pass through blocked Station E");
        assertEquals("A", detourRoute.get(0));
        assertEquals("D", detourRoute.get(detourRoute.size() - 1));
    }
}
