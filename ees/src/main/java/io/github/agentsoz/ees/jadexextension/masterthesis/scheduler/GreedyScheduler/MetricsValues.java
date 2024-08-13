package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;

import java.util.ArrayList;
import java.util.List;

public class MetricsValues {
    private List<Integer> allOdrValues;
    private List<Double> allMinBatteryLevelValues;
    private List<Double> allBatteryLevelValuesAfterAllTrips;
    private List<Double> allTotalDistances;
    private List<Integer> allStopsValues;
    private List<List<Trip>> allTripsWithCharingTimes;
    private List<Boolean> allVaBreaksDownValues;
    private List<Double> allChargingTimes;

    public void addOdr(Integer odr) {
        this.allOdrValues.add(odr);
    }

    public void addMinBatteryLevel(Double minBatteryLevel) {
        this.allMinBatteryLevelValues.add(minBatteryLevel);
    }

    public void addBatteryLevelAfterAllTrips(Double batteryLevel) {
        this.allBatteryLevelValuesAfterAllTrips.add(batteryLevel);
    }

    public void addTotalDistance(Double distance) {
        this.allTotalDistances.add(distance);
    }

    public void addStopps(Integer stopps) {
        this.allStopsValues.add(stopps);
    }

    public void addTripsWithChargingTime(List<Trip> tripList) {
        this.allTripsWithCharingTimes.add(tripList);
    }

    public void addVaBreakDown(Boolean vaBreakDown) {
        this.allVaBreaksDownValues.add(vaBreakDown);
    }

    public void addChargingTimes(Double chargingTime) {
        this.allChargingTimes.add(chargingTime);
    }

    public MetricsValues() {
        this.allOdrValues = new ArrayList<>();
        this.allMinBatteryLevelValues = new ArrayList<>();
        this.allBatteryLevelValuesAfterAllTrips = new ArrayList<>();
        this.allTotalDistances = new ArrayList<>();
        this.allStopsValues = new ArrayList<>();
        this.allTripsWithCharingTimes = new ArrayList<>();
        this.allVaBreaksDownValues = new ArrayList<>();
        this.allChargingTimes = new ArrayList<>();
    }

    public List<Integer> getAllOdrValues() {
        return allOdrValues;
    }

    public void setAllOdrValues(List<Integer> allOdrValues) {
        this.allOdrValues = allOdrValues;
    }

    public List<Double> getAllMinBatteryLevelValues() {
        return allMinBatteryLevelValues;
    }

    public void setAllMinBatteryLevelValues(List<Double> allMinBatteryLevelValues) {
        this.allMinBatteryLevelValues = allMinBatteryLevelValues;
    }

    public List<Double> getAllBatteryLevelValuesAfterAllTrips() {
        return allBatteryLevelValuesAfterAllTrips;
    }

    public void setAllBatteryLevelValuesAfterAllTrips(List<Double> allBatteryLevelValuesAfterAllTrips) {
        this.allBatteryLevelValuesAfterAllTrips = allBatteryLevelValuesAfterAllTrips;
    }

    public List<Double> getAllTotalDistances() {
        return allTotalDistances;
    }

    public void setAllTotalDistances(List<Double> allTotalDistances) {
        this.allTotalDistances = allTotalDistances;
    }

    public List<Integer> getAllStopsValues() {
        return allStopsValues;
    }

    public void setAllStopsValues(List<Integer> allStopsValues) {
        this.allStopsValues = allStopsValues;
    }

    public List<List<Trip>> getAllTripsWithCharingTimes() {
        return allTripsWithCharingTimes;
    }

    public void setAllTripsWithCharingTimes(List<List<Trip>> allTripsWithCharingTimes) {
        this.allTripsWithCharingTimes = allTripsWithCharingTimes;
    }

    public List<Boolean> getAllVaBreaksDownValues() {
        return allVaBreaksDownValues;
    }

    public void setAllVaBreaksDownValues(List<Boolean> allVaBreaksDownValues) {
        this.allVaBreaksDownValues = allVaBreaksDownValues;
    }

    public List<Double> getAllChargingTimes() {
        return allChargingTimes;
    }

    public void setAllChargingTimes(List<Double> allChargingTimes) {
        this.allChargingTimes = allChargingTimes;
    }
}
