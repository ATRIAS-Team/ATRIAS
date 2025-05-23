package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ACOScheduler extends AbstractScheduler {
    private final int numAnts;           // Number of ants (solutions to explore)
    private final double alpha;          // Pheromone influence
    private final double beta;           // Heuristic influence
    private final double evaporationRate; // Pheromone evaporation rate
    private final int maxIterations;     // Maximum iterations
    private Map<String, Map<Integer, Double>> pheromones; // Pheromone levels: tripID -> vehicleID -> value
    private static final long SEED = 815274;
    private static final Random random = new Random(SEED);


    public ACOScheduler(HashMap<String, String> configMap) {
        super(configMap);
        this.numAnts = 100;
        this.alpha = 1.0;
        this.beta = 2.0;
        this.evaporationRate = 0.1;
        this.maxIterations = 100;
        this.pheromones = new HashMap<>();
    }

    public void generateAssignment(ArrayList<Trip> requestedTrips) {
        initializePheromones(requestedTrips);

        ArrayList<Vehicle> bestSolution = null; // TripID:Vehicle
        double bestTotalWaitTime = Double.MAX_VALUE;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            ArrayList<ArrayList<Vehicle>> antSolutions = new ArrayList<>(); // One solution per ant

            // Each ant builds a solution
            for (int ant = 0; ant < numAnts; ant++) {
                ArrayList<Vehicle> solution = constructSolution(requestedTrips);
                double totalWaitTime = evaluateSolution(solution);
                antSolutions.add(solution);

                if (totalWaitTime < bestTotalWaitTime) {
                    bestTotalWaitTime = totalWaitTime;
                    bestSolution = solution;
                }
            }

            // Update pheromones based on ant solutions
            updatePheromones(antSolutions);
        }

        // Apply the best solution found
        vehicles = bestSolution;
    }

    private void initializePheromones(ArrayList<Trip> requestedTrips) {
        pheromones.clear();
        for (Trip trip : requestedTrips) {
            Map<Integer, Double> vehiclePheromones = new HashMap<>();
            for (Vehicle vehicle : vehicles) {
                vehiclePheromones.put(vehicle.id, 1.0); // Initial pheromone level
            }
            pheromones.put(trip.TripID, vehiclePheromones);
        }
    }

    private ArrayList<Vehicle> constructSolution(ArrayList<Trip> requestedTrips) {
        ArrayList<Vehicle> copiedVehicles = copyAllVehicles();

        for (Trip trip : requestedTrips) {
            //copy trip, otherwise call by reference errors
            Trip copiedTrip = new Trip(trip);
            //select vehicle based on pheromones/heuristic
            Vehicle selectedVehicle = selectVehicle(copiedTrip, copiedVehicles);

            Trip approach = selectedVehicle.evaluateApproach(copiedTrip, graph);
            selectedVehicle.refreshVehicle(copiedTrip.bookingTime);
            selectedVehicle.queueTrip(approach);
            selectedVehicle.queueTrip(copiedTrip);
        }
        return copiedVehicles;
    }

    private Vehicle selectVehicle(Trip customerTrip, ArrayList<Vehicle> copiedVehicles) {
        double[] probabilities = new double[copiedVehicles.size()];
        double total = 0.0;

        for (int i = 0; i < copiedVehicles.size(); i++) {
            Vehicle vehicle = copiedVehicles.get(i);

            Trip approach = vehicle.evaluateApproach(customerTrip, graph);

            double waitTime = Duration.between(customerTrip.bookingTime,
                    approach.vaTime.plusSeconds((long) Math.ceil(approach.calculatedPath.travelTime))).toSeconds();
            double heuristic = 1.0 / (waitTime + 1.0); // Avoid division by zero
            double pheromone = pheromones.get(customerTrip.TripID).get(vehicle.id);
            probabilities[i] = Math.pow(pheromone, alpha) * Math.pow(heuristic, beta);
            total += probabilities[i];
        }

        // Roulette wheel selection
        double rand = random.nextDouble() * total;
        double cumulative = 0.0;
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (rand <= cumulative) {
                return copiedVehicles.get(i);
            }
        }
        return copiedVehicles.get(copiedVehicles.size() - 1); // Fallback
    }

    private double evaluateSolution(ArrayList<Vehicle> solution) {
        double totalWaitTime = 0.0;

        for (Vehicle vehicle : solution) {
            totalWaitTime += vehicle.customerWaitingTime;
        }
        return totalWaitTime;
    }

    private void updatePheromones(ArrayList<ArrayList<Vehicle>> solutions) {
        // Evaporate pheromones
        for (Map<Integer, Double> vehiclePheromones : pheromones.values()) {
            for (Integer vehicleId : vehiclePheromones.keySet()) {
                vehiclePheromones.put(vehicleId, vehiclePheromones.get(vehicleId) * (1.0 - evaporationRate));
            }
        }

        // get waiting times (quality) for the solutions
        ArrayList<Double> waitTimes = new ArrayList<>();
        for (ArrayList<Vehicle> solution : solutions) {
            waitTimes.add(evaluateSolution(solution));
        }

        // Deposit pheromones based on solution quality
        for (int i = 0; i < solutions.size(); i++) {
            double deposit = 1.0 / (waitTimes.get(i) + 1.0); // Higher deposit for lower wait times
            ArrayList<Vehicle> solution = solutions.get(i);

            for (Vehicle vehicle : solution) {
                for (Trip trip: vehicle.queuedTrips){
                    //calculate new pheromone level, with ceiling at 1
                    if (pheromones.containsKey(trip.TripID)){
                        double newPheromone = Math.min(1, pheromones.get(trip.TripID).get(vehicle.id) + deposit);
                        pheromones.get(trip.TripID).put(vehicle.id, newPheromone);
                    }
                }
            }
        }
    }
}


