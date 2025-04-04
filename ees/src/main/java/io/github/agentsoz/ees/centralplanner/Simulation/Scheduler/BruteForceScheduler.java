package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Graph.Graph;
import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class BruteForceScheduler extends AbstractScheduler {
    ArrayList<ArrayList<Integer>> vehicleAssignments = new ArrayList<>();
    private static final int SEARCH_DEPTH = 4;


    public BruteForceScheduler(HashMap<String, String> configMap) {
        super(configMap);
        generateAssignmentsHelper(new ArrayList<>());
    }

    public void run() {
        System.out.println("\nScheduling requests using Brute Force Scheduler");

        ArrayList<Integer> bestAssignment = new ArrayList<>();
        double bestWaitingTime = Double.MAX_VALUE;

        for (int i = 0; i < vehicleAssignments.size(); i++) {
            for (int j = 0; j < SEARCH_DEPTH; j++) {
                Vehicle assignedVehicle = vehicles.get(vehicleAssignments.get(i).get(j));
                Trip customerTrip = requestedTrips.get(j);
                Trip approachTrip = assignedVehicle.evaluateApproach(customerTrip, graph);
                assignedVehicle.refreshVehicle(customerTrip.bookingTime);
                assignedVehicle.queueTrip(approachTrip);
                customerTrip.vaTime = approachTrip.vaTime.plusSeconds((long) Math.ceil(approachTrip.calculatedPath.travelTime));
                assignedVehicle.queueTrip(customerTrip);
            }
            double summedWaitingTime = calculateWaitingTime();
            if (summedWaitingTime < bestWaitingTime) {
                bestWaitingTime = summedWaitingTime;
                bestAssignment = vehicleAssignments.get(i);
            }
            for (Vehicle vehicle : vehicles) {
                vehicle.resetVehicle();
            }
            showProgress(i, vehicleAssignments.size()-1);
        }

        for (int j = 0; j < SEARCH_DEPTH; j++) {
            Vehicle assignedVehicle = vehicles.get(bestAssignment.get(j));
            Trip customerTrip = requestedTrips.get(j);
            Trip approachTrip = assignedVehicle.evaluateApproach(customerTrip, graph);
            assignedVehicle.refreshVehicle(customerTrip.bookingTime);
            assignedVehicle.queueTrip(approachTrip);
            customerTrip.vaTime = approachTrip.vaTime.plusSeconds((long) Math.ceil(approachTrip.calculatedPath.travelTime));
            assignedVehicle.queueTrip(customerTrip);
        }

        //update rest of queued trips from vehicles, even after last booking came in
        for (Vehicle vehicle : vehicles){
            if (!vehicle.queuedTrips.isEmpty()){
                Trip lastTrip = vehicle.queuedTrips.get(vehicle.queuedTrips.size()-1);
                vehicle.refreshVehicle(lastTrip.vaTime.plusSeconds((long) Math.ceil(lastTrip.calculatedPath.travelTime)));
            }
            for (Trip customerTrip : vehicle.takenTrips){
                bestVehicleMap.put(customerTrip.TripID, vehicle.id);
            }
        }
    }


    private void generateAssignmentsHelper(List<Integer> currentAssignment) {
        if (currentAssignment.size() == SEARCH_DEPTH) {
            vehicleAssignments.add(new ArrayList<>(currentAssignment));
            return;
        }
        for (int i = 0; i < vehicles.size(); i++) {
            currentAssignment.add(i);
            generateAssignmentsHelper(currentAssignment);
            currentAssignment.remove(currentAssignment.size() - 1);
        }

    }

    private double calculateWaitingTime() {
        double summedWaitingTime = 0.0;
        for (Vehicle vehicle : vehicles) {
            for (Trip trip : vehicle.takenTrips) {
                if (!trip.customerID.equals(vehicle.name)) {
                    int customerWaitTime = (int) Duration.between(trip.bookingTime, trip.vaTime).toSeconds();
                    summedWaitingTime += customerWaitTime;
                }
            }
        }
        return summedWaitingTime;
    }
}
