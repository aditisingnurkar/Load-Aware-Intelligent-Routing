package com.logistics.model;

import java.util.List;

public class RerouteResult {
    private final String shipmentId;
    private final List<String> newPath;
    private final double oldCost;
    private final double newCost;

    public RerouteResult(String shipmentId, List<String> newPath, double oldCost, double newCost) {
        this.shipmentId = shipmentId;
        this.newPath = newPath;
        this.oldCost = oldCost;
        this.newCost = newCost;
    }

    public String getShipmentId()     { return shipmentId; }
    public List<String> getNewPath()  { return newPath; }
    public double getOldCost()        { return oldCost; }
    public double getNewCost()        { return newCost; }

    public double getDelayReductionPercent() {
        if (oldCost == 0) return 0;
        return ((oldCost - newCost) / oldCost) * 100.0;
    }

    @Override
    public String toString() {
        return "RerouteResult{shipment='" + shipmentId + "', oldCost=" + oldCost
                + ", newCost=" + newCost + ", reduction="
                + String.format("%.1f", getDelayReductionPercent()) + "%}";
    }
}
```

        ---

        **File 4 — `sample_network.txt`** (in `input/`)
        ```
        # HUBS
# format: HUB <id> <type>
HUB W1 WAREHOUSE
HUB S1 SORTING
HUB S2 SORTING
HUB R1 REGIONAL
HUB R2 REGIONAL
HUB R3 REGIONAL
HUB D1 DISTRIBUTION
HUB DEL1 DELIVERY
HUB DEL2 DELIVERY
HUB DEL3 DELIVERY

# ROUTES
# format: ROUTE <from> <to> <travelTime>
ROUTE W1 S1 4
ROUTE W1 S2 6
ROUTE S1 R1 3
ROUTE S1 R2 5
ROUTE S2 R2 2
ROUTE S2 R3 4
ROUTE R1 D1 6
ROUTE R2 D1 4
ROUTE R3 D1 3
ROUTE D1 DEL1 2
ROUTE D1 DEL2 3
ROUTE D1 DEL3 5

        # SHIPMENTS
# format: SHIPMENT <id> <hub1> <hub2> ... <hubN>
SHIPMENT SH1 W1 S1 R1 D1 DEL1
SHIPMENT SH2 W1 S1 R2 D1 DEL2
SHIPMENT SH3 W1 S2 R2 D1 DEL2
SHIPMENT SH4 W1 S2 R3 D1 DEL3
SHIPMENT SH5 W1 S1 R1 D1 DEL3

# DELAY EVENT
# format: DELAY <hubId> <initialDelay>
DELAY R2 10
