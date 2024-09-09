package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.GreedyScheduler;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.MetricsValues;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.enums.Strategy;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.metrics.MinMaxMetricsValues;
import io.github.agentsoz.util.Location;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GreedySchedulerTest extends TestCase {

    GreedyScheduler scheduler;
    GreedyScheduler schedulerSzenario1;

    List<Trip> trips;

    @BeforeEach
    public void setUp() {
        List<Location> chargingStations = Arrays.asList(
                new Location("1", 0.0, 1.0),
                new Location("2", 1.0, 1.0),
                new Location("3", 1.0, 0.0)
        );

        Location currentVaLocation = new Location("", 0.0, 0.0);

        scheduler = new GreedyScheduler(chargingStations, 0.4, null, null, null, null, "1", 0.0, null);

        Trip trip1 = new Trip(
                "1",
                "CustomerTrip",
                LocalDateTime.now(),
                new Location("", 0.0 ,0.0),
                new Location("", 0.0 ,1.0),
                "NotStarted"
        );
        Trip trip2 = new Trip(
                "2",
                "CustomerTrip",
                LocalDateTime.now(),
                new Location("", 0.0 ,1.0),
                new Location("", 1.0 ,1.0),
                "NotStarted"
        );
        Trip trip3 = new Trip(
                "3",
                "CustomerTrip",
                LocalDateTime.now(),
                new Location("", 1.0 ,0.0),
                new Location("", 0.0 ,0.0),
                "NotStarted"
        );

        trips = new ArrayList<>(Arrays.asList(trip1, trip2, trip3));
    }

    @Test
    public void testPermutations() {
        List<List<Trip>> result = scheduler.getAllPermutationsOfATripList(trips);

        List<List<String>> ids = result.stream()
                .map(list -> list.stream().map(Trip::getTripID).collect(Collectors.toList()))
                .collect(Collectors.toList());

        List<String> test1 = Arrays.asList("1", "2", "3");
        List<String> test2 = Arrays.asList("1", "3", "2");
        List<String> test3 = Arrays.asList("2", "1", "3");
        List<String> test4 = Arrays.asList("2", "3", "1");
        List<String> test5 = Arrays.asList("3", "2", "1");
        List<String> test6 = Arrays.asList("3", "1", "2");
        List<List<String>> compareObject = Arrays.asList(test1, test2, test3, test4, test5, test6);

        assertTrue(ids.containsAll(compareObject));
    }

    @Test
    public void testDistanceCalculations() {
        double totalDistance = scheduler.calculateTotalDistance(trips);
        assertEquals(3.0, totalDistance);
    }

    @Test
    public void testChargingTripSubsets() {
        List<List<Trip>> result = scheduler.getAllChargingTripSubsets();

        List<List<String>> ids = result.stream()
                .map(list -> list.stream().map(Trip::getTripID).collect(Collectors.toList()))
                .collect(Collectors.toList());

        List<String> test1 = Arrays.asList("CH1");
        List<String> test2 = Arrays.asList("CH2");
        List<String> test3 = Arrays.asList("CH3");
        List<String> test4 = Arrays.asList("CH1", "CH2");
        List<String> test5 = Arrays.asList("CH1", "CH3");
        List<String> test6 = Arrays.asList("CH2", "CH3");
        List<String> test7 = Arrays.asList("CH1", "CH2", "CH3");
        List<List<String>> compareObject = Arrays.asList(test1, test2, test3, test4, test5, test6, test7);

        assertTrue(ids.containsAll(compareObject));
    }

    @Test
    public void testGetAllPermutations() {
        List<List<Trip>> result = scheduler.getAllPermutations(trips);

        List<List<String>> ids = result.stream()
                .map(list -> list.stream().map(Trip::getTripID).collect(Collectors.toList()))
                .collect(Collectors.toList());

        for (List<String> id: ids) {
            System.out.println(id.toString());
        }

        // all permutation of 3 customer trips and 3 charging trips without consecutive charging trips and
        // the permutation of the trip list without charging trips
        assertEquals(438, result.size());
    }

    @Test
    public void testSzenario1() {
        // Szenario 1
        Location chargingStationLocation = new Location("", 1000.0, 1000.0);
        LocalDateTime startSimulationTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(8,0));
        Location currentVaLocationSzenario1 = new Location("VA", 0.0, 0.0);
        schedulerSzenario1 = new GreedyScheduler(
                Collections.singletonList(chargingStationLocation),
                1.0,
                currentVaLocationSzenario1,
                startSimulationTime.plusMinutes(1),
                6.0,
                600.0,
                "AP001",
                0.0,
                null
        );
        List<Trip> tripsSzenario1 = new ArrayList<>();
        Trip trip1Szenario1 = new Trip(
                null,
                "4",
                "CustomerTrip",
                LocalDateTime.now(),
                new Location("", 0.0 ,500.0),
                new Location("", 0.0 ,600.0),
                "NotStarted",
                startSimulationTime
        );
        Trip trip2Szenario1 = new Trip(
                null,
                "5",
                "CustomerTrip",
                LocalDateTime.now(),
                new Location("", 0.0 ,200.0),
                new Location("", 0.0 ,400.0),
                "NotStarted",
                startSimulationTime.plusMinutes(1)
        );
        tripsSzenario1.add(trip1Szenario1);
        tripsSzenario1.add(trip2Szenario1);

        List<Trip> result = schedulerSzenario1.greedySchedule(tripsSzenario1, Strategy.IGNORE_CUSTOMER);

    }

    @Test
    public void testSzenario1_1() {
        // Szenario 1
        Location chargingStationLocation = new Location("", 100.0, 0.0);
        LocalDateTime startSimulationTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(8,0));
        Location currentVaLocationSzenario1 = new Location("VA", 0.0, 0.0);
        schedulerSzenario1 = new GreedyScheduler(
                Collections.singletonList(chargingStationLocation),
                0.06,
                currentVaLocationSzenario1,
                startSimulationTime.plusMinutes(1),
                6.0,
                900.0,
                "1",
                0.0,
                null
        );
        List<Trip> tripsSzenario1 = new ArrayList<>();
        Trip trip1Szenario1 = new Trip(
                null,
                "1",
                "CustomerTrip",
                LocalDateTime.now(),
                new Location("", 0.0 ,500.0),
                new Location("", 0.0 ,600.0),
                "NotStarted",
                startSimulationTime
        );
        Trip trip2Szenario1 = new Trip(
                null,
                "2",
                "CustomerTrip",
                LocalDateTime.now(),
                new Location("", 0.0 ,200.0),
                new Location("", 0.0 ,400.0),
                "NotStarted",
                startSimulationTime.plusMinutes(1)
        );
        tripsSzenario1.add(trip1Szenario1);
        tripsSzenario1.add(trip2Szenario1);

        List<Trip> result = schedulerSzenario1.greedySchedule(tripsSzenario1, Strategy.IGNORE_CUSTOMER);

        assertEquals("CH1", result.get(0).tripID);
        assertEquals("2", result.get(1).tripID);
        assertEquals("1", result.get(2).tripID);
    }

    @Test
    public void testSimpleChargingTimeCalculation() {

    }

    @Test
    public void testGetMinMaxMetricsValues() {
        MetricsValues metricsValues = new MetricsValues();
        metricsValues.setAllVaBreaksDownValues(Arrays.asList(false, false, false, false, false));
        metricsValues.setAllOdrValues(Arrays.asList(1,3,5,7,9));
        metricsValues.setAllTotalDistances(Arrays.asList(100.0, 300.0, 600.0, 330.0, 222.0));
        metricsValues.setAllStopsValues(Arrays.asList(2,2,3,3,3));
        metricsValues.setAllMinBatteryLevelValues(Arrays.asList(0.22, 0.032, 0.46, 0.322, 0.18));
        metricsValues.setAllBatteryLevelValuesAfterAllTrips(Arrays.asList(0.0, 0.77, 0.654, 0.35, 0.387));

        MinMaxMetricsValues minMaxMetricsValues = scheduler.getMinMaxMetricsValues(metricsValues);
        assertEquals(1, minMaxMetricsValues.getMinOdr());
        assertEquals(9, minMaxMetricsValues.getMaxOdr());
        assertEquals(100.0, minMaxMetricsValues.getMinTotalDistance());
        assertEquals(600.0, minMaxMetricsValues.getMaxTotalDistance());
        assertEquals(2, minMaxMetricsValues.getMinStops());
        assertEquals(3, minMaxMetricsValues.getMaxStops());
        assertEquals(0.032, minMaxMetricsValues.getMinBatteryLevel());
        assertEquals(0.46, minMaxMetricsValues.getMaxBatteryLevel());
        assertEquals(0.0, minMaxMetricsValues.getMinBatteryLevelAfterAllTrips());
        assertEquals(0.77, minMaxMetricsValues.getMaxBatteryLevelAfterAllTrips());
    }

    @Test
    public void testNormalizationOfValues() {
        MetricsValues metricsValues = new MetricsValues();
        metricsValues.setAllVaBreaksDownValues(Arrays.asList(false, false, false, false, false));
        metricsValues.setAllOdrValues(Arrays.asList(1,3,5,7,9));
        metricsValues.setAllTotalDistances(Arrays.asList(100.0, 300.0, 600.0, 330.0, 222.0));
        metricsValues.setAllStopsValues(Arrays.asList(2,2,3,3,3));
        metricsValues.setAllMinBatteryLevelValues(Arrays.asList(0.01, 0.03, 0.02, 0.04, 0.05));
        metricsValues.setAllBatteryLevelValuesAfterAllTrips(Arrays.asList(0.0, 0.8, 0.6, 0.2, 0.2));

        MinMaxMetricsValues minMaxMetricsValues = scheduler.getMinMaxMetricsValues(metricsValues);

        List<List<Number>> listOfNormalizedValues = scheduler.normalizeValues(metricsValues, minMaxMetricsValues);
        List<Number> odrValues = listOfNormalizedValues.stream().map(l -> l.get(0)).collect(Collectors.toList());
        List<Number> totalDistanceValues = listOfNormalizedValues.stream().map(l -> l.get(1)).collect(Collectors.toList());
        List<Number> stopValues = listOfNormalizedValues.stream().map(l -> l.get(2)).collect(Collectors.toList());
        List<Number> batteryLevelAfterAllTripsValues = listOfNormalizedValues.stream().map(l -> l.get(3)).collect(Collectors.toList());
        List<Number> minBatteryLevelValues = listOfNormalizedValues.stream().map(l -> l.get(4)).collect(Collectors.toList());

        assertEquals(Arrays.asList(0.0, 0.25, 0.5, 0.75, 1.0), odrValues);
        // inverted values expected here
        assertEquals(Arrays.asList(1.0, 0.6, 0.0, 0.54, 0.756), totalDistanceValues);
        // inverted values expected here
        assertEquals(Arrays.asList(1.0, 1.0, 0.0, 0.0, 0.0), stopValues);
        assertEquals(Arrays.asList(0.0, 0.4999999999999999, 0.25, 0.75, 1.0), minBatteryLevelValues);
        assertEquals(Arrays.asList(0.0, 1.0, 0.7499999999999999, 0.25, 0.25), batteryLevelAfterAllTripsValues);
    }

}