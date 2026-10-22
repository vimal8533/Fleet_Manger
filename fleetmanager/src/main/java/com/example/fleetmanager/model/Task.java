package com.example.fleetmanager.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @JsonProperty("pickupNode")
    @Column(name = "pickup_node", nullable = false, length = 50)
    private String pickupNode;

    @JsonProperty("destinationNode")
    @Column(name = "destination_node", nullable = false, length = 50)
    private String destinationNode;

    @Column(name = "priority", nullable = false)
    private int priority = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status = TaskStatus.CREATED;

    @JsonProperty("assignedRobotId")
    @Column(name = "assigned_robot_id", length = 50)
    private String assignedRobotId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Task() {
    }

    public Task(String id, String pickupNode, String destinationNode, int priority) {
        this.id = id;
        this.pickupNode = pickupNode;
        this.destinationNode = destinationNode;
        this.priority = priority;
        this.status = TaskStatus.CREATED;
    }

    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TaskStatus.CREATED;
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPickupNode() {
        return pickupNode;
    }

    public void setPickupNode(String pickupNode) {
        this.pickupNode = pickupNode;
    }

    public String getDestinationNode() {
        return destinationNode;
    }

    public void setDestinationNode(String destinationNode) {
        this.destinationNode = destinationNode;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getAssignedRobotId() {
        return assignedRobotId;
    }

    public void setAssignedRobotId(String assignedRobotId) {
        this.assignedRobotId = assignedRobotId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
