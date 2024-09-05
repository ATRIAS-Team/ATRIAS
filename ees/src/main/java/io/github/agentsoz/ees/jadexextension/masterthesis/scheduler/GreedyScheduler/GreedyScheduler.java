package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.BatteryModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.enums.Strategy;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.metrics.Metrics;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.metrics.MinMaxMetricsValues;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.util.PrinterUtil;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.SchedulerUtils;
import io.github.agentsoz.util.Location;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GreedyScheduler {

    PrinterUtil printerUtil = new PrinterUtil();
    List<Trip> chargingTrips;
    double batteryLevel;
    Location currentVALocation;
    LocalDateTime simulationTime;
    Double THETA;
    Double DRIVING_SPEED;
    // ToDo: Mit Setter von außen setzen, damit Scheduler nicht immer wieder instanziiert werden muss?
    Double TIME_UNTIL_CURRENT_ACTION_IS_DONE;
    // A charging trip only makes sense if you can charge for at least this amount
    Double CHARGING_THRESHHOLD = 0.05;
    // 12600 seconds for 0% - 100% (400 Watt Battery)
    Double COMPLETE_CHARGING_TIME = 12600.0;
    Double MIN_CHARGING_TIME = COMPLETE_CHARGING_TIME * CHARGING_THRESHHOLD;
    Double BATTERY_THRESHHOLD = 0.4;
    String agentId;
    int chargingTripCounter = 1;
    List<Location> chargingStations;
    Double DISTANCE_FACTOR = 3.0;
    double CHARGE_DECREASE = 0.0001;
    double CHARGE_INCREASE = 0.000079;
    Trip currentTrip;

    /**
     * Current batteryLevel is necessary to be able to evaluate the permutations and is necessary to determine whether
     * the battery level is sufficient or whether the vehicle will break down
     */
    public GreedyScheduler(
            List<Location> chargingStations,
            double batteryLevel,
            Location currentVaLocation,
            LocalDateTime simulationTime,
            Double drivingSpeed, Double theta,
            String agentID,
            Double timeTillCurrentActionIsDone,
            Trip currentTrip
    ) {
        chargingTrips = convertChargingStationsToTrips(chargingStations);
        this.chargingStations = chargingStations;
        this.batteryLevel = batteryLevel;
        this.currentVALocation = currentVaLocation;
        this.simulationTime = simulationTime;
        this.DRIVING_SPEED = drivingSpeed;
        this.THETA = theta;
        this.agentId = agentID;
        this.TIME_UNTIL_CURRENT_ACTION_IS_DONE = timeTillCurrentActionIsDone;
        this.currentTrip = currentTrip;
    }

    /**
     * Returns a list of trips with optimal order in which all n! possible permutations have been considered and the
     * best permutation is determined with the help of an evaluation function
     *
     * @return List of scheduled trips
     */
    public List<Trip> greedySchedule(List<Trip> allTrips, Strategy strategy) {
        Date startTime = new Date(System.currentTimeMillis());
        printerUtil.startScheduler(agentId, batteryLevel, this.simulationTime);
//        System.out.println("TripList size: " +  allTrips.size() + " - " + allTrips.stream().map(t -> t.getTripID()).collect(Collectors.toList()));

        // ToDo: Is there a solution under all cirumstances? What happens if all rating results are 0? Is that possible?
        // ToDo: Charging trip counter needs to be incremented for every inserted charging trip
//        List<List<Trip>> permutations = getAllPermutations(allTrips);

        List<Trip> lastSixElements;
        if (allTrips.size() <= 6) {
            lastSixElements = allTrips;
        } else {
            lastSixElements = allTrips.subList(allTrips.size() - 6, allTrips.size());
        }

        List<Trip> remaining = allTrips.subList(0, allTrips.size() - lastSixElements.size());

        boolean permutateCustomerTrips = false;
        List<List<Trip>> permutations = getAllPermutationsWithOneOption(lastSixElements, permutateCustomerTrips);

        Double batteryAfterEndOfRemaining = getBatteryAfterEndOfRemaining(remaining);

        List<Trip> result =  simpelScheduling(permutations, batteryAfterEndOfRemaining);
        remaining.addAll(result);
        return remaining;
//        for (List<Trip> permutation: permutations) {
//            System.out.println(permutation.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
//        }

//        try {
//            List<Trip> res = fifoWithChargingTimes(allTrips);
//            return res;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }

//        System.out.println("Size of permutations: " + permutations.size() + " origin trip list: " + allTrips.stream().map(t -> t.getTripID()).collect(Collectors.toList()));

//        MetricsValues metricsValues = getAllMetricsValuesForEachPermutation(permutations, strategy);

//        System.out.println("(" + allTrips.stream().map(t -> t.getTripID()).collect(Collectors.toList()) + ") Finished calculating metrics...");
        // Determine the minimum and maximum values for each metric. These are used for normalization right after.
//        MinMaxMetricsValues minMaxMetricsValues = getMinMaxMetricsValues(metricsValues);
//        List<List<Number>> normalizedMetricsValues = normalizeValues(metricsValues, minMaxMetricsValues);

//        printerUtil.metrics(permutations, metricsValues.getAllOdrValues(), metricsValues.getAllTotalDistances(),
//                metricsValues.getAllMinBatteryLevelValues(), metricsValues.getAllBatteryLevelValuesAfterAllTrips(),
//                metricsValues.getAllStopsValues(), metricsValues.getAllChargingTimes());

        // ToDo: Optimize rating function

//        List<Trip> result = gradualApproach(permutations, metricsValues);

//        List<Double> ratings = rating(
//                normalizedMetricsValues,
//                metricsValues.getAllTripsWithCharingTimes(),
//                metricsValues.getAllVaBreaksDownValues()
//        );
//        int idx = idx(ratings, metricsValues.getAllTripsWithCharingTimes());

//        printerUtil.metricsOfIndex(idx, permutations, metricsValues.getAllOdrValues(), metricsValues.getAllTotalDistances(),
//                metricsValues.getAllMinBatteryLevelValues(), metricsValues.getAllBatteryLevelValuesAfterAllTrips(),
//                metricsValues.getAllStopsValues(), metricsValues.getAllChargingTimes());
//        printerUtil.endScheduler(ratings);

//        List<Trip> result = permutations.get(idx);

//        System.out.println("IDs: " + result.stream().map(t -> t.getTripID()).collect(Collectors.toList()));

        /**
         * Kann genutzt werden, um die Ausgabe in eine CSV zu kopieren mit folgenden Spalten:
         * IDs, ODR, Total Distance, MinBattery, BatteryAfterAllTrips, Stops, ChargingTimes, BatteryLevel, Rating
         **/
//         printerUtil.csv(permutations, metricsValues.getAllOdrValues(), metricsValues.getAllTotalDistances(),
//                 metricsValues.getAllMinBatteryLevelValues(), metricsValues.getAllBatteryLevelValuesAfterAllTrips(),
//                 metricsValues.getAllStopsValues(), metricsValues.getAllChargingTimes(), batteryLevel, ratings);

//        System.out.println("End time with schedule time: " + ((new Date(System.currentTimeMillis()).getTime() - startTime.getTime()) / 1000) + " Permutations size: " + permutations.size());

//        for (int i = 0; i < result.size(); i++) {
//            if (isChargingTrip(result.get(i))) {
//                result.get(i).tripID = "CH" + chargingTripCounter;
//                chargingTripCounter++;
//            }
//        }
//        return result;
    }

    private Double getBatteryAfterEndOfRemaining(List<Trip> remaining) {
        Double batteryAtStart = batteryLevel;
        List<Location> locations = getLocationsOfTripListWithAgentLocation(remaining);
        Double distance = getTotalDistanceOfLocationList(locations);
        batteryAtStart -= distance * CHARGE_DECREASE;

        Double chargingTime = remaining.stream().filter(t -> isChargingTrip(t)).map(t -> t.getChargingTime()).mapToDouble(Double::doubleValue).sum();
        batteryAtStart += chargingTime * CHARGE_INCREASE;
        return batteryAtStart;
    }

    public List<Trip> simpelScheduling(List<List<Trip>> permutations, Double batteryLevel) {
        try {
            Double batteryThreshhold = 0.3;

            List<List<Trip>> configuredPermutations = new ArrayList<>();
            Iterator<List<Trip>> it = permutations.iterator();
            while (it.hasNext()) {
                List<Trip> current = it.next();
                // konfiguriere jede Permutation so, dass TrikeAgent nicht liegen bleibt bzw. am ende battery über threshhold ist (berücksichtige currenttrip)
                List<Trip> configured = configurePermutation(current, batteryThreshhold, batteryLevel);
                if (configured == null) {
                    it.remove();
                } else {
                    configuredPermutations.add(configured);
                }
            }

            // calculate metrics
            List<List<Double>> distanceOdrBattery = getDistanceOdrBatteryValues(configuredPermutations, batteryLevel);

            List<List<Double>> normalizedDistanceOdrBattery = normalizeDistanceOdrAndBattery(distanceOdrBattery, configuredPermutations.get(0));

            List<Double> ratings = ratings(normalizedDistanceOdrBattery);

//            printCsv(configuredPermutations, distanceOdrBattery, normalizedDistanceOdrBattery, ratings);

            List<Trip> best = getBest(ratings, permutations);

            if (isChargingTrip(best.get(best.size() - 1))) {
                best.get(best.size() - 1).setChargingTime(-1.0);
            } else {
                Location nearestLocationToEnd = getNearestChargingStationToLocation(best.get(best.size() - 1).getEndPosition());
                best.add(new Trip(
                        "CH" + (this.chargingTripCounter),
                        "ChargingTrip",
                        nearestLocationToEnd,
                        "NotStarted",
                        -1.0
                ));
            }

            return best;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void printCsv(List<List<Trip>> configuredPermutations, List<List<Double>> distanceOdrBattery, List<List<Double>> normalizedDistanceOdrBattery, List<Double> ratings) {
        String filePath = "greedy3-3-3.csv";
        String header = "ids,distance,odr,battery,normDis,normOdr,normBat,rating";

        File file = new File(filePath);
        boolean alreadyExists = file.exists();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (!alreadyExists) {
                writer.write(header);
                writer.newLine();
            }

            for (int i = 0; i < configuredPermutations.size(); i++) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                        configuredPermutations.get(i).stream().map(t -> t.getTripID()).collect(Collectors.toList()).toString().replace(",","-"),
                        distanceOdrBattery.get(0).get(i),
                        distanceOdrBattery.get(1).get(i),
                        distanceOdrBattery.get(2).get(i),
                        normalizedDistanceOdrBattery.get(0).get(i),
                        normalizedDistanceOdrBattery.get(1).get(i),
                        normalizedDistanceOdrBattery.get(2).get(i),
                        ratings.get(i)
                ));
                writer.newLine();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private List<Trip> getBest(List<Double> ratings, List<List<Trip>> permutations) {
        Double max = Collections.max(ratings);
        for (int i = 0; i < ratings.size(); i++) {
            if (ratings.get(i).equals(max)) {
                return permutations.get(i);
            }
        }
        return null;
    }

    private List<Double> ratings(List<List<Double>> normalizedDistanceOdrBattery) {
        List<Double> ratings = new ArrayList<>();
        for (int i = 0; i < normalizedDistanceOdrBattery.get(0).size(); i++) {
            Double distanceFraction = 0.33;
            Double odrFraction = 0.33;
            Double batteryFraction = 0.33;

            Double rating = distanceFraction * normalizedDistanceOdrBattery.get(0).get(i)
                    + odrFraction * normalizedDistanceOdrBattery.get(1).get(i)
                    + batteryFraction * normalizedDistanceOdrBattery.get(2).get(i);
            ratings.add(rating);
        }
        return ratings;
    }

    private List<List<Double>> normalizeDistanceOdrAndBattery(List<List<Double>> distanceOdrBattery, List<Trip> permutations) {
        List<Double> distances = distanceOdrBattery.get(0);
        List<Double> odrs = distanceOdrBattery.get(1);
        List<Double> batterys = distanceOdrBattery.get(2);

        List<Double> normalizedDistance = new ArrayList<>();
        Double minD = Collections.min(distances);
        Double maxD = Collections.max(distances);
        for (Double distance: distances) {
            if (minD.equals(maxD)) {
                normalizedDistance.add(1.0);
            } else {
                normalizedDistance.add(1 - normalize(
                        distance,
                        minD,
                        maxD
                ));
            }
        }

        List<Double> normalizedOdrs = new ArrayList<>();
        Double minO = Collections.min(odrs);
        Double maxO = Collections.max(odrs);
        for (Double odr: odrs) {
            if (minO.equals(maxO)) {
                normalizedOdrs.add(
                        normalizeIfEquals(odr, permutations)
                );
            } else {
                normalizedOdrs.add(1 - normalize(
                        odr,
                        minO,
                        maxO
                ));
            }
        }

        List<Double> normalizedBattery = new ArrayList<>();
        Double minB = Collections.min(batterys);
        Double maxB = Collections.max(batterys);
        for (Double battery: batterys) {
            if (minB.equals(maxB)) {
                normalizedBattery.add(1.0);
            } else {
                normalizedBattery.add(normalize(
                        battery,
                        minB,
                        maxB
                ));
            }
        }


        return Arrays.asList(normalizedDistance, normalizedOdrs, normalizedBattery);
    }

    private Double normalizeIfEquals(Double odr, List<Trip> permutations) {
        Integer sizeOfCustomerTrips = permutations.stream().filter(t -> !isChargingTrip(t)).collect(Collectors.toList()).size();

        return 1 - (odr / sizeOfCustomerTrips);
    }

    private List<List<Double>> getDistanceOdrBatteryValues(List<List<Trip>> configuredPermutations, Double batteryLevelRef) {
        List<Double> distances = new ArrayList<>();
        List<Double> odrs = new ArrayList<>();
        List<Double> batterys = new ArrayList<>();
        Location start = getStartLocation();
        for (int i = 0; i < configuredPermutations.size(); i++) {
            List<Trip> tripList = configuredPermutations.get(i);
            Location currentLocation = start;

            // Step 1: Calculate travel time to each customer trip
            List<Double> distancesOfPerm = new ArrayList<>();
            // calculate travel time to start position of first trip
            double distance = roundedLocationDistanceBetween(currentLocation, tripList.get(0).startPosition);
            currentLocation = tripList.get(0).startPosition;
            for (int j = 0; j < tripList.size(); j++) {
                Trip currentTrip = tripList.get(j);
                Trip nextTrip = null;
                if (!(j == tripList.size() - 1)) {
                    nextTrip = tripList.get(j + 1);
                }
                if (nextTrip == null) {
                    distancesOfPerm.add(distance);
                    break;
                }

                if (isChargingTrip(currentTrip)) {
                    distancesOfPerm.add(0.0);
                    distance += roundedLocationDistanceBetween(currentLocation, nextTrip.startPosition);
                    currentLocation = nextTrip.startPosition;
                } else {
                    distancesOfPerm.add(distance);

                    distance += roundedLocationDistanceBetween(currentLocation, currentTrip.endPosition);
                    currentLocation = currentTrip.endPosition;

                    distance += roundedLocationDistanceBetween(currentLocation, nextTrip.startPosition);
                    currentLocation = nextTrip.startPosition;
                }
            }

            // convert travel distance in travel time
            List<Double> travelTimes = distancesOfPerm.stream().map(d -> SchedulerUtils.calculateTravelTime(d, DRIVING_SPEED)).collect(Collectors.toList());

            for (int k = 0; k < travelTimes.size(); k++) {
                Trip currentTrip = tripList.get(k);
                if (!isChargingTrip(currentTrip)) {
                    double travelAndWaitingTime = travelTimes.get(k) + SchedulerUtils.calculateWaitingTime(
                            currentTrip.bookingTime,
                            this.simulationTime
                    );

                    Double chargingTimeBeforeTrip = 0.0;
                    for (int l = 0; l < k; l++) {
                        if (isChargingTrip(tripList.get(l))) {
                            chargingTimeBeforeTrip += tripList.get(l).getChargingTime();
                        }
                    }

                    // add MIN_CHARGING_TIME for every charging station on the way
                    travelAndWaitingTime += chargingTimeBeforeTrip;
                    travelTimes.set(k, travelAndWaitingTime);
                }
            }

            Double chargingTimeSum = tripList.stream().filter(t -> isChargingTrip(t)).map(t -> t.getChargingTime()).mapToDouble(Double::doubleValue).sum();

            Double totalDistance = distancesOfPerm.get(distancesOfPerm.size() - 1);
            Trip lastTrip = tripList.get(tripList.size() - 1);
            if (!isChargingTrip(lastTrip)) {
                totalDistance += roundedLocationDistanceBetween(lastTrip.getStartPosition(), lastTrip.getEndPosition());
            }
            Double odr = (double) travelTimes.stream().filter(t -> t > THETA).collect(Collectors.toList()).size();
            Double chargingTimeCurrentTrip = currentTrip != null && isChargingTrip(currentTrip) ? currentTrip.getChargingTime() : 0.0;
            Double batteryAfterAll = batteryLevelRef - totalDistance * CHARGE_DECREASE + (chargingTimeSum + chargingTimeCurrentTrip) * CHARGE_INCREASE;

            distances.add(totalDistance);
            odrs.add(odr);
            batterys.add(batteryAfterAll);
        }
        return Arrays.asList(distances, odrs, batterys);
    }


    private List<Trip> configurePermutation(List<Trip> current, Double batteryThreshhold, Double batteryStart) {
        Location start = getStartLocation();
        boolean batteryFallsBelowThreshhold = true;
        Double distance;
        // berechne akku anhand gefahrerener distanz wenn akku < Threshhold
        // dann schaue ob vorher eine tankstelle existiert addiere ladezeit bis thershhold erreicht, wiederhole
        while (batteryFallsBelowThreshhold) {
            boolean chargingNeed = false;
            // get all locations
            Double batteryLevel = batteryStart;
            if (currentTrip != null && isChargingTrip(currentTrip) && currentTrip.getChargingTime() != -1.0) {
                batteryLevel += currentTrip.getChargingTime() * CHARGE_INCREASE;
            }
            distance = 0.0;

            Location lastLocation = start;
            for (int i = 0; i < current.size(); i++) {
                // first trip
                Double currentDistance;
                List<Location> locationsOfTrip = getLocationsOfTrip(current.get(i));


                // start to first trip
                currentDistance = roundedLocationDistanceBetween(lastLocation, locationsOfTrip.get(0));
                lastLocation = locationsOfTrip.get(0);
                if (isChargingTrip(current.get(i))) {
                    Double chargingTime = current.get(i).getChargingTime() == null ? 0.0 : current.get(i).getChargingTime();
                    batteryLevel += chargingTime * CHARGE_INCREASE;
                } else {
                    // first trip is customer - drive to end
                    currentDistance += roundedLocationDistanceBetween(lastLocation, locationsOfTrip.get(1));
                    lastLocation = locationsOfTrip.get(1);
                }
                distance += currentDistance;



                // check if current trip is possible
                if (batteryLevel - (distance * CHARGE_DECREASE) < batteryThreshhold) {
                    chargingNeed = true;
                    Integer indexOfChargingTripBefore = isChargingTripExisting(current, i);
                    // there is no charging trip before
                    if (indexOfChargingTripBefore == -1) {
                        return null;
                    } else {
                        Trip chargingTrip = current.get(indexOfChargingTripBefore);
                        Double chargingTimeNeeded = (batteryThreshhold - (batteryLevel - (distance * CHARGE_DECREASE))) * COMPLETE_CHARGING_TIME;
                        if (chargingTrip.getChargingTime() == null) {
                            chargingTrip.setChargingTime(chargingTimeNeeded);
                        } else {
                            chargingTrip.setChargingTime(chargingTrip.getChargingTime() + chargingTimeNeeded);
                        }
                    }
                }

            }

            if (!chargingNeed) {
                break;
            }
        }

        // no charging time added => one charging station has no charging time
        // => bad permutation => dont drive to charging station
        if (current.stream().filter(t -> isChargingTrip(t) && t.getChargingTime() == null).collect(Collectors.toList()).size() > 0) {
            return null;
        }

        return current;
    }

    private List<Location> getLocationsOfTrip(Trip trip) {
        if (isChargingTrip(trip)) {
            return Arrays.asList(trip.getStartPosition());
        } else {
            return Arrays.asList(trip.getStartPosition(), trip.getEndPosition());
        }
    }

    private Integer isChargingTripExisting(List<Trip> current, int i) {
        // bestimme beim wievelten trip liegen geblieben wurde
        // 0 ist start location
        // 1,2 trip 1; 3,4 trip 2; usw.
        if (i == 0) {
            return isChargingTrip(current.get(0)) ? 0 : -1;
        }
        for (int j = 0; j < i; j++) {
            if (isChargingTrip(current.get(j))) {
                return j;
            }
        }
        return -1;
    }

    private Integer existsChargingTripBeforeIndex(List<Trip> current, int index) {
        for (int i = 0; i < current.size(); i++) {
            if (i == index) {
                return -1;
            }
            if (isChargingTrip(current.get(i))) {
                return i;
            }
        }
        return -1;
    }


    private Location getStartLocation() {
        Location location = this.currentVALocation;
        if (currentTrip != null) {
            switch (currentTrip.getTripType()) {
                case "AtStartLocation":
                    location = currentTrip.getStartPosition();
                    break;
                case "DriveToEnd":
                    // überabschätzung
                    location = currentTrip.getStartPosition();
                    break;
                case "AtEndLocation":
                    location = currentTrip.getEndPosition();
                    break;
                default:
                    location = this.currentVALocation;
                    break;
            }
        }
        return location;
    }

    public List<Trip> fifoWithChargingTimes(List<Trip> input) {
        // no permutation of customer trips
        Double batteryThreshhold = 0.3;
        Double amount = 0.2;
        Double chargingTime = amount * COMPLETE_CHARGING_TIME;

        Double totalDistance = getTotalDistanceOfTripList(input);
        if (batteryLevel - (totalDistance * CHARGE_DECREASE) < batteryThreshhold) {
            // add chargingtrip to end of list

            // get nearest chargingstation to end of triplist
            Location nearestLocationToEnd = getNearestChargingStationToLocation(input.get(input.size() - 1).getEndPosition());
            Trip chargingTrip = new Trip(
                    "CH" + (this.chargingTripCounter),
                    "ChargingTrip",
                    nearestLocationToEnd,
                    "NotStarted",
                    chargingTime
            );
            input.add(chargingTrip);
        }
        return input;
    }

    private Integer getIndexOfLastTripWithoutFallingBelowBatteryThreshhold(List<Trip> trips, Double batteryThreshhold) {
        Double distance = 0.0;
        for (int i = 0; i < trips.size() - 1; i++) {
            Trip currentTrip = trips.get(0);
            if (i == 0) {
                distance += roundedLocationDistanceBetween(this.currentVALocation, currentTrip.getStartPosition());
                if (batteryLevel - (distance * CHARGE_DECREASE) < batteryThreshhold) {
                    // firstTrip is not possible without falling below batterythreshhold
                    return i;
                }
            }

            distance += roundedLocationDistanceBetween(currentTrip.getStartPosition(), currentTrip.getEndPosition());
            if (batteryLevel - (distance * CHARGE_DECREASE) < batteryThreshhold) {
                return i;
            }

            // distance to next trip start
            distance += roundedLocationDistanceBetween(currentTrip.getEndPosition(), trips.get(i + 1).getStartPosition());
            if (batteryLevel - (distance * CHARGE_DECREASE) < batteryThreshhold) {
                return i + 1;
            }

            // check if last trip resulst in falling below threshhold
            if (i == trips.size() - 2) {
                distance += roundedLocationDistanceBetween(trips.get(trips.size() - 1).getStartPosition(), trips.get(trips.size() - 1).getEndPosition());
                if (batteryLevel - (distance * CHARGE_DECREASE) < batteryThreshhold) {
                    return i + 1;
                }
            }
        }
        // all trips possible without falling below threshhold
        return -1;
    }


    public List<Trip> fifoOptimiert(List<Trip> input) {
        try {
            List<List<Trip>> permutations = getAllPermutationsOfATripList(input);
            List<Trip> minDistancePerm = getPermutationWithSmallestDistance(permutations);
            Double distanceOfPerm = getTotalDistanceOfTripList(minDistancePerm);
            List<Double> waitingTimeOfEachTrip = getTravelTimes(minDistancePerm);
            // highest travel time first
            Collections.sort(waitingTimeOfEachTrip, Collections.reverseOrder());
            Location nearestChargingStationToAgent = getNearestChargingStationBetweenTwoLocation2(this.currentVALocation, minDistancePerm.get(0).startPosition);
            Location nearestChargingStationToEndOfLastTrip = getNearestChargingStationToLocation(minDistancePerm.get(minDistancePerm.size() - 1).endPosition);
            Double batteryLevel = this.batteryLevel;
            Double maxTravelTime = getMaxTravelTime(waitingTimeOfEachTrip, batteryLevel);

            List<Double> payoffAndChargingTime = getChargingPayoffPercentage(nearestChargingStationToAgent, minDistancePerm.get(0).getStartPosition(), maxTravelTime);
            Double chargingTime = payoffAndChargingTime.get(1);
            Double percentagePayoffOfCharging = THETA - maxTravelTime > 0
                    ? payoffAndChargingTime.get(0)
                    : 0.0;

            Double batteryLoss = distanceOfPerm * 0.0001;
            boolean doFirst = false;
            // wenn payoff große genug mache trip und prüfe ob batterie ausreichend ist um letzte tankstelle zu erreichen
            if (percentagePayoffOfCharging > 0.03) {
                Double distance = roundedLocationDistanceBetween(this.currentVALocation, nearestChargingStationToAgent) + roundedLocationDistanceBetween(nearestChargingStationToAgent, minDistancePerm.get(0).getStartPosition());
                batteryLoss += distance * 0.0001;
                doFirst = true;
            }
            Double distanceToLastCharging = roundedLocationDistanceBetween(minDistancePerm.get(minDistancePerm.size() - 1).endPosition, nearestChargingStationToEndOfLastTrip);
            batteryLoss += distanceToLastCharging * 0.001;

            // baterriestand fällt unter 0.2
            Double firstCharging = doFirst ? chargingTime : 0.0;
            if (batteryLevel + firstCharging - batteryLoss < 0.35) {
                chargingTime += (0.35 -(batteryLevel - batteryLoss)) * COMPLETE_CHARGING_TIME;
            }

            Trip chargingTripEnd = new Trip(
                    "CH" + (this.chargingTripCounter),
                    "ChargingTrip",
                    nearestChargingStationToEndOfLastTrip,
                    "NotStarted",
                    -1.0
            );
            minDistancePerm.add(chargingTripEnd);

            if (doFirst) {
                Trip chargingTripStart = new Trip(
                        "CH" + (this.chargingTripCounter),
                        "ChargingTrip",
                        nearestChargingStationToAgent,
                        "NotStarted",
                        chargingTime
                );
                minDistancePerm.add(0, chargingTripStart);
            }
            return minDistancePerm;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Double getMaxTravelTime(List<Double> travelTimesToEachTrip, Double batteryLevel) {
        boolean ignoreFirst = batteryLevel < 0.35 ? true : false;
        if (travelTimesToEachTrip.size() == 1) {
            return 0.0;
        } else {
            return travelTimesToEachTrip.get(1);
        }
    }

    private List<Double> getChargingPayoffPercentage(Location nearestChargingStationToAgent, Location startPosition, Double maxWaitingTime) {
        Double totalDistance = roundedLocationDistanceBetween(this.currentVALocation, nearestChargingStationToAgent);
        totalDistance += roundedLocationDistanceBetween(nearestChargingStationToAgent, startPosition);
        Double travelTime = SchedulerUtils.calculateTravelTime(totalDistance, DRIVING_SPEED);
        Double possibleChargingTime = THETA - maxWaitingTime - travelTime;
        // CHARGE_INCREASE_COEFFICIENT = 0.000079
        return Arrays.asList(possibleChargingTime * 0.000079 - totalDistance * 0.0001, possibleChargingTime);
    }

    public Location getNearestChargingStationToLocation(Location location) {
        Double smallestDistance = Double.MAX_VALUE;
        int index = -1;
        for (int i = 0; i < this.chargingStations.size(); i++) {
            Double distance = roundedLocationDistanceBetween(location, this.chargingStations.get(i));
            if (distance < smallestDistance) {
                smallestDistance = distance;
                index = i;
            }
        }
        return this.chargingStations.get(index);
    }

    public List<Double> getTravelTimes(List<Trip> tripList) {
        Location currentLocation = this.currentVALocation;
        // Step 1: Calculate travel time to each customer trip
        List<Double> distances = new ArrayList<>();
        // calculate travel time to start position of first trip
        double distance = roundedLocationDistanceBetween(currentLocation, tripList.get(0).startPosition);
        currentLocation = tripList.get(0).startPosition;
        for (int i = 0; i < tripList.size(); i++) {
            Trip currentTrip = tripList.get(i);
            Trip nextTrip = null;
            if (!(i == tripList.size() - 1)) {
                nextTrip = tripList.get(i + 1);
            }
            if (nextTrip == null) {
                distances.add(distance);
                break;
            }

            if (isChargingTrip(currentTrip)) {
                distances.add(0.0);
                distance += roundedLocationDistanceBetween(currentLocation, nextTrip.startPosition);
                currentLocation = nextTrip.startPosition;
            } else {
                distances.add(distance);

                distance += roundedLocationDistanceBetween(currentLocation, currentTrip.endPosition);
                currentLocation = currentTrip.endPosition;

                distance += roundedLocationDistanceBetween(currentLocation, nextTrip.startPosition);
                currentLocation = nextTrip.startPosition;
            }
        }

        // convert travel distance in travel time
        List<Double> travelTimes = distances.stream().map(d -> SchedulerUtils.calculateTravelTime(d, DRIVING_SPEED)).collect(Collectors.toList());

        for (int i=0; i < travelTimes.size(); i++) {
            Trip currentTrip = tripList.get(i);
            if (!isChargingTrip(currentTrip)) {
                double travelAndWaitingTime = travelTimes.get(i) + SchedulerUtils.calculateWaitingTime(
                        currentTrip.bookingTime,
                        this.simulationTime
                );
                travelTimes.set(i, travelAndWaitingTime);
            }
        }

        return travelTimes;
    }

    public List<Trip> getPermutationWithSmallestDistance(List<List<Trip>> permutations) {
        Double smallestDistance = Double.MAX_VALUE;
        Integer index = -1;
        for (int j = 0; j < permutations.size(); j++) {
            List<Trip> currPerm = permutations.get(j);

            Double totalDistance = getTotalDistanceOfTripList(currPerm);

            if (totalDistance < smallestDistance) {
                smallestDistance = totalDistance;
                index = j;
            }
        }
        return permutations.get(index);
    }

    public Double getTotalDistanceOfTripList(List<Trip> trips) {
        List<Location> locations = getLocationsOfTripListWithAgentLocation(trips);
        return getTotalDistanceOfLocationList(locations);
    }

    private List<Location> getLocationsOfTripListWithAgentLocation(List<Trip> trips) {
        List<Location> locations = getLocationsOfTripListWithoutAgentLocation(trips);
        locations.add(0, currentVALocation);
        return locations;
    }

    private List<Location> getLocationsOfTripListWithoutAgentLocation(List<Trip> trips) {
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < trips.size(); i++) {
            if (isChargingTrip(trips.get(i))) {
                locations.add(trips.get(i).getStartPosition());
            } else {
                locations.add(trips.get(i).getStartPosition());
                locations.add(trips.get(i).getEndPosition());
            }
        }
        return locations;
    }

    public Double getTotalDistanceOfLocationList(List<Location> locs) {
        Double result = 0.0;
        for (int i = 0; i < locs.size() - 1; i++) {
            result += roundedLocationDistanceBetween(locs.get(i), locs.get(i + 1));
        }
        return result;
    }

    private List<Trip> gradualApproach(List<List<Trip>> trips, MetricsValues metrics) {
        try {

            boolean solutionFound = false;
            int odr = 0;
            int maxOdr = Collections.max(metrics.getAllOdrValues());
            int index = -1;
            while (!solutionFound) {
                List<Integer> indicesWithMinOdr = getIndicesWithMinOdr(odr, metrics.getAllOdrValues());

                // permutation with lowest totalDistance is at first place
                List<List<Number>> sortedIndicesDistancesLists = getSortedDistanceListsWithIndices(indicesWithMinOdr, metrics.getAllTotalDistances());

                // permutation with highest battery after all is at first place
                List<List<Number>> sortedBatteryAfterAllTripsList = getSortedBatteryAfterAllTripsList(sortedIndicesDistancesLists, metrics.getAllBatteryLevelValuesAfterAllTrips());

                if (sortedBatteryAfterAllTripsList.size() > 0 && (Double) sortedBatteryAfterAllTripsList.get(0).get(1) > BATTERY_THRESHHOLD) {
                    index = (Integer) sortedBatteryAfterAllTripsList.get(0).get(0);
                    solutionFound = true;
                }

//                for (List<Number> distList : sortedBatteryAfterAllTripsList) {
//                    if (metrics.getAllBatteryLevelValuesAfterAllTrips().get((Integer) distList.get(0)) > BATTERY_THRESHHOLD) {
//                        index = (Integer) distList.get(0);
//                        solutionFound = true;
//                    }
//                }
                if (!solutionFound) {
                    if (odr == maxOdr) {
                        index = trips.size() - 1;
                        break;
                    } else {
                        odr++;
                        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                    }
                }
            }
            return trips.get(index);
        } catch (Exception e) {
            e.printStackTrace();
            return trips.get(0);
        }
    }

    private List<List<Number>> getSortedBatteryAfterAllTripsList(List<List<Number>> sortedIndicesDistancesLists, List<Double> allBatteryLevelValuesAfterAllTrips) {
        List<List<Number>> result = new ArrayList<>();
        for (List<Number> indicesPair: sortedIndicesDistancesLists) {
            Integer index = (Integer) indicesPair.get(0);
            Double battery = allBatteryLevelValuesAfterAllTrips.get(index);

            if (result.size() == 0) {
                result.add(Arrays.asList(index, battery));
            } else {
                for (int i = 0; i < result.size(); i++) {
                    if (!(battery < (Double) result.get(i).get(1))) {
                        if (i == result.size() - 1) {
                            result.add(Arrays.asList(index, battery));
                            break;
                        } else {
                            result.add(i, Arrays.asList(index, battery));
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<List<Number>> getSortedDistanceListsWithIndices(List<Integer> indicesWithMinOdr, List<Double> allTotalDistances) {
        List<List<Number>> result = new ArrayList<>();
        for (Integer idx: indicesWithMinOdr) {
            Double totalDistanceIdx = allTotalDistances.get(idx);

            if (result.size() == 0) {
                result.add(Arrays.asList(idx, totalDistanceIdx));
            } else {
                for (int i = 0; i < result.size(); i++) {
                    if ((Double) result.get(i).get(1) > totalDistanceIdx) {
                        if (i == result.size() - 1) {
                            result.add(Arrays.asList(idx, totalDistanceIdx));
                            break;
                        } else {
                            result.add(i, Arrays.asList(idx, totalDistanceIdx));
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<Integer> getIndicesWithMinOdr(int odr, List<Integer> allOdrValues) {
        List<Integer> minOdrIndices = new ArrayList<>();
        for (int i = 0; i < allOdrValues.size(); i++) {
            if (allOdrValues.get(i).equals(odr)) {
                minOdrIndices.add(i);
            }
        }
        return minOdrIndices;
    }

    public List<List<Number>> normalizeValues(MetricsValues metricsValues, MinMaxMetricsValues minMaxMetricsValues) {
        List<List<Number>> normalized = new ArrayList<>();
        for (int i = 0; i < metricsValues.getAllOdrValues().size(); i++) {
            List<Number> intermediate = new ArrayList<>();

            // Normalize ODR
            double normalizedOdr;
            if (minMaxMetricsValues.getMinOdr() == minMaxMetricsValues.getMaxOdr() && minMaxMetricsValues.getMinOdr() == 0) {
                normalizedOdr = 1;
            } else if (minMaxMetricsValues.getMinOdr() == minMaxMetricsValues.getMaxOdr() && minMaxMetricsValues.getMinOdr() == 1) {
                normalizedOdr = 0;
            } else {
                normalizedOdr = 1 - normalize(
                        metricsValues.getAllOdrValues().get(i),
                        minMaxMetricsValues.getMinOdr(),
                        minMaxMetricsValues.getMaxOdr()
                );
            }
            intermediate.add(normalizedOdr);

            // Normalize TotalDistance
            intermediate.add(1 - normalize(
                    metricsValues.getAllTotalDistances().get(i),
                    minMaxMetricsValues.getMinTotalDistance(),
                    minMaxMetricsValues.getMaxTotalDistance()
            ));

            // Normalize Stops
            intermediate.add(1 - normalize(
                    metricsValues.getAllStopsValues().get(i),
                    minMaxMetricsValues.getMinStops(),
                    minMaxMetricsValues.getMaxStops())
            );

            // Normalize BatteryLevelAfterAllTrips
            intermediate.add(normalize(
                    metricsValues.getAllBatteryLevelValuesAfterAllTrips().get(i),
                    minMaxMetricsValues.getMinBatteryLevelAfterAllTrips(),
                    minMaxMetricsValues.getMaxBatteryLevelAfterAllTrips()
            ));

            // Normalize MinBatteryLevel (throughout the trips)
            intermediate.add(normalize(
                    metricsValues.getAllMinBatteryLevelValues().get(i),
                    minMaxMetricsValues.getMinBatteryLevel(),
                    minMaxMetricsValues.getMaxBatteryLevel())
            );

            normalized.add(intermediate);
        }
        return normalized;
    }


    public MetricsValues getAllMetricsValuesForEachPermutation(List<List<Trip>> permutations, Strategy strategy) {
        MetricsValues metricsValues = new MetricsValues();
        for (int i = 0; i < permutations.size(); i++) {
            List<Trip> permutation = permutations.get(i);
            // Simulate the ride of the trip list for each permutation and determine the metrics in the process
            Metrics metrics = simulateTripListAndCalculateMetricsOld(permutation, strategy);

            if (metrics.isVaBreaksDown()) {
                metricsValues.addOdr(-1);
                metricsValues.addMinBatteryLevel(0.0);
            } else {
                metricsValues.addOdr(metrics.getOdr());
                metricsValues.addMinBatteryLevel(metrics.getMinBatteryLevel());
            }
            Double chargingTime = permutation.stream()
                    .filter(t -> isChargingTrip(t))
                    .map(t -> t.getChargingTime())
                    .collect(Collectors.summingDouble(Double::doubleValue));

            metricsValues.addChargingTimes(chargingTime);
            metricsValues.addTripsWithChargingTime(metrics.getTripsWithChargingTime());
            metricsValues.addTotalDistance(metrics.getTotalDistance());
            metricsValues.addStopps(permutation.size());
            metricsValues.addBatteryLevelAfterAllTrips(metrics.getBatteryLevelAfterAllTrips());
            metricsValues.addVaBreakDown(metrics.isVaBreaksDown());
        }

        return metricsValues;
    }

    public MinMaxMetricsValues getMinMaxMetricsValues(MetricsValues metricsValues) {
        MinMaxMetricsValues minMaxMetricsValues = new MinMaxMetricsValues();
        int minOdr = Integer.MAX_VALUE;
        for (int i = 0; i < metricsValues.getAllVaBreaksDownValues().size(); i++) {
            if (metricsValues.getAllOdrValues().get(i) >= 0) {
                minOdr = Math.min(minOdr, metricsValues.getAllOdrValues().get(i));
            }
        }
        minMaxMetricsValues.setMinOdr(minOdr);
        minMaxMetricsValues.setMaxOdr(Collections.max(metricsValues.getAllOdrValues()));
        minMaxMetricsValues.setMinTotalDistance(Collections.min(metricsValues.getAllTotalDistances()));
        minMaxMetricsValues.setMaxTotalDistance(Collections.max(metricsValues.getAllTotalDistances()));
        minMaxMetricsValues.setMinStops(Collections.min(metricsValues.getAllStopsValues()));
        minMaxMetricsValues.setMaxStops(Collections.max(metricsValues.getAllStopsValues()));
        minMaxMetricsValues.setMinBatteryLevel(Collections.min(metricsValues.getAllMinBatteryLevelValues()));
        minMaxMetricsValues.setMaxBatteryLevel(Collections.max(metricsValues.getAllMinBatteryLevelValues()));
        minMaxMetricsValues.setMinBatteryLevelAfterAllTrips(Collections.min(metricsValues.getAllBatteryLevelValuesAfterAllTrips()));
        minMaxMetricsValues.setMaxBatteryLevelAfterAllTrips(Collections.max(metricsValues.getAllBatteryLevelValuesAfterAllTrips()));

        return minMaxMetricsValues;
    }

    // helper functions
    public List<List<Trip>> getAllPermutations(List<Trip> trips) {
        List<List<Trip>> allChargingTripsSubsets = getAllChargingTripSubsets();
        List<List<Trip>> mergedTripListWithChargingTripSubset = new ArrayList<>();
        // permutation without charging trips
        mergedTripListWithChargingTripSubset = Stream.concat(
                mergedTripListWithChargingTripSubset.stream(),
                getAllPermutationsOfATripList(trips).stream()
        ).collect(Collectors.toList());
        // permutations with charging trips
        for (List<Trip> tripSubset : allChargingTripsSubsets) {
            // Otherwise, when calculating the charging time, the time for different permutations is set to the same
            // for one trip, as all subsets reference the same trips
            List<Trip> copyOfChargingTrips = createDeepCopyOfSubset(tripSubset);
            List<Trip> merged = Stream.concat(copyOfChargingTrips.stream(), trips.stream()).collect(Collectors.toList());
            List<List<Trip>> allPermutationsOfMerged = getAllPermutationsOfATripList(merged);
            mergedTripListWithChargingTripSubset = Stream.concat(
                    mergedTripListWithChargingTripSubset.stream(),
                    allPermutationsOfMerged.stream()
            ).collect(Collectors.toList());
        }
        return mergedTripListWithChargingTripSubset;
    }

    public List<List<Trip>> getAllPermutationsWithOneOption(List<Trip> trips, boolean permutateCustomer) {
        try {
            trips = trips.stream().filter(t -> t.getTripID().startsWith("AP")).collect(Collectors.toList());
            List<List<Trip>> tripListPermutations;
            if (permutateCustomer) {
                tripListPermutations = getAllPermutationsOfATripList(trips);
            } else {
                tripListPermutations = Arrays.asList(trips);
            }
            System.out.println("Perms of triplist: " + tripListPermutations.size());
            List<List<Trip>> permutationsWithChargingStations = new ArrayList<>();
            for (List<Trip> permutation : tripListPermutations) {
                // gehe alle trips durch und suche passende tankstelle
                List<Trip> chargingStations = new ArrayList<>();
                Trip chargingStationBetweenAgentAndFirstTrip = getNearestChargingStationBetweenTwoLocation(getStartLocation(), permutation.get(0).startPosition);
                chargingStations.add(chargingStationBetweenAgentAndFirstTrip);
                for (int i = 0; i < permutation.size() - 1; i++) {
                    Trip chargingTrip = getNearestChargingStationBetweenTwoLocation(
                            permutation.get(i).getEndPosition(),
                            permutation.get(i + 1).getStartPosition()
                    );
                    chargingStations.add(chargingTrip);
                }
                chargingStations.add(nearestChargingStationToEndOfLastTrip(permutation));

                permutationsWithChargingStations.addAll(getCombinations(permutation, chargingStations));
            }


            return permutationsWithChargingStations;
            // ToDo Create copy of charging trip

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught Exception");
            return null;
        }
    }

    private List<List<Trip>> getCombinations(List<Trip> trips, List<Trip> chargingTrips) {
        List<List<Trip>> result = new ArrayList<>();
        // without any charging station
        result.add(trips);

        // get subset an add to
        int allMasks = (1 << chargingTrips.size());
        for (int i = 1; i < allMasks; i++) {

            List<Trip> tripsCopy = new ArrayList<>(trips);
            // new permutation
            List<Trip> newPermutation = new ArrayList<>();

            for (int j = 0; j < chargingTrips.size(); j++) {
                if (j > 0) {
                    newPermutation.add(tripsCopy.get(0));
                    tripsCopy.remove(0);
                }

                // j is index to set charging station
                if ((i & (1 << j)) > 0) {
                    // case add charging trip

                    newPermutation.add(chargingTrips.get(j));
                }

//                if (chargingTrips.size() =)
            }

            for (int k = 0; k < tripsCopy.size(); k++) {
                newPermutation.add(tripsCopy.get(0));
                tripsCopy.remove(0);
            }
            result.add(newPermutation);
        }
//        for (List<Trip> trip: result) {
//            System.out.println(trip.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
//        }
        return result;
    }

    private Trip nearestChargingStationToEndOfLastTrip(List<Trip> permutation) {
        Location lastTripEnd = permutation.get(permutation.size() - 1).getEndPosition();
        Double minDistance = Double.MAX_VALUE;
        Location result = null;
        for (Location station: chargingStations) {
            Double distance = Location.distanceBetween(lastTripEnd, station) * DISTANCE_FACTOR;
            if (distance < minDistance) {
                minDistance = distance;
                result = station;
            }
        }
        Trip chargingTrip = new Trip(
                "CH" + (this.chargingTripCounter),
                "ChargingTrip",
                result,
                "NotStarted");
        this.chargingTripCounter++;
        return chargingTrip;
    }

    private Location getNearestChargingStationBetweenTwoLocation2(Location loation1, Location location2) {
        Double minDistance = Double.MAX_VALUE;
        Location result = null;
        for (Location station: chargingStations) {
            Double distance = calcDistanceBetweenThreePoints(loation1, station, location2);
            if (distance < minDistance) {
                minDistance = distance;
                result = station;
            }
        }
        return result;
    }

    private Trip getNearestChargingStationBetweenTwoLocation(Location loation1, Location location2) {
        Double minDistance = Double.MAX_VALUE;
        Location result = null;
        for (Location station: chargingStations) {
            Double distance = calcDistanceBetweenThreePoints(loation1, station, location2);
            if (distance < minDistance) {
                minDistance = distance;
                result = station;
            }
        }
        Trip chargingTrip = new Trip(
                "CH" + (this.chargingTripCounter),
                "ChargingTrip",
                result,
                "NotStarted");
        this.chargingTripCounter++;
        return chargingTrip;
    }

    private Double calcDistanceBetweenThreePoints(Location first, Location second, Location  third) {
        return (Location.distanceBetween(first, second) + Location.distanceBetween(second, third)) * DISTANCE_FACTOR;
    }

    // generation of all permutations take approx 4 seconds for n = 10, 40 sec. for n = 11
    public List<List<Trip>> getAllPermutationsOfATripList(List<Trip> tripList) {
//        tripList = tripList.stream().filter(t -> !isChargingTrip(t)).collect(Collectors.toList());
        List<List<Trip>> result = new ArrayList<>();
        if (tripList.size() == 1) {
            result.add(tripList);
            return result;
        }

        Trip trip = tripList.get(0);
        List<Trip> remainder = tripList.subList(1, tripList.size());

        List<List<Trip>> previous = getAllPermutationsOfATripList(remainder);

        for (List<Trip> t : previous) {
            for (int i = 0; i <= t.size(); i++) {
                List<Trip> newPermutations = new ArrayList<>();
                newPermutations.addAll(t.subList(0, i));
                newPermutations.add(trip);
                newPermutations.addAll(t.subList(i, t.size()));
                if (noConsecutiveChargingTrips(newPermutations)) {
                    result.add(newPermutations);
                }
            }
        }
        return result;
    }

    /**
     * Calculates the total distance traveled, taking into account the current position of the VA.
     *
     * @param allTrips
     * @return totalDistance
     */
    public double calculateTotalDistance(List<Trip> allTrips) {
        double totalDistance = roundedLocationDistanceBetween(allTrips.get(0).startPosition, currentVALocation);
        List<Location> allLocations = allTrips.stream()
                .map(trip -> {
                    if (trip.getTripID().equals("ChargingTrip")) {
                        return Arrays.asList(trip.getStartPosition());
                    } else {
                        return Arrays.asList(trip.getStartPosition(), trip.getEndPosition());
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (int i = 0; i < allLocations.size() - 1; i++) {
            double distance = roundedLocationDistanceBetween(allLocations.get(i), allLocations.get(i + 1));
            totalDistance += distance;
        }
        return totalDistance;
    }

    public List<List<Trip>> getAllChargingTripSubsets() {
        List<List<Trip>> result = new ArrayList<>();

        // Bitmask idea derived from:
        // https://stackoverflow.com/questions/7206442/printing-all-possible-subsets-of-a-list
        int allMasks = (1 << chargingTrips.size());
        for (int i = 1; i < allMasks; i++) {
            List<Trip> temp = new ArrayList<>();
            for (int j = 0; j < chargingTrips.size(); j++) {
                if ((i & (1 << j)) > 0) {
                    temp.add(chargingTrips.get(j));
                }
            }
            result.add(temp);
        }

        return result;
    }

    public List<Double> rating(List<List<Number>> normalizedMetricsValues, List<List<Trip>> tripsWithChargingTime,
                         List<Boolean> vaBreaksDown) {
        /**
         * 0 is the worst possible value, 1 is the best possible value
         *
         * If the battery level is not sufficient for the permutation, the evaluation function should return the value
         * 0 for this permutation.
         *
         * (relevant?) the shortest possible distance (add to end value) use min and max values for normalization
         * => val = (currVal - minVal) / (maxVal - minVal)
         *
         * how to determine charging time and percent? => vaTime and bookingTime for Jobs,
         * specify the number of percent loaded per time unit (determine if battery level is sufficient!)
         * Check if battery level is sufficient with distance of trips and discharging of battery
         * instantiate a battery with the charge level of the actual battery for each permutation and run the scenario
         * (using the discharge function)
         *
         * amount of stops (use normalization function)
         *
         * order drop out rate? => vaTime
         *
         * battery level after last trip (0-100) no normalization need, divide by 100 provides the result
         *
         * Average Travel Distance ATD: Overall travel distance divided by the number of successfully served trips
         * for the entire fleet
         *
         */

        // Vehicle Agent breaks down
        // or unnecessary charging trip is included in trip list

        // Wenn es unter den Battery Threshhold fällt muss auf jeden Fall getankt werden
        boolean tendencyToCharge = batteryLevel < BATTERY_THRESHHOLD ? true : false;

        // Trips mit möglichst viele ChargingTime (mit möglichst wenig Stops?) sollten den höchsten Score bekommen
        double chargingImportanceFraction = 0.25;
        double totalDistanceFraction = 0.25;
        double odrFraction = 0.25;
        double batteryAfterAllTripsFraction = 0.15;
        double batteryFraction = 0.05;
        double stopsFraction = 0.05;
        List<Double> chargingTripRatings;
        if (tendencyToCharge) {
            chargingTripRatings = normalizedAndRatedChargingTimes(tripsWithChargingTime);
        } else {
            chargingTripRatings = new ArrayList<>();
        }
//        System.out.println("Tendency to charge: " + tendencyToCharge);
        List<Double> ratings = IntStream.range(0, normalizedMetricsValues.size())
                .mapToObj(i -> {
                    List<Number> metricValues = normalizedMetricsValues.get(i);

                    if (vaBreaksDown.get(i) || oneChargingTripHasZeroChargingTime(tripsWithChargingTime.get(i))) {
                        return 0.0;
                    }

                    double chargingRating = 0.0;
                    if (tendencyToCharge && oneOrMoreTripIsChargingTrip(tripsWithChargingTime.get(i)) && !vaBreaksDown.get(i)) {
                        chargingRating = chargingTripRatings.get(i);
                    }


                    Double odr = odrFraction * (double) metricValues.get(0);
                    Double distance = totalDistanceFraction * (double) metricValues.get(1);
                    Double stops = stopsFraction * (double) metricValues.get(2);
                    Double battAfterAll = batteryAfterAllTripsFraction * (double) metricValues.get(3);
                    Double batt = batteryFraction * (double) metricValues.get(4);
                    Double chargeImp = chargingImportanceFraction * chargingRating;

                    Double rating = odr + distance + stops + battAfterAll + batt + chargeImp;

                    // create csv with relevant infos
//                    csv(i, tripsWithChargingTime, metricValues, chargingRating, odr, distance, stops, battAfterAll, batt, chargeImp, rating);

                    return rating;
                }).collect(Collectors.toList());

        return ratings;
    }

    private void csv(int i, List<List<Trip>> tripsWithChargingTime, List<Number> metricValues, double chargingRating, Double odr, Double distance, Double stops, Double battAfterAll, Double batt, Double chargeImp, Double rating) {
        String filePath = "csvToAnalyze-Greedy.csv";
        String header = "trips,odr,distance,stops,battAfterAll,batt,chargeImp,odrFrac,disFrac,stopsFrac,battAfterAllFrac,battFrac,chargeImpFrac,rating";
        File file = new File(filePath);
        boolean alreadyExists = file.exists();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            // Wenn die Datei nicht existiert, schreibe den Header
            if (!alreadyExists) {
                writer.write(header);
                writer.newLine();
            }

            // Füge die neuen Daten hinzu
            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    tripsWithChargingTime.get(i).stream().map(t -> t.getTripID()).collect(Collectors.toList()).toString().replace(",", "-"),
                    metricValues.get(0),
                    metricValues.get(1),
                    metricValues.get(2),
                    metricValues.get(3),
                    metricValues.get(4),
                    chargingRating,
                    odr,
                    distance,
                    stops,
                    battAfterAll,
                    batt,
                    chargeImp,
                    rating
            ));
            writer.newLine();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Double> normalizedAndRatedChargingTimes(List<List<Trip>> trips) {
        List<Double> ratings = new ArrayList<>();

        for (int i = 0; i < trips.size(); i++) {
            List<Trip> tripList = trips.get(i);
            Double rating = rateChargingTripList(tripList);
            ratings.add(rating);
        }

        // normalize ratings
        Double minRating = Collections.min(ratings);
        Double maxRating = Collections.max(ratings);
        ratings = ratings.stream().map(r -> normalize(r, minRating, maxRating)).collect(Collectors.toList());

        return ratings;
    }

    private Double rateChargingTripList(List<Trip> trips) {
        Double rating = 0.0;
        List<Trip> chargingTrips = trips.stream().filter(t -> isChargingTrip(t)).collect(Collectors.toList());
        if (chargingTrips.size() > 0) {
            Double totalChargingTime = chargingTrips.stream()
                    .map(t -> {
                        if (t.getChargingTime() == -1) {
                            return 6000.0;
                        } else {
                            return t.getChargingTime();
                        }
                    })
                    .reduce(0.0, Double::sum);
            rating = totalChargingTime / chargingTrips.size();
        }
        return rating;
    }

    private boolean oneOrMoreTripIsChargingTrip(List<Trip> trips) {
        return trips.stream()
                .filter(t -> isChargingTrip(t))
                .collect(Collectors.toList())
                .size() > 0;
    }

    private boolean oneChargingTripHasZeroChargingTime(List<Trip> tripList) {
        boolean result = tripList.stream()
                .filter(t -> {
                    if (isChargingTrip(t)) {
                        return t.getChargingTime() == 0.0;
                    }
                    return false;
                }).count() > 0;
        return result;
    }

    private double normalize(double value, double min, double max) {
        return (value - min) / (max - min);
    }

    private List<Trip> convertChargingStationsToTrips(List<Location> chargingStations) {
        List<Trip> trips = new ArrayList<>();
        if (chargingStations != null) {
            for (int i = 0; i < chargingStations.size(); i++) {
                Trip trip = new Trip(
                        "CH" + (i + 1),
                        "ChargingTrip",
                        chargingStations.get(i),
                        "NotStarted");
                trips.add(trip);
            }
        }
        return trips;
    }

    private boolean noConsecutiveChargingTrips(List<Trip> trips) {
        List<String> ids = trips.stream()
                .map(trip -> trip.getTripID())
                .collect(Collectors.toList());

        String prev = ids.get(0);
        for (int i = 1; i < ids.size(); i++) {
            if (prev.startsWith("C") && ids.get(i).startsWith("C")) {
                return false;
            }
            prev = ids.get(i);
        }
        return true;
    }

    private Metrics simulateTripListAndCalculateMetrics(List<Trip> trips, Strategy strategy) {
        /**
         * Explanation:
         * Step 1: Set charging time for each chargingStation to minium charging time to ensure that each charging trip
         * is worthwhile.
         *
         * Step 2: Simulate tripList, determine Trips where THETA is exceeded and make a copy of the tripList without
         * these trips, if strategy is equal to IGNORE_CUSTOMER. If the Agent breaks down, stop the simulation.
         *
         * Step 3: Calculate real charging time for trips (without the trips where theta is exceeded)
         *
         * Step 4: Calculate battery level after all trips
         *
         */

        try {

            boolean vaBreaksDown = true;

            /** Step 1
             * Set all charging times to MIN_CHARGING_TIME. IFthis increases the ODR, then ignore the trip(s) that increase
             * the ODR, if the strategy is equal to IGNORE_CUSTOMER. Then calculate the MaximumWaitingTime and adjust the
             * charging times accordingly.
             */
            setMinChargingTimeForAllChargingStations(trips);

            // Step 2
            List<Trip> copy = new ArrayList<>(trips);

            int indexOfBreakDown = -1;
            int odr = 0;

            // for chargingTrips this array should store 0.0 as value
            // used to derive waiting time of each customer
            List<Double> waitingTimeOfEachTrip = new ArrayList<>();
            Double waitingTime;
            double totalDistance = 0.0;
            double minBatteryLevel = 1.0;

            while (vaBreaksDown) {
                try {
                    // get total distance driven for trip list
                    // count charging trips and add battery level
                    BatteryModel battery = new BatteryModel();
                    battery.setMyChargestate(this.batteryLevel);

                    totalDistance = 0.0;
                    minBatteryLevel = 1.0;
                    double totalTravelTimeSeconds = TIME_UNTIL_CURRENT_ACTION_IS_DONE;
                    odr = 0;

                    // for chargingTrips this array should store 0.0 as value
                    // used to derive waiting time of each customer
                    waitingTimeOfEachTrip = new ArrayList<>();
                    waitingTime = 0.0;

                    // Drive to start of first trip
                    double distance = roundedLocationDistanceBetween(this.currentVALocation, trips.get(0).startPosition);
                    minBatteryLevel = Math.min(
                            minBatteryLevel,
                            makeTripAndDischargeBattery(battery, distance)
                    );
                    totalDistance += distance;
                    Trip firstTrip = trips.get(0);
                    totalTravelTimeSeconds += SchedulerUtils.calculateTravelTime(distance, DRIVING_SPEED);

                    if (odr(totalTravelTimeSeconds, firstTrip)) {
                        odr++;
                        if (strategy.equals(Strategy.IGNORE_CUSTOMER)) {
                            copy.remove(firstTrip);
                            totalDistance -= distance;
                        }
                    }

                    Double travelTimeToFirstTrip = SchedulerUtils.calculateTravelTime(distance, DRIVING_SPEED);
                    waitingTime += travelTimeToFirstTrip;
                    if (!isChargingTrip(trips.get(0))) {
                        waitingTimeOfEachTrip.add(travelTimeToFirstTrip);
                    } else {
                        // charging trips don't have waiting times
                        waitingTimeOfEachTrip.add(0.0);
                    }

                    // ToDo: Beim Löschen in Copy verschieben sich die Indices?
                    for (int i = 0; i < trips.size() - 1; i++) {
                        indexOfBreakDown = i;
                        Trip currentTrip = trips.get(i);
                        Trip nextTrip = trips.get(i + 1);

                        // Case Charging Trip
                        if (isChargingTrip(currentTrip)) {
                            double chargingTime = currentTrip.getChargingTime();
                            chargeBattery(battery, chargingTime);
                            totalTravelTimeSeconds += chargingTime;

                            double distanceNextTripStart = roundedLocationDistanceBetween(currentTrip.startPosition, nextTrip.startPosition);
                            minBatteryLevel = Math.min(
                                    minBatteryLevel,
                                    makeTripAndDischargeBattery(battery, distanceNextTripStart)
                            );
                            totalDistance += distanceNextTripStart;

                            // determine waiting time of next trip
                            Double travelTimeToNextTrip = SchedulerUtils.calculateTravelTime(distanceNextTripStart, DRIVING_SPEED);
                            Double waitingTimeOfNextTrip = MIN_CHARGING_TIME + travelTimeToNextTrip;
                            waitingTime += waitingTimeOfNextTrip;
                            waitingTimeOfEachTrip.add(waitingTime);


                            totalTravelTimeSeconds += SchedulerUtils.calculateTravelTime(distanceNextTripStart, DRIVING_SPEED);
                            if (odr(totalTravelTimeSeconds, nextTrip)) {
                                odr++;
                                if (strategy.equals(Strategy.IGNORE_CUSTOMER)) {
                                    copy.remove(nextTrip);
                                    totalDistance -= distanceNextTripStart;
                                }
                            }
                        } else {
                            // Case Customer Trip
                            double distanceOfCurrentTrip = roundedLocationDistanceBetween(currentTrip.startPosition, currentTrip.endPosition);
                            double distanceToNextTrip = roundedLocationDistanceBetween(currentTrip.endPosition, nextTrip.startPosition);
                            double totalDistanceCustomerTrip = distanceOfCurrentTrip + distanceToNextTrip;

                            minBatteryLevel = Math.min(
                                    minBatteryLevel,
                                    makeTripAndDischargeBattery(battery, totalDistanceCustomerTrip)
                            );
                            totalDistance += totalDistanceCustomerTrip;

                            // determine waiting time of next trip
                            Double travelTimeToNextTrip = SchedulerUtils.calculateTravelTime(totalDistance, DRIVING_SPEED);
                            Double waitingTimeOfNextTrip = travelTimeToNextTrip;
                            waitingTime += waitingTimeOfNextTrip;
                            if (isChargingTrip(nextTrip)) {
                                waitingTimeOfEachTrip.add(0.0);
                            } else {
                                waitingTimeOfEachTrip.add(waitingTime);
                            }

                            totalTravelTimeSeconds += SchedulerUtils.calculateTravelTime(
                                    totalDistanceCustomerTrip,
                                    DRIVING_SPEED
                            );
                            if (odr(totalTravelTimeSeconds, nextTrip)) {
                                odr++;
                                if (strategy.equals(Strategy.IGNORE_CUSTOMER)) {
                                    copy.remove(nextTrip);
                                    totalDistance -= totalDistanceCustomerTrip;
                                }
                            }
                        }

                        if (i == trips.size() - 2 && nextTrip.getTripType().equals("CustomerTrip")) {
                            // drive to end position of last trip
                            double distanceLastTripEnd = roundedLocationDistanceBetween(nextTrip.startPosition, nextTrip.endPosition);
                            minBatteryLevel = Math.min(
                                    minBatteryLevel,
                                    makeTripAndDischargeBattery(battery, distanceLastTripEnd)
                            );
                            totalDistance += distanceLastTripEnd;
                        }
                    }

                    // Case triplist consists only of one trip
                    if (trips.size() == 1 && !isChargingTrip(trips.get(0))) {
                        double distanceLastTripEnd = roundedLocationDistanceBetween(trips.get(0).startPosition, trips.get(0).endPosition);
                        minBatteryLevel = Math.min(
                                minBatteryLevel,
                                makeTripAndDischargeBattery(battery, distanceLastTripEnd)
                        );
                        totalDistance += distanceLastTripEnd;
                    }

                    // successfull permutation
                    vaBreaksDown = false;
                } catch (RuntimeException e) {
                    // vaBreaksDown => add more charging time to last chargingStation before indexOfBreakDown
                    // if there is no chargingstation set vaBreaksDownTo = false and set odr to amount of customertrips

                    try {
                        if (indexOfBreakDown == 0) {
                            vaBreaksDown = false;
                            odr = trips.stream().filter(t -> !isChargingTrip(t)).collect(Collectors.toList()).size();
                        } else {
                            for (int i = indexOfBreakDown - 1; i >= 0; i--) {
                                if (isChargingTrip(trips.get(i))) {
                                    trips.get(i).setChargingTime(trips.get(i).getChargingTime() + BATTERY_THRESHHOLD * COMPLETE_CHARGING_TIME);
                                } else if (i == 0) {
                                    vaBreaksDown = false;
                                    odr = trips.stream().filter(t -> !isChargingTrip(t)).collect(Collectors.toList()).size();
                                }
                            }
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        System.out.println("Caught exception");
                    }
                }
            }

//            Double totalDistance2 = calculateTotalDistance(trips);


            List<Trip> resultTrip = trips;
            /**
             * After the trip list has been simulated and the vehicle has not broken down, calculate the actual possible
             * charging times. This is only done at this point, as the trip list can change if the strategy is equal to
             * IGNORE_CUSTOMER and therefore also the possible landing times.
             * If a customer could not be reached either way and a charging station is scheduled in the trip list, the
             * calculation would result in the charging time being 0 seconds and the charging trip would still be run.
             **/
            if (!vaBreaksDown) {
                resultTrip = setChargingTimeForMultipleChargingStations(copy);
            }

            Double battAfterAll = calculateBatteryLevel(copy);
            try {

                // new 22.08
                if (battAfterAll < BATTERY_THRESHHOLD) {
                    // add more charging time to last charging station in triplist

                    boolean additionalChargingTimeAdded = false;
                    Double additionalChargingTime = 0.0;
                    Integer indexOfChargingTrip = -1;
                    for (int i = copy.size() - 1; i >= 0; i--) {
                        if (isChargingTrip(trips.get(i))) {
                            Double diffToBattyThresh = BATTERY_THRESHHOLD - battAfterAll;
                            additionalChargingTime = diffToBattyThresh * COMPLETE_CHARGING_TIME;
                            copy.get(i).setChargingTime(trips.get(i).chargingTime + additionalChargingTime);
                            additionalChargingTimeAdded = true;
                            indexOfChargingTrip = i;
                        }
                    }

                    // update odr values
                    if (additionalChargingTimeAdded) {
                        for (int i = indexOfChargingTrip + 1; i < copy.size() - 1; i++) {
                            // TODO: CHECK IF THIS SOLUTION IS APPLICABLE
                            // WaitingTimeOfEachTrip is smaller than copy size if vehicle breaks down
                            if (waitingTimeOfEachTrip.size() > i) {
                                if (odr((waitingTimeOfEachTrip.get(i) + additionalChargingTime), copy.get(i))) {
                                    odr++;
                                }
                            }
                        }
                    }

                }

                battAfterAll = calculateBatteryLevel(copy);
                // ToDo: Check if battery threshhold is undercut, if so add charging time to last charging trip (update odr)

            } catch (Exception e) {
                e.printStackTrace();
            }

            Double batteryLevelAfterTrips = vaBreaksDown ? 0.0 : battAfterAll;
            return new Metrics(
                    odr,
                    minBatteryLevel,
                    resultTrip,
                    batteryLevelAfterTrips,
                    totalDistance,
                    vaBreaksDown
            );

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught exception");
            return null;
        }
    }

    private Metrics simulateTripListAndCalculateMetricsOld(List<Trip> trips, Strategy strategy) {
        /**
         * Explanation:
         * Step 1: Set charging time for each chargingStation to minium charging time to ensure that each charging trip
         * is worthwhile.
         *
         * Step 2: Simulate tripList, determine Trips where THETA is exceeded and make a copy of the tripList without
         * these trips, if strategy is equal to IGNORE_CUSTOMER. If the Agent breaks down, stop the simulation.
         *
         * Step 3: Calculate real charging time for trips (without the trips where theta is exceeded)
         *
         * Step 4: Calculate battery level after all trips
         */
        boolean vaBreaksDown = false;

        /** Step 1
         * Set all charging times to MIN_CHARGING_TIME. IFthis increases the ODR, then ignore the trip(s) that increase
         * the ODR, if the strategy is equal to IGNORE_CUSTOMER. Then calculate the MaximumWaitingTime and adjust the
         * charging times accordingly.
         */
        setMinChargingTimeForAllChargingStations(trips);

        // Step 2
        List<Trip> copy = new ArrayList<>(trips);

        // get total distance driven for trip list
        // count charging trips and add battery level
        BatteryModel battery = new BatteryModel();
        battery.setMyChargestate(this.batteryLevel);
        Location vaLocation = this.currentVALocation;

        double totalDistance = 0.0;
        double minBatteryLevel = 1.0;
        double totalTravelTimeSeconds = TIME_UNTIL_CURRENT_ACTION_IS_DONE;
        int odr = 0;
        try {
            // Drive to start of first trip
            double distance = roundedLocationDistanceBetween(vaLocation, trips.get(0).startPosition);
            minBatteryLevel = Math.min(
                    minBatteryLevel,
                    makeTripAndDischargeBattery(battery, distance)
            );
            totalDistance += distance;
            Trip firstTrip = trips.get(0);

            vaLocation = firstTrip.startPosition;
            totalTravelTimeSeconds += SchedulerUtils.calculateTravelTime(distance, DRIVING_SPEED);

            if (odr(totalTravelTimeSeconds, firstTrip)) {
                odr++;
                if (strategy.equals(Strategy.IGNORE_CUSTOMER)) {
                    copy.remove(firstTrip);
                    totalDistance -= distance;
                }
            }

            // ToDo: Beim Löschen in Copy verschieben sich die Indices?
            for (int i = 0; i < trips.size() - 1; i++) {
                Trip currentTrip = trips.get(i);
                Trip nextTrip = trips.get(i + 1);

                // Case Charging Trip
                if (isChargingTrip(currentTrip)) {
                    double chargingTime = currentTrip.getChargingTime();
                    chargeBattery(battery, chargingTime);
                    totalTravelTimeSeconds += chargingTime;

                    double distanceNextTripStart = roundedLocationDistanceBetween(vaLocation, nextTrip.startPosition);
                    minBatteryLevel = Math.min(
                            minBatteryLevel,
                            makeTripAndDischargeBattery(battery, distanceNextTripStart)
                    );
                    totalDistance += distanceNextTripStart;
                    vaLocation = nextTrip.startPosition;
                    totalTravelTimeSeconds += SchedulerUtils.calculateTravelTime(distanceNextTripStart, DRIVING_SPEED);
                    if (odr(totalTravelTimeSeconds, nextTrip)) {
                        odr++;
                        if (strategy.equals(Strategy.IGNORE_CUSTOMER)) {
                            copy.remove(nextTrip);
                            totalDistance -= distanceNextTripStart;
                        }
                    }
                } else {
                    // Case Customer Trip
                    double distanceCurrentTripEnd = roundedLocationDistanceBetween(vaLocation, currentTrip.endPosition);
                    minBatteryLevel = Math.min(
                            minBatteryLevel,
                            makeTripAndDischargeBattery(battery, distanceCurrentTripEnd)
                    );
                    totalDistance += distanceCurrentTripEnd;
                    vaLocation = currentTrip.endPosition;

                    // drive to startposition of next trip
                    double distanceNextTripStart = roundedLocationDistanceBetween(vaLocation, nextTrip.startPosition);
                    totalDistance += distanceNextTripStart;
                    minBatteryLevel = Math.min(
                            minBatteryLevel,
                            makeTripAndDischargeBattery(battery, distanceNextTripStart)
                    );
                    vaLocation = nextTrip.startPosition;
                    totalTravelTimeSeconds += SchedulerUtils.calculateTravelTime(
                            distanceCurrentTripEnd + distanceNextTripStart,
                            DRIVING_SPEED
                    );
                    if (odr(totalTravelTimeSeconds, nextTrip)) {
                        odr++;
                        if (strategy.equals(Strategy.IGNORE_CUSTOMER)) {
                            copy.remove(nextTrip);
                            totalDistance -= distanceCurrentTripEnd + distanceNextTripStart;
                        }
                    }
                }

                if (i == trips.size() - 2 && nextTrip.getTripType().equals("CustomerTrip")) {
                    // drive to end position of last trip
                    double distanceLastTripEnd = roundedLocationDistanceBetween(vaLocation, nextTrip.endPosition);
                    minBatteryLevel = Math.min(
                            minBatteryLevel,
                            makeTripAndDischargeBattery(battery, distanceLastTripEnd)
                    );
                    totalDistance += distanceLastTripEnd;
                    vaLocation = nextTrip.endPosition;
                }
            }

            // Case triplist consists only of one trip
            if (trips.size() == 1 && !isChargingTrip(trips.get(0))) {
                double distanceLastTripEnd = roundedLocationDistanceBetween(vaLocation, trips.get(0).endPosition);
                minBatteryLevel = Math.min(
                        minBatteryLevel,
                        makeTripAndDischargeBattery(battery, distanceLastTripEnd)
                );
                totalDistance += distanceLastTripEnd;
            }
        } catch (RuntimeException e) {
            vaBreaksDown = true;
        }

        List<Trip> resultTrip = trips;
        /**
         * After the trip list has been simulated and the vehicle has not broken down, calculate the actual possible
         * charging times. This is only done at this point, as the trip list can change if the strategy is equal to
         * IGNORE_CUSTOMER and therefore also the possible landing times.
         * If a customer could not be reached either way and a charging station is scheduled in the trip list, the
         * calculation would result in the charging time being 0 seconds and the charging trip would still be run.
         **/
        if (!vaBreaksDown) {
            resultTrip = setChargingTimeForMultipleChargingStations(copy);
        }
        Double batteryLevelAfterTrips = vaBreaksDown ? 0.0 : calculateBatteryLevel(copy);

        return new Metrics(
                odr,
                minBatteryLevel,
                resultTrip,
                batteryLevelAfterTrips,
                totalDistance,
                vaBreaksDown
        );
    }

    // ToDo: If the last trip is a charging trip the battery model can load until 100%
    // ToDo: What if after that a new trip will be added to the trip list? => The bike will load to long
    //  currentTrip is ChargingTrip and chargingEndTime needs to be minimized
    public Double calculateBatteryLevel(List<Trip> tripList) {
        // Step 1: Get total distance between all trips and discharge battery
        if (tripList.size() == 0) { return this.batteryLevel; }
        List<Location> allLocation = tripList.stream()
                .map(t -> {
                    if (isChargingTrip(t)) { return Arrays.asList(t.startPosition); }
                    return Arrays.asList(t.startPosition, t.endPosition);
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
        Double totalDistance = roundedLocationDistanceBetween(this.currentVALocation, allLocation.get(0));
        for (int i=0; i < allLocation.size() - 1; i++) {
            totalDistance += roundedLocationDistanceBetween(
                    allLocation.get(i),
                    allLocation.get(i + 1)
            );
        }
        BatteryModel model = new BatteryModel();
        model.setMyChargestate(this.batteryLevel);
        model.discharge(totalDistance, 0, false);


        // Step 2: Get total charging time
        Double totalChargingTime = tripList.stream()
                .filter(t -> isChargingTrip(t))
                .map(t -> t.getChargingTime())
                .collect(Collectors.summingDouble(Double::doubleValue));

        // Step 3: Charge battery
        model.charge(totalChargingTime);

        return model.getMyChargestate() <= 0 ? 0 : model.getMyChargestate();
    }

    /**
     * Determines whether a customer does not take their trip.
     *
     * @param travelTime
     * @param trip
     * @return
     */
    private boolean odr(double travelTime, Trip trip) {
        if (isChargingTrip(trip)) {
            return false;
        }
        double currentWaitingTimeOfTrip = SchedulerUtils.calculateWaitingTime(trip.bookingTime, this.simulationTime);
        double totalWaitingTime = travelTime + currentWaitingTimeOfTrip;

        return totalWaitingTime > THETA ? true : false;
    }

    private void chargeBattery(BatteryModel battery, double chargingTime) {
        battery.charge(chargingTime);
    }

    private double makeTripAndDischargeBattery(BatteryModel battery, double distance) {
        battery.discharge(distance, 0, false);
        if (battery.my_chargestate <= 0.0) {
            throw new RuntimeException();
        }
        return battery.my_chargestate;
    }

    private boolean isChargingTrip(Trip trip) {
        return trip.getTripType().equals("ChargingTrip");
    }

    private void setMinChargingTimeForAllChargingStations(List<Trip> tripList) {
        for (int i=0; i < tripList.size(); i++) {
            if (isChargingTrip(tripList.get(i))) {
                tripList.get(i).setChargingTime(MIN_CHARGING_TIME);
            }
        }
    }

    private List<Trip> setChargingTimeForMultipleChargingStations(List<Trip> tripList) {

        // If there is no charging station in the permutation skip this method
        // TripList can be empty if the permutation previously consisted only of CustomerTrips that cannot be reached
        // in time
        if (tripList.stream().filter(t -> isChargingTrip(t)).count() == 0 || tripList.size() == 0) {
            return tripList;
        }
        // all trips are removed from previous function => permutation is bad
        if (tripList.stream().filter(t -> isChargingTrip(t)).collect(Collectors.toList()).size() == tripList.size()) {
            // Results in the rating of this permutation being 0
            tripList.get(0).setChargingTime(0.0);
        }

        Location currentLocation = this.currentVALocation;

        // Step 1: Calculate travel time to each customer trip
        List<Double> distances = new ArrayList<>();
        // calculate travel time to start position of first trip
        double distance = roundedLocationDistanceBetween(currentLocation, tripList.get(0).startPosition);
        currentLocation = tripList.get(0).startPosition;
        for (int i = 0; i < tripList.size(); i++) {
            Trip currentTrip = tripList.get(i);
            Trip nextTrip = null;
            if (!(i == tripList.size() - 1)) {
                nextTrip = tripList.get(i + 1);
            }
            if (nextTrip == null) {
                distances.add(distance);
                break;
            }

            if (isChargingTrip(currentTrip)) {
                distances.add(0.0);
                distance += roundedLocationDistanceBetween(currentLocation, nextTrip.startPosition);
                currentLocation = nextTrip.startPosition;
            } else {
                distances.add(distance);

                distance += roundedLocationDistanceBetween(currentLocation, currentTrip.endPosition);
                currentLocation = currentTrip.endPosition;

                distance += roundedLocationDistanceBetween(currentLocation, nextTrip.startPosition);
                currentLocation = nextTrip.startPosition;
            }
        }

        // convert travel distance in travel time
        List<Double> travelTimes = distances.stream().map(d -> SchedulerUtils.calculateTravelTime(d, DRIVING_SPEED)).collect(Collectors.toList());



        //TODO ADD BOOKING TIME DIFFERENCES TO EACH ENTRY
//        for (int j = 0; j < waitingTimesOfEachTrip.size(); j++) {
//            if (waitingTimesOfEachTrip.get(j) != 0.0) {
//                long diffToBookingTime = SchedulerUtils.calculateWaitingTime(tripList.get(j).bookingTime, simulationTime);
//                waitingTimesOfEachTrip.set(j, waitingTimesOfEachTrip.get(j) + diffToBookingTime);
//            }
//        }

        // convert travel distance in travel time
        List<Integer> indecesOfChargingStations = getIndecesOfChargingStation(tripList);

        // Step 3: Iterate through charging stations
        Integer maxIndex = -1;
        for (Integer index: indecesOfChargingStations) {
            if (index == tripList.size() - 1) {
                // charging trip is at last position => -1 means, load until a new CustomerTrip is available
                tripList.get(index).setChargingTime(-1.0);
            } else if (maxIndex < index) {
                    // Step 4: Specify all trips that are after current charging station
                    List<Double> waitingTimeAfterChargingStation = travelTimes.subList(index, tripList.size());
                    // Step 5: Get maximum waitingtime of those trips and calculate charging time through difference to theta
                    Double maxWaitingTime = Collections.max(waitingTimeAfterChargingStation);

                    // TODO: was macht index + 1?
                    maxIndex = maxIndex(waitingTimeAfterChargingStation) + index + 1;
                    Double additionalChargingTime = THETA - maxWaitingTime > 0 ? THETA - maxWaitingTime : 0;
                    Double totalChargingTime = tripList.get(index).getChargingTime() + additionalChargingTime;
                    tripList.get(index).setChargingTime(roundToOneDecimalPlace(totalChargingTime));

                    // Step 6: Add charging time to all trips after the current charging station and get back to Step 3
                    for (int i = index + 1; i < waitingTimeAfterChargingStation.size(); i++) {
                        waitingTimeAfterChargingStation.set(i, waitingTimeAfterChargingStation.get(i) + additionalChargingTime);
                    }
            }
        }

        return tripList;
    }

    private List<Integer> getIndecesOfChargingStation(List<Trip> tripList) {
        List<Integer> result = new ArrayList<>();
        for (int i=0; i < tripList.size(); i++) {
            if (isChargingTrip(tripList.get(i))) {
                result.add(i);
            }
        }
        return result;
    }

    private Integer countChargingStations(int i, List<Integer> chargingStationIndeces) {
        Integer result = 0;
        for (Integer index: chargingStationIndeces) {
            if (i > index) {
                result++;
            }
        }
        return result;
    }

    private Integer maxIndex(List<Double> waitingTime) {
        double max = 0.0;
        Integer index = -1;
        for (int i=0; i < waitingTime.size(); i++) {
            if (max < waitingTime.get(i)) {
                max = waitingTime.get(i);
                index = i;
            }
        }
        return index;
    }

    private Double roundToOneDecimalPlace(Double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private List<Trip> getPermutationWithHighestRating(List<Double> allRatings, List<List<Trip>> permutations) {
        double maxWert = Collections.max(allRatings);
        int index = 0;
        for (int i = 0; i < allRatings.size(); i++) {
            if (allRatings.get(i) == maxWert) {
                index = i;
                break;
            }
        };
        return permutations.get(index);
    }

    private int idx(List<Double> allRatings, List<List<Trip>> permutations) {
        double maxWert = Collections.max(allRatings);
        int index = 0;
        for (int i = 0; i < allRatings.size(); i++) {
            if (allRatings.get(i) == maxWert) {
                index = i;
                break;
            }
        };
        return index;
    }

    private Double roundedLocationDistanceBetween(Location a, Location b) {
//        return (double) Math.round(
//                Location.distanceBetween(a, b)
//        );
        return Location.distanceBetween(a, b) * DISTANCE_FACTOR;
    }

    private List<Trip> createDeepCopyOfSubset(List<Trip> chargingTrips) {
        List<Trip> result = new ArrayList<>();
        for (Trip chargingTrip: chargingTrips) {
            result.add(
                    new Trip(
                            chargingTrip.tripID,
                            chargingTrip.tripType,
                            chargingTrip.startPosition,
                            "NotStarted")
            );
        }
        return result;
    }


    // SETTER

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public void setCurrentVALocation(Location currentVALocation) {
        this.currentVALocation = currentVALocation;
    }

    public void setSimulationTime(LocalDateTime simulationTime) {
        this.simulationTime = simulationTime;
    }

    public void setTHETA(Double THETA) {
        this.THETA = THETA;
    }

    public void setTIME_UNTIL_CURRENT_ACTION_IS_DONE(Double TIME_UNTIL_CURRENT_ACTION_IS_DONE) {
        this.TIME_UNTIL_CURRENT_ACTION_IS_DONE = TIME_UNTIL_CURRENT_ACTION_IS_DONE;
    }
}