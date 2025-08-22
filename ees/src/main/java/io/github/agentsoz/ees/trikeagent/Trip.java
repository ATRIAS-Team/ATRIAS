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

public class Trip {

    public String tripID;

    public Job job;

    public DecisionTask decisionTask;

    public String tripType; //charging trip, customer trip, ...

    public LocalDateTime vaTime; // vehicle arriving time
    public Location startPosition; // use this for trips with just one Geolocation
    public Location endPosition ; // End of the trip used for customer trips
    public String progress;

    public LocalDateTime arriveTime;

    public LocalDateTime endTime;

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

    @Override
    public String toString() {
        return "{" +
                "tripID='" + tripID + '\'' +
                ", job=" + job +
                ", tripType='" + tripType + '\'' +
                ", vaTime=" + vaTime +
                ", startPosition=" + startPosition +
                ", endPosition=" + endPosition +
                ", progress='" + progress + '\'' +
                ", endTime=" + endTime + '\'' +
                ", arriveTime=" + arriveTime +
                '}';
    }





    //####################################################################################
    // getter
    //####################################################################################

    //@Marcel musste public machen

    public DecisionTask getDecisionTask() {
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

    public LocalDateTime getEndTime(){
        return this.endTime;
    }

    public LocalDateTime getArriveTime() {return this.arriveTime; }


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

    public void setEndTime(LocalDateTime endTime){
        this.endTime = endTime;
    }

    public void setArriveTime(LocalDateTime arriveTime){
        this.arriveTime = arriveTime;
    }
}
