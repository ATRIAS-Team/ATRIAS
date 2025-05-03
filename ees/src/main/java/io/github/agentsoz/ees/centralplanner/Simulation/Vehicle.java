package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static io.github.agentsoz.ees.centralplanner.util.Util.timeParser;

public class Vehicle {
    public String name;
    public int id;
    public String currentPosition; //position vehicle has at a given timestep
    public String futurePosition; //the position vehicle has when finished with its jobs
    public LocalDateTime busyUntil;
    public ArrayList<Trip> queuedTrips = new ArrayList<>();
    public ArrayList<Trip> takenTrips = new ArrayList<>();
    public ArrayList<Trip> missedTrips = new ArrayList<>();
    public double customerWaitingTime;
    public HashMap<String, String> configMap;

    public Vehicle(int id, String home, HashMap<String, String> configMap) {
        this.name = "trike:" + id;
        this.id = id;
        this.currentPosition = home;
        this.futurePosition = home;

        this.busyUntil = timeParser(configMap.get("SIMULATION_START_TIME"));
        this.configMap = configMap;
    }

    public Vehicle(Vehicle other) {
        // for copying and mutating the vehicle
        this.name = other.name;
        this.id = other.id;
        this.futurePosition = other.futurePosition;
        this.currentPosition = other.currentPosition;

        this.busyUntil = other.busyUntil;
        this.takenTrips = new ArrayList<>(other.takenTrips);
        this.queuedTrips = new ArrayList<>(other.queuedTrips);
        this.missedTrips = new ArrayList<>(other.missedTrips);
        this.configMap = other.configMap;
        this.customerWaitingTime = other.customerWaitingTime;
    }

    //allocates trip to the vehicle by adding it to the queue
    public void queueTrip(Trip trip){
        if (queuedTrips.contains(trip)) {
            System.out.println("Trip already queued: " + trip);
        }
        //add trip to queue
        queuedTrips.add(trip);

        //this function recalculates the vaTimes
        reevaluateQueuedTrips();
        trip.vehicleBusyTime = busyUntil;
        calculateCustomerWaitingTime();
    }

    private void removeTakenTripsFromActiveQueue(LocalDateTime currentTime){
        //iterate using an iterator to avoid index shifting issues during removal
        Iterator<Trip> iterator = queuedTrips.iterator();
        while (iterator.hasNext()) {
            Trip currentTrip = iterator.next();
            //get the tripEndTime
            LocalDateTime tripEndTime = currentTrip.vaTime.plusSeconds((long) Math.ceil(currentTrip.calculatedPath.travelTime));

            // if the trip has ended, remove it from the active queue
            if (tripEndTime.isBefore(currentTime) || tripEndTime.isEqual(currentTime)) {
                // check if Customer Misses are enabled in the config
                if (Boolean.parseBoolean(configMap.get("ALLOW_CUSTOMER_MISS"))){
                    //if customer is gone (vehicle arrived after bookingTime + theta, remove it from the queue and add it to the missed trips list)
                    if (tripEndTime.isAfter(currentTrip.bookingTime.plusSeconds((long) Double.parseDouble(configMap.get("THETA")))) && currentTrip.driveOperationNumber == 2){
                        missedTrips.add(currentTrip);
                    }
                    else {
                        takenTrips.add(currentTrip);
                    }
                    iterator.remove(); // Safe removal during iteration
                } else {
                    takenTrips.add(currentTrip);
                    iterator.remove();
                }
            } else {
                // break if first queue objects end lies in the future
                break;
            }
        }
    }

    public void refreshVehicle(LocalDateTime currentTime){
        //update busyUntil if value is in the past
        if(busyUntil.isBefore(currentTime)){
            busyUntil = currentTime;
        }

        //update finished queuedTrips and move them to takenTrips
        removeTakenTripsFromActiveQueue(currentTime);

        //update position
        if (takenTrips.isEmpty() && queuedTrips.isEmpty()){
            // if vehicle hasn't had any jobs, return without updating the position
            return;
        }

        if (queuedTrips.isEmpty()){
            //if vehicle has no queued trips, it should be at the destination of the last taken trip
            Trip lastTakenTrip = takenTrips.get(takenTrips.size()-1);
            futurePosition = lastTakenTrip.nearestEndNode;
        } else {
            //if the vehicle has queued trips, it should be on route of the currently first queued trip.
            Trip currentQueuedTrip = queuedTrips.get(0);
            //calculate the timedelta since the trip has started
            LocalDateTime tripStartedAt = currentQueuedTrip.vaTime;
            double timedelta = Duration.between(tripStartedAt, currentTime).toSeconds();
            //calculate where the vehicles position is depending on the time and the current trip
            for (Edge edge: currentQueuedTrip.calculatedPath.path){
                if (timedelta-edge.travelTime>0){
                    timedelta = timedelta-edge.travelTime;
                } else {
                    //sets the position if timedelta < 50% of edgetraveltime to the from node, otherwise to the to node
                    if (timedelta<edge.travelTime/2){
                        currentPosition = edge.from.id;
                    } else {
                        currentPosition = edge.to.id;
                    }
                }
            }
        }
    }

