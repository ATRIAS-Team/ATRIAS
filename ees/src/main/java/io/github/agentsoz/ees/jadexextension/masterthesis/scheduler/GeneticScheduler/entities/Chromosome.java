package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.BatteryModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.utils.GeneticUtils;
import io.github.agentsoz.util.Location;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.SchedulerUtils.calculateTravelTime;
import static io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.SchedulerUtils.calculateWaitingTime;

public class Chromosome {

    private Config config;
    private final List<Gene> customerChromosome;
    private final List<Gene> chargingChromosome;
    private final Random random;
    private final GeneticUtils geneticUtils;
    private String representation;


    public Chromosome(List<Gene> customerChromosome, List<Gene> chargingChromosome, Config config) {
        this.customerChromosome = Collections.unmodifiableList(customerChromosome);
        this.chargingChromosome = Collections.unmodifiableList(chargingChromosome);
        this.config = config;
        this.random = new Random();
        this.geneticUtils = new GeneticUtils(config);
        this.representation = "";
        updateRepresentation();
    }

    public List<Gene> mergeGenes() {
        List<Gene> resultGene = new ArrayList<>();
        try {
            for (int i = 0; i < customerChromosome.size(); i++) {
                if (chargingChromosome.get(i) != null) {
                    resultGene.add(chargingChromosome.get(i));
                }
                resultGene.add(customerChromosome.get(i));
            }
            // add last charging chromosome if not null
            if (chargingChromosome.get(customerChromosome.size()) != null) {
                resultGene.add(chargingChromosome.get(customerChromosome.size()));
            }
            return resultGene;
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            System.out.println("what the...");
        }
        return resultGene;
    }

    String filePath = "genetic3-4-3-Neu1.csv";
    String header = "ids,waitingTimes,distance,odr,battery,disFrac,odrFrac,battFrac,fitness";

    public double fitnessOld() {
        try {

        // calculate fitness for merged genes
        List<Gene> chromosomeToEvaluate = mergeGenes();

        boolean vehicleBreaksDownRisk = false;
        BatteryModel model = new BatteryModel();
        model.setMyChargestate(config.getBatteryLevel() + getCurrentTripChargingTime() * config.getCHARGE_INCREASE());
        // distance to agentlocation of first gene => create gene for agentLocation
        // if end location is null the start location is used to determine the distance between two genes
        Gene agentGene = new Gene(null, geneticUtils.getStartLocation(), null, null, null, config);
        double distance = agentGene.distance(chromosomeToEvaluate.get(0));
        double currWaitingTime = calculateTravelTime(distance, config.getDRIVING_SPEED());
        List<Double> waitingTimes = new ArrayList<>();
        if (chromosomeToEvaluate.get(0).getEnd() != null) {
            double diffToBookingTime = calculateWaitingTime(chromosomeToEvaluate.get(0).getBookingTime(), config.getSimulationTime());
            waitingTimes.add(currWaitingTime + diffToBookingTime);
        }

        double penaltyForLoadingToLong = 0.0;
        for (int i = 0; i < chromosomeToEvaluate.size() - 1; i++) {
            // calc distance of trip (start to end)
            double geneDistance = getGeneDistance(chromosomeToEvaluate.get(i));

            // gene is charging gene if distance equals 0.0
            if (geneDistance == 0.0) {
                double chargingTime = chromosomeToEvaluate.get(i).getChargingTime();
                currWaitingTime += chargingTime;
                if (model.getMyChargestate() + chargingTime * config.getCHARGE_INCREASE() > 1.0) {
                    penaltyForLoadingToLong = 0.05;
                }
                model.charge(chargingTime);
            }

            // calc distance to next trip
            double distanceToNext = chromosomeToEvaluate.get(i).distance(chromosomeToEvaluate.get(i + 1)) ;
            model.discharge(geneDistance + distanceToNext, 0, false);
            // battery level shouldn't get near battery threshhold
            if (model.getMyChargestate() <= config.getBattThreshhold()) {
                vehicleBreaksDownRisk = true;
                break;
            }
            distance += geneDistance + distanceToNext;

            // add time needed for the trip itself and time needed for the next trip
            currWaitingTime += calculateTravelTime(geneDistance + distanceToNext, config.getDRIVING_SPEED());

//                currWaitingTime += chromosomeToEvaluate.get(i).calculateWaitingTime(chromosomeToEvaluate.subList(0, i + 1));

            // add waiting time if next trip is customer trip
            if (chromosomeToEvaluate.get(i + 1).getEnd() != null) {
                double diffToBookingTime = calculateWaitingTime(chromosomeToEvaluate.get(i + 1).getBookingTime(), config.getSimulationTime());
                waitingTimes.add(currWaitingTime + diffToBookingTime);
            }
        }

        // last trip has not yet been considered
        double lastGeneDistance =  getGeneDistance(chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1));
        distance += lastGeneDistance;
        if (lastGeneDistance == 0.0) {
            double chargingTime = chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1).getChargingTime();
            model.charge(chargingTime);
        }
        model.discharge(lastGeneDistance, 0, false);
        // battery level shouldn't get near 25%
        if (model.getMyChargestate() <= config.getBattThreshhold()) {
            vehicleBreaksDownRisk = true;
        }

//            Double chargingTime = chromosomeToEvaluate.stream()
//                    .map(g -> g.getChargingTime() == null ? 0.0 : g.getChargingTime())
//                    .collect(Collectors.summingDouble(Double::doubleValue));
//            model.charge(chargingTime);

