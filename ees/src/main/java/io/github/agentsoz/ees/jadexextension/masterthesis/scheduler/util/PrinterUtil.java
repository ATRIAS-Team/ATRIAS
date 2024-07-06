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
    }

    public void csv(List<List<Trip>> permutations, List<Integer> allOdrValues, List<Double> allTotalDistances, List<Double> allMinBatteryLevelValues, List<Double> allBatteryLevelValuesAfterAllTrips, List<Integer> allStopsValues, List<Double> allChargingTimes, Double currBatteryLevel, List<Double> allRatings) {
        List<List<String>> ids = permutations
                .stream()
                .map(p -> p.stream().map(t -> t.getTripID()).collect(Collectors.toList()))
                .collect(Collectors.toList());


        for (int i = 0; i < allStopsValues.size(); i++) {
            System.out.println(
                    String.join(
                            ",",
                            ids.get(i).toString().replace(",", " "),
                            allOdrValues.get(i).toString(),
                            String.valueOf(Math.round(allTotalDistances.get(i))),
                            String.valueOf(Math.round(allMinBatteryLevelValues.get(i))),
                            String.valueOf(Math.round(allBatteryLevelValuesAfterAllTrips.get(i))),
                            String.valueOf(Math.round(allStopsValues.get(i))),
                            String.valueOf(Math.round(allChargingTimes.get(i))),
                            String.valueOf(Math.round(currBatteryLevel * 100)),
                            String.valueOf(Math.round(allRatings.get(i) * 100.0))
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
