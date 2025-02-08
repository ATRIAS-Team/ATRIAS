package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.github.agentsoz.ees.centralplanner.util.ProgressTracker.showProgress;

public class Simulation {
    private final ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>();
    private final HashMap<String, Integer> bestVehicleMap = new HashMap<>();
    private final ArrayList<Trip> requestedTrips;
    private final Graph graph;
    private double elapsedWaitingTime = 0.0;
    private final String outputFilePath;

    public Simulation(String vehicleConfigFilePath, String requestsFilePath, String populationFilePath, String outputFilePath, Graph graph) {
        generateFromXmlFile(vehicleConfigFilePath, graph);
        requestedTrips = new RequestReader(requestsFilePath, graph).requestedTrips;
        this.outputFilePath = outputFilePath;
        this.graph = graph;
    }

    public void run(){
        System.out.println("\nSimulation started");
        // start by iterating over requests
        for (int i = 0; i < requestedTrips.size(); i++) {
            Trip customerTrip = requestedTrips.get(i);

            showProgress(i, requestedTrips.size()-1);

            double bestTravelTime = Double.MAX_VALUE;
            Vehicle bestVehicle = null;
            Trip bestApproach = null;

            //iterate over each vehicle and calculate the approach time for the current position of the vehicle
            for (Vehicle vehicle : vehicles){
                //refresh vehicles queue with current time of the loop iteration
                vehicle.refreshVehicle(customerTrip.bookingTime);

                //calculate trip vatime for vehicles with given queue
                Trip evaluatedApproach = vehicle.evaluateApproach(customerTrip, graph);

                double customerWaitTime = Duration.between(customerTrip.bookingTime,
                                evaluatedApproach.vaTime.plusSeconds((long) Math.ceil(evaluatedApproach.calculatedPath.travelTime)))
                        .toSeconds();

                if (customerWaitTime<bestTravelTime){
                    bestTravelTime = customerWaitTime;
                    bestVehicle = vehicle;
                    bestApproach = evaluatedApproach;
                }
            }
            if (bestVehicle == null){
                System.out.println("Vehicle " + bestVehicle + " not found");
                return;
            }

            bestVehicleMap.put(customerTrip.TripID, bestVehicle.id);

            bestVehicle.queueTrip(bestApproach);
            elapsedWaitingTime += bestApproach.calculatedPath.travelTime;
            customerTrip.vaTime = bestApproach.vaTime.plusSeconds((long) Math.ceil(bestApproach.calculatedPath.travelTime));

            bestVehicle.queueTrip(customerTrip);
        }
        //update rest of queued trips from vehicles, even after last booking came in
        for (Vehicle vehicle : vehicles){
            if (!vehicle.queuedTrips.isEmpty()){
                Trip lastTrip = vehicle.queuedTrips.get(vehicle.queuedTrips.size()-1);
                vehicle.refreshVehicle(lastTrip.vaTime.plusSeconds((long) Math.ceil(lastTrip.calculatedPath.travelTime)));
            }
        }
        initializeOutputFolder();
    }

    public void saveIndividualVehicleTrips(){
        for (Vehicle vehicle : vehicles){

            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"customerID","TripID","bookingTime","vaTime","startX","startY","endX","endY", "battery"});
            for (Trip trip : vehicle.takenTrips){
                data.add(new String[]{trip.customerID,
                        trip.TripID,
                        trip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        trip.vaTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        trip.startX,
                        trip.startY,
                        trip.endX,
                        trip.endY,
                        String.valueOf(trip.batteryLevel)
                });
            }

            StringBuilder csvContentBuilder = new StringBuilder();
            for (String[] row : data) {
                csvContentBuilder.append(String.join(";", row)).append("\n");
            }

