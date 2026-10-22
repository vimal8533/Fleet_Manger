package com.example.fleetmanager.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.fleetmanager.model.Robot;
import com.example.fleetmanager.model.Task;
import com.example.fleetmanager.model.TaskStatus;
import com.example.fleetmanager.repository.TaskRepository;

@Service
public class RobotMonitorService {

    private final RobotService robotService;
    private final DispatcherService dispatcherService;
    private final RouterService routerService;
    private final TaskRepository taskRepository;
    private final ResourceSchedulerService resourceScheduler;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public RobotMonitorService(RobotService robotService,
                               DispatcherService dispatcherService,
                               RouterService routerService,
                               TaskRepository taskRepository,
                               ResourceSchedulerService resourceScheduler) {
        this.robotService = robotService;
        this.dispatcherService = dispatcherService;
        this.routerService = routerService;
        this.taskRepository = taskRepository;
        this.resourceScheduler = resourceScheduler;
    }

    @Scheduled(fixedDelay = 4000)
    public void checkRobotStatus() {
        // Step 1: Dispatch any pending CREATED tasks to available IDLE robots
        dispatcherService.dispatchPendingTasks();

        List<Robot> currentRobots = robotService.getAllRobots();
        List<Robot> updatedRobots = new ArrayList<>();

        for (Robot r : currentRobots) {
            String id = r.id();
            String node = r.Currentnode();
            int battery = r.batterypercent();
            String status = r.status();

            Optional<Task> activeTaskOpt = dispatcherService.getActiveTaskForRobot(id);

            if (activeTaskOpt.isPresent()) {
                Task task = activeTaskOpt.get();

                if (task.getStatus() == TaskStatus.ASSIGNED) {
                    // Stage 1: Moving to Pickup Node
                    if (node.equalsIgnoreCase(task.getPickupNode())) {
                        task.setStatus(TaskStatus.RUNNING);
                        taskRepository.save(task);

                        // Advance toward destination
                        MoveResult result = attemptMove(id, node, task.getDestinationNode());
                        node = result.nextNode();
                        status = result.status();
                        battery = Math.max(0, battery - (result.moved() ? 3 : 1));

                        if (node.equalsIgnoreCase(task.getDestinationNode())) {
                            task.setStatus(TaskStatus.COMPLETED);
                            taskRepository.save(task);
                            resourceScheduler.releaseAllForRobot(id);
                            status = "IDLE";
                        }
                    } else {
                        MoveResult result = attemptMove(id, node, task.getPickupNode());
                        node = result.nextNode();
                        status = result.status();
                        battery = Math.max(0, battery - (result.moved() ? 3 : 1));

                        if (node.equalsIgnoreCase(task.getPickupNode())) {
                            task.setStatus(TaskStatus.RUNNING);
                            taskRepository.save(task);
                        }
                    }

                } else if (task.getStatus() == TaskStatus.RUNNING) {
                    // Stage 2: Moving from Pickup to Destination Node
                    if (node.equalsIgnoreCase(task.getDestinationNode())) {
                        task.setStatus(TaskStatus.COMPLETED);
                        taskRepository.save(task);
                        resourceScheduler.releaseAllForRobot(id);
                        status = "IDLE"; // Stop moving at destination
                    } else {
                        MoveResult result = attemptMove(id, node, task.getDestinationNode());
                        node = result.nextNode();
                        status = result.status();
                        battery = Math.max(0, battery - (result.moved() ? 3 : 1));

                        if (node.equalsIgnoreCase(task.getDestinationNode())) {
                            task.setStatus(TaskStatus.COMPLETED);
                            taskRepository.save(task);
                            resourceScheduler.releaseAllForRobot(id);
                            status = "IDLE"; // Stop moving at destination
                        }
                    }
                }

                // If battery drops critically during task, safely fail task
                if (battery <= 10) {
                    task.setStatus(TaskStatus.FAILED);
                    taskRepository.save(task);
                    resourceScheduler.releaseAllForRobot(id);
                    status = "CHARGING";
                }

            } else {
                // Free/Standby Robot: Ensure any stale reservations are cleared
                resourceScheduler.releaseAllForRobot(id);

                if ("CHARGING".equalsIgnoreCase(status)) {
                    battery = Math.min(100, battery + 6);
                    if (battery >= 100) {
                        status = "IDLE";
                    }
                } else if (battery <= 20) {
                    status = "CHARGING";
                } else {
                    status = "IDLE"; // Standby at current node
                }
            }

            updatedRobots.add(new Robot(id, node, battery, status));
        }

        robotService.updateRobots(updatedRobots);

        System.out.println("\n-----------------------------------------------------");
        System.out.println("🤖 Fleet Monitor Ping [" + LocalTime.now().format(TIME_FORMATTER) + "]");
        System.out.println("-----------------------------------------------------");
        for (Robot r : updatedRobots) {
            Optional<Task> activeTask = dispatcherService.getActiveTaskForRobot(r.id());
            String taskInfo = activeTask.map(t -> String.format("[%s: %s->%s (%s)]",
                    t.getId(), t.getPickupNode(), t.getDestinationNode(), t.getStatus())).orElse("No Task (Standby)");
            System.out.printf("  %-4s -> Node: %-2s | Battery: %3d%% | Status: %-8s | %s%n",
                    r.id(), r.Currentnode(), r.batterypercent(), r.status(), taskInfo);
        }
        System.out.println("-----------------------------------------------------");
    }

    private record MoveResult(String nextNode, String status, boolean moved) {}

    private MoveResult attemptMove(String robotId, String currentNode, String targetNode) {
        if (currentNode.equalsIgnoreCase(targetNode)) {
            return new MoveResult(currentNode, "IDLE", false);
        }

        List<String> path;
        try {
            // Check if deadlock avoidance / detour routing should trigger
            if (resourceScheduler.isDeadlockDetected(robotId)) {
                Set<String> blockedNodes = new HashSet<>(resourceScheduler.getNodeReservations().keySet());
                blockedNodes.remove(currentNode); // Can use own node
                path = routerService.findShortestPathAvoidingNodes(currentNode, targetNode, blockedNodes);
                System.out.printf("⚠️ Deadlock Bypass triggered for %s! Detour path: %s%n", robotId, path);
            } else {
                path = routerService.findShortestPath(currentNode, targetNode);
            }

            if (path.size() >= 2) {
                String nextHop = path.get(1);

                // Request exclusive reservation on target node and edge corridor
                boolean reserved = resourceScheduler.tryAcquireReservation(robotId, currentNode, nextHop);

                if (reserved) {
                    // Lock acquired! Proceed safely
                    resourceScheduler.releaseNode(robotId, currentNode);
                    resourceScheduler.releaseEdge(robotId, currentNode, nextHop);
                    return new MoveResult(nextHop, "MOVING", true);
                } else {
                    // Collision prevented! Hold position safely
                    return new MoveResult(currentNode, "WAITING", false);
                }
            }
        } catch (Exception e) {
            // Unreachable or graph error
        }

        return new MoveResult(currentNode, "WAITING", false);
    }
}
