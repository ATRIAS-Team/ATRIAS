package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;



import io.github.agentsoz.util.Location;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Trip {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public String tripID;
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

    Trip(String tripID, String tripType, LocalDateTime vaTime, Location startPosition, Location endPosition, String progress){
        this.tripID = tripID;
        this.tripType = tripType;
        this.vaTime = vaTime;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.progress = progress;
    }

    //####################################################################################
    // getter
    //####################################################################################

    String getTripID() {
        return this.tripID;
    }

    String getTripType() {
        return this.tripType;
    }

    LocalDateTime getVaTime() {
        return this.vaTime;
    }

    Location getStartPosition() {
        return this.startPosition;
    }

    Location getEndPosition() {
        return this.endPosition;
    }

    String getProgress() {
        return this.progress;
    }



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
