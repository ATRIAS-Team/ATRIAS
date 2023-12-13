package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.time.LocalDateTime;
import java.util.List;
import io.github.agentsoz.util.Location;

public class LocatedAgent {
    private String agentID;
    private Location lastPosition;
    private double timeOfLastUpdate;
    //private String formerZone; // maybe useful in the future

    public LocatedAgent(String agentID, Location lastPosition, double timeOfLastUpdate){
        this.agentID = agentID;
        this.lastPosition = lastPosition;
        this.timeOfLastUpdate = timeOfLastUpdate;
    }

    public LocatedAgent(String agentID, Location lastPosition){
        this.agentID = agentID;
        this.lastPosition = lastPosition;
    }

    public void updateLocatedAgent(Location lastPosition, double timeOfLastUpdate){
        this.lastPosition = lastPosition;
        this.timeOfLastUpdate = timeOfLastUpdate;
    }

    public String getAgentID() { return this.agentID; }
    public Location getLastPosition() {
        return this.lastPosition;
    }
    public double getTimeOfLastUpdate() {
        return this.timeOfLastUpdate;
    }

    public void setTimeOfLastUpdate(Double timeOfLastUpdate){this.timeOfLastUpdate = timeOfLastUpdate;}
}
