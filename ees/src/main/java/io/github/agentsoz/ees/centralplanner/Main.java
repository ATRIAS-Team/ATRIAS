package io.github.agentsoz.ees.centralplanner;

import io.github.agentsoz.ees.centralplanner.Graph.*;
import io.github.agentsoz.ees.centralplanner.Simulation.*;

import io.github.agentsoz.ees.centralplanner.Simulation.Scheduler.*;

import java.util.ArrayList;

import static io.github.agentsoz.ees.centralplanner.util.Util.generateFromXmlFile;
import static io.github.agentsoz.ees.centralplanner.util.Util.initializeOutputFolder;

public class Main {
    public static void main(String[] args) {
        String vehicleConfigFilePath = "configs/Boston_A1_T32_CNP_TRUE_S1.xml";

        String mapFilePath = "ees/scenarios/matsim-boston/boston_matsim-JOSM-UTM-medium.xml";

        String requestsFilePath = "ees/subsample_1.csv";

        String outputFilePath = "centralplanner";

        // create Graph of given map
        Graph graph = new Graph();
        graph.generateFromXmlFile(mapFilePath);

        // create vehicles
        ArrayList<Vehicle> vehicles = generateFromXmlFile(vehicleConfigFilePath, graph);

        // initialize vehicles
//        AntColonyScheduler sim = new AntColonyScheduler(vehicles, requestsFilePath, outputFilePath, graph);
//        GeneticAlgorithmScheduler sim = new GeneticAlgorithmScheduler(vehicles, requestsFilePath, outputFilePath, graph);
//        BruteForceScheduler sim = new BruteForceScheduler(vehicles, requestsFilePath, outputFilePath, graph);
//        GreedyScheduler sim = new GreedyScheduler(vehicles, requestsFilePath, outputFilePath, graph);
        GreedyWithReschedulingScheduler sim = new GreedyWithReschedulingScheduler(vehicles, requestsFilePath, outputFilePath, graph);
        sim.run();

        System.out.println(graph.pathfindingMethod);
        initializeOutputFolder(outputFilePath);
        sim.saveVehicleTrips();
        sim.saveBestVehicleMapping();
        sim.vehicleSummary();
    }
}


