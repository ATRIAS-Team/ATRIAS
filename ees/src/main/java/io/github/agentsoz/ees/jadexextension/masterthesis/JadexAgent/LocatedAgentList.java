package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import io.github.agentsoz.util.Location;

public class LocatedAgentList {

    public List<LocatedAgent> LocatedAgentList = new ArrayList<>();


    //public LocatedAgentList(){

    //}

    //TODO deregister
    //public void

    public Integer size(){
        return LocatedAgentList.size();
    }

    public void updateLocatedAgentList(LocatedAgent newAgent, Double simTime, String action){

        if (action.equals("register")){
            LocatedAgentList.add(newAgent);
        }
        else if (action.equals("update")){
            for (int i= 0; i<LocatedAgentList.size(); i++){
                if(newAgent.getAgentID().equals(LocatedAgentList.get(i).getAgentID())){
                    LocatedAgentList.get(i).updateLocatedAgent(newAgent.getLastPosition(),
                            LocatedAgentList.get(i).getTimeOfLastUpdate() + simTime);
                }
            }
        }
        else if (action.equals("deregister")){
            for (int i= 0; i<LocatedAgentList.size(); i++){
                if(newAgent.getAgentID().equals(LocatedAgentList.get(i).getAgentID())){
                    LocatedAgentList.remove(i);
                }
            }
        }
        else {
            //TODO: ERROR handling
        }
    }



    public String calculateClosestLocatedAgent(Location startPosition){
        //TODO mabe find a better way to determine distance
        //TODO handle cases with no located agents
        //TODO calculate closest Agent from List

        String closestAgentID = "NoAgentsLocated";

        Double lowestDistance = Double.MAX_VALUE;
        Double compareDistance;

        for (int i = 0; i<LocatedAgentList.size(); i++){
            LocatedAgent toInvestigate = LocatedAgentList.get(i);
            compareDistance = Location.distanceBetween(startPosition, toInvestigate.getLastPosition());
            if (compareDistance<lowestDistance){
                lowestDistance = compareDistance; //fixed! was compareDistance = lowestDistance before
                closestAgentID = toInvestigate.getAgentID();

            }

        }


        return closestAgentID;

    }






}