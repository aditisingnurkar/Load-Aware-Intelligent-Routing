package com.logistics.model;

import java.util.List;

public class RerouteResult {
    private final String id;
    private final List<String> path;
    private final double oldCost;
    private final double newCost;

    public RerouteResult(String id, List<String> path, double oldCost, double newCost) {
        this.id = id;
        this.path = path;
        this.oldCost = oldCost;
        this.newCost = newCost;
    }

    public String getId()         { return id; }
    public List<String> getPath() { return path; }
    public double getOldCost()    { return oldCost; }
    public double getNewCost()    { return newCost; }

    // Percentage improvement from old to new cost
    public double getDelayReductionPercent() {
        if (oldCost == 0) return 0;
        return ((oldCost - newCost) / oldCost) * 100;
    }

    @Override
    public String toString() {
        return "RerouteResult{id='" + id + "', oldCost=" + oldCost +
                ", newCost=" + newCost + ", reduction=" +
                String.format("%.1f", getDelayReductionPercent()) + "%}";
    }
}


