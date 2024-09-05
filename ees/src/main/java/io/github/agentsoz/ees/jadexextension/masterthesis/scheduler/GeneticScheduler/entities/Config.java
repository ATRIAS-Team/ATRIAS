package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.util.Location;

import java.time.LocalDateTime;
import java.util.List;

public class Config {

    private LocalDateTime simulationTime;
    private Double DRIVING_SPEED;
    private Double MIN_CHARGING_TIME;
    private Double MAX_CHARGING_TIME;
    private Double COMPLETE_CHARGING_TIME;
    private Double batteryLevel;
    private Location agentLocation;
    private Double THETA;
    private List<Location> chargingStations;
    private Double DISTANCE_FACTOR;
    private Double CHARGE_INCREASE;
    private List<Trip> currentTrip;
    private Double simtime;
    private Double battThreshhold;

    public Double getCHARGE_DECREASE() {
        return CHARGE_DECREASE;
    }

    public void setCHARGE_DECREASE(Double CHARGE_DECREASE) {
        this.CHARGE_DECREASE = CHARGE_DECREASE;
    }

    public Double getCHARGE_INCREASE() {
        return CHARGE_INCREASE;
    }

    public void setCHARGE_INCREASE(Double CHARGE_INCREASE) {
        this.CHARGE_INCREASE = CHARGE_INCREASE;
    }

    private Double CHARGE_DECREASE;


    // to prevent instantiation
    public Config() { }

    public Double getDISTANCE_FACTOR() {
        return DISTANCE_FACTOR;
    }
    public void setDISTANCE_FACTOR(Double DISTANCE_FACTOR) {
        this.DISTANCE_FACTOR = DISTANCE_FACTOR;
    }

    public LocalDateTime getSimulationTime() {
        return simulationTime;
    }

    public void setSimulationTime(LocalDateTime simulationTime) {
        this.simulationTime = simulationTime;
    }

    public Double getDRIVING_SPEED() {
        return DRIVING_SPEED;
    }

    public void setDRIVING_SPEED(Double DRIVING_SPEED) {
        this.DRIVING_SPEED = DRIVING_SPEED;
    }

    public Double getMIN_CHARGING_TIME() {
        return MIN_CHARGING_TIME;
    }

    public void setMIN_CHARGING_TIME(Double MIN_CHARGING_TIME) {
        this.MIN_CHARGING_TIME = MIN_CHARGING_TIME;
    }

    public Double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Double batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public Location getAgentLocation() {
        return agentLocation;
    }

    public void setAgentLocation(Location agentLocation) {
        this.agentLocation = agentLocation;
    }

    public Double getTHETA() {
        return THETA;
    }

    public void setTHETA(Double THETA) {
        this.THETA = THETA;
    }

    public List<Location> getChargingStations() {
        return chargingStations;
    }

    public void setChargingStations(List<Location> chargingStations) {
        this.chargingStations = chargingStations;
    }

    public Double getMAX_CHARGING_TIME() {
        return MAX_CHARGING_TIME;
    }

    public void setMAX_CHARGING_TIME(Double MAX_CHARGING_TIME) {
        this.MAX_CHARGING_TIME = MAX_CHARGING_TIME;
    }

    public Double getCOMPLETE_CHARGING_TIME() {
        return COMPLETE_CHARGING_TIME;
    }

    public void setCOMPLETE_CHARGING_TIME(Double COMPLETE_CHARGING_TIME) {
        this.COMPLETE_CHARGING_TIME = COMPLETE_CHARGING_TIME;
    }

    public List<Trip> getCurrentTrip() {
        return currentTrip;
    }

    public void setCurrentTrip(List<Trip> currentTrip) {
        this.currentTrip = currentTrip;
    }

    public Double getSimtime() {
        return simtime;
    }

    public void setSimtime(Double simtime) {
        this.simtime = simtime;
    }

    public Double getBattThreshhold() {
        return battThreshhold;
    }

    public void setBattThreshhold(Double battThreshhold) {
        this.battThreshhold = battThreshhold;
    }
}
