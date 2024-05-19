package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
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

        scheduler = new GreedyScheduler(chargingStations, 0.4, null, null, null, null);

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
                600.0
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

        List<Trip> result = schedulerSzenario1.greedySchedule(tripsSzenario1);

    }

    @Test
    public void testSzenario1_1() {
        // Szenario 1
        Location chargingStationLocation = new Location("", 100.0, 0.0);
        LocalDateTime startSimulationTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(8,0));
        Location currentVaLocationSzenario1 = new Location("VA", 0.0, 0.0);
        schedulerSzenario1 = new GreedyScheduler(
                Collections.singletonList(chargingStationLocation),
                0.011,
                currentVaLocationSzenario1,
                startSimulationTime.plusMinutes(1),
                6.0,
                600.0
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

        List<Trip> result = schedulerSzenario1.greedySchedule(tripsSzenario1);

    }

}