package com.logistics.model;

import java.util.List;

/**
 * Stores the result of rerouting a shipment.
 * Includes both original and new paths along with cost comparison.
 */
public class RerouteResult {

    private final String shipmentId;
    private final List<String> originalPath;
    private final List<String> newPath;
    private final double oldCost;
    private final double newCost;

    public RerouteResult(String shipmentId,
                         List<String> originalPath,
                         List<String> newPath,
                         double oldCost,
                         double newCost) {
        this.shipmentId   = shipmentId;
        this.originalPath = originalPath;
        this.newPath      = newPath;
        this.oldCost      = oldCost;
        this.newCost      = newCost;
    }

    public String getId() {
        return shipmentId;
    }

    public List<String> getOriginalPath() {
        return originalPath;
    }

    // Returns rerouted path
    public List<String> getPath() {
        return newPath;
    }

    public double getOldCost() {
        return oldCost;
    }

    public double getNewCost() {
        return newCost;
    }

    // Percentage improvement (positive = better, negative = worse)
    public double getDelayReductionPercent() {
        if (oldCost == 0) return 0.0;
        return ((oldCost - newCost) / oldCost) * 100;
    }
}