            File csvFile = new File(outputFilePath, vehicle.id + ".csv");

            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.write(csvContentBuilder.toString());
            } catch (IOException e) {
                System.err.println("Error saving CSV file: " + e.getMessage());
            }
        }
    }

    public void saveBestVehicleRequestMapping(){
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"customerID","jobID","bookingTime","vaTime","startX","startY","endX","endY", "bestVehicle"});

        for (Trip trip : requestedTrips){
            data.add(new String[]{trip.customerID,
                    trip.TripID,
                    trip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
//                    trip.vaTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                    trip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                    trip.startX,
                    trip.startY,
                    trip.endX,
                    trip.endY,
                    String.valueOf(bestVehicleMap.get(trip.TripID)),
            });

        }
        StringBuilder csvContentBuilder = new StringBuilder();
        for (String[] row : data) {
            csvContentBuilder.append(String.join(";", row)).append("\n");
        }

        File csvFile = new File(outputFilePath, "results.csv");

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write(csvContentBuilder.toString());
        } catch (IOException e) {
            System.err.println("Error saving CSV file: " + e.getMessage());
        }

    }

    private void initializeOutputFolder(){
        File directory = new File(outputFilePath);

        if (!directory.exists()) {
            boolean created = directory.mkdirs(); // Creates the directory and any necessary parent directories
            if (created) {
                System.out.println("Directory created successfully: " + outputFilePath);
            } else {
                System.err.println("Failed to create directory: " + outputFilePath);
            }
        } else {
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            if (files != null) {
                for (File file : files) {
                    boolean deleted = file.delete();
                    if (!deleted) {
                        System.err.println("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    public void dataAnalysis(){
        System.out.println("\nData Analysis started");
        for (Vehicle vehicle : vehicles){
            ArrayList<Integer> waitingTimes = new ArrayList<>() {
            };
            //iterate over takenTrips of the vehicle
            for (Trip trip : vehicle.takenTrips){
                //and retrieve the waiting time/approach time for all the approach trips
                if (!Objects.equals(trip.customerID, vehicle.name)){
                    waitingTimes.add((int) Duration.between(trip.bookingTime, trip.vaTime).toSeconds());
                }
            }

            if (waitingTimes.isEmpty()){
                continue;
            }

            // Calculate minimum, maximum, and average
            int min = Collections.min(waitingTimes);
            int max = Collections.max(waitingTimes);
            double average = waitingTimes.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);

            // Print basic statistics
            System.out.println("-------------------"+ vehicle.name + "-------------------");
            System.out.println("Minimum Waiting Time: " + min + " sec");
            System.out.println("Maximum Waiting Time: " + max + " sec");
            System.out.println("Average Waiting Time: " + average + " sec");
        }
    }

    private void generateFromXmlFile(String path, Graph graph){
        HashMap<String, String> configMap = new HashMap<>();
        HashMap<Integer, List<String>> populationMap = new HashMap<>();

        try {
            // Parse XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(path));

            // Normalize the document
            document.getDocumentElement().normalize();

            // Read node
            NodeList nodeList = document.getElementsByTagName("file");
            configMap.put("POPULATION_FILE", ((Element) nodeList.item(0)).getAttribute("target_file"));
            configMap.put("VEHICLES", ((Element) ((Element) nodeList.item(1)).getElementsByTagName("component").item(0)).getAttribute("number"));
            NodeList nodeList2 = document.getElementsByTagName("field");
            for (int i=0;i<nodeList2.getLength();i++){
                Element element = (Element) nodeList2.item(i);
                configMap.put(element.getAttribute("field_name"), element.getTextContent());
            }

            Document populationDoc = builder.parse(new File(configMap.get("POPULATION_FILE")));
            populationDoc.getDocumentElement().normalize();

            NodeList popNodeList = populationDoc.getElementsByTagName("person");

            for (int i=0;i<popNodeList.getLength();i++){
                List<String> coords = new ArrayList<String>();
                String x = ((Element) popNodeList.item(i)).getElementsByTagName("activity").item(0).getAttributes().getNamedItem("x").getNodeValue();
                String y = ((Element) popNodeList.item(i)).getElementsByTagName("activity").item(0).getAttributes().getNamedItem("y").getNodeValue();
                coords.add(x);
                coords.add(y);
                populationMap.put(i, coords);
            }

            for (int i=0;i<Integer.parseInt(configMap.get("VEHICLES"));i++){
                String homeNode = graph.getNearestNodeID(populationMap.get(i).get(0), populationMap.get(i).get(1));
                Vehicle vehicle = new Vehicle(i, homeNode);
                vehicles.add(vehicle);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
