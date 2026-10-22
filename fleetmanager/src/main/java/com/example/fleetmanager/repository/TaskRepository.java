package com.example.fleetmanager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fleetmanager.model.Task;
import com.example.fleetmanager.model.TaskStatus;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByStatusOrderByPriorityDescCreatedAtAsc(TaskStatus status);

    Optional<Task> findFirstByAssignedRobotIdAndStatusIn(String robotId, List<TaskStatus> statuses);

    List<Task> findByAssignedRobotId(String robotId);
}
