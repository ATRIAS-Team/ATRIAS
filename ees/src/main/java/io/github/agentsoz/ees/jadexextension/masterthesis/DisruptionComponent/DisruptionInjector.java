package io.github.agentsoz.ees.jadexextension.masterthesis.DisruptionComponent;

import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.SimIDMapper;

import java.util.ArrayList;
import java.util.List;

public class DisruptionInjector {
    String toDisruptAgentID;

    String disruptionType;

    Integer disruptionTime;

    Integer disruptionCancelTime;


    public DisruptionInjector(String toDisruptAgentID, String disruptionType, int disruptionTime) {
        this.toDisruptAgentID = toDisruptAgentID;
        this.disruptionType = disruptionType;
        this.disruptionTime = disruptionTime;
    }

    public String getToDisruptAgentID() {
        return toDisruptAgentID;
    }

    public void setToDisruptAgentID(String toDisruptAgentID) {
        this.toDisruptAgentID = toDisruptAgentID;
    }

    public String getDisruptionType() {
        return disruptionType;
    }

    public void setDisruptionType(String disruptionType) {
        this.disruptionType = disruptionType;
    }

    public Integer getDisruptionTime() {
        return disruptionTime;
    }

    public void setDisruptionTime(Integer disruptionTime) {
        this.disruptionTime = disruptionTime;
    }

    public Integer getDisruptionCancelTime() {
        return disruptionCancelTime;
    }

    public void setDisruptionCancelTime(Integer disruptionCancelTime) {
        this.disruptionCancelTime = disruptionCancelTime;
    }

}
