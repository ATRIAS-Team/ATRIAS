package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.metrics;

import java.util.Collections;

public class MinMaxMetricsValues {
    private int minOdr;
    private int maxOdr;
    private double minTotalDistance;
    private double maxTotalDistance;
    private int minStops;
    private int maxStops;
    private double minBatteryLevel;
    private double maxBatteryLevel;
    private double minBatteryLevelAfterAllTrips;
    private double maxBatteryLevelAfterAllTrips;

    public MinMaxMetricsValues() {
    }

    public int getMinOdr() {
        return minOdr;
    }

    public void setMinOdr(int minOdr) {
        this.minOdr = minOdr;
    }

    public int getMaxOdr() {
        return maxOdr;
    }

    public void setMaxOdr(int maxOdr) {
        this.maxOdr = maxOdr;
    }

    public double getMinTotalDistance() {
        return minTotalDistance;
    }

    public void setMinTotalDistance(double minTotalDistance) {
        this.minTotalDistance = minTotalDistance;
    }

    public double getMaxTotalDistance() {
        return maxTotalDistance;
    }

    public void setMaxTotalDistance(double maxTotalDistance) {
        this.maxTotalDistance = maxTotalDistance;
    }

    public int getMinStops() {
        return minStops;
    }

    public void setMinStops(int minStops) {
        this.minStops = minStops;
    }

    public int getMaxStops() {
        return maxStops;
    }

    public void setMaxStops(int maxStops) {
        this.maxStops = maxStops;
    }

    public double getMinBatteryLevel() {
        return minBatteryLevel;
    }

    public void setMinBatteryLevel(double minBatteryLevel) {
        this.minBatteryLevel = minBatteryLevel;
    }

    public double getMaxBatteryLevel() {
        return maxBatteryLevel;
    }

    public void setMaxBatteryLevel(double maxBatteryLevel) {
        this.maxBatteryLevel = maxBatteryLevel;
    }

    public double getMinBatteryLevelAfterAllTrips() {
        return minBatteryLevelAfterAllTrips;
    }

    public void setMinBatteryLevelAfterAllTrips(double minBatteryLevelAfterAllTrips) {
        this.minBatteryLevelAfterAllTrips = minBatteryLevelAfterAllTrips;
    }

    public double getMaxBatteryLevelAfterAllTrips() {
        return maxBatteryLevelAfterAllTrips;
    }

    public void setMaxBatteryLevelAfterAllTrips(double maxBatteryLevelAfterAllTrips) {
        this.maxBatteryLevelAfterAllTrips = maxBatteryLevelAfterAllTrips;
    }
}
