package com.example.fleetmanager.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fleetmanager.model.Task;
import com.example.fleetmanager.model.TaskStatus;
import com.example.fleetmanager.repository.TaskRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(String id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found: " + id
            ));
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public Task createTask(Task newTask) {
        if (newTask == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task body cannot be null");
        }

        if (newTask.getPickupNode() == null || newTask.getPickupNode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup node is required");
        }

        if (newTask.getDestinationNode() == null || newTask.getDestinationNode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination node is required");
        }

        String pickup = newTask.getPickupNode().trim().toUpperCase();
        String destination = newTask.getDestinationNode().trim().toUpperCase();

        if (pickup.equalsIgnoreCase(destination)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup and destination nodes cannot be the same");
        }

        String taskId = newTask.getId();
        if (taskId == null || taskId.trim().isEmpty()) {
            taskId = "TSK-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } else {
            taskId = taskId.trim().toUpperCase();
            if (taskRepository.existsById(taskId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Task already exists with ID: " + taskId);
            }
        }

        int priority = newTask.getPriority() <= 0 ? 1 : newTask.getPriority();

        Task taskToSave = new Task(taskId, pickup, destination, priority);
        taskToSave.setStatus(TaskStatus.CREATED);

        return taskRepository.save(taskToSave);
    }

    public Task updateTask(Task task) {
        return taskRepository.save(task);
    }
}
