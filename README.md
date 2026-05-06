# 🚗 Vehicle Motion Simulation (Akka + Spring Boot)

##  Overview

This project simulates a fleet of autonomous vehicles (AGVs) moving between two points (shore crane ↔ yard crane) in a 2D space.
Made to work with Java 11.

Each vehicle:

* Moves continuously using vector-based physics
* Avoids collisions with nearby vehicles
* Queues when a destination station is busy
* Communicates through an Akka actor system

The system is designed to demonstrate:

* Actor-based concurrency (Akka Typed)
* Real-time simulation
* Basic AI (steering behaviors: seek + avoidance)
* WebSocket-based visualization

---

##  Architecture

### Main Components

* **VehicleActor**

  * Represents a single vehicle
  * Handles movement logic (AI / steering behavior)
  * Sends position updates

* **SpatialActor**

  * Central coordinator
  * Tracks all vehicle positions
  * Detects neighbors
  * Manages station locking (busy/free)

* **SimulationManager**

  * Entry point for spawning and controlling vehicles
  * Handles safe spawning using Ask pattern

* **WebSocketHandler**

  * Broadcasts simulation state to frontend clients

* **MovementStrategy**

  * Encapsulates movement logic (Strategy Pattern)
  * Easily replaceable AI behavior

---


##  Movement Logic (AI)

Vehicles use **steering behaviors**:

### 1. Seek

Move toward target:

```text
desired = target - position
```

### 2. Avoidance

Avoid nearby vehicles using:

* Repulsion (push away)
* Side-stepping (slide around)

### 3. Blending

```text
totalForce = seek + avoidance
```

### 4. Queueing

* Vehicles slow down near busy stations
* Minimal forward drift prevents deadlock


---

##  Simulation Loop

Each vehicle runs:

```text
Tick (every 30ms):
    → compute forces
    → update velocity
    → update position
    → notify SpatialActor
```

---
##  User Interaction & Usage
This simulation is controlled entirely through a **browser-based UI**.

### 1. Open the Simulation UI
At **http://localhost:8080/**
You will see:

* A **canvas** (simulation area)
* A **control panel** for spawning and managing vehicles

---

### 2. Configure Crane Positions (Before Spawning)

The simulation includes:

* **Shore Cranes (blue)** → left side
* **Yard Cranes (red)** → right side

####  Drag & Drop

* Click and drag any crane to reposition it
* Positions update in real-time
* Dropdowns update automatically

**Important:**
Once you spawn vehicles, crane positions become **locked**.

---

### 3. Spawn Vehicles

#### Controls:

* Select:

  * **Shore crane (start point)**
  * **Yard crane (destination)**
* Choose number of vehicles (`1–5`)
* Click **Spawn**

#### Behavior:

* Vehicles appear at the selected shore crane
* If spawn location is occupied:

  * The system retries automatically
* Vehicles immediately start moving toward their target

---

###  4. Observe the Simulation

On the canvas:

* 🟩 **Green vehicles** → carrying container (shore → yard)
* ⬛ **Black vehicles** → empty (yard → shore)

Each vehicle:

* Moves continuously
* Avoids collisions
* Queues if destination is busy
* Displays its **ID label**

---

### 5. Manage Vehicles (Dynamic Control)

#### Select a vehicle:

* Use the **Vehicle dropdown**

#### Change mission:

* Choose new:

  * Shore crane
  * Yard crane
* Click **Change Mission**

#### Behavior:

* Vehicle updates its route dynamically
* If too close to destination → change is ignored

---

###  Example Workflow User Interaction
This simulation is controlled entirely through a browser-based UI.


1. Drag cranes into desired positions
2. Spawn multiple vehicles
3. Watch traffic behavior:

   * collision avoidance
   * queuing at stations
4. Select a vehicle
5. Change its mission dynamically

---
## Interaction Rules

* ❌ Cannot move cranes after spawning vehicles
* ❌ Cannot update mission if vehicle is too close to target
* ✔ Vehicles are controlled only via UI (no manual movement)

---

##  Notes / Limitations

* No pathfinding (straight-line movement only)
* No persistence (in-memory simulation)
* No collision physics (only avoidance)
* Single spatial coordinator (not distributed)

---

##  Future Improvements

* Pathfinding (A*, navigation grid)
* Smarter coordination between vehicles
* Multiple movement strategies (Strategy Pattern)
* Distributed spatial actors

---

##  Author Notes

The implementation successfully follows the requirements and use cases defined in the task.

* All **core use cases** have been implemented:

  * Spawning multiple vehicles from the UI
  * Dynamically updating vehicle destinations
* The **optional use cases** are also covered:

  * Real-time visualization of vehicle movement
  * Interaction between vehicles using AI-based collision avoidance

In addition, the solution respects the **technical constraints**:

* Uses **Java 11+**
* Implements **asynchronous communication** via Akka actors
* Leverages **multi-threading** through the actor system
* Provides a **web-based UI** for interaction and visualization

Overall, the application delivers a complete, interactive simulation that meets both the functional and technical expectations of the assignment.


---
