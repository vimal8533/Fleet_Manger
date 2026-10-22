package com.example.fleetmanager.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fleetmanager.model.Robot;
import com.example.fleetmanager.repository.RobotRepository;

@Service
public class RobotService {

    private final RobotRepository robotRepository;

    public RobotService(RobotRepository robotRepository) {
        this.robotRepository = robotRepository;
    }

    public List<Robot> getAllRobots() {
        return robotRepository.findAll();
    }

    public List<Robot> getRobotsByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return getAllRobots();
        }
        return robotRepository.findByStatusIgnoreCase(status.trim());
    }

    public List<Robot> searchRobots(String status, String node, Boolean lowBattery) {
        boolean hasStatus = (status != null && !status.trim().isEmpty());
        boolean hasNode = (node != null && !node.trim().isEmpty());

        if (Boolean.TRUE.equals(lowBattery)) {
            return robotRepository.findByBatterypercentLessThan(25);
        }

        if (hasStatus && hasNode) {
            return robotRepository.findByCurrentnodeIgnoreCaseAndStatusIgnoreCase(node.trim(), status.trim());
        }

        if (hasNode) {
            return robotRepository.findByCurrentnodeIgnoreCase(node.trim());
        }

        if (hasStatus) {
            return robotRepository.findByStatusIgnoreCase(status.trim());
        }

        return robotRepository.findAll();
    }

    public Robot getRobotById(String id) {
        return robotRepository.findById(id)
            .or(() -> robotRepository.findById("R" + id))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Robot not found: " + id
            ));
    }

    public void updateRobots(List<Robot> updatedList) {
        robotRepository.saveAll(updatedList);
    }

    public Robot addRobot(Robot newRobot) {
        if (newRobot == null || newRobot.getId() == null || newRobot.getId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Robot ID cannot be empty");
        }

        String formattedId = newRobot.getId().trim().toUpperCase();
        if (robotRepository.existsById(formattedId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Robot already exists with ID: " + formattedId);
        }

        String node = (newRobot.getCurrentnode() == null || newRobot.getCurrentnode().trim().isEmpty()) ? "A" : newRobot.getCurrentnode().trim().toUpperCase();
        int battery = Math.max(0, Math.min(100, newRobot.getBatterypercent()));
        String status = (newRobot.getStatus() == null || newRobot.getStatus().trim().isEmpty()) ? "IDLE" : newRobot.getStatus().trim().toUpperCase();

        Robot created = new Robot(formattedId, node, battery, status);
        return robotRepository.save(created);
    }

    public List<Robot> addRobotsBulk(List<Robot> newRobots) {
        if (newRobots == null || newRobots.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Robot list cannot be empty");
        }

        List<Robot> toSave = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (Robot r : newRobots) {
            if (r == null || r.getId() == null || r.getId().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Robot ID cannot be empty in bulk list");
            }
            String formattedId = r.getId().trim().toUpperCase();
            if (!seenIds.add(formattedId) || robotRepository.existsById(formattedId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate or existing Robot ID: " + formattedId);
            }

            String node = (r.getCurrentnode() == null || r.getCurrentnode().trim().isEmpty()) ? "A" : r.getCurrentnode().trim().toUpperCase();
            int battery = Math.max(0, Math.min(100, r.getBatterypercent()));
            String status = (r.getStatus() == null || r.getStatus().trim().isEmpty()) ? "IDLE" : r.getStatus().trim().toUpperCase();

            toSave.add(new Robot(formattedId, node, battery, status));
        }

        return robotRepository.saveAll(toSave);
    }

    public Robot updateRobot(String id, Robot updatedData) {
        Robot existing = getRobotById(id);

        if (updatedData != null) {
            if (updatedData.getCurrentnode() != null && !updatedData.getCurrentnode().trim().isEmpty()) {
                existing.setCurrentnode(updatedData.getCurrentnode().trim().toUpperCase());
            }

            if (updatedData.getBatterypercent() >= 0) {
                existing.setBatterypercent(Math.max(0, Math.min(100, updatedData.getBatterypercent())));
            }

            if (updatedData.getStatus() != null && !updatedData.getStatus().trim().isEmpty()) {
                existing.setStatus(updatedData.getStatus().trim().toUpperCase());
            }
        }

        return robotRepository.save(existing);
    }

    public void deleteRobot(String id) {
        String targetId = id.toUpperCase();
        if (!robotRepository.existsById(targetId) && robotRepository.existsById("R" + targetId)) {
            targetId = "R" + targetId;
        }

        if (!robotRepository.existsById(targetId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Robot not found with ID: " + id);
        }
        robotRepository.deleteById(targetId);
    }
}
