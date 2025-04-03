package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Graph.Graph;
import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.ArrayList;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class GreedyWithReschedulingScheduler extends AbstractScheduler {
    public GreedyWithReschedulingScheduler(String configFilePath) {
        super(configFilePath);
    }

    public void run(){
        System.out.println("\nScheduling requests using Greedy with Rescheduling");
        // start by iterating over requests
        for (int i = 0; i < requestedTrips.size(); i++) {
            Trip customerTrip = requestedTrips.get(i);

            showProgress(i, requestedTrips.size()-1);

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

            rescheduleVehicles(vehicles);

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

    private void rescheduleVehicles(ArrayList<Vehicle> vehicles) {
        // Iterate over each vehicle
        for (Vehicle vehicle : vehicles) {
            ArrayList<Trip> rescheduleCandidates = vehicle.getOpenQueuedTrips();

            for (Trip trip : rescheduleCandidates) {
                Vehicle bestVehicle = vehicle;
                double bestTravelTime = Double.MAX_VALUE;
                Trip bestApproach = null;

                // Try to find a better vehicle for this trip
                for (Vehicle otherVehicle : vehicles) {
                    if (otherVehicle == vehicle) continue; // Skip current vehicle

                    // Evaluate alternative trip approach time
                    Trip evaluatedApproach = otherVehicle.evaluateApproach(trip, graph);
                    double customerWaitTime = Duration.between(trip.bookingTime,
                                    evaluatedApproach.vaTime.plusSeconds((long) Math.ceil(evaluatedApproach.calculatedPath.travelTime)))
                            .toSeconds();

                    // If a better option is found, mark it
                    if (customerWaitTime < bestTravelTime && customerWaitTime < Duration.between(trip.bookingTime, trip.vaTime).toSeconds()) {
                        bestTravelTime = customerWaitTime;
                        bestVehicle = otherVehicle;
                        bestApproach = evaluatedApproach;
                    }
                }

                // If a better vehicle was found, reassign the trip
                if (bestVehicle != vehicle) {
                    vehicle.removeQueuedTrip(trip); // Remove from current queue
                    bestVehicle.queueTrip(bestApproach); // Assign to better vehicle
                    bestVehicle.queueTrip(trip); // Assign to better vehicle
                    trip.vaTime = bestApproach.vaTime.plusSeconds((long) Math.ceil(bestApproach.calculatedPath.travelTime));
                }
            }
        }
    }
}
