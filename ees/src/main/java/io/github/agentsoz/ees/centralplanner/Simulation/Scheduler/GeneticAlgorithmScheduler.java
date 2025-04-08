package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.time.Duration;
import java.util.*;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class GeneticAlgorithmScheduler extends AbstractScheduler {

    private static final int POPULATION_SIZE = 100;
    private static final int KEEP_BEST_INDIVIDUALS = 40;
    private static final int MAX_GENERATIONS = 200;
    private static final double MUTATION_RATE = 0.005;
    private static final long SEED = 815274;
    private static final Random random = new Random(SEED);
    PriorityQueue<Individual> bestIndividuals = new PriorityQueue<>(Comparator.comparingDouble(Individual::getFitness));

    public GeneticAlgorithmScheduler(HashMap<String, String> configMap) {
        super(configMap);
    }

    public void run() {
        if (progressionLogging){
            System.out.println("\nScheduling requests using Genetic Algorithm");
        }
        // randomly generate a population, meaning a number of individuals with random trip assignments
        ArrayList<Individual> population = initializePopulation();

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            Collections.sort(population);
            bestIndividuals.add(population.get(0));

            double fitness = population.get(0).getFitness();
            if (progressionLogging){
                showProgress(generation, MAX_GENERATIONS-1, "  Fitness " + fitness);
            }

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
                offspring.assignTrips(copyAllVehicles());
                newPopulation.add(offspring);
            }
            //apply random mutations
            for (int i = 0; i < POPULATION_SIZE; i++) {
                Individual individual = newPopulation.get(i);
                mutate(individual);
            }

            population = newPopulation;
        }

        Individual bestIndividual = bestIndividuals.poll();
        graph.pathfindingMethod = "fast_dijkstra";
        bestIndividual.assignTrips(vehicles);
    }

    private ArrayList<Individual> initializePopulation() {
        ArrayList<Individual> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Individual individual = new Individual();
            individual.assignTrips(copyAllVehicles());
            population.add(individual);
        }
        return population;
    }

    private Individual selectParent(ArrayList<Individual> population) {
        int tournamentSize = 20;
        ArrayList<Individual> tournament = new ArrayList<>();
        for (int i = 0; i < tournamentSize; i++) {
            tournament.add(population.get(random.nextInt(POPULATION_SIZE)));
        }
        Collections.sort(tournament);
        return tournament.get(0);
    }

    private Individual crossover(Individual parent1, Individual parent2) {
        Individual offspring = new Individual();
        for (int i = 0; i < requestedTrips.size(); i++) {
            if (random.nextBoolean()) {
                offspring.getGenes().set(i, parent1.getGenes().get(i));
            } else {
                offspring.getGenes().set(i, parent2.getGenes().get(i));
            }
        }
        offspring.assignTrips(copyAllVehicles());
        return offspring;
    }

    private void mutate(Individual individual) {
        boolean mutated = false;
        for (int i = 0; i < requestedTrips.size(); i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                mutated = true;
                individual.getGenes().set(i, random.nextInt(vehicles.size()));
            }
        }
        if (mutated) {
            individual.assignTrips(copyAllVehicles());
        }
    }

    private class Individual implements Comparable<Individual> {
        private final ArrayList<Integer> genes;
        private ArrayList<Vehicle> copiedVehicles;
        private double fitness;

        public Individual() {
            //on init randomizes trip vehicle distribution
            genes = new ArrayList<>(Collections.nCopies(requestedTrips.size(), 0));
            for (int i = 0; i < requestedTrips.size(); i++) {
                genes.set(i, random.nextInt(vehicles.size()));
            }
        }

        public void assignTrips(ArrayList<Vehicle> vehicles) {
            copiedVehicles = vehicles;
            for (int i = 0; i < requestedTrips.size(); i++) {;
                Trip customerTrip = new Trip(requestedTrips.get(i));
                Vehicle assignedVehicle = vehicles.get(genes.get(i));
                assignedVehicle.refreshVehicle(customerTrip.bookingTime);
                Trip approachTrip = assignedVehicle.evaluateApproach(customerTrip, graph);
                assignedVehicle.queueTrip(approachTrip);
                assignedVehicle.queueTrip(customerTrip);
            }
            //update rest of queued trips from vehicles, even after last booking came in
            for (Vehicle vehicle : copiedVehicles) {
                if (!vehicle.queuedTrips.isEmpty()) {
                    vehicle.refreshVehicle(vehicle.busyUntil);
                }
            }
            fitness = getFitness();
        }

        public ArrayList<Integer> getGenes() {
            return genes;
        }

        public double getFitness() {
            double fitness = 0.0;
            for (Vehicle vehicle : copiedVehicles){
                //fitness value can be customer waiting time or missed Trips

//                fitness += vehicle.customerWaitingTime;
                fitness += vehicle.getMissedTrips();
            }
            return fitness;
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(this.fitness, other.fitness);
        }
    }
}
