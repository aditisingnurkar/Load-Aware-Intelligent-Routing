# Load-Aware-Intelligent-Routing
# LAIR
> When a hub fails, LAIR detects the cascade, isolates the bottleneck, and reroutes every affected shipment — balancing speed against congestion in real time.

---

## What it does

LAIR models a logistics network as a directed weighted graph and runs a four-phase automated recovery pipeline the moment a hub goes down:

1. **Delay propagation** — BFS fans out downstream from the failing hub, tagging every affected node with its worst-case delay.
2. **Bottleneck detection** — a max-heap scores every affected hub by downstream impact and shipment load, then isolates the highest scorer by removing all its incident edges.
3. **Reachability check** — Union-Find queries the pruned graph before Dijkstra runs; shipments whose destination is now in a disconnected component are immediately marked `FAILED`, skipping unnecessary computation.
4. **Load-aware rerouting** — Dijkstra finds the lowest-cost path using a composite score of `α × travel_time + β × hub_load`, so successive reroutes naturally spread load instead of piling onto the same hubs.

---

## Algorithms & data structures

| Component | Structure / Algorithm | Role |
|---|---|---|
| Network | Adjacency list | Directed weighted graph |
| Hub tracking | Hash map | Live shipment count per hub |
| Isolated nodes | Hash set | Guards all routing operations |
| Delay propagation | BFS (max-delay variant) | Worst-case delay per hub |
| Bottleneck ranking | Max-heap | Highest-impact hub extraction |
| Reachability | Union-Find | Pre-flight check before Dijkstra |
| Rerouting | Load-aware Dijkstra | α×travel + β×load composite cost |

---

## Project structure

```
src/main/java/com/logistics/
├── model/          # Hub, Edge, Shipment, enums, DelayEvent, RerouteResult, HeapEntry
├── graph/          # LogisticsGraph — adjacency list, load map, isolated set
├── algorithm/      # BFSDelayPropagator, BottleneckDetector, UnionFind, LoadAwareDijkstra
├── simulation/     # SimulationEngine (pipeline), ShipmentManager (load lifecycle)
└── io/             # InputParser, OutputFormatter
```

---

## Running the project

**Requirements:** Java 17+, IntelliJ IDEA, Maven or Gradle

```bash
# Clone the repo
git clone https://github.com/<your-org>/lair.git
cd lair

# Build
mvn clean install         # Maven
# or
./gradlew build           # Gradle

# Run with the sample network
mvn exec:java -Dexec.mainClass="com.logistics.Main" -Dexec.args="input/sample_network.txt"
```

---

## Input format

`sample_network.txt` defines the network in four sections:

```
HUBS
W1 WAREHOUSE
S1 SORTING
S2 SORTING
R1 REGIONAL
D1 DISTRIBUTION
P1 DELIVERY
P2 DELIVERY

ROUTES
W1 S1 4
W1 S2 6
S1 R1 3
S2 R1 5
R1 D1 2
D1 P1 3
D1 P2 4

SHIPMENTS
SHP001 W1 S1 R1 D1 P1
SHP002 W1 S2 R1 D1 P2

DELAY
S1 10
```

## Configuration

Alpha and beta are tunable at runtime to shift routing policy:

| Setting | α | β | Effect |
|---|---|---|---|
| Speed-first | 1.0 | 0.0 | Pure travel time, ignores load |
| Balanced | 0.7 | 0.3 | Default — recommended |
| Load-first | 0.2 | 0.8 | Maximises even load distribution |

--
