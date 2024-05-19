package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.BatteryModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.util.Location;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GreedyScheduler {

    List<Trip> chargingTrips;
    double batteryLevel;
    Location currentVALocation;
    LocalDateTime simulationTime;
    Double THETA;
    Double DRIVING_SPEED;
    // Determination of the load increase by 20% in 2 Minutes
    // A charging trip only makes sense if you can charge for at least this time
    // Charges by 10% in 1 minute
    Double CHARGING_THRESHHOLD = 0.2;
    Double MIN_CHARGING_TIME = CHARGING_THRESHHOLD * 10;

    /**
     * Current batteryLevel is necessary to be able to evaluate the permutations and is necessary to determine whether
     * the battery level is sufficient or whether the vehicle will break down
     */
    public GreedyScheduler(List<Location> chargingStations, double batteryLevel, Location currentVaLocation, LocalDateTime simulationTime, Double drivingSpeed, Double theta) {
        chargingTrips = convertChargingStationsToTrips(chargingStations);
        this.batteryLevel = batteryLevel;
        this.currentVALocation = currentVaLocation;
        this.simulationTime = simulationTime;
        this.DRIVING_SPEED = drivingSpeed;
        this.THETA = theta;
    }

    /**
     * Returns a list of trips with optimal order in which all n! possible permutations have been considered and the
     * best permutation is determined with the help of an evaluation function
     *
     * @return List of scheduled trips
     */
    public List<Trip> greedySchedule(List<Trip> allTrips) {
        /**
         * Requirements:
         *
         * Position of ChargingStations
         * Start and Endposition of all Trips
         *
         * A function that validates the feasibility of a permutation (in relation to the battery level)
         *
         * A score for every permutation (higher score if the total distance is low)
         *
         * Time between trips until theta is exceeded (time that the charging trip including charging may take)
         * => Perhaps just a threshold that indicates when a charging trip is made,
         * e.g. if it is less than 5 minutes in total and more than Epsilon (e.g. 25% can be charged in this time).
         */

        System.out.println("Greedy Schedule startet");
        System.out.println("Current Simulation Time: " + this.simulationTime);
        System.out.println("Booking Time of first trip: " + allTrips.get(0).bookingTime);

        // Euclidian distance
        // double distance = Location.distanceBetween(location1, location2);

        // a permutation consists of trips and chargingTrips
        // Trip chargingTrip = new Trip(tripID, "ChargingTrip", getNextChargingStation(), "NotStarted");
        // ToDo: Charging trip counter needs to be incremented for every inserted charging trip

        List<List<Trip>> permutations = getAllPermutations(allTrips);

        //  ToDo: Berechne Metriken bevor der Iteration um korrekte min und max values zu haben
        List<Integer> allOdrValues = new ArrayList<>();
        List<Double> allBatteryLevelValues = new ArrayList<>();
        List<Double> allTotalDistances = new ArrayList<>();
        List<Integer> allStopsValues = new ArrayList<>();
        for (int i = 0; i < permutations.size(); i++) {
            List<Trip> permutation = permutations.get(i);

            List<Number> odrAndMinBatteryLevel = validateBatteryLevelIsSufficientAndODR(permutation);
            if (odrAndMinBatteryLevel.contains(-1)) {
                allOdrValues.add(-1);
                allBatteryLevelValues.add(-1.0);
            } else {
                allOdrValues.add((Integer) odrAndMinBatteryLevel.get(0));
                allBatteryLevelValues.add((Double) odrAndMinBatteryLevel.get(1));
            }
            allTotalDistances.add(calculateTotalDistance(permutation));
            allStopsValues.add(permutation.size());
        }


        int minOdr = Integer.MAX_VALUE;
        for (int i = 0; i < permutations.size(); i++) {
            if (allOdrValues.get(i) >= 0) {
                minOdr = Math.min(minOdr, allOdrValues.get(i));
            }
        }
        int maxOdr = Collections.max(allOdrValues);
        double minTotalDistance = Collections.min(allTotalDistances);
        double maxTotalDistance = Collections.max(allTotalDistances);
        int minStops = Collections.min(allStopsValues);
        int maxStops = Collections.max(allStopsValues);
        double minBatteryLevel = Collections.min(allBatteryLevelValues);
        double maxBatteryLevel = Collections.max(allBatteryLevelValues);

        List<List<String>> ids = permutations
                .stream()
                .map(p -> p.stream().map(t -> t.getTripID()).collect(Collectors.toList()))
                .collect(Collectors.toList());
        System.out.println("PERMUTATIONS: " + ids);
        System.out.println("ODR: " + allOdrValues);
        System.out.println("TOTAL DISTANCES: " + allTotalDistances);
        System.out.println("BATTERY LEVELS: " + allBatteryLevelValues);
        System.out.println("STOPS: " + allStopsValues);

        List<Double> allRatings = new ArrayList<>();
        for (int i = 0; i < permutations.size(); i++) {
            allRatings.add(
                    rating(
                            allOdrValues.get(i),
                            allTotalDistances.get(i),
                            allStopsValues.get(i),
                            minOdr,
                            maxOdr,
                            minTotalDistance,
                            maxTotalDistance,
                            minStops,
                            maxStops,
                            allBatteryLevelValues.get(i),
                            minBatteryLevel,
                            maxBatteryLevel
                    )
            );
        }

        double minWert = Collections.min(allRatings);
        int index = 0;
        for (int i=0; i < allRatings.size(); i++) {
            if (allRatings.get(i) == minWert) {
                index = i;
                break;
            }
        }

        System.out.println("RATINGS: " + allRatings);

        return permutations.get(index);
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
            List<Trip> merged = Stream.concat(tripSubset.stream(), trips.stream()).collect(Collectors.toList());
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
     * @param allTrips
     * @return totalDistance
     */
    public double calculateTotalDistance(List<Trip> allTrips) {
        double totalDistance = Location.distanceBetween(allTrips.get(0).startPosition, currentVALocation);
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
            double distance = Location.distanceBetween(allLocations.get(i), allLocations.get(i + 1));
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

    public double rating(int odr, double totalDistance, int stops, int minOdr, int maxOdr, double minTotalDistance, double maxTotalDistance, int minStops, int maxStops, double batteryLevel, double minBatteryLevel, double maxBatteryLevel) {
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
        if (odr == -1) { return 1.0; }

        double normalizedOdr;
        if (minOdr == maxOdr) {
            normalizedOdr = 0;
        } else {
            normalizedOdr = normalize(odr, minOdr, maxOdr);
        }
        double normalizedTotalDistance = normalize(totalDistance, minTotalDistance, maxTotalDistance);
        double normalizedStops = normalize(stops, minStops, maxStops);

        // ToDo: Strafe für niedrigeren Akkustand bzw. Belohnung für höheren Akkustand
        double normalizedBatteryLevel = normalize(batteryLevel, minBatteryLevel, maxBatteryLevel);
        double invertedNormalizedBatteryLevel = 1 - normalizedBatteryLevel;
        // Je größer alles desto schlechter => d.h. 0.0 ist der beste Wert
        double totalDistanceFraction = 0.40;
        double odrFraction = 0.30;
        double batteryFraction = 0.20;
        double stopsFraction = 0.10;

        return odrFraction * normalizedOdr
                + totalDistanceFraction * normalizedTotalDistance
                + stopsFraction * normalizedStops
                + batteryFraction * invertedNormalizedBatteryLevel;
    }

    private double normalize(double value, double min, double max) {
        return (value - min) / (max - min);
    }

    private double normalize(int value, int min, int max) {
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

    private double calculateTravellingTime(double metersDriven) {
        return (metersDriven / 1000) / DRIVING_SPEED;
    }

    /**
     * Determines whether the battery level is sufficient. If the trip is possible, a list consisting of odr and the
     * minimum battery level during all trips is returned. If the trip is not possible a list of -1, -1 is returned.
     * @param trips
     * @return odr (if battery is sufficient otherwise returns -1), minBatteryLevel (if battery is sufficient otherwise returns -1)
     */
    private List<Number> validateBatteryLevelIsSufficientAndODR(List<Trip> trips) {
        // get total distance driven for trip list
        // count charging trips and add battery level
        BatteryModel battery = new BatteryModel();
        battery.setMyChargestate(this.batteryLevel);
        Location vaLocation = this.currentVALocation;

        double minBatteryLevel = 1.0;

        double totalTravelTime = 0.0;
        int odr = 0;
        try {
            double distance = Location.distanceBetween(vaLocation, trips.get(0).startPosition);
            minBatteryLevel = Math.min(
                    minBatteryLevel,
                    makeTripAndDischargeBattery(battery, distance)
            );
            Trip firstTrip = trips.get(0);
            if (firstTrip.getTripType().equals("ChargingTrip")) {
                battery.setMyChargestate(battery.getMyChargestate() + CHARGING_THRESHHOLD);
            }
            vaLocation = firstTrip.startPosition;
            totalTravelTime += calculateTravelTime(distance);
            if (odr(totalTravelTime, firstTrip)) { odr++; };

            for (int i = 0; i < trips.size() - 1; i++) {
                Trip currentTrip = trips.get(i);
                Trip nextTrip = trips.get(i + 1);

                // Case Charging Trip
                if (currentTrip.getTripType().equals("ChargingTrip")) {
                    battery.setMyChargestate(battery.getMyChargestate() + CHARGING_THRESHHOLD);

                    double distanceNextTripStart = Location.distanceBetween(vaLocation, nextTrip.startPosition);
                    minBatteryLevel = Math.min(
                            minBatteryLevel,
                            makeTripAndDischargeBattery(battery, distanceNextTripStart)
                    );
                    vaLocation = nextTrip.startPosition;
                    totalTravelTime += calculateTravelTime(distanceNextTripStart);
                    if (odr(totalTravelTime, nextTrip)) { odr++; };
                } else {
                    // Case Customer Trip
                    // drive to endposition
                    double distanceCurrentTripEnd = Location.distanceBetween(vaLocation, currentTrip.endPosition);
                    minBatteryLevel = Math.min(
                            minBatteryLevel,
                            makeTripAndDischargeBattery(battery, distanceCurrentTripEnd)
                    );
                    vaLocation = currentTrip.endPosition;

                    // drive to startposition of next trip
                    double distanceNextTripStart = Location.distanceBetween(vaLocation, nextTrip.startPosition);
                    minBatteryLevel = Math.min(
                            minBatteryLevel,
                            makeTripAndDischargeBattery(battery, distanceNextTripStart)
                    );
                    vaLocation = nextTrip.startPosition;
                    totalTravelTime += calculateTravelTime(distanceCurrentTripEnd + distanceNextTripStart);
                    if (odr(totalTravelTime, nextTrip)) { odr++; };
                }


            }
        } catch (RuntimeException e) {
            return Collections.singletonList(-1);
        }
        return Arrays.asList(odr, minBatteryLevel);
    }

    /**
     * Determines whether a customer does not take their trip.
     * @param travelTime
     * @param trip
     * @return
     */
    private boolean odr(double travelTime, Trip trip) {
        if (trip.getTripType().equals("ChargingTrip")) { return false; }
        double currentWaitingTimeOfTrip = calculateWaitingTime(trip);
        double totalWaitingTime = travelTime + currentWaitingTimeOfTrip;

        return totalWaitingTime > THETA ? true : false;
    }

    private double makeTripAndDischargeBattery(BatteryModel battery, double distance) {
        battery.discharge(distance, 0);

        if (battery.my_chargestate <= 0.0) {
            throw new RuntimeException();
        }

        return battery.my_chargestate;
    }

    /**
     * Calculates the maximum possible loading time. Based on the time required for the VA to reach the
     * ChargingStation and the longest waiting customer.
     * @param chargingTrip
     * @param nextTrip
     * @param currentMaxWaitingTime
     * @return chargingTime
     */
    private double calculateChargingTime(Trip chargingTrip, Trip nextTrip, double currentMaxWaitingTime) {
        double vaDistanceToChargingStation = Location.distanceBetween(
                this.currentVALocation,
                chargingTrip.startPosition
        );
        double distanceChargingStationAndNexTrip = Location.distanceBetween(
                chargingTrip.startPosition,
                nextTrip.startPosition
        );
        double totalDistance = vaDistanceToChargingStation + distanceChargingStationAndNexTrip;

        double travelTimeForChargingTrip = calculateTravellingTime(totalDistance);

        // ToDo: Wenn Ergebnis < 0, dann ist Tankfahrt nicht möglich
        // Case 1: Kunde wird durch Tankfahrt nicht erreicht
        // Case 2: Kunde wird auch ohne Tankfahrt nicht erreicht
        // Bestimme, ob alle Kuden ohne Tankfahrt erreicht werden und Batteriestand reicht?
        return THETA - currentMaxWaitingTime - travelTimeForChargingTrip;
    }

    private double calculateTravelTime(Double distance) {
        return ((distance / 1000) / DRIVING_SPEED) * 60 * 60;
    }

    private void caclulateChargingTimeForEachChargingStation(List<Trip> allTrips) {
        List<Double> distancesBetweenTrips = new ArrayList<>();
        distancesBetweenTrips.add(Location.distanceBetween(
                this.currentVALocation,
                allTrips.get(0).startPosition
        ));
        for (int i = 0; i < allTrips.size() - 1; i++) {
            distancesBetweenTrips.add(Location.distanceBetween(
                    allTrips.get(i).endPosition,
                    allTrips.get(i + 1).startPosition
            ));
        }

        // wie lange wartet jemand currSimTime - bookingTime?
        // bestimme die Stellen an denen Tankfahrten stehen
        List<Integer> positionOfChargingTrips = allTrips.stream()
                .map(t -> {
                    if (t.getTripID().equals("ChargingTrip")) {
                        return 1;
                    }
                    return 0;
                }).collect(Collectors.toList());

        // split trip list in teile und bestimme warte zeiten
        // derjenige mit der längsten Wartezeit ist am kritischsten

        // fange beim letzten knoten an und berechne WT durch: WT(letzterKnoten) + Streckenzeit bis vorletzten Knoten
        // Vergleiche WT des vorletzten Knoten mit dem vorvorletztzen Knoten und nehme das Maximum für die weitere
        // Berechnung


        // ToDo: Ziel gib Array mit Wartzeiten an, falls ChargingTrip dann Ladezeit?
        // Aus 3 Trips (Trip 2 ist Charging) wird [3.0, 4.0, 8.0]
        // Oder 2 Arrays Ladezeiten Array und Wartezeit Array => Je kleiner Summe von Wartezeit desto besser
        // Je größer Ladezeit desto bessser?
        double currentMaxWaitingTime = 0.0;
        for (int i = allTrips.size() - 1; i > 1; i--) {
            Trip currentTrip = allTrips.get(i);
            Trip previousTrip = allTrips.get(i - 1);

            if (previousTrip.getTripID().equals("ChargingTrip")) {
                // ToDo: Case currentMaxWaitingTime > 10 bzw. Theta was dann?
                double chargingTime = calculateChargingTime(previousTrip, currentTrip, currentMaxWaitingTime);

            }

            long waitingTime = calculateWaitingTime(currentTrip);

            // calc distance bewteen start of current to end of previous and end of previous to start of previous
            double distanceToPreviousTrip = Location.distanceBetween(
                    previousTrip.endPosition,
                    currentTrip.startPosition
            );
            distanceToPreviousTrip += Location.distanceBetween(
                    previousTrip.startPosition,
                    previousTrip.endPosition
            );
            // Wie hoch ist die Geschwindigkeit des VA?
            // waiting time with distance between previous und current trip
            double timeToTravelToCurrentTrip = calculateTravellingTime(distanceToPreviousTrip);
            double totalWaitingTimeCurrentTrip = waitingTime + timeToTravelToCurrentTrip;

            double waitingTimePrevTrip = calculateWaitingTime(previousTrip);
            double maxWaitingTimeOfTrips = Math.max(waitingTimePrevTrip, totalWaitingTimeCurrentTrip);
            currentMaxWaitingTime = Math.max(currentMaxWaitingTime, maxWaitingTimeOfTrips);

        }
    }
}
