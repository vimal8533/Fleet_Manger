package com.example.fleetmanager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.fleetmanager.model.Robot;
import com.example.fleetmanager.service.RobotMonitorService;
import com.example.fleetmanager.service.RobotService;

@RestController
@RequestMapping("/api/robots")
public class RobotController {

    private final RobotService robotService;
    private final RobotMonitorService robotMonitorService;

    public RobotController(RobotService robotService, RobotMonitorService robotMonitorService) {
        this.robotService = robotService;
        this.robotMonitorService = robotMonitorService;
    }

    @GetMapping
    public List<Robot> getRobots(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "node", required = false) String node,
            @RequestParam(name = "lowBattery", required = false) Boolean lowBattery) {
        return robotService.searchRobots(status, node, lowBattery);
    }

    @GetMapping("/monitor")
    public List<Robot> monitor() {
        robotMonitorService.checkRobotStatus();
        return robotService.getAllRobots();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Robot addRobot(@RequestBody Robot robot) {
        return robotService.addRobot(robot);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Robot> addRobotsBulk(@RequestBody List<Robot> robots) {
        return robotService.addRobotsBulk(robots);
    }

    @GetMapping("/{id}")
    public Robot getRobotById(@PathVariable("id") String id) {
        return robotService.getRobotById(id);
    }

    @PutMapping("/{id}")
    public Robot updateRobot(@PathVariable("id") String id, @RequestBody Robot updatedRobot) {
        return robotService.updateRobot(id, updatedRobot);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRobot(@PathVariable("id") String id) {
        robotService.deleteRobot(id);
    }
}
