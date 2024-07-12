package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.BatteryModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.enums.Strategy;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.metrics.Metrics;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.metrics.MinMaxMetricsValues;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.util.PrinterUtil;
import io.github.agentsoz.util.Location;

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
    Double CHARGING_THRESHHOLD = 0.15;
    // 12600 seconds for 0% - 100% (400 Watt Battery)
    Double COMPLETE_CHARGING_TIME = 12600.0;
    Double MIN_CHARGING_TIME = COMPLETE_CHARGING_TIME * CHARGING_THRESHHOLD;
    Double BATTERY_THRESHHOLD = 0.9;
    String agentId;

    /**
     * Current batteryLevel is necessary to be able to evaluate the permutations and is necessary to determine whether
     * the battery level is sufficient or whether the vehicle will break down
     */
    public GreedyScheduler(List<Location> chargingStations,
                           double batteryLevel,
                           Location currentVaLocation,
                           LocalDateTime simulationTime,
                           Double drivingSpeed, Double theta,
                           String agentID,
                           Double timeTillCurrentActionIsDone) {
        chargingTrips = convertChargingStationsToTrips(chargingStations);
        this.batteryLevel = batteryLevel;
        this.currentVALocation = currentVaLocation;
        this.simulationTime = simulationTime;
        this.DRIVING_SPEED = drivingSpeed;
        this.THETA = theta;
        this.agentId = agentID;
        this.TIME_UNTIL_CURRENT_ACTION_IS_DONE = timeTillCurrentActionIsDone;
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
        System.out.println("TripList size: " +  allTrips.size() + " - " + allTrips.stream().map(t -> t.getTripID()).collect(Collectors.toList()));

        // ToDo: Is there a solution under all cirumstances? What happens if all rating results are 0? Is that possible?
        // ToDo: Charging trip counter needs to be incremented for every inserted charging trip
        List<List<Trip>> permutations = getAllPermutations(allTrips);

        System.out.println("Size of permutations: " + permutations.size());

        MetricsValues metricsValues = getAllMetricsValuesForEachPermutation(permutations, strategy);
        // Determine the minimum and maximum values for each metric. These are used for normalization right after.
        MinMaxMetricsValues minMaxMetricsValues = getMinMaxMetricsValues(metricsValues);
        List<List<Number>> normalizedMetricsValues = normalizeValues(metricsValues, minMaxMetricsValues);

//        printerUtil.metrics(permutations, metricsValues.getAllOdrValues(), metricsValues.getAllTotalDistances(),
//                metricsValues.getAllMinBatteryLevelValues(), metricsValues.getAllBatteryLevelValuesAfterAllTrips(),
//                metricsValues.getAllStopsValues(), metricsValues.getAllChargingTimes());

        // ToDo: Optimize rating function
        List<Double> ratings = rating(
                normalizedMetricsValues,
                metricsValues.getAllTripsWithCharingTimes(),
                metricsValues.getAllVaBreaksDownValues()
        );
        int idx = idx(ratings, metricsValues.getAllTripsWithCharingTimes());

        printerUtil.metricsOfIndex(idx, permutations, metricsValues.getAllOdrValues(), metricsValues.getAllTotalDistances(),
                metricsValues.getAllMinBatteryLevelValues(), metricsValues.getAllBatteryLevelValuesAfterAllTrips(),
                metricsValues.getAllStopsValues(), metricsValues.getAllChargingTimes());
//        printerUtil.endScheduler(ratings);

        List<Trip> result = permutations.get(idx);

        System.out.println("IDs: " + result.stream().map(t -> t.getTripID()).collect(Collectors.toList()));

        /**
         * Kann genutzt werden, um die Ausgabe in eine CSV zu kopieren mit folgenden Spalten:
         * IDs, ODR, Total Distance, MinBattery, BatteryAfterAllTrips, Stops, ChargingTimes, BatteryLevel, Rating
         **/
        // printerUtil.csv(permutations, metricsValues.getAllOdrValues(), metricsValues.getAllTotalDistances(),
        //         metricsValues.getAllMinBatteryLevelValues(), metricsValues.getAllBatteryLevelValuesAfterAllTrips(),
        //         metricsValues.getAllStopsValues(), metricsValues.getAllChargingTimes(), batteryLevel, ratings);

        System.out.println("End time with schedule time: " + ((new Date(System.currentTimeMillis()).getTime() - startTime.getTime()) / 1000));

        return result;
    }

    public List<List<Number>> normalizeValues(MetricsValues metricsValues, MinMaxMetricsValues minMaxMetricsValues) {
        List<List<Number>> normalized = new ArrayList<>();
        for (int i = 0; i < metricsValues.getAllOdrValues().size(); i++) {
            List<Number> intermediate = new ArrayList<>();

            // Normalize ODR
            double normalizedOdr;
            if (minMaxMetricsValues.getMinOdr() == minMaxMetricsValues.getMaxOdr()) {
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
            Metrics metrics = simulateTripListAndCalculateMetrics(permutation, strategy);

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

    // generation of all permutations take approx 4 seconds for n = 10, 40 sec. for n = 11
    public List<List<Trip>> getAllPermutationsOfATripList(List<Trip> tripList) {
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
        List<Double> chargingTime;
        if (tendencyToCharge) {
            chargingTime = tripsWithChargingTime.stream()
                    .map(listTrip -> listTrip.stream()
                            .map(t -> {
                                if (isChargingTrip(t)) {
                                    return t.getChargingTime();
                                } else {
                                    return 0.0;
                                }
                            })
                            .reduce(0.0, Double::sum))
                    .collect(Collectors.toList());
        } else {
            chargingTime = new ArrayList<>();
        }

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
        System.out.println("Tendency to charge: " + tendencyToCharge);
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

                    return rating;
                }).collect(Collectors.toList());

        return ratings;
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

    private long calculateWaitingTime(Trip trip) {
        return Duration
                .between(trip.bookingTime, this.simulationTime)
                .getSeconds();
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
            totalTravelTimeSeconds += calculateTravelTime(distance);

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
                    totalTravelTimeSeconds += calculateTravelTime(distanceNextTripStart);
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
                    totalTravelTimeSeconds += calculateTravelTime(distanceCurrentTripEnd + distanceNextTripStart);
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
        double currentWaitingTimeOfTrip = calculateWaitingTime(trip);
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

    private double calculateTravelTime(Double distance) {
        return ((distance / 1000) / DRIVING_SPEED) * 60 * 60;
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
        List<Double> travelTimes = distances.stream().map(d -> calculateTravelTime(d)).collect(Collectors.toList());
        List<Integer> indecesOfChargingStations = getIndecesOfChargingStation(tripList);

        // Step 2: Add travel time to current waiting time of each trip and MIN_CHARGING_TIME for each preceeding
        // charging station
        for (int i=0; i < travelTimes.size(); i++) {
            Trip currentTrip = tripList.get(i);
            if (!isChargingTrip(currentTrip)) {
                double travelAndWaitingTime = travelTimes.get(i) + calculateWaitingTime(currentTrip);
                Integer chargingStationCounter = countChargingStations(i, indecesOfChargingStations);
                // add MIN_CHARGING_TIME for every charging station on the way
                travelAndWaitingTime += chargingStationCounter * MIN_CHARGING_TIME;
                travelTimes.set(i, travelAndWaitingTime);
            }
        }


        // Step 3: Iterate through charging stations
        Integer maxIndex = -1;
        for (Integer index: indecesOfChargingStations) {
            if (index == tripList.size() - 1) {
                // charging trip is at last position => -1 means, load until a new CustomerTrip is available
                tripList.get(index).setChargingTime(-1.0);
            } else if (maxIndex < index) {
                    // Step 4: Specify all trips that are after current charging station
                    List<Double> waitingTimeAfterChargingStation = travelTimes.subList(index, tripList.size());
                    // Step 5: Get maximum of those trips and calculate charging time through difference to theta
                    Double maxWaitingTime = Collections.max(waitingTimeAfterChargingStation);
                    maxIndex = maxIndex(waitingTimeAfterChargingStation) + index + 1;
                    Double additionalChargingTime = THETA - maxWaitingTime > 0 ? THETA - maxWaitingTime : 0;
                    Double totalChargingTime = tripList.get(index).getChargingTime() + additionalChargingTime;
                    tripList.get(index).setChargingTime(roundToOneDecimalPlace(totalChargingTime));

                    // Step 6: Add charging time to all trips after the current charging station and get back to Step 3
                    for (int i = index + 1; i < travelTimes.size(); i++) {
                        travelTimes.set(i, travelTimes.get(i) + additionalChargingTime);
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
        return (double) Math.round(
                Location.distanceBetween(a, b)
        );
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