package io.github.agentsoz.ees.centralplanner;

import io.github.agentsoz.ees.centralplanner.Graph.Graph;
import io.github.agentsoz.ees.centralplanner.Graph.Path;
import io.github.agentsoz.ees.centralplanner.Simulation.Scheduler.*;

import java.util.HashMap;

import static io.github.agentsoz.ees.centralplanner.util.Util.xmlConfigParser;

public class Main {
    public static void main(String[] args) {
        String configFilePath = "configs/Subsample 1/Boston_S1.xml";


//        HashMap<String, String> configMap = xmlConfigParser(configFilePath);
//        Graph graph = new Graph(configMap);
//
//        String start = "61358687";
//        String end = "73567934";
//
//        Path path = graph.fast_dijkstra(start, end);
//        System.out.println("\ndistance: " + path.distance + " traveltime: " + path.travelTime + " edges: " + path.path.size());
//
//        Path path2 = graph.aStar(start, end);
//        System.out.println("distance: " + path2.distance + " traveltime: " + path2.travelTime + " edges: " + path2.path.size());
//        AntColonyScheduler sim = new AntColonyScheduler(vehicles, requestsFilePath, outputFilePath, graph);
//        GeneticAlgorithmScheduler sim = new GeneticAlgorithmScheduler(vehicles, requestsFilePath, outputFilePath, graph);
//        BruteForceScheduler sim = new BruteForceScheduler(vehicles, requestsFilePath, outputFilePath, graph);
        GreedyScheduler sim = new GreedyScheduler(configFilePath);
//        GreedyWithReschedulingScheduler sim = new GreedyWithReschedulingScheduler(vehicles, requestsFilePath, outputFilePath, graph, "fast_dijkstra");
        sim.run();

        sim.saveVehicleTrips();
        sim.saveBestVehicleMapping();
        sim.vehicleSummary();
    }
}


