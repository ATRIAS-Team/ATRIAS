package io.github.agentsoz.ees.centralplanner;

import io.github.agentsoz.ees.centralplanner.Graph.*;
import io.github.agentsoz.ees.centralplanner.Simulation.*;

public class Main {
    public static void main(String[] args) {
//        String vehicleConfigFilePath = "scenarios/frankfurt/vehicle-config.xml";
        String vehicleConfigFilePath = "XMLConfig.xml";

//        String mapFilePath = "scenarios/frankfurt/campus-layer-utm.xml";
        String mapFilePath = "ees/scenarios/matsim-boston/boston_matsim-JOSM-UTM.xml";

//        String requestsFilePath = "scenarios/frankfurt/data-utm-1000.csv";
        String requestsFilePath = "ees/subsample_2.csv";

        String populationFilePath = "ees/scenarios/matsim-boston/boston-population.xml";

//        String outputFilePath = "scenarios/frankfurt/output";
        String outputFilePath = "centralplanner";

        // create Graph of given map
        Graph graph = new Graph();
        graph.generateFromXmlFile(mapFilePath);

        // initialize vehicles
        Simulation sim = new Simulation(vehicleConfigFilePath, requestsFilePath, populationFilePath, outputFilePath, graph);
        sim.run();

        sim.saveIndividualVehicleTrips();
        sim.saveBestVehicleRequestMapping();
        sim.dataAnalysis();
    }
}


