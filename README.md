# LAIR – Logistics Adaptive Intelligent Routing

## Overview

LAIR is a logistics network simulation system that models how deliveries move through a multi-stage supply chain and how disruptions affect them. The system detects critical bottlenecks, isolates them, and reroutes shipments using load-aware pathfinding.

The goal is not to remove delays, but to minimize their impact by adapting routes dynamically.

---

## Problem

In real-world logistics systems, a delay at a single hub can propagate across the network and affect multiple shipments. Traditional routing approaches do not adapt efficiently under such disruptions.

---

## Solution

LAIR models the logistics network as a graph and processes disruptions in three phases:

1. **Delay propagation** – simulates how delay spreads through connected hubs
2. **Bottleneck detection** – identifies the most critical hub based on impact
3. **Adaptive rerouting** – computes new paths while avoiding the bottleneck

The system ensures:

* shipments avoid failed hubs
* routing remains feasible
* load is distributed across the network

---

## Core Concepts

* **Graph Representation**
  Hubs are nodes and routes are directed edges.

* **Delay Propagation (BFS)**
  Delay spreads forward through the network based on travel time.

* **Bottleneck Scoring**
  Each hub is scored using:

  ```
  score = downstream impact × number of dependent shipments
  ```

* **Load-Aware Routing (Modified Dijkstra)**
  Routing cost considers both travel time and congestion:

  ```
  cost = α × travel time + β × hub load
  ```

* **Connectivity Check (Union-Find)**
  Ensures a shipment can still reach its destination before rerouting.

* **Position-Aware Rerouting**
  Shipments reroute based on their current location, not from origin.

---

## System Flow

```
Input → Graph Build → Delay Propagation → Bottleneck Detection
      → Hub Isolation → Rerouting → Output
```

---

## Features

* Detects and isolates critical hubs
* Dynamically reroutes affected shipments
* Handles invalid inputs and unreachable paths
* Tracks shipment status: ACTIVE / DELIVERED / FAILED
* Computes cost improvement after rerouting
* Provides both console output and JavaFX visualization

---

## Input Format

The system reads from a text file with four sections:

```
HUB <id> <type>
ROUTE <from> <to> <time>
SHIPMENT <id> <path...> | CURRENT <hub>
DELAY <hub> <value>
```

Example:

```
HUB W1 WAREHOUSE
ROUTE W1 S1 5
SHIPMENT SH1 W1 S1 R1 D1 P1 | CURRENT W1
DELAY R2 15
```

---

## Output

* Bottleneck hub and score
* Rerouted paths for affected shipments
* Failed shipments (if unreachable)
* Total cost before and after rerouting
* Percentage improvement
* Final load distribution across hubs

---

## How to Run

1. Place `sample_network.txt` in the project root
2. Run:

```
com.logistics.Main
```

3. Use the UI “NEXT” button to step through:

   * Phase 1: Initial state
   * Phase 2: Delay + bottleneck
   * Phase 3: Rerouting

---

## Technologies Used

* Java
* JavaFX (UI)
* Graph algorithms (BFS, Dijkstra)
* Union-Find (Disjoint Set)

---

## Key Takeaway

LAIR demonstrates how a logistics system can remain operational under disruption by adapting routes intelligently instead of failing completely.

It focuses on **resilience, not elimination of delay**.
