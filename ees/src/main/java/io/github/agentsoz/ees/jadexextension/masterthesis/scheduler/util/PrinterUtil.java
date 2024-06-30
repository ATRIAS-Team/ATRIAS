package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.util;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PrinterUtil {

    public void startScheduler(String agentId, Double batteryLevel, LocalDateTime simulationTime) {
        System.out.println("*******************************************************************************************");
        System.out.println("Greedy Schedule started...");
        System.out.println("AgentID: " + agentId);
        System.out.println("BatteryLevel of Vehicle: " + batteryLevel);
        System.out.println("Current Simulation Time: " + simulationTime);
    }

    public void metrics(List<List<Trip>> permutations, List<Integer> allOdrValues, List<Double> allTotalDistances, List<Double> allMinBatteryLevelValues, List<Double> allBatteryLevelValuesAfterAllTrips, List<Integer> allStopsValues, List<Double> allChargingTimes) {
        List<List<String>> ids = permutations
                .stream()
                .map(p -> p.stream().map(t -> t.getTripID()).collect(Collectors.toList()))
                .collect(Collectors.toList());

        System.out.println("PERMUTATIONS: " + ids);
        System.out.println("ODR: " + allOdrValues);
        System.out.println("TOTAL DISTANCES: " + allTotalDistances);
        System.out.println("MINIMUM BATTERY LEVEL ACROSS ALL TRIPS : " + allMinBatteryLevelValues);
        System.out.println("BATTERY LEVEL AFTER ALL TRIPS : " + allBatteryLevelValuesAfterAllTrips);
        System.out.println("STOPS: " + allStopsValues);
        System.out.println("CHARGING TIMES: " + allChargingTimes);

        for (int i = 0; i < allStopsValues.size(); i++) {
            System.out.println(
                    String.join(
                            ",",
                            allOdrValues.get(i).toString(),
                            allTotalDistances.get(i).toString(),
                            allMinBatteryLevelValues.get(i).toString(),
                            allBatteryLevelValuesAfterAllTrips.get(i).toString(),
                            allStopsValues.get(i).toString(),
                            allChargingTimes.get(i).toString()
                    )
            );
        }
    }

    public void csv(List<List<Trip>> permutations, List<Integer> allOdrValues, List<Double> allTotalDistances, List<Double> allMinBatteryLevelValues, List<Double> allBatteryLevelValuesAfterAllTrips, List<Integer> allStopsValues, List<Double> allChargingTimes, List<Double> allRatings) {
        List<List<String>> ids = permutations
                .stream()
                .map(p -> p.stream().map(t -> t.getTripID()).collect(Collectors.toList()))
                .collect(Collectors.toList());


        for (int i = 0; i < allStopsValues.size(); i++) {
            System.out.println(
                    String.join(
                            ",",
                            allOdrValues.get(i).toString(),
                            allTotalDistances.get(i).toString(),
                            allMinBatteryLevelValues.get(i).toString(),
                            allBatteryLevelValuesAfterAllTrips.get(i).toString(),
                            allStopsValues.get(i).toString(),
                            allChargingTimes.get(i).toString(),
                            allRatings.get(i).toString()
                    )
            );
        }
    }

    public void endScheduler(List<Double> allRatings) {
        System.out.println("RATINGS: " + allRatings);
        System.out.println("Greedy Schedule End...");
        System.out.println("*******************************************************************************************");
    }
}
