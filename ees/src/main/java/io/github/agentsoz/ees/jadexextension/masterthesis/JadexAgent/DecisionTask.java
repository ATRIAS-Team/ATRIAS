package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import io.github.agentsoz.util.Location;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class DecisionTask {
    private Job job;

    public long timeStamp;


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
        DELEGATED, FAILED, NOT_ASSIGNED
    }
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
}
