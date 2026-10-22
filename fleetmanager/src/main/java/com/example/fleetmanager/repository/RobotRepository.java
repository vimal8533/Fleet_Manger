package com.example.fleetmanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fleetmanager.model.Robot;

@Repository
public interface RobotRepository extends JpaRepository<Robot, String> {
    List<Robot> findByStatusIgnoreCase(String status);
    List<Robot> findByCurrentnodeIgnoreCase(String node);
    List<Robot> findByCurrentnodeIgnoreCaseAndStatusIgnoreCase(String node, String status);
    List<Robot> findByBatterypercentLessThan(int threshold);
}
