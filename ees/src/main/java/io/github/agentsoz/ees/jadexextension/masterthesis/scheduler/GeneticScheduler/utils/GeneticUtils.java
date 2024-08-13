package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.utils;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Chromosome;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Config;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Gene;
import io.github.agentsoz.util.Location;
import org.apache.batik.anim.timing.Trace;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GeneticUtils {

    private final Config config;
    private final Random random = new Random();

    public GeneticUtils(Config config) {
        this.config = config;
    }

    public List<List<Gene>> split(List<Gene> chromosome) {
        List<List<Gene>> result = new ArrayList<>();
        result.add(chromosome.subList(0, chromosome.size() / 2));
        result.add(chromosome.subList(chromosome.size() / 2, chromosome.size()));
        return result;
    }

    public Chromosome create(final Gene[] customerGeneInput, Gene[] chargingGeneInput) {
        // create random solutions for initial population
        // create chromosome for customerTrips
        final List<Gene> customerGene = Arrays.asList(Arrays.copyOf(customerGeneInput, customerGeneInput.length));
        Collections.shuffle(customerGene);

        // create chromosome for chargingTrips
        // get random number of chargingStations between 0 and sie of customerTrip + 1
        List<Gene> chargingGenes = getRandomAmountOfChargingStations(
                chargingGeneInput,
                customerGeneInput.length + 1
        );

        return new Chromosome(customerGene, chargingGenes, config);
    }

    public Gene mapTripToGene(Trip trip) {
        try {
            return new Gene(
                    trip.tripID,
                    trip.startPosition,
                    trip.endPosition,
                    trip.bookingTime,
                    trip.getChargingTime(),
                    config
            );
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
        // ToDo: Extend with booking time and more?
    }

    public List<Gene> getRandomAmountOfChargingStations(Gene[] chargingGene, int resultSize) {
        try {
            // create list consisting of null values
            List<Gene> result = new ArrayList<>();
            for (int i = 0; i < resultSize; i++) {
                result.add(null);
            }

            // set at random position random charging stations
            int count = random.nextInt(resultSize);
            for (int i = 0; i < count; i++) {
                int randomIndex = random.nextInt(chargingGene.length);
                int indexToSet = random.nextInt(resultSize);
                while (result.get(indexToSet) != null) {
                    indexToSet = random.nextInt(resultSize);
                }
                result.set(indexToSet, chargingGene[randomIndex]);
            }
            return result;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    public List<List<Gene>> makeCrossoverCustomer(List<Gene> first, List<Gene> second) {
        // case there is only one customer trip
        try {

            if (first.size() == 1) {
                return Arrays.asList(first, first);
            }

            final List<List<Gene>> thisDNA = split(first);
            final List<List<Gene>> otherDNA = split(second);
//        System.out.println("CrossOver - thisDNA.size = " + thisDNA.size() + ", otherDNA.size = " + otherDNA.size());

            final List<Gene> firstCrossOver = new ArrayList<>(thisDNA.get(0));

            for (int i = 0; i < otherDNA.get(0).size(); i++) {
//            System.out.println("First For Loop");
                if (!firstCrossOver.contains(otherDNA.get(0).get(i))) {
                    firstCrossOver.add(otherDNA.get(0).get(i));
                }
            }

            for (int i = 0; i < otherDNA.get(1).size(); i++) {
//            System.out.println("Second For Loop");
                if (!firstCrossOver.contains(otherDNA.get(1).get(i))) {
                    firstCrossOver.add(otherDNA.get(1).get(i));
                }
            }

            final List<Gene> secondCrossOver = new ArrayList<>(otherDNA.get(1));

            for (int i = 0; i < thisDNA.get(0).size(); i++) {
//            System.out.println("Third For Loop");
                if (!secondCrossOver.contains(thisDNA.get(0).get(i))) {
                    secondCrossOver.add(thisDNA.get(0).get(i));
                }
            }

            for (int i = 0; i < thisDNA.get(1).size(); i++) {
//            System.out.println("Fourth For Loop");
                if (!secondCrossOver.contains(thisDNA.get(1).get(i))) {
                    secondCrossOver.add(thisDNA.get(1).get(i));
                }
            }

            if (firstCrossOver.size() > first.size()) {
                System.out.println("First while");
                removeElements(firstCrossOver, first.size());
            }

            if (secondCrossOver.size() > first.size()) {
                System.out.println("Second while");
                removeElements(secondCrossOver, first.size());
            }

            return Arrays.asList(firstCrossOver, secondCrossOver);
        } catch (Exception e) {
            System.out.println("Caught exception: " + e);
            e.printStackTrace();
        }
        return null;
    }

    public List<List<Gene>> makeCrossoverCharging(List<Gene> first, List<Gene> second) {
        try {

            final List<List<Gene>> thisDNA = split(first);
            final List<List<Gene>> otherDNA = split(second);

            final List<Gene> firstCrossOver = new ArrayList<>(thisDNA.get(0));
            firstCrossOver.addAll(otherDNA.get(1));

            final List<Gene> secondCrossOver = new ArrayList<>(otherDNA.get(0));
            secondCrossOver.addAll(thisDNA.get(1));

            return Arrays.asList(firstCrossOver, secondCrossOver);
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
        return null;
    }

    public Gene[] generateChargingGenes() {
        try {
            List<Location> chargingStations = config.getChargingStations();
            Double minChargingTime = config.getMIN_CHARGING_TIME();
            Gene[] chargingGene = new Gene[chargingStations.size()];
            for (int i = 0; i < chargingStations.size(); i++) {
                chargingGene[i] = mapTripToGene(
                        new Trip(
                                "CH" + (i + 1),
                                "ChargingTrip",
                                chargingStations.get(i),
                                "NotStarted",
                                minChargingTime)
                );
            }
            return chargingGene;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    public List<Gene> mutateChargingTimes(List<Gene> genes) {
        try {
            int startSize = genes.size();
            List<Integer> chargingIndeces = findChargingStations(genes);
            if (chargingIndeces.size() == 0) {
                return genes;
            }
            int changeAmount = random.nextInt(chargingIndeces.size());
            for (int i = 0; i < changeAmount; i++) {
                // get random charging gene
                int index = chargingIndeces.get(random.nextInt(chargingIndeces.size()));
                Gene chargingGene = genes.get(index);

                Gene copy = new Gene(
                        chargingGene.getId(),
                        chargingGene.getStart(),
                        chargingGene.getEnd(),
                        null,
                        chargingGene.getChargingTime(),
                        config
                );
                // get random charging time between mincharging time and max charging time
                Double randomChargingTime = ThreadLocalRandom.current().nextDouble(
                        config.getMIN_CHARGING_TIME(),
                        (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) > config.getMIN_CHARGING_TIME() + 1.0 ? (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) : config.getMIN_CHARGING_TIME() + 1.0
                );
                copy.setChargingTime(randomChargingTime);
                genes.set(index, copy);
            }

            if (genes.size() != startSize) {
                System.out.println("Caught Exception in MutateChargingTimes");
            }

            return genes;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
        return null;
    }

    // helper functions
    private List<Integer> findChargingStations(List<Gene> genes) {
        try {
            List<Integer> indeces = new ArrayList<>();
            for (int i = 0; i < genes.size(); i++) {
                if (genes.get(i) != null) {
                    indeces.add(i);
                }
            }
            return indeces;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    private void removeElements(List<Gene> genes, int size) {
        try {
            while (genes.size() > size) {
                System.out.println("While");
                int randomIndex = random.nextInt(genes.size());
                int currentIndex = 0;
                Iterator iterator = genes.iterator();
                while (iterator.hasNext()) {
                    iterator.next();
                    if (currentIndex == randomIndex) {
                        iterator.remove();
                        break;
                    }
                    currentIndex++;
                }
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }

    }
}
