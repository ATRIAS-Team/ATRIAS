package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.*;
import io.github.agentsoz.ees.trikeagent.BatteryModel;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Vehicle {
    public String name;
    public int id;
    public String futurePosition; //the position vehicle has when finished with its jobs
    public String currentPosition; //position vehicle has at a given timestep
    public String home; //home node
    public LocalDateTime busyUntil;
    public ArrayList<Trip> queuedTrips = new ArrayList<>();
    public ArrayList<Trip> takenTrips = new ArrayList<>();
    public BatteryModel battery = new BatteryModel();
    public BatteryModel futureBattery = new BatteryModel();
    public int chargingTrips = 0;
    public int rebalancingTrips = 0;
    public double customerWaitingTime;
    public HashMap<String, String> configMap;

    public Vehicle(int id, String home, HashMap<String, String> configMap) {
        this.name = "trike:" + id;
        this.id = id;
        this.currentPosition = home;
        this.futurePosition = home;
        this.home = home;
        this.configMap = configMap;
    }

    public Vehicle(Vehicle other) {
        // for copying and mutating the vehicle
        this.name = other.name;
        this.id = other.id;
        this.futurePosition = other.futurePosition;
        this.currentPosition = other.currentPosition;
        this.home = other.home;
        this.busyUntil = other.busyUntil;
        this.takenTrips = new ArrayList<>(other.takenTrips);
        this.queuedTrips = new ArrayList<>(other.queuedTrips);
        this.battery = new BatteryModel();
        battery.setMyBatteryHealth(other.battery.getMyBatteryHealth());
        battery.setMyNumberOfCharges(other.battery.getMyNumberOfCharges());
        battery.setMyChargestate(other.battery.getMyChargestate());
        this.futureBattery = new BatteryModel();
        futureBattery.setMyBatteryHealth(other.futureBattery.getMyBatteryHealth());
        futureBattery.setMyNumberOfCharges(other.futureBattery.getMyNumberOfCharges());
        futureBattery.setMyChargestate(other.futureBattery.getMyChargestate());
        this.configMap = other.configMap;
        this.chargingTrips = other.chargingTrips;
        this.rebalancingTrips = other.rebalancingTrips;
        this.customerWaitingTime = other.customerWaitingTime;
    }

    //allocates trip to the vehicle by adding it to the queue
    public void queueTrip(Trip trip){
        if (queuedTrips.contains(trip)){
            return;
        }
        //add trip to queue
        queuedTrips.add(trip);
        reevaluateQueuedTrips();
        calculateCustomerWaitingTime();
    }

    public void queueChargingTrip(Graph graph){
        String nearestChargingStation = graph.getNearestChargingStation(futurePosition);
        chargingTrips++;
        Trip vehicleChargingTrip = new Trip(name,
                "CH"+chargingTrips,
                1,
                "ChargingTrip",
                busyUntil.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                busyUntil.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                graph.getNodeCoordinates(futurePosition)[0],
                graph.getNodeCoordinates(futurePosition)[1],
                graph.getNodeCoordinates(nearestChargingStation)[0],
                graph.getNodeCoordinates(nearestChargingStation)[1],
                futurePosition,
                nearestChargingStation
        );
        vehicleChargingTrip.calculateTrip(graph);
        queueTrip(vehicleChargingTrip);
        futureBattery.loadBattery();
    }

    public void queueRebalancingTrip(Graph graph, String rebalancingNode){
        rebalancingTrips++;
        Trip vehicleRebalancingTrip = new Trip(name,
                "RB"+rebalancingTrips,
                3,
                "RebalancingTrip",
                busyUntil.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                busyUntil.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                graph.getNodeCoordinates(futurePosition)[0],
                graph.getNodeCoordinates(futurePosition)[1],
                graph.getNodeCoordinates(rebalancingNode)[0],
                graph.getNodeCoordinates(rebalancingNode)[1],
                futurePosition,
                rebalancingNode
        );
        vehicleRebalancingTrip.calculateTrip(graph);
        queueTrip(vehicleRebalancingTrip);
    }

    public void refreshVehicle(LocalDateTime currentTime){
        // iterate using an iterator to avoid index shifting issues during removal
        Iterator<Trip> iterator = queuedTrips.iterator();

        while (iterator.hasNext()) {
            Trip currentTrip = iterator.next();
            LocalDateTime tripEndsAt = currentTrip.vaTime.plusSeconds((long) Math.ceil(currentTrip.calculatedPath.travelTime));

            // if the trip has ended, move it to the takenTrips list and remove it from the queue
            if (currentTime.isAfter(tripEndsAt) || currentTime.isEqual(tripEndsAt)) {
                battery.discharge(currentTrip.calculatedPath.distance, 0);

                if (currentTrip.tripType.equals("ChargingTrip")){
                    battery.loadBattery();
                }
                currentTrip.batteryBefore = battery.getMyChargestate();
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

        return vehicleApproachTrip;
    }

    public double getTotalTravelTime(){
        double totalTravelTime = 0;
        for (Trip trip: queuedTrips){
            totalTravelTime += trip.calculatedPath.travelTime;
        }
        return totalTravelTime;
    }

    public ArrayList<Trip> getOpenQueuedTrips() {
        // Ensure the list is not empty to avoid IndexOutOfBoundsException
        ArrayList<Trip> openTrips = new ArrayList<>();
        for (Trip trip: queuedTrips){
            if (!trip.customerID.equals(name) && !trip.rescheduled){
                openTrips.add(new Trip(trip));
            }
        }
        if (!openTrips.isEmpty()){
            //remove the trip that is currently in progress
            openTrips.remove(0);
        }

        // Return a new ArrayList containing all elements except the first
        return openTrips;
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
        queuedTrips.removeIf(approach -> approach.TripID.contains(trip.TripID));
        queuedTrips.remove(trip);
        reevaluateQueuedTrips();
    }

    private void reevaluateQueuedTrips(){
        if (queuedTrips.isEmpty()){
            return;
        }

        LocalDateTime vaTime = queuedTrips.get(0).vaTime;
        double travelTime = queuedTrips.get(0).calculatedPath.travelTime;
        double accumulatedDistance = queuedTrips.get(0).calculatedPath.distance;

        for (int i = 1; i < queuedTrips.size(); i++){
            //call by reference, new trip instance has to be invoked
            queuedTrips.get(i).vaTime = vaTime.plusSeconds((long) Math.ceil(travelTime));
            vaTime = queuedTrips.get(i).vaTime;
            travelTime = queuedTrips.get(i).calculatedPath.travelTime;
            accumulatedDistance += queuedTrips.get(i).calculatedPath.distance;

            //if i == last trip in queue
            if (i == queuedTrips.size()-1){
                //set busy until and position to the arrival time and end node of the last trip
                busyUntil = vaTime.plusSeconds((long) Math.ceil(travelTime));
                futurePosition = queuedTrips.get(i).nearestEndNode;

                futureBattery = new BatteryModel();
                futureBattery.my_chargestate = battery.getMyChargestate();
                futureBattery.discharge(accumulatedDistance, 0);
            }
        }
    }

    public void handleCharging(Graph graph){
        //evaluate if charging is necessary
        if (futureBattery.getMyChargestate() <= Float.parseFloat(configMap.get("CHARGING_THRESHOLD"))){
            //calculates the closest charging station and queues a trip
            queueChargingTrip(graph);
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