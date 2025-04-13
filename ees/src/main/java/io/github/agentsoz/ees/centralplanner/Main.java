package io.github.agentsoz.ees.centralplanner;

import io.github.agentsoz.ees.centralplanner.Simulation.Rebalancing.CreateSupportingMatrices;
import io.github.agentsoz.ees.centralplanner.Simulation.Scheduler.*;

import java.util.HashMap;

import static io.github.agentsoz.ees.centralplanner.util.Util.xmlConfigParser;

public class Main {
    public static void main(String[] args) {
        String configFilePath = "configs/Subsample 1/Boston_S1.xml";
        HashMap<String, String> configMap = xmlConfigParser(configFilePath);

//        AntColonyScheduler sim = new AntColonyScheduler(configMap);
//        GeneticAlgorithmScheduler sim = new GeneticAlgorithmScheduler(configMap);
//        BruteForceScheduler sim = new BruteForceScheduler(configMap);
//        GreedyScheduler sim = new GreedyScheduler(configMap);
        GreedyRebalancing sim = new GreedyRebalancing(configMap);
//        GreedyGreedyRescheduling sim = new GreedyGreedyRescheduling(configMap);
//        GreedyBruteForceRescheduling sim = new GreedyBruteForceRescheduling(configMap);
//        GreedyGARescheduling sim = new GreedyGARescheduling(configMap);
//        CreateSupportingMatrices sim = new CreateSupportingMatrices(configMap);


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
}


