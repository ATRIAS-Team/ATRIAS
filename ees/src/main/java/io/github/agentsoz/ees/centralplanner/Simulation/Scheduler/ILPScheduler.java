package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class ILPScheduler extends AbstractScheduler {
    ArrayList<ArrayList<Integer>> vehicleAssignments = new ArrayList<>();
    public int SEARCH_DEPTH = 1;

    public ILPScheduler(HashMap<String, String> configMap) {
        super(configMap);
    }

    public void run() {
        if (progressionLogging){
            System.out.println("\nScheduling requests using Brute Force Scheduler");
        }

        ArrayList<ArrayList<Trip>> movingWindows = movingWindowGenerator();

        for (int mw = 0; mw < movingWindows.size(); mw++) {
            ArrayList<Trip> currentTripWindow = movingWindows.get(mw);
            // generate all permutations depending on the search depth
            vehicleAssignments = new ArrayList<>();
            generateAssignmentsHelper(new ArrayList<>());

            // saves best permutation
            ArrayList<Vehicle> bestAssignment = new ArrayList<>();
            double bestWaitingTime = Double.MAX_VALUE;

            //iterate over all permutations
            for (int i = 0; i < vehicleAssignments.size(); i++) {
                ArrayList<Vehicle> copiedVehicles = copyAllVehicles();
                //iterate over the selected trips of the permutation
                for (int j = 0; j < currentTripWindow.size(); j++) {
                    //retrieve the copied vehicle of the corresponding permutation and depth
                    Vehicle assignedVehicle = copiedVehicles.get(vehicleAssignments.get(i).get(j));
                    Trip customerTrip = new Trip(currentTripWindow.get(j));
                    assignedVehicle.refreshVehicle(customerTrip.bookingTime);

                    Trip approachTrip = assignedVehicle.evaluateApproach(customerTrip, graph);
                    assignedVehicle.queueTrip(approachTrip);
                    assignedVehicle.queueTrip(customerTrip);
                }
                //if assignment took place, calculate the waiting time
                double summedWaitingTime = calculateWaitingTime(copiedVehicles);
                //refresh best result
                if (summedWaitingTime < bestWaitingTime) {
                    bestWaitingTime = summedWaitingTime;
                    bestAssignment = copiedVehicles;
                }

                if (progressionLogging) {
                    showProgress(mw, movingWindows.size() - 1, " Permutation: " + (i+1) + "/" + vehicleAssignments.size());
                }
            }
            vehicles = bestAssignment;
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

    private ArrayList<ArrayList<Trip>> movingWindowGenerator(){
        ArrayList<ArrayList<Trip>> movingWindowList = new ArrayList<>();
        int windowElement = 0;
        for (Trip trip : requestedTrips) {
            if (windowElement%SEARCH_DEPTH == 0){
                movingWindowList.add(new ArrayList<>());
            }
            movingWindowList.get(movingWindowList.size()-1).add(trip);
            windowElement++;
        }
        return movingWindowList;
    }

    private double calculateWaitingTime(ArrayList<Vehicle> copiedVehicles) {
        double summedWaitingTime = 0.0;
        for (Vehicle vehicle : copiedVehicles) {
//            summedWaitingTime += vehicle.customerWaitingTime;
            summedWaitingTime += (vehicle.getMissedTrips()*20) + vehicle.customerWaitingTime;
        }
        return summedWaitingTime;
    }
}
