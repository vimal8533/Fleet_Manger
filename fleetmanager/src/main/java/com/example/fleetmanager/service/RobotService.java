package com.example.fleetmanager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fleetmanager.model.Robot;

@Service
public class RobotService {

    private final List<Robot> robots = new CopyOnWriteArrayList<>(List.of(
        new Robot("R1", "A", 85, "IDLE"),
        new Robot("R2", "B", 60, "MOVING"),
        new Robot("R3", "C", 18, "CHARGING")
    ));

    public List<Robot> getAllRobots() {
        return new ArrayList<>(robots);
    }

    public Robot getRobotById(String id) {
        for (Robot robot : robots) {
            if (robot.id().equalsIgnoreCase(id) || robot.id().equalsIgnoreCase("R" + id)) {
                return robot;
            }
        }

        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Robot not found: " + id
        );
    }

    public void updateRobots(List<Robot> updatedList) {
        robots.clear();
        robots.addAll(updatedList);
    }

    public Robot addRobot(Robot newRobot) {
        if (newRobot == null || newRobot.id() == null || newRobot.id().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Robot ID cannot be empty");
        }

        String formattedId = newRobot.id().trim().toUpperCase();
        for (Robot r : robots) {
            if (r.id().equalsIgnoreCase(formattedId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Robot already exists with ID: " + formattedId);
            }
        }

        String node = (newRobot.Currentnode() == null || newRobot.Currentnode().trim().isEmpty()) ? "A" : newRobot.Currentnode().trim().toUpperCase();
        int battery = Math.max(0, Math.min(100, newRobot.batterypercent()));
        String status = (newRobot.status() == null || newRobot.status().trim().isEmpty()) ? "IDLE" : newRobot.status().trim().toUpperCase();

        Robot created = new Robot(formattedId, node, battery, status);
        robots.add(created);
        return created;
    }

    public void deleteRobot(String id) {
        boolean removed = robots.removeIf(r -> r.id().equalsIgnoreCase(id) || r.id().equalsIgnoreCase("R" + id));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Robot not found with ID: " + id);
        }
    }
}
