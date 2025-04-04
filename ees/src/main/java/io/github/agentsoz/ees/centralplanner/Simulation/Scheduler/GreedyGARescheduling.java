package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class GreedyGARescheduling extends AbstractScheduler {
    public GreedyGARescheduling(HashMap<String, String> configMap) {
        super(configMap);
    }

    public void run(){
        if (progressionLogging){
            System.out.println("\nScheduling requests using Greedy with GA Rescheduling");
        }
        // start by iterating over requests
        for (int i = 0; i < requestedTrips.size(); i++) {
            Trip customerTrip = requestedTrips.get(i);

            if (progressionLogging){
                showProgress(i, requestedTrips.size()-1);
            }

            double bestTravelTime = Double.MAX_VALUE;
            Vehicle bestVehicle = null;
            Trip bestApproach = null;

            //iterate over each vehicle and calculate the approach time for the current position of the vehicle
            for (Vehicle vehicle : vehicles){
                //refresh vehicles queue with current time of the loop iteration
                vehicle.refreshVehicle(customerTrip.bookingTime);

                //calculate trip vatime for vehicles with given queue
                Trip evaluatedApproach = vehicle.evaluateApproach(customerTrip, graph);

                double customerWaitTime = Duration.between(customerTrip.bookingTime,
                                evaluatedApproach.vaTime.plusSeconds((long) Math.ceil(evaluatedApproach.calculatedPath.travelTime)))
                        .toSeconds();

                if (customerWaitTime<bestTravelTime){
                    bestTravelTime = customerWaitTime;
                    bestVehicle = vehicle;
                    bestApproach = evaluatedApproach;
                }
            }
            if (bestVehicle == null){
                System.out.println("Vehicle " + bestVehicle + " not found");
                return;
            }

            bestVehicle.queueTrip(bestApproach);
            customerTrip.vaTime = bestApproach.vaTime.plusSeconds((long) Math.ceil(bestApproach.calculatedPath.travelTime));

            //queue the trip for the best vehicle
            bestVehicle.queueTrip(customerTrip);
//            bestVehicle.handleCharging(graph);

            rescheduleVehicles();
        }
    }

    private void rescheduleVehicles() {
        // Iterate over each vehicle
        ArrayList<Trip> rescheduleCandidates = new ArrayList<>();
        HashMap<Integer, ArrayList<Trip>> vehicleTripMap = new HashMap<>();
        for (Vehicle vehicle : vehicles) {
            ArrayList<Trip> openTrips = vehicle.getOpenQueuedTrips();
            vehicleTripMap.put(vehicle.id, openTrips);
            rescheduleCandidates.addAll(openTrips);
        }
        if (rescheduleCandidates.size() > 5){
            rescheduleCandidates.sort(Comparator.comparing(trip -> trip.bookingTime));
            GreedyScheduler sim = new GreedyScheduler(configMap);
            sim.vehicles = copyAllVehicles();
            sim.removeTripsFromVehicles(vehicleTripMap);
            sim.requestedTrips = rescheduleCandidates;
            sim.graph = graph;
            sim.run();
            vehicles = sim.vehicles;
        }
    }
}
