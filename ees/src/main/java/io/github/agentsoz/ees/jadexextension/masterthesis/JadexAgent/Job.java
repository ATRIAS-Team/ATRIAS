package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import io.github.agentsoz.util.Location;

public class Job {
    private String customerID;
    private String jobID;
    private LocalDateTime bookingTime;
    private LocalDateTime vaTime;
    //private List<Double> startPosition; //old Version Mahkam
    //private List<Double> endPosition; //old Version Mahkam
    private Location startPosition;
    private Location endPosition;


    //####################################################################################
    // Constructors
    //####################################################################################

    public Job(String customerID, String jobID, LocalDateTime bookingTime, LocalDateTime vaTime, Location startPosition, Location endPosition){
        this.customerID = customerID;
        this.jobID = jobID;
        this.bookingTime = bookingTime;
        this.vaTime = vaTime;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }


    public Job(String messageJob) {

        //String str = "1986-04-08 12:30";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        //LocalDateTime dateTime = LocalDateTime.parse(str, formatter);


        String segments[] = messageJob.split("#");
        String customerID = segments[0];
        String jobID = segments[1];
        //LocalDateTime bookingTime = LocalDateTime.parse(segments[2], formatter); //Error
        //LocalDateTime vaTime = LocalDateTime.parse(segments[3], formatter); //Error
        LocalDateTime bookingTime = LocalDateTime.now(); //Error
        LocalDateTime vaTime = LocalDateTime.now(); //Error


        Double startPosX = Double.parseDouble(segments[4]);
        Double startPosY = Double.parseDouble(segments[5]);
        Double endPosX = Double.parseDouble(segments[6]);
        Double endPosY = Double.parseDouble(segments[7]);

        this.customerID = customerID;
        this.jobID = jobID;
        this.bookingTime = bookingTime;
        this.vaTime = vaTime;
        this.startPosition = new Location("", startPosX, startPosY);
        this.endPosition = new Location("", endPosX, endPosY);

    }

    //####################################################################################
    // method
    //####################################################################################

    public String JobForTransfer(){
        //TODO: find better format for all kind of messages like in Visio extra class

        String messageJob = customerID + "#" + jobID + "#" + bookingTime + "#" + vaTime + "#" + Double.toString(startPosition.getX()) + "#" + Double.toString(startPosition.getY()) + "#" + Double.toString(endPosition.getX()) + "#" + Double.toString(endPosition.getY());

        return messageJob;
    }
    //Serialization and Deserialization






    public String getCustomerID() {
        return this.customerID;
    }

    public String getID(){
        return jobID;
    }

    public LocalDateTime getbookingTime() {
        return this.bookingTime;
    }

    public LocalDateTime vaTime() {
        return this.vaTime;
    }

    public Location getStartPosition() {
        return this.startPosition;
    }

    public Location getEndPosition() {
        return this.endPosition;
    }




}
