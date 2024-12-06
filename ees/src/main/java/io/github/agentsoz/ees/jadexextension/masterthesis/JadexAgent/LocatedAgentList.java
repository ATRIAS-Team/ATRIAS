package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import io.github.agentsoz.util.Location;

public class LocatedAgentList {

    public List<LocatedAgent> LocatedAgentList = new ArrayList<>();

    public int size(){
        return LocatedAgentList.size();
    }

    public void updateLocatedAgentList(LocatedAgent newAgent, Double simTime, String action){
        switch (action){
            case "register": {
                LocatedAgentList.add(newAgent);
                break;
            }
            case "update": {
                for (LocatedAgent locatedAgent : LocatedAgentList) {
                    if (newAgent.getAgentID().equals(locatedAgent.getAgentID())) {
                        locatedAgent.updateLocatedAgent(newAgent.getLastPosition(), simTime);
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
