package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;

public class Vehicle {
    public String name;
    public int id;
    public String futurePosition; //the position vehicle has when finished with its jobs
    public String currentPosition; //position vehicle has at a given timestep
    public String home; //home node
    public LocalDateTime busyUntil;
    public double powerConsumption;
    public double battery;
    public ArrayList<Trip> queuedTrips = new ArrayList<>();
    public ArrayList<Trip> takenTrips = new ArrayList<>();

    public Vehicle(int id, String home, float powerConsumption, float battery) {
        this.name = "Vehicle-" + id;
        this.id = id;
        this.futurePosition = home;
        this.home = home;
        this.powerConsumption = powerConsumption;
        this.battery = battery;
    }
    public Vehicle(int id, String home) {
        this.name = "Vehicle-" + id;
        this.id = id;
        this.futurePosition = home;
        this.home = home;
        this.powerConsumption = 1;
        this.battery = 1000;
    }

    //allocates trip to the vehicle by adding it to the queue
    public void queueTrip(Trip trip){
        //check if trip starts and ends on the same node
        if (trip.calculatedPath.distance == 0){
            return;
        }

        //add trip to queue
        queuedTrips.add(trip);
        battery = trip.batteryLevel;
        futurePosition = trip.calculatedPath.path.get(trip.calculatedPath.path.size()-1).to.id;

        //calculates when a vehicle is done with its jobs
        busyUntil = trip.vaTime.plusSeconds((long) Math.ceil(trip.calculatedPath.travelTime));
    }

    public void refreshVehicle(LocalDateTime currentTime){
        // iterate using an iterator to avoid index shifting issues during removal
        Iterator<Trip> iterator = queuedTrips.iterator();

        while (iterator.hasNext()) {
            Trip currentTrip = iterator.next();
            LocalDateTime tripEndsAt = currentTrip.vaTime.plusSeconds((long) Math.ceil(currentTrip.calculatedPath.travelTime));

            // if the trip has ended, move it to the takenTrips list and remove it from the queue
            if (currentTime.isAfter(tripEndsAt) || currentTime.isEqual(tripEndsAt)) {
                takenTrips.add(currentTrip);
                iterator.remove(); // Safe removal during iteration
            } else {
                // break if first queue objects end lies in the future
                break;
            }
        }

        //update position
        if (takenTrips.isEmpty() && queuedTrips.isEmpty()){
            // if vehicle hasn't had any jobs, return without updating the position
            return;
        }
        if (queuedTrips.isEmpty()){
            //if vehicle has no queued trips, it should be at the destination of the last taken trip
            Trip lastTakenTrip = takenTrips.get(takenTrips.size()-1);
            futurePosition = lastTakenTrip.calculatedPath.path.get(lastTakenTrip.calculatedPath.path.size()-1).to.id;
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
        if (busyUntil == null){
            busyUntil = customerTrip.bookingTime;
        }

        if (busyUntil.isBefore(customerTrip.bookingTime)){
            busyUntil = customerTrip.bookingTime;
        }
        //first generate trip to get to the customer
        Trip vehicleApproachTrip = new Trip(name,
                customerTrip.TripID+"-approach",
                customerTrip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                busyUntil.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                graph.getNodeCoordinates(futurePosition)[0],
                graph.getNodeCoordinates(futurePosition)[1],
                customerTrip.startX,
                customerTrip.startY
        );
        //calculate the path of the trip
        vehicleApproachTrip.calculateTrip(graph);

        return vehicleApproachTrip;
    }


}