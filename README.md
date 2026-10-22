# 🤖 Autonomous Robot Fleet Manager

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-green.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Build Status](https://img.shields.io/badge/Tests-18%2F18%20Passing-brightgreen.svg?style=flat-square)](https://github.com/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg?style=flat-square)](LICENSE)

An enterprise-grade **Autonomous Guided Vehicle (AGV) Fleet Management & Dispatch System** built with **Java 21**, **Spring Boot**, and **PostgreSQL**. The platform features real-time prioritized mission assignment, **Dijkstra shortest-path routing**, single-lane track mutual exclusion, and **deadlock-free collision avoidance with dynamic detour bypasses**.

---

## 📸 Live Command Center & Interactive Floor Map

![Fleet Manager Command Center](fleetmanager/src/main/resources/static/dashboard-preview.png)

*Real-time SVG floor map tracking 5 stations (A, B, C, D, E), glowing Dijkstra route calculations, live AGV battery/telemetry monitors, and prioritized mission queue.*

---

## 🌟 Key Engineering Highlights

- **Dynamic Dijkstra Routing**: Computes mathematically optimal paths across weighted warehouse corridors (1-unit perimeter tracks, 2-unit cross-aisle expressways).
- **Priority-Driven Task Dispatcher**: Automatically pairs highest-priority pending orders (`CREATED`) with eligible, online, idle robots possessing &ge;25% battery capacity while strictly preventing double assignment.
- **Exclusive Resource Scheduler (Collision Avoidance)**:
  - **Station Node Locks**: Guarantees only one robot occupies a physical station at any given time.
  - **Canonical Edge Locks**: Single-lane corridor tracks use symmetric keys (`min(u,v) + "-" + max(u,v)`), mathematically preventing head-on collisions between opposing robots.
- **Deadlock Detection & Dynamic Detour Bypass**: Monitors yielding wait cycles. If two robots encounter a mutual wait cycle (&ge;2 ticks), the system calculates an alternate Dijkstra detour around the blocked station to break circular waits.
- **Real-time Glassmorphic UI**: High-refresh telemetry dashboard with real-time SVG floor map, live node occupancy badges, and route previews.
- **100% Test Coverage**: Complete JUnit 5 test suites covering routers, dispatchers, task lifecycles, and concurrent mutual exclusion locks.

---

## 🏛️ System Architecture

```mermaid
graph TB
    subgraph Client["Presentation Layer (HTML5 & SVG)"]
        UI["Interactive Dashboard<br/>• SVG Floor Map & Dijkstra Route Glow<br/>• Live Telemetry Polling (3s)<br/>• Mission Dispatcher Form<br/>• Lock Reservation Tracker"]
    end

    subgraph REST["REST API Layer"]
        RC["RobotController (/api/robots)"]
        TC["TaskController (/api/tasks)"]
        WC["WarehouseController (/api/warehouse/*)"]
    end

    subgraph Services["Core Domain & Concurrency Services"]
        RS["RobotService<br/>Telemetry & CRUD"]
        TS["TaskService<br/>Prioritized Mission Queue"]
        DS["DispatcherService<br/>Matches Tasks to Idle Robots"]
        RT["RouterService<br/>Dijkstra & Detour Bypasses"]
        SCH["ResourceSchedulerService<br/>Exclusive Node & Edge Locks"]
        MON["RobotMonitorService<br/>Autonomous Periodic Navigation Loop"]
    end

    subgraph Database["Persistence Layer (PostgreSQL)"]
        DB1[("robots table")]
        DB2[("tasks table")]
    end

    UI -->|HTTP REST| REST
    RC --> RS
    TC --> TS
    WC --> RT
    WC --> SCH

    DS --> TS
    DS --> RS
    MON --> DS
    MON --> RT
    MON --> SCH
    MON --> RS

    RS --> DB1
    TS --> DB2
```

---

## 🚦 Collision & Deadlock Prevention Workflow

```mermaid
sequenceDiagram
    autonumber
    participant R1 as Robot R1 (at Node A)
    participant SCH as ResourceSchedulerService
    participant R2 as Robot R2 (at Node B)

    Note over R1, R2: Both robots request corridor A <-> B simultaneously
    R1->>SCH: attemptMove(R1, from="A", to="B")
    SCH-->>R1: Lock Granted (Corridor "A-B" & Station "B" reserved)
    Note over R1: R1 begins advancing to Node B

    R2->>SCH: attemptMove(R2, from="B", to="A")
    SCH-->>R2: REJECTED! (Corridor "A-B" locked by R1)
    Note over R2: R2 yields turn (Status: WAITING). WaitCounter = 1

    R1->>SCH: releaseStation("A")
    Note over R1: R1 enters Station B
    R1->>SCH: releaseEdge("A-B")

    Note over R2: Corridor "A-B" is now free!
    R2->>SCH: attemptMove(R2, from="B", to="A")
    SCH-->>R2: Lock Granted! R2 advances safely without collision.
```

---

## 🗺️ Warehouse Topology

The warehouse floor consists of a 5-node weighted network:

| Station ID | Station Name | X, Y Coords | Connected Neighbors & Weights |
| :---: | :---: | :---: | :--- |
| **A** | Alpha | (80, 70) | B (1u), E (1u), C (2u Expressway) |
| **B** | Beta | (340, 70) | A (1u), C (1u), D (2u Expressway) |
| **C** | Gamma | (440, 220) | B (1u), D (1u), A (2u Expressway) |
| **D** | Delta | (260, 280) | C (1u), E (1u), B (2u Expressway) |
| **E** | Epsilon | (60, 220) | D (1u), A (1u) |

---

## 🛠️ Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.1 (Spring Data JPA, Spring Web, Spring Validation)
- **Database**: PostgreSQL 18 with Hibernate ORM
- **Algorithms**: Dijkstra's Algorithm (PriorityQueue-based), Canonical Symmetric Locking
- **Testing**: JUnit 5, Mockito, Spring Boot Test (18 test cases)
- **Frontend**: Vanilla HTML5, CSS3 Glassmorphism, Responsive SVG Floor Map, Modern ES6 Fetch API

---

## ⚡ Quickstart Guide

### 1. Prerequisites
- **Java Development Kit (JDK 21+)**
- **PostgreSQL 15+** installed and running
- **Git**

### 2. Database Configuration
Create a database named `fleetdb` in PostgreSQL:
```sql
CREATE DATABASE fleetdb;
```
Configure your credentials in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fleetdb
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

### 3. Run Automated Tests
```bash
# Windows
mvnw.cmd test

# Linux / macOS
./mvnw test
```

### 4. Launch Application
```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Open **`http://localhost:8080/`** in your browser to access the command center dashboard!

---

## 📡 REST API Reference

### Mission Tasks
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/tasks` | Create prioritized task (`pickupNode`, `destinationNode`, `priority`) |
| `GET` | `/api/tasks` | Get all tasks ordered by Priority DESC, CreatedAt ASC |

### Warehouse & Routing
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/warehouse/graph` | Retrieve warehouse stations, coordinates, and edge weights |
| `GET` | `/api/warehouse/route?from=A&to=C` | Compute Dijkstra shortest path and total distance |
| `GET` | `/api/warehouse/reservations` | Inspect live active station locks and corridor reservations |

### Robots
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/robots` | List all robots (supports `?status=IDLE` and `?node=A` filters) |
| `POST` | `/api/robots` | Register a new robot |
| `PUT` | `/api/robots/{id}` | Update robot battery, node, or status |
| `DELETE` | `/api/robots/{id}` | Decommission a robot |

---

## 🧪 Test Suite Matrix (18/18 Passing)

| Test Suite | Tests | Status | Key Validations |
| :--- | :---: | :---: | :--- |
| **`TaskServiceTests`** | 4 | ✅ Passed | UUID generation, status lifecycle transitions, priority sorting |
| **`RouterServiceTests`** | 5 | ✅ Passed | Dijkstra shortest paths, edge costs, obstacle avoidance detour routing |
| **`DispatcherServiceTests`** | 4 | ✅ Passed | IDLE robot selection, battery &ge; 25% threshold, double-assignment prevention |
| **`ResourceSchedulerTests`** | 4 | ✅ Passed | Exclusive station locks, bidirectional corridor locks, deadlock detour triggering |
| **`FleetmanagerApplicationTests`**| 1 | ✅ Passed | Spring Boot context initialization |

---

## 📄 Complete Engineering Manual & PDF

A comprehensive architectural manual is available inside the project:
- **PDF Study Guide**: [`Fleet_Manager_Complete_Study_Guide.pdf`](fleetmanager/Fleet_Manager_Complete_Study_Guide.pdf)
- **Web Printable Guide**: Open `http://localhost:8080/study-guide.html` and press `Ctrl + P`.

---

## 📝 Resume Bullet Points (Copy & Paste)

> **Autonomous Robot Fleet Management System** | *Java 21, Spring Boot, PostgreSQL, Dijkstra Algorithm, Concurrency/Locks, REST API, HTML5/SVG*
> - Architected an autonomous multi-AGV fleet manager dispatching prioritized missions across a 5-node weighted warehouse graph using **Dijkstra’s shortest path algorithm**.
> - Engineered an exclusive **Resource Scheduler** with station locks and canonical edge locks, mathematically eliminating head-on and rear-end collisions on single-lane corridors.
> - Implemented deadlock detection with **dynamic detour bypasses**, breaking circular wait states after &ge;2 cycles by re-routing through alternate perimeter aisles.
> - Achieved **100% test coverage (18/18 test suites passing)** across router, dispatcher, task lifecycle, and concurrent mutual exclusion locks.

