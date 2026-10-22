package com.example.fleetmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.fleetmanager.model.Task;
import com.example.fleetmanager.model.TaskStatus;
import com.example.fleetmanager.service.TaskService;

@SpringBootTest
@Transactional
class TaskServiceTests {

    @Autowired
    private TaskService taskService;

    @Test
    void testCreateTaskSuccess() {
        Task task = new Task(null, "A", "C", 5);
        Task created = taskService.createTask(task);

        assertNotNull(created.getId());
        assertEquals("A", created.getPickupNode());
        assertEquals("C", created.getDestinationNode());
        assertEquals(5, created.getPriority());
        assertEquals(TaskStatus.CREATED, created.getStatus());
    }

    @Test
    void testCreateTaskRejectsSamePickupAndDestination() {
        Task task = new Task(null, "A", "A", 1);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            taskService.createTask(task);
        });
        assertTrue(ex.getMessage().contains("Pickup and destination nodes cannot be the same"));
    }

    @Test
    void testCreateTaskRejectsMissingPickup() {
        Task task = new Task(null, "", "C", 1);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            taskService.createTask(task);
        });
        assertTrue(ex.getMessage().contains("Pickup node is required"));
    }

    @Test
    void testGetTasksByStatus() {
        Task task = new Task(null, "B", "E", 2);
        taskService.createTask(task);

        List<Task> createdTasks = taskService.getTasksByStatus(TaskStatus.CREATED);
        assertTrue(createdTasks.size() >= 1);
    }
}
