package com.example.fleetmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.fleetmanager.model.Robot;
import com.example.fleetmanager.model.Task;
import com.example.fleetmanager.model.TaskStatus;
import com.example.fleetmanager.repository.RobotRepository;
import com.example.fleetmanager.repository.TaskRepository;
import com.example.fleetmanager.service.DispatcherService;
import com.example.fleetmanager.service.RouterService;

@SpringBootTest
@Transactional
class DispatcherServiceTests {

    @Autowired
    private DispatcherService dispatcherService;

    @Autowired
    private RouterService routerService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RobotRepository robotRepository;

    @Test
    void testRobotSelectionCriteria() {
        Task task = new Task("T-TEST-1", "A", "C", 5);

        Robot lowBattery = new Robot("R-LOW", "A", 15, "IDLE");
        Robot charging = new Robot("R-CHG", "A", 90, "CHARGING");
        Robot moving = new Robot("R-MOV", "A", 85, "MOVING");
        Robot eligible = new Robot("R-GOOD", "B", 80, "IDLE");

        List<Robot> candidates = List.of(lowBattery, charging, moving, eligible);
        Optional<Robot> chosen = dispatcherService.selectEligibleRobot(task, candidates);

        assertTrue(chosen.isPresent());
        assertEquals("R-GOOD", chosen.get().getId());
    }

    @Test
    void testDoubleAssignmentPrevention() {
        // Setup 1 idle robot
        robotRepository.deleteAll();
        taskRepository.deleteAll();

        Robot robot = new Robot("R-SOLO", "A", 95, "IDLE");
        robotRepository.save(robot);

        // Create 2 tasks
        Task task1 = new Task("T-FIRST", "A", "B", 10);
        Task task2 = new Task("T-SECOND", "C", "D", 5);
        taskRepository.save(task1);
        taskRepository.save(task2);

        // Run dispatch
        int assignedCount = dispatcherService.dispatchPendingTasks();

        // Exactly 1 task should be assigned to R-SOLO; second task must stay CREATED
        assertEquals(1, assignedCount);

        Task updatedTask1 = taskRepository.findById("T-FIRST").orElseThrow();
        Task updatedTask2 = taskRepository.findById("T-SECOND").orElseThrow();

        assertEquals(TaskStatus.ASSIGNED, updatedTask1.getStatus());
        assertEquals("R-SOLO", updatedTask1.getAssignedRobotId());

        assertEquals(TaskStatus.CREATED, updatedTask2.getStatus());
        assertEquals(null, updatedTask2.getAssignedRobotId());

        // Verify robot is recognized as already assigned
        assertTrue(dispatcherService.isRobotAlreadyAssigned("R-SOLO"));
    }

    @Test
    void testUnreachableOrInvalidDestination() {
        assertThrows(ResponseStatusException.class, () -> {
            routerService.findShortestPath("A", "NON_EXISTENT_NODE");
        });
    }

    @Test
    void testValidDijkstraRoutes() {
        List<String> path = routerService.findShortestPath("A", "C");
        assertNotNull(path);
        assertTrue(path.size() >= 2);
        assertEquals("A", path.get(0));
        assertEquals("C", path.get(path.size() - 1));
    }
}
