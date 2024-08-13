package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities;

import io.github.agentsoz.util.Location;

import java.util.List;

public class Config {

    private Double DRIVING_SPEED;
    private Double MIN_CHARGING_TIME;
    private Double MAX_CHARGING_TIME;
    private Double batteryLevel;
    private Location agentLocation;
    private Double THETA;
    private List<Location> chargingStations;

    // to prevent instantiation
    public Config() { }


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
}
