package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.utils;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Chromosome;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Config;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Gene;
import io.github.agentsoz.util.Location;

import java.util.*;

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

        List<Gene> copyChargingGenes = copyList(chargingGenes);
        return new Chromosome(customerGene, copyChargingGenes, config);
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
                Gene gene = chargingGene[randomIndex];
                Gene copyOfGene = gene.createDeepCopy();

                int min = config.getMIN_CHARGING_TIME().intValue();
                Double temp = (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) > config.getMIN_CHARGING_TIME() + 1.0
                        ? (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel()))
                        : config.getMIN_CHARGING_TIME() + 1.0;
                int max = temp.intValue();
                int randomInt = random.nextInt((max - min) + 1) + min;

                copyOfGene.setChargingTime(Double.valueOf(randomInt));
                result.set(indexToSet, copyOfGene);
            }
            return result;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    public List<List<Gene>> makeCrossoverCustomerWithHashset(List<Gene> first, List<Gene> second) {
        // case there is only one customer trip
        try {
            if (first.size() == 1) {
                return Arrays.asList(first, first);
            }

            final List<List<Gene>> thisDNA = split(first);
            final List<List<Gene>> otherDNA = split(second);

            final Set<Gene> firstCrossOver = new LinkedHashSet<>(thisDNA.get(0));

            for (int i = 0; i < otherDNA.get(0).size(); i++) {
                firstCrossOver.add(otherDNA.get(0).get(i));
            }

            for (int i = 0; i < otherDNA.get(1).size(); i++) {
                firstCrossOver.add(otherDNA.get(1).get(i));
            }

            final Set<Gene> secondCrossOver = new LinkedHashSet<>(otherDNA.get(1));

            for (int i = 0; i < thisDNA.get(0).size(); i++) {
                secondCrossOver.add(thisDNA.get(0).get(i));
            }

            for (int i = 0; i < thisDNA.get(1).size(); i++) {
                secondCrossOver.add(thisDNA.get(1).get(i));
            }

            return Arrays.asList(
                    new ArrayList<>(firstCrossOver),
                    new ArrayList<>(secondCrossOver)
            );
        } catch (Exception e) {
            System.out.println("Caught exception: " + e);
            e.printStackTrace();
            return null;
        }
    }

    public List<List<Gene>> makeCrossoverCharging(List<Gene> first, List<Gene> second) {
        try {
            final List<List<Gene>> thisDNA = split(first);
            final List<List<Gene>> otherDNA = split(second);

            List<List<Gene>> copyThisDNA = makeCopyList(thisDNA);
            List<List<Gene>> copyOtherDNA = makeCopyList(otherDNA);

            final List<Gene> firstCrossOver = new ArrayList<>(copyThisDNA.get(0));
            firstCrossOver.addAll(copyOtherDNA.get(1));

            final List<Gene> secondCrossOver = new ArrayList<>(copyOtherDNA.get(0));
            secondCrossOver.addAll(copyThisDNA.get(1));

            return Arrays.asList(firstCrossOver, secondCrossOver);
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
        return null;
    }

    private List<List<Gene>> makeCopyList(List<List<Gene>> thisDNA) {
        List<List<Gene>> result = new ArrayList<>();
        for (List<Gene> intermediate: thisDNA) {
            List<Gene> copy = copyList(intermediate);
            result.add(copy);
        }
        return result;
    }

    private List<Gene> copyList(List<Gene> genes) {
        List<Gene> copy = new ArrayList<>();
        for (Gene gene: genes) {
            if (gene == null) {
                copy.add(null);
            } else {
                copy.add(gene.createDeepCopy());
            }
        }
        return copy;
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


    // one charging trips get mutated
    public List<Gene> mutateChargingTimesInRelationToBatteryLevelConsideringPreviousTrips(List<Gene> genes, List<Gene> customergenes) {
        try {
            genes = copyList(genes);
            boolean atLeastOneWasMutated = false;
            outerloop:
            while (!atLeastOneWasMutated) {
                for (int i = 0; i < genes.size(); i++) {
                    if (genes.get(i) != null) {
                        boolean rand = random.nextBoolean();
                        if (rand) {
                            Double batteryLevel = getBatteryLevelConsideringPreviousGenes(customergenes, genes, i);
                            Double chargingTimeTillThreshhold = 0.0;
                            if (batteryLevel < 0.2) {
                                chargingTimeTillThreshhold = (0.3 - batteryLevel) * config.getCOMPLETE_CHARGING_TIME();
                            }
                            Double minChargingTime = config.getMIN_CHARGING_TIME() < chargingTimeTillThreshhold ? chargingTimeTillThreshhold : config.getMIN_CHARGING_TIME();
                            int min = config.getMIN_CHARGING_TIME().intValue();
                            Double temp = (config.getMAX_CHARGING_TIME() * (1 - batteryLevel)) > minChargingTime + 1.0
                                    ? (config.getMAX_CHARGING_TIME() * (1 - batteryLevel))
                                    : minChargingTime + 1.0;
                            int max = temp.intValue();
                            int randomInt = random.nextInt((max - min) + 1) + min;

                            genes.get(i).setChargingTime(Double.valueOf(randomInt));
                            atLeastOneWasMutated = true;
                            break outerloop;
                        }
                    }
                }
            }


//                int startSize = genes.size();
//                List<Integer> chargingIndeces = findChargingStations(genes);
//                if (chargingIndeces.size() == 0) {
//                    return genes;
//                }
//                int changeAmount = random.nextInt(chargingIndeces.size());
//                for (int i = 0; i < changeAmount; i++) {
//                    // get random charging gene
//                    int index = chargingIndeces.get(random.nextInt(chargingIndeces.size()));
//                    Gene chargingGene = genes.get(index);
//
//                    Gene copy = new Gene(
//                            chargingGene.getId(),
//                            chargingGene.getStart(),
//                            chargingGene.getEnd(),
//                            null,
//                            chargingGene.getChargingTime(),
//                            config
//                    );
//                    // get random charging time between mincharging time and max charging time
//                    Double randomChargingTime = ThreadLocalRandom.current().nextDouble(
//                            config.getMIN_CHARGING_TIME(),
//                            (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) > config.getMIN_CHARGING_TIME() + 1.0 ? (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) : config.getMIN_CHARGING_TIME() + 1.0
//                    );
//                    copy.setChargingTime(randomChargingTime);
//                    genes.set(index, copy);
//                }
//
//                if (genes.size() != startSize) {
//                    System.out.println("Caught Exception in MutateChargingTimes");
//                }
//
            return genes;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }
        return null;
    }

    private Double getBatteryLevelConsideringPreviousGenes(List<Gene> customerGenes, List<Gene> chargingGenes, int i) {
        // i = 0 no previous trip, only to start location, i = 1 one charging one customer possible, i = 2 two charging to customer
        Double startBattery = config.getBatteryLevel();
        if (i == 0 && chargingGenes.get(i) != null) {
            Double distance = (Location.distanceBetween(getStartLocation(), chargingGenes.get(i).getStart()) * config.getDISTANCE_FACTOR());
            startBattery -= distance * config.getCHARGE_DECREASE();
        } else {
            // get locations list calculate distance
            List<Location> locations = getLocationsOfGenes(customerGenes, chargingGenes, i);
            Double distance = getTotalDistanceOfLocationList(locations);
            startBattery -= distance * config.getCHARGE_DECREASE();

            // get charging times till i
            Double chargingTimes = 0.0;
            for (int j = 0; j < i; j++) {
                if (chargingGenes.get(j) != null)
                    chargingTimes += chargingGenes.get(j).getChargingTime();
            }
            startBattery += chargingTimes * config.getCHARGE_INCREASE();
        }
        return startBattery;
    }

    private List<Location> getLocationsOfGenes(List<Gene> customerGenes, List<Gene> chargingGenes, int i) {
        List<Location> locations = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            if (chargingGenes.get(j) != null) {
                locations.add(chargingGenes.get(j).getStart());
            }
            locations.add(customerGenes.get(j).getStart());
            locations.add(customerGenes.get(j).getEnd());
        }
        if (chargingGenes.get(i) != null) {
            locations.add(chargingGenes.get(i).getStart());
        }
        return locations;
    }

    public Double getTotalDistanceOfLocationList(List<Location> locs) {
        Double result = 0.0;
        for (int i = 0; i < locs.size() - 1; i++) {
            result += Location.distanceBetween(locs.get(i), locs.get(i + 1)) * config.getDISTANCE_FACTOR();
        }
        return result;
    }

    public Location getStartLocation() {
        Location location = config.getAgentLocation();
        List<Trip> currentTrip = config.getCurrentTrip();
        if (currentTrip.size() > 0) {
            switch (currentTrip.get(0).getTripType()) {
                case "AtStartLocation":
                    location = currentTrip.get(0).getStartPosition();
                    break;
                case "DriveToEnd":
                    // überabschätzung
                    location = currentTrip.get(0).getStartPosition();
                    break;
                case "AtEndLocation":
                    location = currentTrip.get(0).getEndPosition();
                    break;
                default:
                    location = config.getAgentLocation();
                    break;
            }
        }
        return location;
    }
}
