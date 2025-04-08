package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class GreedyBruteForceRescheduling extends AbstractScheduler {
    public int reschedules = 0;
    public GreedyBruteForceRescheduling(HashMap<String, String> configMap) {
        super(configMap);
    }

    public void run(){
        if (progressionLogging){
            System.out.println("\nScheduling requests using Greedy with Rescheduling");
        }
        // start by iterating over requests
        for (int i = 0; i < requestedTrips.size(); i++) {
            Trip customerTrip = new Trip(requestedTrips.get(i));

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

            //queue the trips for the best vehicle
            bestVehicle.queueTrip(bestApproach);
            bestVehicle.queueTrip(customerTrip);
            //bestVehicle.handleCharging(graph);

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
        if (rescheduleCandidates.size() > 2){
            reschedules ++;
            for (Trip trip : rescheduleCandidates){
                trip.rescheduled = true;
            }
            rescheduleCandidates.sort(Comparator.comparing(trip -> trip.bookingTime));
            BruteForceScheduler sim = new BruteForceScheduler(configMap);
            sim.SEARCH_DEPTH = 1;
            sim.vehicles = copyAllVehicles();
            sim.removeTripsFromVehicles(vehicleTripMap);
            sim.requestedTrips = rescheduleCandidates;
            sim.graph = graph;
            sim.run();
            vehicles = sim.vehicles;
        }
    }
}
