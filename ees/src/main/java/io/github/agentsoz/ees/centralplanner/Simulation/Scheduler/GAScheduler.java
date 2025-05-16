package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.util.*;

public class GAScheduler extends AbstractScheduler {
    private static final int POPULATION_SIZE = 100;
    private static final int KEEP_BEST_INDIVIDUALS = 10;
    private static final int MAX_GENERATIONS = 500;
    private static final double MUTATION_RATE = 0.05;
    private static final long SEED = 815274;
    private static final Random random = new Random(SEED);

    private ArrayList<Trip> requestedTrips = new ArrayList<>();

    public GAScheduler(HashMap<String, String> configMap) {
        super(configMap);
    }

    public void generateAssignment(ArrayList<Trip> requestedTrips) {
        //make it easier to access trips for methods by not passing it through every call
        this.requestedTrips = requestedTrips;

        // randomly generate a population, meaning a number of individuals with random trip assignments
        ArrayList<Individual> population = initializePopulation();

        //start the evolution for given generations
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            //sorts all individuals inside the population
            Collections.sort(population);

            ArrayList<Individual> newPopulation = new ArrayList<>();

            //keep best individuals
            for (int i = 0; i < KEEP_BEST_INDIVIDUALS; i++) {
                newPopulation.add(population.get(i));
            }
            //fill population using reproduction
            for (int i = 0; i < POPULATION_SIZE-KEEP_BEST_INDIVIDUALS; i++) {
                Individual parent1 = selectParent(population);
                Individual parent2 = selectParent(population);

                //generate offspring through crossover with parent1 and parent2
                Individual offspring = crossover(parent1, parent2);

                //apply random mutations to offspring
                mutate(offspring);

                offspring.assignTrips();
                newPopulation.add(offspring);
            }
            population = newPopulation;
        }

        //assign solution
        vehicles = population.get(0).copiedVehicles;
    }

    private ArrayList<Individual> initializePopulation() {
        ArrayList<Individual> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Individual individual = new Individual();
            individual.assignTrips();
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
        return offspring;
    }

    private void mutate(Individual individual) {
        //go through each gene
        for (int i = 0; i < individual.genes.size(); i++) {
            //randomly mutate the gene by assigning a new vehicle id to it
            if (random.nextDouble() < MUTATION_RATE) {
                individual.getGenes().set(i, random.nextInt(vehicles.size()));
            }
        }
    }

    private class Individual implements Comparable<Individual> {
        private final ArrayList<Integer> genes;
        private ArrayList<Vehicle> copiedVehicles;
        private double fitness;

        public Individual() {
            //on init randomizes trip vehicle distribution

            //generate genes consisting of a list of integers indicating which vehicle id is responsible for which trip
            genes = new ArrayList<>(Collections.nCopies(requestedTrips.size(), 0));
            for (int i = 0; i < requestedTrips.size(); i++) {
                //go through each gene, changing the gene so that a random vehicle is chosen
                genes.set(i, random.nextInt(vehicles.size()));
            }

            copiedVehicles = copyAllVehicles();
        }

        public void assignTrips() {
            for (int i = 0; i < requestedTrips.size(); i++) {;
                //copy trip to be not affected by calls by reference
                Trip customerTrip = new Trip(requestedTrips.get(i));
                //retrieve assigned vehicle from gene pool/chromosome
                Vehicle assignedVehicle = copiedVehicles.get(genes.get(i));

                Trip approachTrip = assignedVehicle.evaluateApproach(customerTrip, graph);
                assignedVehicle.queueTrip(approachTrip);
                assignedVehicle.queueTrip(customerTrip);
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

                fitness += vehicle.customerWaitingTime;
//                fitness += vehicle.getMissedTrips();
            }
            return fitness;
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(this.fitness, other.fitness);
        }
    }
}
