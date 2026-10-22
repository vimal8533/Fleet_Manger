package com.example.fleetmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "robots")
public class Robot {

    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @JsonProperty("Currentnode")
    @Column(name = "current_node", nullable = false)
    private String currentnode;

    @JsonProperty("batterypercent")
    @Column(name = "battery_percent", nullable = false)
    private int batterypercent;

    @Column(name = "status", nullable = false)
    private String status;

    public Robot() {
    }

    public Robot(String id, String currentnode, int batterypercent, String status) {
        this.id = id;
        this.currentnode = currentnode;
        this.batterypercent = batterypercent;
        this.status = status;
    }

    // Standard Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCurrentnode() {
        return currentnode;
    }

    public void setCurrentnode(String currentnode) {
        this.currentnode = currentnode;
    }

    public int getBatterypercent() {
        return batterypercent;
    }

    public void setBatterypercent(int batterypercent) {
        this.batterypercent = batterypercent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Record-style compatibility methods
    public String id() {
        return id;
    }

    public String Currentnode() {
        return currentnode;
    }

    public int batterypercent() {
        return batterypercent;
    }

    public String status() {
        return status;
    }
}
