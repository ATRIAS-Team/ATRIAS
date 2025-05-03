package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.*;

public class GreedyScheduler extends AbstractScheduler {
    public GreedyScheduler(HashMap<String, String> configMap) {
        super(configMap);
    }

    public void generateAssignment(ArrayList<Trip> requestedTrips){
        // allocate each trip on a greedy basis
        for (Trip customerTrip : requestedTrips){

            double bestTravelTime = Double.MAX_VALUE;
            Vehicle bestVehicle = null;
            Trip bestApproach = null;

            //iterate over each vehicle and calculate the approach time for the current position of the vehicle
            for (Vehicle vehicle : vehicles) {

                //calculate trip vatime for vehicles with given queue
                Trip evaluatedApproach = vehicle.evaluateApproach(customerTrip, graph);

                double customerWaitTime = Duration.between(customerTrip.bookingTime,
                                evaluatedApproach.vaTime.plusSeconds((long) Math.ceil(evaluatedApproach.calculatedPath.travelTime)))
                                .toSeconds();

                if (customerWaitTime < bestTravelTime) {
                    bestTravelTime = customerWaitTime;
                    bestVehicle = vehicle;
                    bestApproach = evaluatedApproach;
                }
            }
            assert bestVehicle != null;
            bestVehicle.queueTrip(bestApproach);
            bestVehicle.queueTrip(customerTrip);
        }
    }
}