        // overall waiting time minimieren?
        Double delay = 150.0;
        int odr = waitingTimes.stream().filter(wt -> wt > config.getTHETA() - delay).collect(Collectors.toList()).size();
//        double waitingTimeSum = waitingTimes.stream().mapToDouble(Double::doubleValue).sum();

        if (vehicleBreaksDownRisk) {
            return 0.0;
        }

        // Becomes smaller the greater the distance
        double avgCusTripDistance = 3000;
        double avgChaTripDistance = 1500;
        double tripsTimesAvgDistance = avgCusTripDistance * this.customerChromosome.size()
                + avgChaTripDistance * (chromosomeToEvaluate.size() - this.customerChromosome.size());

        Double fitnessFractionDistance = (distance / tripsTimesAvgDistance) > 1.0 ? 1.0 : (distance / tripsTimesAvgDistance);
        fitnessFractionDistance = 1 - fitnessFractionDistance;
        // 0.1 was added, as otherwise the fraction is equal to 1 with an ODR value of 1. However, this should only be
        // 1 if the ODR is 0.
        Double fitnessFractionODR = 1 - (odr / Double.valueOf(this.customerChromosome.size()));

//        double thetaTimeCusTrips = this.customerChromosome.size() * config.getTHETA();
//        Double fitnessFractionWaitingTimeSum = (waitingTimeSum / thetaTimeCusTrips) > 1.0 ? 1.0 : (thetaTimeCusTrips / 10000);
//        fitnessFractionWaitingTimeSum =  1 - fitnessFractionWaitingTimeSum;

        // The higher the fitness, the better it is
        // fitnessFractionDistance, fitnessFractionODR, model.getMyChargeState € [0,1]

//            double distanceWeight = 0.2192;
//            double waitingTimeSumWeight = 0.3781;
//            double odrWeight = 0.3707;
//            double batteryWeight = 0.0321;

//        double distanceWeight = 0.3;
//        double waitingTimeSumWeight = 0.3;
//        double odrWeight = 0.3;
//        double batteryWeight = 0.1;

        double distanceWeight = 0.3;
//        double waitingTimeSumWeight = 0.3;
        double odrWeight = 0.6;
        double batteryWeight = 0.1;

        Double considerFuture = 0.0;
        // last element is charging gene
        if (isChargingGene(chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1))) {
            considerFuture = 0.1;
        }

