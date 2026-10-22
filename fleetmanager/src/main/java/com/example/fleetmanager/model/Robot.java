package com.example.fleetmanager.model;

public record Robot(
    String id,
    String Currentnode,
    int batterypercent,
    String status) {
}
