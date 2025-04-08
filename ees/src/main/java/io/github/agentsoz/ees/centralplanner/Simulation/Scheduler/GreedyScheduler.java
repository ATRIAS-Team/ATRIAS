package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Graph.*;
import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.*;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class GreedyScheduler extends AbstractScheduler {
    public GreedyScheduler(HashMap<String, String> configMap) {
        super(configMap);
    }

    public void run(){
        if (progressionLogging){
            System.out.println("\nScheduling requests using Greedy");
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
            //evaluate if charging is necessary
//            bestVehicle.handleCharging(graph);
        }
    }
}
