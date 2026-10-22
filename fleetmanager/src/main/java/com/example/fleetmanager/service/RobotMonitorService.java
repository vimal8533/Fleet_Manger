package com.example.fleetmanager.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.fleetmanager.model.Robot;

@Service
public class RobotMonitorService {

    private final RobotService robotService;
    private final Random random = new Random();
    private static final String[] NODES = {"A", "B", "C", "D", "E"};
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public RobotMonitorService(RobotService robotService) {
        this.robotService = robotService;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkRobotStatus() {
        List<Robot> currentRobots = robotService.getAllRobots();
        List<Robot> updatedRobots = new ArrayList<>();

        for (Robot r : currentRobots) {
            String id = r.id();
            String node = r.Currentnode();
            int battery = r.batterypercent();
            String status = r.status();

            switch (status.toUpperCase()) {
                case "CHARGING" -> {
                    battery = Math.min(100, battery + 5);
                    if (battery >= 100) {
                        status = "IDLE";
                    }
                }
                case "MOVING" -> {
                    battery = Math.max(0, battery - 4);
                    node = getNextNode(node);
                    if (battery <= 20) {
                        status = "CHARGING";
                    }
                }
                case "IDLE" -> {
                    if (battery <= 20) {
                        status = "CHARGING";
                    } else if (random.nextBoolean()) {
                        status = "MOVING";
                        node = getNextNode(node);
                        battery = Math.max(0, battery - 2);
                    } else {
                        battery = Math.max(0, battery - 1);
                    }
                }
                default -> status = "IDLE";
            }

            updatedRobots.add(new Robot(id, node, battery, status));
        }

        robotService.updateRobots(updatedRobots);

        System.out.println("\n-----------------------------------------------------");
        System.out.println("🤖 Fleet Monitor Ping [" + LocalTime.now().format(TIME_FORMATTER) + "]");
        System.out.println("-----------------------------------------------------");
        for (Robot r : updatedRobots) {
            System.out.printf("  %s -> Node: %-2s | Battery: %3d%% | Status: %-8s%n",
                    r.id(), r.Currentnode(), r.batterypercent(), r.status());
        }
        System.out.println("-----------------------------------------------------");
    }

    private String getNextNode(String currentNode) {
        for (int i = 0; i < NODES.length; i++) {
            if (NODES[i].equalsIgnoreCase(currentNode)) {
                return NODES[(i + 1) % NODES.length];
            }
        }
        return NODES[0];
    }
}
