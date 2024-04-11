package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.util.Location;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DecisionTask {
    //private String customerID;
    //private String jobID;
    //private LocalDateTime bookingTime;
    //private LocalDateTime vaTime;
    //private Location startPosition;
    //private Location endPosition;

    private Job job;

    private LocalDateTime creationTime;

    private String origin;

    private ArrayList<UTScore> UTScoreList = new ArrayList<>();

    private String status;

    private ArrayList<String> neighbourIDs = new ArrayList<>();

    private String associatedTrip;


    //####################################################################################
    // Constructors
    //####################################################################################

    public DecisionTask(Job job, String origin, String status) {
        this.job = job;
        this.origin = origin;
        this.status = status;

    }

    //todo: maybe find a better way to ensure
    public boolean testAllProposalsReceived(){
        boolean complete = false;
        if (neighbourIDs.size() == UTScoreList.size()-1){
            complete = true;
        }
        return complete;
    }

    public void tagBestScore(String ownAgentID){
        Integer positionBestScore = 0;
        Double highestScore = 0.0;
        for (int i=0; i<UTScoreList.size(); i++){
            if(UTScoreList.get(i).getScore() > highestScore){
                highestScore = UTScoreList.get(i).getScore();
                positionBestScore = i;
            }
        }
        for (int i=0; i<UTScoreList.size(); i++) {
            UTScoreList.get(i).setTag("RejectProposal");
        }
        if(UTScoreList.get(positionBestScore).getBidderID().equals(ownAgentID)){
            UTScoreList.get(positionBestScore).setTag("AcceptSelf");
        }
        else{
            UTScoreList.get(positionBestScore).setTag("AcceptProposal");
        }
    }


    public void setUtillityScore(String agentID, Double UTScore){

        UTScore agentScore = new UTScore(agentID, UTScore);
        UTScoreList.add(agentScore);
    }

    public void setStatus(String newStatus){ this.status = newStatus;}

    public void setNeighbourIDs(ArrayList<String> neighbourIDs){this.neighbourIDs = neighbourIDs;}

    public Job getJob(){return job;}



    public ArrayList<UTScore> getUTScoreList(){return UTScoreList;}

    public ArrayList<String> getNeighbourIDs(){return neighbourIDs;}

    public String getStatus(){
        return status;
    }

    public String getOrigin(){
        return origin;
    }

    public String getIDFromJob(){ //todo:redundant replace it by method below
        return job.getID();
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
