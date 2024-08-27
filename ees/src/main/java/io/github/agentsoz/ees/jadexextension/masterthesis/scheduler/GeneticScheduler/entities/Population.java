package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.utils.GeneticUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Population {
    private List<Chromosome> population;
    private final int initialPopulationSize;
    private Gene[] customerGene;
    private Gene[] chargingGene;
    private final GeneticUtils geneticUtils;
    private final Config config;
    private final Random random = new Random();
    private Set<String> representationSet;

    public Population(int initialPopulationSize, List<Trip> trips, Config config) {
        this.initialPopulationSize = initialPopulationSize;
        this.geneticUtils = new GeneticUtils(config);
        deconstructInputTripListIntoGenes(trips);
        this.config = config;
        this.representationSet = new HashSet<>();
        this.population = init(initialPopulationSize);


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
            if (config.getBatteryLevel() > 0.90 && customerGene.length == 1) {
                // popsize kann nicht erreicht werden bzw. nur schwer
                while (initialPopulationSize < 20) {
                    Chromosome newChromosome = geneticUtils.create(customerGene, chargingGene);
                    if (representationSet.add(newChromosome.getRepresentation())) {
                        initPopulation.add(newChromosome);
                    }
                }
            } else {
                while (initPopulation.size() < initialPopulationSize) {
                    Chromosome newChromosome = geneticUtils.create(customerGene, chargingGene);
                    if (representationSet.add(newChromosome.getRepresentation())) {
                        initPopulation.add(newChromosome);
                    }
                }
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
//        System.out.println("CrossOver");
            crossover();
//        System.out.println("Mutation");
            mutation();
//        System.out.println("Spawn");
            noveltySearch();
//        System.out.println("Selection");
            selection();
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
    }

    private void tournamentSelection() {

    }

    private void selection() {
        try {
//                System.out.println("Population size before selection: " + population.size());
//                this.population.sort(Comparator.comparingDouble(Chromosome::fitness).reversed());
//                // keep population size constant but keep the fittest individuals
//                this.population = this.population.stream().limit(this.initialPopulationSize).collect(Collectors.toList());
//                if (Boolean.parseBoolean(System.getenv("csv"))) {
//                    System.out.println("tripIds,chargingTimes,coords,distance,waitingTimesSum,odr,battery,distanceFraction,waitingTimesSumFraction,odrFraction,batteryFraction,fitness");
//                }
            this.population = selectTopNChromosomes();
//                updateRepresentationSet();
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
    }

    public List<Chromosome> selectTopNChromosomes() {
        PriorityQueue<Chromosome> minHeap = new PriorityQueue<>(this.initialPopulationSize, Comparator.comparingDouble(Chromosome::fitnessOld));

        for (Chromosome chromosome : this.population) {
            if (minHeap.size() < this.initialPopulationSize) {
                minHeap.add(chromosome);
            } else if (chromosome.fitnessOld() > minHeap.peek().fitnessOld()) {
                minHeap.poll();
                minHeap.add(chromosome);
            }
        }


        List<Chromosome> topChromosomes = new ArrayList<>(minHeap);
        topChromosomes.sort(Comparator.comparingDouble(Chromosome::fitnessOld).reversed());
        List<Chromosome> withoutDuplicats = new ArrayList<>();
        representationSet = new HashSet<>();
        for (Chromosome chromosome: topChromosomes) {
            if (representationSet.add(chromosome.getRepresentation())) {
                withoutDuplicats.add(chromosome);
            }
        }
        return withoutDuplicats;
    }

    private void noveltySearch() {
        try {
            int added = 0;
            while (added != 20) {
                Chromosome c = geneticUtils.create(customerGene, geneticUtils.generateChargingGenes());
                if (representationSet.add(c.getRepresentation())) {
                    this.population.add(c);
                    added++;
                }
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
    }

    private void mutation() {
        try {
            int size = population.size();
            int amountNewElems = size / 5;
            int added = 0;
            while (added != amountNewElems) {
                if (population.get(0).getCustomerChromosome().size() > 1) {
                    Chromosome mutated  = this.population.get(random.nextInt(population.size())).mutate();
                    if (representationSet.add(mutated.getRepresentation())) {
                        population.add(mutated);
                        added++;
                    }
                }
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
                cache.addAll(Arrays.asList(chromosome.crossover(partner)));
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
            List<Chromosome> cache = new ArrayList<>();
            // no candidates exists for a crossover
            if (population.size() == 1) {
                return;
            }
            for (int i = 0; i < population.size() / 2; i++) {
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
