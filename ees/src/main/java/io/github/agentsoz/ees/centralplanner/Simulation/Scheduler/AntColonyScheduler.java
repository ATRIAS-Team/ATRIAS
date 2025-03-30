package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Graph.Graph;
import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.*;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class AntColonyScheduler extends AbstractScheduler {
    private final int numAnts;           // Number of ants (solutions to explore)
    private final double alpha;          // Pheromone influence
    private final double beta;           // Heuristic influence
    private final double evaporationRate; // Pheromone evaporation rate
    private final int maxIterations;     // Maximum iterations
    private Map<String, Map<Integer, Double>> pheromones; // Pheromone levels: tripID -> vehicleID -> value

    public AntColonyScheduler(ArrayList<Vehicle> vehicles, String requestsFilePath, String outputFilePath, Graph graph) {
        super(vehicles, requestsFilePath, outputFilePath, graph, "dijkstra");
        this.numAnts = 10;         // Tunable
        this.alpha = 1.0;          // Pheromone weight
        this.beta = 2.0;           // Heuristic weight
        this.evaporationRate = 0.1; // Pheromone decay
        this.maxIterations = 10;   // Iterations limit
        this.pheromones = new HashMap<>();
        initializePheromones();
    }

    private void initializePheromones() {
        for (Trip trip : requestedTrips) {
            Map<Integer, Double> vehiclePheromones = new HashMap<>();
            for (Vehicle vehicle : vehicles) {
                vehiclePheromones.put(vehicle.id, 1.0); // Initial pheromone level
            }
            pheromones.put(trip.TripID, vehiclePheromones);
        }
    }

    public void run() {
        System.out.println("\nScheduling requests using Ant Colony Optimization");

        Map<String, Vehicle> bestSolution = null; // TripID -> Vehicle
        double bestTotalWaitTime = Double.MAX_VALUE;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            List<Map<String, Vehicle>> antSolutions = new ArrayList<>(); // One solution per ant
            List<Double> antWaitTimes = new ArrayList<>();

            // Each ant builds a solution
            for (int ant = 0; ant < numAnts; ant++) {
                Map<String, Vehicle> solution = constructSolution();
                double totalWaitTime = evaluateSolution(solution);
                antSolutions.add(solution);
                antWaitTimes.add(totalWaitTime);

                if (totalWaitTime < bestTotalWaitTime) {
                    bestTotalWaitTime = totalWaitTime;
                    bestSolution = new HashMap<>(solution);
                }
            }

            // Update pheromones based on ant solutions
            updatePheromones(antSolutions, antWaitTimes);
            showProgress(iteration, maxIterations - 1);
        }

        // Apply the best solution found
        applySolution(bestSolution);
    }

    private Map<String, Vehicle> constructSolution() {
        Map<String, Vehicle> solution = new HashMap<>();
        List<Vehicle> tempVehicles = deepCopyVehicles(vehicles); // Avoid modifying original state

        for (Trip trip : requestedTrips) {
            Vehicle selectedVehicle = selectVehicle(trip, tempVehicles);
            Trip approach = selectedVehicle.evaluateApproach(trip, graph);
            selectedVehicle.queueTrip(approach);
            selectedVehicle.queueTrip(trip);
            solution.put(trip.TripID, selectedVehicle);
        }
        return solution;
    }

    private Vehicle selectVehicle(Trip trip, List<Vehicle> vehicles) {
        double[] probabilities = new double[vehicles.size()];
        double total = 0.0;

        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            vehicle.refreshVehicle(trip.bookingTime);
            Trip approach = vehicle.evaluateApproach(trip, graph);
            double waitTime = Duration.between(trip.bookingTime,
                    approach.vaTime.plusSeconds((long) Math.ceil(approach.calculatedPath.travelTime))).toSeconds();
            double heuristic = 1.0 / (waitTime + 1.0); // Avoid division by zero
            double pheromone = pheromones.get(trip.TripID).get(vehicle.id);
            probabilities[i] = Math.pow(pheromone, alpha) * Math.pow(heuristic, beta);
            total += probabilities[i];
        }

        // Roulette wheel selection
        double rand = Math.random() * total;
        double cumulative = 0.0;
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (rand <= cumulative) {
                return vehicles.get(i);
            }
        }
        return vehicles.get(vehicles.size() - 1); // Fallback
    }

    private double evaluateSolution(Map<String, Vehicle> solution) {
        List<Vehicle> tempVehicles = deepCopyVehicles(vehicles);
        double totalWaitTime = 0.0;

        for (Trip trip : requestedTrips) {
            Vehicle vehicle = solution.get(trip.TripID);
            Vehicle tempVehicle = tempVehicles.get(vehicle.id);
            tempVehicle.refreshVehicle(trip.bookingTime);
            Trip approach = tempVehicle.evaluateApproach(trip, graph);
            tempVehicle.queueTrip(approach);
            tempVehicle.queueTrip(trip);
            double waitTime = Duration.between(trip.bookingTime,
                    approach.vaTime.plusSeconds((long) Math.ceil(approach.calculatedPath.travelTime))).toSeconds();
            totalWaitTime += waitTime;
        }
        return totalWaitTime;
    }

    private void updatePheromones(List<Map<String, Vehicle>> solutions, List<Double> waitTimes) {
        // Evaporate pheromones
        for (Map<Integer, Double> vehiclePheromones : pheromones.values()) {
            for (Integer vehicleId : vehiclePheromones.keySet()) {
                vehiclePheromones.put(vehicleId, vehiclePheromones.get(vehicleId) * (1.0 - evaporationRate));
            }
        }

        // Deposit pheromones based on solution quality
        for (int i = 0; i < solutions.size(); i++) {
            double deposit = 1.0 / (waitTimes.get(i) + 1.0); // Higher deposit for lower wait times
            Map<String, Vehicle> solution = solutions.get(i);
            for (Map.Entry<String, Vehicle> entry : solution.entrySet()) {
                String tripId = entry.getKey();
                Integer vehicleId = entry.getValue().id;
                double newPheromone = pheromones.get(tripId).get(vehicleId) + deposit;
                pheromones.get(tripId).put(vehicleId, newPheromone);
            }
        }
    }

    private void applySolution(Map<String, Vehicle> solution) {
        for (Trip trip : requestedTrips) {
            Vehicle vehicle = vehicles.get(solution.get(trip.TripID).id);
            Trip approach = vehicle.evaluateApproach(trip, graph);
            vehicle.refreshVehicle(trip.bookingTime);
            vehicle.queueTrip(approach);
            vehicle.queueTrip(trip);
            bestVehicleMap.put(trip.TripID, vehicle.id);
        }

        // Final refresh for all vehicles
        for (Vehicle vehicle : vehicles) {
            if (!vehicle.queuedTrips.isEmpty()) {
                vehicle.refreshVehicle(vehicle.busyUntil);
            }
            for (Trip customerTrip : vehicle.takenTrips) {
                bestVehicleMap.put(customerTrip.TripID, vehicle.id);
            }
        }
    }

    private List<Vehicle> deepCopyVehicles(List<Vehicle> original) {
        List<Vehicle> copy = new ArrayList<>();
        for (Vehicle v : original) {
            copy.add(new Vehicle(v));
//            copy.add(v.cloneVehicle());
        }
        return copy;
    }
}