    public Trip evaluateApproach(Trip customerTrip, Graph graph){
        //first generate trip to get to the customer
        Trip vehicleApproachTrip = new Trip(name,
                customerTrip.TripID,
                1,
                "ApproachTrip",
                customerTrip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                busyUntil.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                graph.getNodeCoordinates(futurePosition)[0],
                graph.getNodeCoordinates(futurePosition)[1],
                customerTrip.startX,
                customerTrip.startY,
                futurePosition,
                customerTrip.nearestStartNode
        );
        //calculate the path of the trip
        vehicleApproachTrip.calculateTrip(graph);
        //add fixed delay to mitigate Jadex-MATSim inaccuracies during contract-net-protocol negotiation and customers entering/leaving vehicles

        return vehicleApproachTrip;
    }

    public int getMissedTrips(){
        int missedTrips = 0;
        for (Trip trip: queuedTrips){
            int approachTime = (int) Duration.between(trip.bookingTime, trip.vaTime).toSeconds();
            if (approachTime > Double.parseDouble(configMap.get("THETA")) && trip.driveOperationNumber == 2){
                missedTrips++;
            }
        }
        for (Trip trip: takenTrips){
            int approachTime = (int) Duration.between(trip.bookingTime, trip.vaTime).toSeconds();
            if (approachTime > Double.parseDouble(configMap.get("THETA")) && trip.driveOperationNumber == 2){
                missedTrips++;
            }
        }
        return missedTrips;
    }

    public void removeQueuedTrip(Trip trip){
        //this takes a customer trip and removes it as well as the corresponding approach Trip if its not currently in progress
        ArrayList<Trip> copiedQueuedTrips = new ArrayList<>(queuedTrips);
        for (int i = 1; i < copiedQueuedTrips.size(); i++){
            if (copiedQueuedTrips.get(i).TripID.equals(trip.TripID)){
                queuedTrips.remove(copiedQueuedTrips.get(i));
            }
        }

        reevaluateQueuedTrips();
    }

    private void reevaluateQueuedTrips(){
        if (queuedTrips.isEmpty()){
            return;
        }

        double travelTime = queuedTrips.get(0).calculatedPath.travelTime;
        LocalDateTime timeAfterPrevTrip = queuedTrips.get(0).vaTime.plusSeconds((long) Math.ceil(travelTime));

        busyUntil = timeAfterPrevTrip;
        futurePosition = queuedTrips.get(0).nearestEndNode;

        //updates the queued trips vaTimes and the vehicle busyTime and future Position
        for (int i = 1; i < queuedTrips.size(); i++){
            Trip queuedTrip = queuedTrips.get(i);
            queuedTrip.vaTime = timeAfterPrevTrip;
            timeAfterPrevTrip = queuedTrip.vaTime.plusSeconds((long) Math.ceil(queuedTrip.calculatedPath.travelTime));

            busyUntil = timeAfterPrevTrip;
            futurePosition = queuedTrip.nearestEndNode;

        }
    }

    public void calculateCustomerWaitingTime(){
        //calculates the waiting time for taken and queued trips meaning the summed duration between booking and arrival times
        customerWaitingTime = 0;
        for (Trip trip: takenTrips){
            //number 2 is the customer trip itself, which holds the information of when it was booked and when the vehicle got there
            if (trip.driveOperationNumber == 2){
                customerWaitingTime += (int) Duration.between(trip.bookingTime, trip.vaTime).toSeconds();
            }
        }
        for (Trip trip: queuedTrips){
            if (trip.driveOperationNumber == 2){
                customerWaitingTime += (int) Duration.between(trip.bookingTime, trip.vaTime).toSeconds();
            }
        }
    }
}