//                + waitingTimeSumWeight * fitnessFractionWaitingTimeSum
        double fitness = distanceWeight * (fitnessFractionDistance)
                + odrWeight * fitnessFractionODR
                + batteryWeight * model.getMyChargestate()
                + considerFuture - penaltyForLoadingToLong;
        fitness = fitness > 1.0 ? 1.0 : fitness;

        if (false) {
            File file = new File(filePath);
            boolean dateiExistiert = file.exists();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                if (!dateiExistiert) {
                    writer.write(header);
                    writer.newLine();
                }

                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        representation,
                        waitingTimes.stream().map(w -> String.format("%.2f", w)).collect(Collectors.toList()).toString().replace(",", "-"),
//                        chromosomeToEvaluate.stream().map(g -> g.getChargingTime() == null ? 0 : g.getChargingTime()).collect(Collectors.toList()).toString().replace(",", "-"),
//                                chromosomeToEvaluate.stream()
//                                        .map(g -> {
//                                            if (g.getEnd() == null) {
//                                                return Arrays.asList(g.getStart());
//                                            } else {
//                                                return Arrays.asList(g.getStart(), g.getEnd());
//                                            }
//                                        })
//                                        .flatMap(List::stream)
//                                        .collect(Collectors.toList()).toString().replace(",", "-"),
                        String.format("%.2f", distance),
//                        waitingTimeSum,
                        odr,
                        String.format("%.2f", model.getMyChargestate()),
                        String.format("%.2f", fitnessFractionDistance),
//                        fitnessFractionWaitingTimeSum,
                        String.format("%.2f", fitnessFractionODR),
                        String.format("%.2f", model.getMyChargestate()),
                        String.format("%.2f", fitness)
                ));
                writer.newLine();

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
//                System.out.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
//                                chromosomeToEvaluate.stream().map(g -> g.getId()).collect(Collectors.toList()).toString().replace(",", "-"),
//                                chromosomeToEvaluate.stream().map(g -> g.getChargingTime() == null ? 0 : g.getChargingTime()).collect(Collectors.toList()).toString().replace(",", "-"),
//                                chromosomeToEvaluate.stream()
//                                        .map(g -> {
//                                            if (g.getEnd() == null) {
//                                                return Arrays.asList(g.getStart());
//                                            } else {
//                                                return Arrays.asList(g.getStart(), g.getEnd());
//                                            }
//                                        })
//                                        .flatMap(List::stream)
//                                        .collect(Collectors.toList()).toString().replace(",", "-"),
//                                distance,
//                                waitingTimeSum,
//                                odr,
//                                model.getMyChargestate(),
//                                fitnessFractionDistance,
//                                fitnessFractionWaitingTimeSum,
//                                fitnessFractionODR,
//                                model.getMyChargestate(),
//                                fitness
//                        )
//                );
        }

        return fitness;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    double fitnessRefactored() {
        try {
            // calculate fitness for merged genes
            List<Gene> chromosomeToEvaluate = mergeGenes();

            Location vaLocation = geneticUtils.getStartLocation();

            Double batteryAfterList;
            List<Double> distancesToTrips = new ArrayList<>();
            Double batteryLevel = config.getBatteryLevel();
            Double batteryThreshhold = 0.3;
            Boolean batteryFallsBelowThreshhold = false;
            Double distance = 0.0;
            for (int i = 0; i < chromosomeToEvaluate.size(); i++) {
                Gene currentGene = chromosomeToEvaluate.get(i);
                List<Location> locationsOfGene = getLocationsOfGene(currentGene);

                // drive to start of current trip
                Double currentDistance = getDistanceWithDistanceFactor(vaLocation, locationsOfGene.get(0));
                distance += currentDistance;
                distancesToTrips.add(distance);
                vaLocation = locationsOfGene.get(0);

                if (isChargingGene(currentGene)) {
                    Double chargingTime = currentGene.getChargingTime() == null ? 0.0 : currentGene.getChargingTime();
                    batteryLevel += chargingTime * config.getCHARGE_INCREASE();
                } else {
                    // first trip is customer - drive to end
                    distance += getDistanceWithDistanceFactor(vaLocation, locationsOfGene.get(1));
                    vaLocation = locationsOfGene.get(1);
                }
            }

            List<Double> waitingTimesOfGenes = getWaitingTimeOfGenes(distancesToTrips, chromosomeToEvaluate);

            // add distance of last trip from start to end
            Double totalDistance = distancesToTrips.get(distancesToTrips.size() - 1);
            Gene lastGene = chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1);
            if (!isChargingGene(lastGene)) {
                totalDistance += getDistanceWithDistanceFactor(lastGene.getStart(), lastGene.getEnd());
            }

            // berücksichtige ob current trip charging trip und dann chargingtime bei Bestimmung der ODR
            batteryAfterList = batteryLevel - (totalDistance * config.getCHARGE_DECREASE()) + getCurrentTripChargingTime() * config.getCHARGE_INCREASE();

            // charged to long => odr may get to high also
            double penaltyForLoadingToLong = 0.0;
            if (batteryAfterList > 1.0) {
                batteryAfterList = 1.0;
                penaltyForLoadingToLong = 0.05;
            }

            // check if battery threshhold is undercut
            if (batteryAfterList < batteryThreshhold) {
                batteryFallsBelowThreshhold = true;
            }

            // rating
            if (batteryFallsBelowThreshhold) {
                return 0.0;
            }

            int odr = 0;
            Double delay = 120.0;
            for (int i = 0; i < chromosomeToEvaluate.size(); i++) {
                if (!isChargingGene(chromosomeToEvaluate.get(i)) && waitingTimesOfGenes.get(i) > config.getTHETA() - delay) {
                    odr++;
                }
            }

            // weighted sum consisting of totalDistance, odr, batteryAfterAll

            // Becomes smaller the greater the distance
            // analyzed from data.csv
            double avgCusTripDistance = 1100;
            double avgChaTripDistance = 450;
            double tripsTimesAvgDistance = avgCusTripDistance * this.customerChromosome.size()
                    + avgChaTripDistance * (chromosomeToEvaluate.size() - this.customerChromosome.size());

            Double fitnessFractionDistance = (totalDistance / tripsTimesAvgDistance) > 1.0
                    ? 1.0
                    : (tripsTimesAvgDistance / 10000);
            fitnessFractionDistance = 1 - fitnessFractionDistance;

            // 0.1 was added, as otherwise the fraction is equal to 1 with an ODR value of 1. However, this should only be
            // 1 if the ODR is 0.
            Double fitnessFractionODR = 1 - (odr / Double.valueOf(this.customerChromosome.size()));

            Double waitingTimeSum = waitingTimesOfGenes.stream().mapToDouble(Double::doubleValue).sum();
            double thetaTimeCusTrips = this.customerChromosome.size() * config.getTHETA();
            Double fitnessFractionWaitingTimeSum = (waitingTimeSum / thetaTimeCusTrips) > 1.0
                    ? 1.0
                    : (waitingTimeSum / thetaTimeCusTrips);
            fitnessFractionWaitingTimeSum = 1 - fitnessFractionWaitingTimeSum;

            // The higher the fitness, the better it is
            // fitnessFractionDistance, fitnessFractionODR, model.getMyChargeState € [0,1]

//            double distanceWeight = 0.2192;
//            double waitingTimeSumWeight = 0.3781;
//            double odrWeight = 0.3707;
//            double batteryWeight = 0.0321;


            double distanceWeight = 1 / 6.0;
//            double waitingTimeSumWeight = 0.3;
            double odrWeight = 3 / 6.0;
            double batteryWeight = 2 / 6.0;

            Double considerFuture = 0.0;
            if (isChargingGene(chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1)) && batteryAfterList < 0.85) {
                considerFuture = 0.1;
            }

            double fitness = distanceWeight * (fitnessFractionDistance)
