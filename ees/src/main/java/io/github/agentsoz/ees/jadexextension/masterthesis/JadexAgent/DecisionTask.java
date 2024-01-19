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

    private List<UTScore> UTScoreList = new ArrayList<>();

    private String status;

    private String associatedTrip;


    //####################################################################################
    // Constructors
    //####################################################################################

    public DecisionTask(Job job, String origin, String status) {
        this.job = job;
        this.origin = origin;
        this.status = status;

    }

    public void setUtillityScore(String agentID, Double UTScore){

        UTScore agentScore = new UTScore(agentID, UTScore);
        UTScoreList.add(agentScore);
    }

    public void setStatus(String newStatus){ this.status = newStatus;}

    public String getStatus(){
        return status;
    }

    public String getIDFromJob(){
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
