package io.github.agentsoz.ees.trikeagent;

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

import io.github.agentsoz.ees.shared.Job;
import io.github.agentsoz.util.Location;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class DecisionTask {
    private Job job;

    public long timeStamp;

    public long numRequests = 0;
    public long numResponses = 0;


    private String origin;

    private ArrayList<UTScore> UTScoreList = new ArrayList<>();

    private Status status;

    private ArrayList<String> agentIds = new ArrayList<>();



    private String associatedTrip;


    //####################################################################################
    // Constructors
    //####################################################################################

    public DecisionTask(Job job, String origin, Status status) {
        this.job = job;
        this.origin = origin;
        this.status = status;
    }

    public void tagBestScore(String ownAgentID){
        int positionBestScore = 0;
        double highestScore = 0.0;

        for (int i=0; i<UTScoreList.size(); i++){
            if(UTScoreList.get(i).getScore() > highestScore){
                highestScore = UTScoreList.get(i).getScore();
                positionBestScore = i;
            }
        }
        for (UTScore utScore : UTScoreList) {
            utScore.setTag("RejectProposal");
        }

        if(UTScoreList.get(positionBestScore).getBidderID().equals(ownAgentID)){
            UTScoreList.get(positionBestScore).setTag("AcceptSelf");
        }
        else{
            UTScoreList.get(positionBestScore).setTag("AcceptProposal");
        }
    }


    public void setUtilityScore(String agentID, Double UTScore){
        UTScore agentScore = new UTScore(agentID, UTScore);
        UTScoreList.add(agentScore);
    }

    public void setStatus(Status newStatus){ this.status = newStatus;}

    public void setAgentIds(ArrayList<String> agentIds){this.agentIds = agentIds;}

    public void addNeighbourIDs(ArrayList<String> neighbourIDs){this.agentIds.addAll(neighbourIDs);}

    public Job getJob(){return job;}



    public ArrayList<UTScore> getUTScoreList(){return UTScoreList;}

    public ArrayList<String> getAgentIds(){return agentIds;}

    public enum Status{
        NEW, DELEGATE, COMMIT, CFP_READY,
        DECISION_READY, CONFIRM_READY, PROPOSED,
        WAITING_NEIGHBOURS, WAITING_PROPOSALS, WAITING_CONFIRM, WAITING_MANAGER,
        DELEGATED, FAILED, NOT_ASSIGNED, COMMITTED
    }

    public String extra;

    public Status getStatus(){
        return status;
    }

    public String getOrigin(){
        return origin;
    }

    public String getJobID(){
        return job.getID();
    }

    public LocalDateTime getVATimeFromJob(){
        return job.getVATime();
    }

    public Location getStartPositionFromJob() {
        return job.getStartPosition();
    }

    public Location getEndPositionFromJob() {
        return job.getEndPosition();
    }

    public void initRequestCount(long numRequests){
        this.numRequests = numRequests;
        this.numResponses = 0;
    }

    public boolean responseReady(){
        return numRequests == numResponses;
    }
}
