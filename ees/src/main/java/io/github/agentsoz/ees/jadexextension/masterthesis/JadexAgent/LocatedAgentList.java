package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;


import java.util.ArrayList;
import java.util.List;
import io.github.agentsoz.util.Location;

public class LocatedAgentList {

    public List<LocatedAgent> LocatedAgentList = new ArrayList<>();


    //TODO deregister
    //public void






    public String CalculateClosestLocatedAgent (Location startPosition){
        //TODO mabe find a better way to determine distance
        //TODO handle cases with no located agents
        //TODO calculate closest Agent from List

        String closestAgentID = "NoAgentLocated";

        Double lowestDistance = Double.MAX_VALUE;
        Double compareDistance;

        for (int i = 0; i<LocatedAgentList.size(); i++){
            LocatedAgent toInvestigate = LocatedAgentList.get(0);
            compareDistance = Location.distanceBetween(startPosition, toInvestigate.getLastPosition());
            if (compareDistance<lowestDistance){
                compareDistance = lowestDistance;
                closestAgentID = toInvestigate.getAgentID();

            }

        }


        return closestAgentID;

    }

    




}
