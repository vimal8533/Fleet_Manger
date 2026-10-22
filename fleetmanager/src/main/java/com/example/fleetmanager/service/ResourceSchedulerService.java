package com.example.fleetmanager.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class ResourceSchedulerService {

    // Node locks: NodeID (e.g. "A") -> RobotID (e.g. "R1")
    private final Map<String, String> nodeReservations = new ConcurrentHashMap<>();

    // Edge locks: canonical "U-V" -> RobotID
    private final Map<String, String> edgeReservations = new ConcurrentHashMap<>();

    // Deadlock detection: RobotID -> Consecutive waiting cycles
    private final Map<String, Integer> waitCounters = new ConcurrentHashMap<>();

    private static final int DEADLOCK_CYCLE_THRESHOLD = 2;

    public synchronized boolean tryAcquireReservation(String robotId, String fromNode, String toNode) {
        if (robotId == null || toNode == null) {
            return false;
        }

        String targetNode = toNode.trim().toUpperCase();
        String originNode = (fromNode != null) ? fromNode.trim().toUpperCase() : targetNode;

        // 1. Check Node Availability (Exclusive Node Lock)
        String nodeOwner = nodeReservations.get(targetNode);
        if (nodeOwner != null && !nodeOwner.equalsIgnoreCase(robotId)) {
            // Target station is occupied by another robot -> Yield!
            incrementWait(robotId);
            return false;
        }

        // 2. Check Track/Edge Availability (Prevent Head-On Track Collisions)
        String edgeKey = getCanonicalEdgeKey(originNode, targetNode);
        String edgeOwner = edgeReservations.get(edgeKey);
        if (edgeOwner != null && !edgeOwner.equalsIgnoreCase(robotId)) {
            // Corridor is occupied by another robot -> Yield!
            incrementWait(robotId);
            return false;
        }

        // 3. Grant Exclusive Reservation Locks
        nodeReservations.put(targetNode, robotId);
        edgeReservations.put(edgeKey, robotId);
        waitCounters.put(robotId, 0); // Reset wait counter upon successful reservation

        return true;
    }

    public synchronized void releaseNode(String robotId, String node) {
        if (node != null && robotId != null) {
            String key = node.trim().toUpperCase();
            if (robotId.equalsIgnoreCase(nodeReservations.get(key))) {
                nodeReservations.remove(key);
            }
        }
    }

    public synchronized void releaseEdge(String robotId, String fromNode, String toNode) {
        if (fromNode != null && toNode != null && robotId != null) {
            String edgeKey = getCanonicalEdgeKey(fromNode, toNode);
            if (robotId.equalsIgnoreCase(edgeReservations.get(edgeKey))) {
                edgeReservations.remove(edgeKey);
            }
        }
    }

    public synchronized void releaseAllForRobot(String robotId) {
        if (robotId == null) return;
        nodeReservations.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(robotId));
        edgeReservations.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(robotId));
        waitCounters.remove(robotId);
    }

    public boolean isDeadlockDetected(String robotId) {
        return waitCounters.getOrDefault(robotId, 0) >= DEADLOCK_CYCLE_THRESHOLD;
    }

    public int getWaitCycles(String robotId) {
        return waitCounters.getOrDefault(robotId, 0);
    }

    private void incrementWait(String robotId) {
        waitCounters.merge(robotId, 1, Integer::sum);
    }

    public Map<String, String> getNodeReservations() {
        return Collections.unmodifiableMap(new HashMap<>(nodeReservations));
    }

    public Map<String, String> getEdgeReservations() {
        return Collections.unmodifiableMap(new HashMap<>(edgeReservations));
    }

    private String getCanonicalEdgeKey(String u, String v) {
        String n1 = u.trim().toUpperCase();
        String n2 = v.trim().toUpperCase();
        return (n1.compareTo(n2) < 0) ? (n1 + "-" + n2) : (n2 + "-" + n1);
    }
}
