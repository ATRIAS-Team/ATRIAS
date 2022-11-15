package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.time.LocalDateTime;
import java.util.List;
import io.github.agentsoz.util.Location;

public class LocatedAgent {
    private String agentID;
    private Location lastPosition;
    private LocalDateTime timeOfLastUpdate;
    //private String formerZone; // maybe useful in the future

    public LocatedAgent(String agentID, Location lastPosition, LocalDateTime timeOfLastUpdate){
        this.agentID = agentID;
        this.lastPosition = lastPosition;
        this.timeOfLastUpdate = timeOfLastUpdate;
    }

    public void updateLocatedAgent(Location lastPosition, LocalDateTime timeOfLastUpdate){
        this.lastPosition = lastPosition;
        this.timeOfLastUpdate = timeOfLastUpdate;
    }


    public String getAgentID() { return this.agentID; }
    public Location getLastPosition() {
        return this.lastPosition;
    }

    public LocalDateTime getTimeOfLastUpdate() {
        return this.timeOfLastUpdate;
    }






}
