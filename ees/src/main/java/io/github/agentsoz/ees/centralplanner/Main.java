package io.github.agentsoz.ees.centralplanner;

import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Scheduler.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Supplier;

import static io.github.agentsoz.ees.centralplanner.util.Util.xmlConfigParser;

public class Main {
    public static void main(String[] args) {
        String configFilePath = "configs/Subsample 1/Subsample_1_T32_CNP.xml";
        HashMap<String, String> configMap = xmlConfigParser(configFilePath);

        AbstractScheduler sim = getScheduler(configMap);

        //initializes the simulation by reading in the graph, vehicles, requests and population from the config
        sim.init();

        //runs the scheduling
        sim.run();

        //finishes the vehicle trips by updating one last time with the latest arrival time of each vehicle
        sim.updateWithLastArrivalTime();

        //saves the results and prints a summary to terminal
        sim.vehicleSummary();
        sim.saveVehicleTrips();
        sim.saveBestVehicleMapping();
    }

    public static AbstractScheduler getScheduler(HashMap<String, String> configMap) {
        switch (configMap.get("SCHEDULER")) {
            case "GreedyScheduler":
                return new GreedyScheduler(configMap);
            case "GAScheduler":
                return new GAScheduler(configMap);
            case "ACOScheduler":
                return new ACOScheduler(configMap);
            default:
                throw new RuntimeException("Unknown scheduler: " + configMap.get("SCHEDULER"));
        }
    }
}