package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.utils.GeneticUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Population {
    private List<Chromosome> population;
//    private List<Chromosome> newPopulation = new ArrayList<>();
    private final int initialPopulationSize;
    private Gene[] customerGene;
    private Gene[] chargingGene;
    private final GeneticUtils geneticUtils;
    private final Config config;
    private final Random random = new Random();
    private Set<String> representationSet;
    private List<Chromosome> elite;

    public Population(int initialPopulationSize, List<Trip> trips, Config config) {
        this.initialPopulationSize = initialPopulationSize;
        this.geneticUtils = new GeneticUtils(config);
        deconstructInputTripListIntoGenesWithChargingTripRemoval(trips);
        this.config = config;
        this.representationSet = new HashSet<>();
        this.population = init(initialPopulationSize);
        this.elite = new ArrayList<>();


        // füge anfängliches chromosom in die population ein => könnte bereits eine gute Lösung sein
        Chromosome origin = new Chromosome(Arrays.asList(customerGene), Arrays.asList(chargingGene), config);
        if (representationSet.add(origin.getRepresentation())) {
            this.population.add(origin);
        }
    }

    public void updateRepresentationSet() {
        this.representationSet = new HashSet<>();
        for (Chromosome c: population) {
            this.representationSet.add(c.getRepresentation());
        }
    }

    private void deconstructInputTripListIntoGenesWithChargingTripRemoval(List<Trip> trips) {
        List<Gene> chargingGenes = new ArrayList<>();
        List<Gene> customerGenes = new ArrayList<>();

        List<Trip> customerTrips = trips.stream()
                .filter(t -> t.getTripType().equals("CustomerTrip"))
                .collect(Collectors.toList());

        for (Trip customerTrip: customerTrips) {
            customerGenes.add(geneticUtils.mapTripToGene(customerTrip));
        }

        for (int i = 0; i < customerGenes.size() + 1; i++) {
            chargingGenes.add(null);
        }

        if ((customerGenes.size() + 1) != chargingGenes.size()) {
            System.out.println("Caught Exception in Deconstruct " + trips.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
        }

        this.customerGene = new Gene[customerGenes.size()];
        this.chargingGene = new Gene[chargingGenes.size()];
        this.customerGene = customerGenes.toArray(new Gene[0]);
        this.chargingGene = chargingGenes.toArray(new Gene[0]);
    }

    private void deconstructInputTripListIntoGenes(List<Trip> trips) {
        List<Gene> chargingGenes = new ArrayList<>();
        List<Gene> customerGenes = new ArrayList<>();
        if (trips.size() == 1) {
            customerGenes.add(geneticUtils.mapTripToGene(trips.get(0)));
            chargingGenes.add(null);
            chargingGenes.add(null);
        } else {
            Trip prevTrip = trips.get(0);
            if (prevTrip.getTripType().equals("ChargingTrip")) {
                chargingGenes.add(geneticUtils.mapTripToGene(prevTrip));
            } else {
                chargingGenes.add(null);
                customerGenes.add(geneticUtils.mapTripToGene(prevTrip));
            }

            for (int i = 1; i < trips.size(); i++) {
                Trip currTrip = trips.get(i);
                if (currTrip.getTripType().equals("ChargingTrip")) {
                    chargingGenes.add(geneticUtils.mapTripToGene(currTrip));
                } else {
                    if (prevTrip.getTripType().equals("CustomerTrip")
                            && currTrip.getTripType().equals("CustomerTrip")) {
                        chargingGenes.add(null);
                    }
                    customerGenes.add(geneticUtils.mapTripToGene(currTrip));
                }
                prevTrip = currTrip;
            }

            if (trips.get(trips.size() - 1).getTripType().equals("CustomerTrip")) {
                chargingGenes.add(null);
            }
        }

        if ((customerGenes.size() + 1) != chargingGenes.size()) {
            System.out.println("Caught Exception in Deconstruct " + trips.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
        }

        this.customerGene = new Gene[customerGenes.size()];
        this.chargingGene = new Gene[chargingGenes.size()];
        this.customerGene = customerGenes.toArray(new Gene[0]);
        this.chargingGene = chargingGenes.toArray(new Gene[0]);
    }

    public Chromosome getBestChromosome() {
        // is sorted and therefore the best chromosome is at first place in the list
        return this.population.get(0);
    }

    private List<Chromosome> init(int initialPopulationSize) {
        try {
            Gene[] chargingGene = geneticUtils.generateChargingGenes();

            representationSet = new HashSet<>();
            List<Chromosome> initPopulation = new ArrayList<>();

            int triedTimes = 0;
            while (initPopulation.size() < initialPopulationSize && triedTimes < 2000) {
//                System.out.println("Init");
                Chromosome newChromosome = geneticUtils.create(customerGene, chargingGene);
                if (representationSet.add(newChromosome.getRepresentation())) {
                    initPopulation.add(newChromosome);
                }
                triedTimes++;
            }

            return initPopulation;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    public void update() {
        try {
//            elitism();
            noveltySearch();
            crossover();
            mutation();
            elitismAndStochasticSelection();
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
    }

    private void elitism() {
        int tenPercent = this.population.size() < 100 ? (int) (this.population.size() * (1/10.0)) : 10;
        this.elite.addAll(this.population.subList(0, tenPercent));
        this.population = this.population.subList(tenPercent, this.population.size());
    }

    private void selection() {
        try {
//            System.out.println("Selection");
            if (this.population.size() > this.initialPopulationSize) {
                // iterate over population calculate fitness and save result with index in new list
                List<List<Number>> fitnessAndIndices = new ArrayList<>();
                for (int i = 0; i < population.size(); i++) {
                    fitnessAndIndices.add(
                            Arrays.asList(population.get(i).fitness(), i)
                    );
                }

                // sort new list with fitness value
                List<List<Number>> sorted = fitnessAndIndices.stream().sorted(Comparator.comparingDouble(a -> (Double) a.get(0))).collect(Collectors.toList());
                Collections.reverse(sorted);

                List<Chromosome> newPopulation = new ArrayList<>();

                for (int j = 0; j < this.initialPopulationSize; j++) {
                    newPopulation.add(population.get((Integer) sorted.get(j).get(1)));
                }

                this.population = newPopulation;

//            this.population.addAll(this.elite);
//            this.population.sort(Comparator.comparingDouble(Chromosome::fitnessOld).reversed());
//            if (this.population.size() > 100) {
//                this.population = this.population.subList(0, 100);
//            }
//                System.out.println("Population size before selection: " + population.size());
//                this.population.sort(Comparator.comparingDouble(Chromosome::fitness).reversed());
//                // keep population size constant but keep the fittest individuals
//                this.population = this.population.stream().limit(this.initialPopulationSize).collect(Collectors.toList());
//                if (Boolean.parseBoolean(System.getenv("csv"))) {
//                    System.out.println("tripIds,chargingTimes,coords,distance,waitingTimesSum,odr,battery,distanceFraction,waitingTimesSumFraction,odrFraction,batteryFraction,fitness");
//                }
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
    }

    private void elitismAndStochasticSelection() {
        try {
            if (this.population.size() > this.initialPopulationSize) {

                // iterate over population calculate fitness and save result with index in new list
                // List of List with [fitness, indexInPopulation]
                List<List<Number>> fitnessAndIndices = new ArrayList<>();
                for (int i = 0; i < population.size(); i++) {
                    fitnessAndIndices.add(
                            Arrays.asList(population.get(i).fitness(), i)
                    );
                }

                // Elitism
                // sort new list with fitness value
                List<List<Number>> sorted = fitnessAndIndices.stream().sorted(Comparator.comparingDouble(a -> (Double) a.get(0))).collect(Collectors.toList());
                // fittest elements in the beginning
                Collections.reverse(sorted);

                List<Chromosome> newPopulation = new ArrayList<>();

                double percentageOfElitism = 0.1;
                int size = (int) (this.initialPopulationSize * percentageOfElitism);
                for (int j = 0; j < size; j++) {
                    newPopulation.add(population.get((Integer) sorted.get(j).get(1)));
                }

                // Stochastic selection
                // start at first element after elitism selection
//                for (int j = size; j < sorted.size(); j++) {
//                    if (stochasticSelect((Double) sorted.get(j).get(0))) {
//                        newPopulation.add(population.get((Integer) sorted.get(j).get(1)));
//                    }
//                }

                List<List<Number>> remainingSorted = sorted.subList(size, sorted.size());

                // tournament selection
                for (int j = size; j < initialPopulationSize; j++) {
                    // get 3 random elements
                    int remainingSortedSize = remainingSorted.size();

                    Collections.shuffle(remainingSorted);
                    List<Number> one = remainingSorted.get(0);
                    List<Number> two = remainingSorted.get(1);
                    List<Number> three = remainingSorted.get(2);

                    double fitOne = (double) one.get(0);
                    double fitTwo = (double) one.get(0);
                    double fitThree = (double) one.get(0);

                    Chromosome newElem;
                    if (fitOne > fitTwo && fitOne > fitThree) {
                        newElem = population.get((Integer) one.get(1));
                    } else if (fitTwo > fitOne && fitTwo > fitThree) {
                        newElem = population.get((Integer) two.get(1));
                    } else {
                        newElem = population.get((Integer) three.get(1));
                    }

                    newPopulation.add(newElem);
                    sorted.remove(newElem);
                }

                this.population = newPopulation;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean stochasticSelect(Double fitness) {
        // generates random value between 0.0 and 1.0
        Double randDouble = random.nextDouble();
        return randDouble < fitness;
    }

    private void noveltySearch() {
        try {
            int added = 0;
            int triedTimes = 0;
            int size = (int) (population.size() / 10.0);
            while (added != size && triedTimes < 2000) {
                Chromosome c = geneticUtils.create(customerGene, geneticUtils.generateChargingGenes());
                if (representationSet.add(c.getRepresentation())) {
                    this.population.add(c);
                    added++;
                }
                triedTimes++;
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
    }

    private void mutation() {
        try {
            int size = population.size();
            // 10% of population gets mutated
            int amountNewElems = (int) (size * (0.2));
            int added = 0;
            int triedTimes = 0;

            // 0 => mutate customer trips, 1 => mutate charging trips, 2 => mutate charging times
            int mutateType = -1;
            while (added < amountNewElems && triedTimes < 5000) {
                mutateType = (mutateType + 1) % 3;
                if (mutateType == 0 || mutateType == 1) {
                    if (population.get(0).getCustomerChromosome().size() > 1) {
                        // mutate either customer trip list or charging trip list
//                    List<Chromosome> mutated  = Arrays.asList(this.population.get(random.nextInt(population.size())).mutateAlternately(mutateCustomer));
                        Chromosome mutated  = this.population.get(random.nextInt(population.size())).mutateAlternately(mutateType);

                        if (representationSet.add(mutated.getRepresentation())) {
                            population.add(mutated);
                            added++;
                        }

                    }
                }
                if (mutateType == 2) {
                    Chromosome chargingChromosome = this.population.get(random.nextInt(population.size()));
                    // get new charging chromosome if there are no charging genes
                    while (chargingChromosome.mergeGenes().size() == chargingChromosome.getCustomerChromosome().size()) {
                        chargingChromosome = this.population.get(random.nextInt(population.size()));
                    }
                    Chromosome mutated2 = chargingChromosome.mutateChargingTimes();
                    if (representationSet.add(mutated2.getRepresentation())) {
                        population.add(mutated2);
                        added++;
                    }
                }
                triedTimes++;
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
    }

    private void crossoverWithPopulationReplacement() {
        try {
            List<Chromosome> cache = new ArrayList<>();
            // no candidates exists for a crossover
            if (population.size() == 1) {
                return;
            }
            for (Chromosome chromosome : this.population) {
                Chromosome partner = getCrossOverPartner(chromosome);
                cache.addAll(Arrays.asList(chromosome.uniformCrossover(partner)));
            }

            for (Chromosome c : cache) {
                representationSet = new HashSet<>();
                this.population = new ArrayList<>();
                if (c.fallsNotBelowThreshhold() & representationSet.add(c.getRepresentation())) {
                    this.population.add(c);
                }
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
    }

    private void crossover() {
        try {
//            System.out.println("Crossover");
            List<Chromosome> cache = new ArrayList<>();
            // no candidates exists for a crossover
            if (population.size() == 1) {
                return;
            }
            int size = (int) (population.size() * (5 /10.0));
            for (int i = 0; i < size; i++) {
                Chromosome ith = population.get(i);
                Chromosome partner = getCrossOverPartner(ith);
                cache.addAll(Arrays.asList(ith.crossover(partner)));
            }

            for (Chromosome c : cache) {
                if (representationSet.add(c.getRepresentation())) {
                    this.population.add(c);
                }
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
    }

    private Chromosome getCrossOverPartner(Chromosome chromosome) {
        try {
            Chromosome partner = this.population.get(random.nextInt(population.size()));
            while (chromosome.getRepresentation() == partner.getRepresentation()) {
                partner = this.population.get(random.nextInt(population.size()));
            }
            return partner;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }
}
