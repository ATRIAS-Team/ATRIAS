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
import java.util.ArrayList;
import java.util.List;
import io.github.agentsoz.util.Location;

public class LocatedAgentList {

    public List<LocatedAgent> LocatedAgentList = new ArrayList<>();

    public int size(){
        return LocatedAgentList.size();
    }

    public void updateLocatedAgentList(LocatedAgent newAgent, long timestamp, String action){
        switch (action){
            case "register": {
                LocatedAgentList.add(newAgent);
                break;
            }
            case "update": {
                for (LocatedAgent locatedAgent : LocatedAgentList) {
                    if (newAgent.getAgentID().equals(locatedAgent.getAgentID())) {
                        locatedAgent.updateLocatedAgent(newAgent.getLastPosition(), timestamp);
                    }
                }
               break;
            }
            case "deregister": {
                for (int i= 0; i<LocatedAgentList.size(); i++){
                    if(newAgent.getAgentID().equals(LocatedAgentList.get(i).getAgentID())){
                        LocatedAgentList.remove(i);
                        break;
                    }
                }
                break;
            }
        }
    }

    public String calculateClosestLocatedAgent(Location startPosition){
        String closestAgentID = null;

        double lowestDistance = Double.MAX_VALUE;
        double compareDistance;

        for (int i = 0; i < size(); i++){
            LocatedAgent toInvestigate = LocatedAgentList.get(i);
            compareDistance = Location.distanceBetween(startPosition, toInvestigate.getLastPosition());
            if (compareDistance<lowestDistance){
                lowestDistance = compareDistance;
                closestAgentID = toInvestigate.getAgentID();

            }
        }
        return closestAgentID;
    }
}
