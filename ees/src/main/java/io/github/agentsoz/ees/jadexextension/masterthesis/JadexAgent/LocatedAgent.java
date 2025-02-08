package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
