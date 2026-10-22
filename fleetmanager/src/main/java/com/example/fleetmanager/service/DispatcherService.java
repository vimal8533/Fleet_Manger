package com.example.fleetmanager.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleetmanager.model.Robot;
import com.example.fleetmanager.model.Task;
import com.example.fleetmanager.model.TaskStatus;
import com.example.fleetmanager.repository.RobotRepository;
import com.example.fleetmanager.repository.TaskRepository;

@Service
public class DispatcherService {

    public static final int MIN_BATTERY_FOR_DISPATCH = 25;

    private final TaskRepository taskRepository;
    private final RobotRepository robotRepository;
    private final RouterService routerService;

    public DispatcherService(TaskRepository taskRepository,
                             RobotRepository robotRepository,
                             RouterService routerService) {
        this.taskRepository = taskRepository;
        this.robotRepository = robotRepository;
        this.routerService = routerService;
    }

    @Transactional
    public synchronized int dispatchPendingTasks() {
        List<Task> pendingTasks = taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(TaskStatus.CREATED);
        if (pendingTasks.isEmpty()) {
            return 0;
        }

        List<Robot> allRobots = robotRepository.findAll();
        int assignedCount = 0;

        for (Task task : pendingTasks) {
            Optional<Robot> selectedRobot = selectEligibleRobot(task, allRobots);
            if (selectedRobot.isPresent()) {
                Robot robot = selectedRobot.get();
                task.setAssignedRobotId(robot.getId());
                task.setStatus(TaskStatus.ASSIGNED);
                taskRepository.save(task);

                // Lock this robot for the remainder of this dispatch cycle
                robot.setStatus("MOVING");
                robotRepository.save(robot);

                assignedCount++;
            }
        }

        return assignedCount;
    }

    public Optional<Robot> selectEligibleRobot(Task task, List<Robot> candidates) {
        return candidates.stream()
                .filter(r -> "IDLE".equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getBatterypercent() >= MIN_BATTERY_FOR_DISPATCH)
                .filter(r -> !isRobotAlreadyAssigned(r.getId()))
                .min(Comparator.comparingInt((Robot r) -> calculateDistanceToPickup(r, task.getPickupNode()))
                        .thenComparing(Comparator.comparingInt(Robot::getBatterypercent).reversed()));
    }

    public boolean isRobotAlreadyAssigned(String robotId) {
        return taskRepository.findFirstByAssignedRobotIdAndStatusIn(
                robotId,
                List.of(TaskStatus.ASSIGNED, TaskStatus.RUNNING)
        ).isPresent();
    }

    public Optional<Task> getActiveTaskForRobot(String robotId) {
        return taskRepository.findFirstByAssignedRobotIdAndStatusIn(
                robotId,
                List.of(TaskStatus.ASSIGNED, TaskStatus.RUNNING)
        );
    }

    private int calculateDistanceToPickup(Robot robot, String pickupNode) {
        try {
            String currentNode = robot.getCurrentnode();
            if (currentNode == null || currentNode.isEmpty()) {
                currentNode = "A";
            }
            List<String> path = routerService.findShortestPath(currentNode, pickupNode);
            return routerService.calculatePathDistance(path);
        } catch (Exception e) {
            return Integer.MAX_VALUE / 2; // Unreachable fallback
        }
    }
}
