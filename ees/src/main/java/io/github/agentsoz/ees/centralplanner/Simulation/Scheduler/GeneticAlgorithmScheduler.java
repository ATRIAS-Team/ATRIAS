package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Graph.Graph;
import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.*;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class GeneticAlgorithmScheduler extends AbstractScheduler {

    private static final int POPULATION_SIZE = 30;
    private static final int KEEP_BEST_INDIVIDUALS = 10;
    private static final int MAX_GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.005;
    private static final long SEED = 815274;
    private static final Random random = new Random(SEED);

    public GeneticAlgorithmScheduler(ArrayList<Vehicle> vehicles, String requestsFilePath, String outputFilePath, Graph graph) {
        super(vehicles, requestsFilePath, outputFilePath, graph, "dijkstra");
    }

    public void run() {
        System.out.println("\nScheduling requests using Genetic Algorithm");

        ArrayList<Individual> population = initializePopulation();
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            Collections.sort(population);

//            double fitness = population.stream().mapToDouble(Individual::getFitness).sum()/POPULATION_SIZE;
            double fitness = population.get(0).getFitness();
            showProgress(generation, MAX_GENERATIONS-1, "  Fitness " + fitness);

            ArrayList<Individual> newPopulation = new ArrayList<>();

            //keep best individuals
            for (int i = 0; i < KEEP_BEST_INDIVIDUALS; i++) {
                newPopulation.add(population.get(i));
            }
            //fill population using reproduction
            for (int i = 0; i < POPULATION_SIZE-KEEP_BEST_INDIVIDUALS; i++) {
                Individual parent1 = selectParent(population);
                Individual parent2 = selectParent(population);

                Individual offspring = crossover(parent1, parent2);

                newPopulation.add(offspring);
            }
            //apply random mutations
            for (int i = 0; i < POPULATION_SIZE; i++) {
                Individual individual = newPopulation.get(i);
                mutate(individual);
                individual.assignTrips();
            }

            population = newPopulation;
        }

        Individual bestIndividual = population.get(0);

        this.vehicles = bestIndividual.vehicles;

        //update rest of queued trips from vehicles, even after last booking came in
        for (Vehicle vehicle : vehicles){
            for (Trip customerTrip : vehicle.takenTrips){
                bestVehicleMap.put(customerTrip.TripID, vehicle.id);
            }
        }
    }

    private ArrayList<Individual> initializePopulation() {
        ArrayList<Individual> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Individual individual = new Individual(vehicles, requestedTrips, graph);
            individual.randomize();
            individual.assignTrips();
            population.add(individual);
        }
        return population;
    }

    private Individual selectParent(ArrayList<Individual> population) {
        int tournamentSize = 5;
        ArrayList<Individual> tournament = new ArrayList<>();
        for (int i = 0; i < tournamentSize; i++) {
            tournament.add(population.get(random.nextInt(POPULATION_SIZE)));
        }
        Collections.sort(tournament);
        return tournament.get(0);
    }

    private Individual crossover(Individual parent1, Individual parent2) {
        Individual offspring = new Individual(vehicles, requestedTrips, graph);
        for (int i = 0; i < requestedTrips.size(); i++) {
            if (random.nextBoolean()) {
                offspring.getGenes().set(i, parent1.getGenes().get(i));
            } else {
                offspring.getGenes().set(i, parent2.getGenes().get(i));
            }
        }
        offspring.assignTrips();
        return offspring;
    }

    private void mutate(Individual individual) {
        for (int i = 0; i < requestedTrips.size(); i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                individual.getGenes().set(i, random.nextInt(vehicles.size()));
            }
        }
    }

    private class Individual implements Comparable<Individual> {
        private ArrayList<Integer> genes;
        private ArrayList<Vehicle> vehicles;
        private ArrayList<Trip> requestedTrips;
        private Graph graph;

        public Individual(ArrayList<Vehicle> vehicles, ArrayList<Trip> requestedTrips, Graph graph) {
            this.vehicles = vehicles;
            this.requestedTrips = requestedTrips;
            this.graph = graph;
            this.genes = new ArrayList<>(Collections.nCopies(requestedTrips.size(), 0));
            resetVehicles();
        }

        public void randomize() {
            for (int i = 0; i < requestedTrips.size(); i++) {
                genes.set(i, random.nextInt(vehicles.size()));
            }
        }

        public void assignTrips() {
            resetVehicles();
            for (int i = 0; i < requestedTrips.size(); i++) {
                Trip customerTrip = requestedTrips.get(i);
                Vehicle assignedVehicle = vehicles.get(genes.get(i));
                Trip approachTrip = assignedVehicle.evaluateApproach(customerTrip, graph);
                assignedVehicle.refreshVehicle(customerTrip.bookingTime);
                assignedVehicle.queueTrip(approachTrip);
                customerTrip.vaTime = approachTrip.vaTime.plusSeconds((long) Math.ceil(approachTrip.calculatedPath.travelTime));
                assignedVehicle.queueTrip(customerTrip);
            }
            for (Vehicle vehicle : vehicles) {
                if (!vehicle.queuedTrips.isEmpty()) {
                    Trip lastTrip = vehicle.queuedTrips.get(vehicle.queuedTrips.size() - 1);
                    vehicle.refreshVehicle(lastTrip.vaTime.plusSeconds((long) Math.ceil(lastTrip.calculatedPath.travelTime)));
                }
            }
        }

        private void resetVehicles(){
            for (Vehicle vehicle : vehicles) {
                vehicle.resetVehicle();
            }
        }

        public ArrayList<Integer> getGenes() {
            return genes;
        }

        public double getFitness() {
            double fitness = 0.0;
            for (Vehicle vehicle : vehicles){
                //iterate over takenTrips of the vehicle
                for (Trip trip : vehicle.takenTrips){
                    //and retrieve the waiting time/approach time for all the approach trips
                    if (!Objects.equals(trip.customerID, vehicle.name)){
                        int customerWaitTime = (int) Duration.between(trip.bookingTime, trip.vaTime).toSeconds();
                        fitness += customerWaitTime;
                    }
                }
            }
            return fitness;
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(this.getFitness(), other.getFitness());
        }
    }
}