//                    + waitingTimeSumWeight * fitnessFractionWaitingTimeSum
                    + odrWeight * fitnessFractionODR
                    + batteryWeight * batteryAfterList
                    + considerFuture
                    - penaltyForLoadingToLong;

            if (false) {
                File file = new File(filePath);
                boolean dateiExistiert = file.exists();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    if (!dateiExistiert) {
                        writer.write(header);
                        writer.newLine();
                    }

                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                            chromosomeToEvaluate.stream().map(g -> g.getId()).collect(Collectors.toList()).toString().replace(",", "-"),
                            chromosomeToEvaluate.stream().filter(g -> isChargingGene(g)).map(g -> g.getChargingTime()).mapToDouble(Double::doubleValue).sum(),
                            totalDistance,
                            waitingTimeSum,
                            odr,
                            batteryAfterList,
                            fitnessFractionDistance,
                            fitnessFractionWaitingTimeSum,
                            fitnessFractionODR,
                            batteryAfterList,
                            fitness
                    ));
                    writer.newLine();

                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            return fitness;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    Boolean fallsNotBelowThreshhold() {
        try {
            Double threshhold = 0.3;
            List<Location> locations = new ArrayList<>();
            locations.add(geneticUtils.getStartLocation());
            locations.addAll(getLocations());
            Double currChargingTime = getCurrentTripChargingTime();

            Double totalDistance = geneticUtils.getTotalDistanceOfLocationList(locations);
            Double battery = config.getBatteryLevel() + currChargingTime * config.getCHARGE_INCREASE();
            battery -= totalDistance * config.getCHARGE_DECREASE();

            List<Gene> genesWithoutLast = chargingChromosome.subList(0, chargingChromosome.size() - 1);
            Double chargingTimeInChromosom = genesWithoutLast.stream()
                    .filter(g -> g != null)
                    .map(g -> g.getChargingTime())
                    .mapToDouble(Double::doubleValue)
                    .sum();
            battery += chargingTimeInChromosom * config.getCHARGE_INCREASE();

            return battery > threshhold;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private List<Location> getLocations() {
        List<Location> result = new ArrayList<>();
        for (int i = 0; i < chargingChromosome.size(); i++) {
            if (chargingChromosome.get(i) != null) {
                result.add(chargingChromosome.get(i).getStart());
            }
            if (i != chargingChromosome.size() - 1) {
                result.add(customerChromosome.get(i).getStart());
                result.add(customerChromosome.get(i).getEnd());
            }
        }
        return result;
    }

    private List<Double> getWaitingTimeOfGenes(List<Double> distancesToTrips, List<Gene> chromosome) {
        List<Double> travelTimesToEachStart = distancesToTrips.stream()
                .map(d -> calculateTravelTime(d, config.getDRIVING_SPEED()))
                .collect(Collectors.toList());

        for (int i = 0; i < travelTimesToEachStart.size(); i++) {
            Gene gene = chromosome.get(i);
            if (!isChargingGene(gene)) {
                double travelAndWaitingTime = travelTimesToEachStart.get(i) + calculateWaitingTime(
                        gene.getBookingTime(),
                        config.getSimulationTime()
                );

                Double chargingTimeBeforeTrip = getCurrentTripChargingTime();
                for (int j = 0; j < i; j++) {
                    if (isChargingGene(chromosome.get(j))) {
                        chargingTimeBeforeTrip += chromosome.get(j).getChargingTime();
                    }
                }

                // add MIN_CHARGING_TIME for every charging station on the way
                travelAndWaitingTime += chargingTimeBeforeTrip;
                travelTimesToEachStart.set(i, travelAndWaitingTime);
            }
        }
        return travelTimesToEachStart;
    }

    private Double getCurrentTripChargingTime() {
        List<Trip> currentTrip = config.getCurrentTrip();
        if (currentTrip.size() == 0) { return 0.0; }
        else {
            return currentTrip.get(0).getTripType().equals("ChargingTrip")
                    ? currentTrip.get(0).getChargingTime()
                    : 0.0;
        }
    }

    private boolean isChargingGene(Gene currentGene) {
        return currentGene.getEnd() == null;
    }

    private Double getDistanceWithDistanceFactor(Location startLocation, Location location) {
        return Location.distanceBetween(startLocation, location) * config.getDISTANCE_FACTOR();
    }

    private List<Location> getLocationsOfGene(Gene gene) {
        if (gene.getEnd() == null) {
            return Arrays.asList(gene.getStart());
        } else {
            return Arrays.asList(gene.getStart(), gene.getEnd());
        }
    }

    private double getGeneDistance(Gene gene) {
        return gene.getEnd() != null
                ? Location.distanceBetween(gene.getStart(), gene.getEnd()) * config.getDISTANCE_FACTOR()
                : 0.0;
    }

    private void updateRepresentation() {
        String result = "";
        List<Gene> mergedGenes = mergeGenes();
        for (Gene gene: mergedGenes) {
            result += gene.getId();
            if (gene.getChargingTime() != null) {
                result += gene.getChargingTime();
            }
        }
        this.representation = result;
    }

    Chromosome[] crossover(Chromosome otherChromosome) {
        try {
            // halbiere das Chromosom und füge die zweite hälfte des ersten an die erste hälfte des zweiten ein und umgekehrt
            List<List<Gene>> customerCrossOver = geneticUtils.makeCrossoverCustomerWithHashset(customerChromosome, otherChromosome.customerChromosome);
            List<List<Gene>> chargingCrossOver = geneticUtils.makeCrossoverCharging(chargingChromosome, otherChromosome.chargingChromosome);

            if (customerCrossOver.get(0).size() != customerChromosome.size()
                    || customerCrossOver.get(1).size() != customerChromosome.size()
                    || chargingCrossOver.get(0).size() != chargingChromosome.size()
                    || chargingCrossOver.get(1).size() != chargingChromosome.size()) {
                System.out.println("Caught Exception in Crossover: \n  this.customer - " + this.customerChromosome
                        + " | this.charging - " + this.chargingChromosome
                        + "\n  Other customer - " + otherChromosome.customerChromosome + " | Other charging - " + otherChromosome.chargingChromosome);
            }

            return new Chromosome[] {
                    new Chromosome(customerCrossOver.get(0), chargingCrossOver.get(0), config),
                    new Chromosome(customerCrossOver.get(1), chargingCrossOver.get(1), config)
            };
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    Chromosome[] uniformCrossover(Chromosome other) {
        List<Gene> thisCustomer = customerChromosome;
        List<Gene> otherCustomer = other.customerChromosome;

        Set<Gene> firstCross = new LinkedHashSet<>();
        Set<Gene> secondCross = new LinkedHashSet<>();
        for (int i = 0; i < thisCustomer.size(); i++) {
            // flip a coin
            boolean flip = random.nextDouble() < 0.5;
            if (flip) {
                firstCross.add(otherCustomer.get(i));
                secondCross.add(thisCustomer.get(i));
            } else {
                firstCross.add(thisCustomer.get(i));
                secondCross.add(otherCustomer.get(i));
            }
        }

        if (firstCross.size() != thisCustomer.size()) {
            for (Gene g: otherCustomer) {
                if (!firstCross.contains(g)) {
                    firstCross.add(g);
                }
            }
        }

        if (secondCross.size() != thisCustomer.size()) {
            for (Gene g: thisCustomer) {
                if (!secondCross.contains(g)) {
                    secondCross.add(g);
                }
            }
        }



        List<Gene> thisCharging = copyList(chargingChromosome);
        List<Gene> otherCharging = copyList(other.chargingChromosome);

        List<Gene> firstChargCross = new ArrayList<>();
        List<Gene> secondChargCross = new ArrayList<>();
        for (int i = 0; i < thisCharging.size(); i++) {
            // flip a coin
            boolean flip = random.nextDouble() < 0.5;
            if (flip) {
                firstChargCross.add(otherCharging.get(i));
                secondChargCross.add(thisCharging.get(i));
            } else {
                firstChargCross.add(thisCharging.get(i));
                secondChargCross.add(otherCharging.get(i));
            }
        }

        return new Chromosome[] {
                new Chromosome(new ArrayList<>(firstCross), firstChargCross, config),
                new Chromosome(new ArrayList<>(secondCross), secondChargCross, config)
        };
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

    Chromosome mutateAlternately(int mutateCustomer) {
        try {
            // mutate customer trips and insert or delete charging trips

            if (mutateCustomer == 0) {
                // case customer trip
                final List<Gene> customerCopy = new ArrayList<>(this.customerChromosome);
                if (this.customerChromosome.size() > 1) {
                    int size = this.customerChromosome.size();
                    int indexA = random.nextInt(size);
                    int indexB = random.nextInt(size);
                    while (indexA == indexB) {
                        indexA = random.nextInt(size);
                    }
                    Collections.swap(customerCopy, indexA, indexB);
                }

                return new Chromosome(customerCopy, this.chargingChromosome, config);
            }

            // case charging trip
            // swap random or change the charging times of random charging trips

            else if (mutateCustomer == 1) {
                List<Gene> chargingCopy = copyList(this.chargingChromosome);

                if (chargingCopy.size() == 2) {
                    Collections.swap(chargingCopy, 0, 1);
                } else {
                    int size = this.chargingChromosome.size();
                    int indexACharging = random.nextInt(size);
                    int indexBCharging = random.nextInt(size);
                    while (indexACharging == indexBCharging) {
                        indexACharging = random.nextInt(size);
                    }
                    Collections.swap(chargingCopy, indexACharging, indexBCharging);
                }

                return new Chromosome(this.customerChromosome, chargingCopy, config);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    Chromosome[] mutate() {
        try {
            // mutate customer trips and insert or delete charging trips

            // case customer trip
            final List<Gene> customerCopy = new ArrayList<>(this.customerChromosome);
            if (this.customerChromosome.size() > 1) {
                int size = this.customerChromosome.size();
                int indexA = random.nextInt(size);
                int indexB = random.nextInt(size);
                while (indexA == indexB) {
                    indexA = random.nextInt(size);
                    indexB = random.nextInt(size);
                }
                Collections.swap(customerCopy, indexA, indexB);
            }

            // case charging trip
            // swap random or change the charging times of random charging trips

            List<Gene> chargingCopy = copyList(this.chargingChromosome);

            if (chargingCopy.size() == 2) {
                Collections.swap(chargingCopy, 0, 1);
            } else {
                int size = this.chargingChromosome.size();
                int indexACharging = random.nextInt(size);
                int indexBCharging = random.nextInt(size);
                while (indexACharging == indexBCharging) {
                    indexACharging = random.nextInt(size);
                    indexBCharging = random.nextInt(size);
                }
                Collections.swap(chargingCopy, indexACharging, indexBCharging);
            }

            return new Chromosome[] {
                    new Chromosome(this.customerChromosome, chargingCopy, config),
                    new Chromosome(customerCopy, this.chargingChromosome, config)
            };
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    Chromosome mutateChargingTimes() {
        final List<Gene> customerCopy = new ArrayList<>(this.customerChromosome);
        List<Gene> chargingCopy = new ArrayList<>(this.chargingChromosome);
        List<Gene> mutated = geneticUtils.mutateChargingTimesInRelationToBatteryLevelConsideringPreviousTrips(chargingCopy, customerCopy);
        return new Chromosome(customerCopy, mutated, config);
    }

    public List<Gene> getCustomerChromosome() {
        return customerChromosome;
    }

    public List<Gene> getChargingChromosome() {
        return chargingChromosome;
    }

    public String getRepresentation() {
        return representation;
    }
}
