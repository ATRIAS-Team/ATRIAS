package io.github.agentsoz.ees.centralplanner;

import io.github.agentsoz.ees.centralplanner.Simulation.Scheduler.*;

import java.util.HashMap;

import static io.github.agentsoz.ees.centralplanner.util.Util.xmlConfigParser;

public class Main {
    public static void main(String[] args) {
        String configFilePath = "configs/Subsample 1/Boston_S1.xml";
        HashMap<String, String> configMap = xmlConfigParser(configFilePath);

//        AntColonyScheduler sim = new AntColonyScheduler(configFilePath);
//        GeneticAlgorithmScheduler sim = new GeneticAlgorithmScheduler(configFilePath);
//        BruteForceScheduler sim = new BruteForceScheduler(configFilePath);
        GreedyScheduler sim = new GreedyScheduler(configMap);
//        GreedyWithReschedulingScheduler sim = new GreedyWithReschedulingScheduler(configFilePath);

//        GreedyGARescheduling sim = new GreedyGARescheduling(configMap);

        //initializes the simulation by reading in the graph, vehicles, requests and population from the config
        sim.init();

        //runs the scheduling
        sim.run();

        //finishes the vehicle trips by updating one last time with the latest arrival time of each vehicle
        sim.updateWithLastArrivalTime();

        //saves the results and prints a summary to terminal
        sim.saveVehicleTrips();
        sim.saveBestVehicleMapping();
        sim.vehicleSummary();
    }
}


