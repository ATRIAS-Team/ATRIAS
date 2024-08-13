package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.metrics;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;

import java.util.List;

public class Metrics {
    private int odr;
    private double minBatteryLevel;
    private List<Trip> tripsWithChargingTime;
    private double batteryLevelAfterAllTrips;
    private double totalDistance;
    private boolean vaBreaksDown;

    public Metrics(int odr, double minBatteryLevel, List<Trip> tripsWithChargingTime, double batteryLevelAfterAllTrips, double totalDistance, boolean vaBreaksDown) {
        this.odr = odr;
        this.minBatteryLevel = minBatteryLevel;
        this.tripsWithChargingTime = tripsWithChargingTime;
        this.batteryLevelAfterAllTrips = batteryLevelAfterAllTrips;
        this.totalDistance = totalDistance;
        this.vaBreaksDown = vaBreaksDown;
    }

    public int getOdr() {
        return odr;
    }

    public void setOdr(int odr) {
        this.odr = odr;
    }

    public double getMinBatteryLevel() {
        return minBatteryLevel;
    }

    public void setMinBatteryLevel(double minBatteryLevel) {
        this.minBatteryLevel = minBatteryLevel;
    }

    public List<Trip> getTripsWithChargingTime() {
        return tripsWithChargingTime;
    }

    public void setTripsWithChargingTime(List<Trip> tripsWithChargingTime) {
        this.tripsWithChargingTime = tripsWithChargingTime;
    }

    public double getBatteryLevelAfterAllTrips() {
        return batteryLevelAfterAllTrips;
    }

    public void setBatteryLevelAfterAllTrips(double batteryLevelAfterAllTrips) {
        this.batteryLevelAfterAllTrips = batteryLevelAfterAllTrips;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public boolean isVaBreaksDown() {
        return vaBreaksDown;
    }

    public void setVaBreaksDown(boolean vaBreaksDown) {
        this.vaBreaksDown = vaBreaksDown;
    }
}
