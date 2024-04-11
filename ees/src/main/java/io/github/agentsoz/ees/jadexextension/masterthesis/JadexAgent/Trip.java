package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;



import io.github.agentsoz.util.Location;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Trip {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public String tripID;

    private Job job;

    public DecisionTask decisionTask;

    public String tripType; //charging trip, customer trip, ...
    public LocalDateTime vaTime; // vehicle arriving time
    public Location startPosition; // use this for trips with just one Geolocation
    public Location endPosition ; // End of the trip used for customer trips
    public String progress;

    //####################################################################################
    // Constructors
    //####################################################################################

    public Trip(String tripID, String tripType, Location startPosition, String progress){
        this.tripID = tripID;
        this.tripType = tripType;
        this.startPosition = startPosition;
        this.progress = progress;
    }


    public Trip(String tripID, String tripType, LocalDateTime vaTime, Location startPosition, Location endPosition, String progress){
        this.tripID = tripID;
        this.tripType = tripType;
        this.vaTime = vaTime;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.progress = progress;
    }

    public Trip(DecisionTask decisionTask, String tripID, String tripType, LocalDateTime vaTime, Location startPosition, Location endPosition, String progress){
        this.decisionTask = decisionTask;
        this.tripID = tripID;
        this.tripType = tripType;
        this.vaTime = vaTime;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.progress = progress;
    }

    public Trip(String tripID, Job job, String tripType, LocalDateTime vaTime, Location startPosition, Location endPosition, String progress){
        this.tripID = tripID;
        this.job = job;
        this.tripType = tripType;
        this.vaTime = vaTime;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.progress = progress;
    }


    //short Trip
    public Trip(String messageTrip){


        String segments[] = messageTrip.split("#");
        String tripID = segments[0];
        String tripType = segments[1];
        Double startPosX = Double.parseDouble(segments[2]);
        Double startPosY = Double.parseDouble(segments[3]);
        String progress = segments[4];

        Location startPosition = new Location("", startPosX, startPosY);

        this.tripID = tripID;
        this.tripType = tripType;
        this.startPosition = startPosition;
        this.progress = progress;
    }

    //####################################################################################
    // method
    //####################################################################################

    //short Trip
    public String tripForTransfer(){
        //List<Integer> messageTrip = new ArrayList<Integer>(String);
        //messageTrip.add(tripID);
        //messageTrip.add(tripType)
        String messageTrip = tripID + "#" + tripType + "#" + Double.toString(startPosition.getX()) + "#" + Double.toString(startPosition.getY()) + "#" + progress;

        //vaTime
        //endPosition.getX();
        //endPosition.getY();
        return messageTrip;
    }




    //####################################################################################
    // getter
    //####################################################################################

    //@Marcel musste public machen

    public DecisionTask getDecisionTaskD() {
        return this.decisionTask;
    }
    public String getTripID() {
        return this.tripID;
    }

    public String getTripType() {
        return this.tripType;
    }

    public LocalDateTime getVATime() {
        return this.vaTime;
    }

    public Location getStartPosition() {
        return this.startPosition;
    }

    public Location getEndPosition() {
        return this.endPosition;
    }

    public String getProgress() {
        return this.progress;
    }

    /**
    public Job getJob(){
        this.Job.getOrigin();

        return this.Job; }
    **/


    //####################################################################################
    // setter
    //####################################################################################

    void setTripID(String tripID) {
        this.tripType = tripID;
    }

    void setTripType(String tripType) {
        this.tripType = tripType;
    }

    void setVaTime(LocalDateTime vaTime) {
        this.vaTime = vaTime;
    }

    void setStartPosition(Location startPosition) {
        this.startPosition = startPosition;
    }

    void setEndPosition(Location endPosition) {
        this.endPosition = endPosition;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

}
