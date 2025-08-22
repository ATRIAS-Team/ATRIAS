package io.github.agentsoz.ees.gui.model;


import java.time.LocalDateTime;

public class Trip {

    public String tripID;

    public Job job;

    public DecisionTask decisionTask;

    public String tripType; //charging trip, customer trip, ...

    public LocalDateTime vaTime; // vehicle arriving time
    public Location startPosition; // use this for trips with just one Geolocation
    public Location endPosition ; // End of the trip used for customer trips
    public String progress;

    public LocalDateTime endTime;

    public LocalDateTime arriveTime;
}
