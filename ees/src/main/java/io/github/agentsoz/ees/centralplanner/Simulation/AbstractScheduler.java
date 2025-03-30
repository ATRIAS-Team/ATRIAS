package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.Graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

public abstract class AbstractScheduler implements Simulation {
    public ArrayList<Vehicle> vehicles;
    protected final ArrayList<Trip> requestedTrips;
    protected final HashMap<String, Integer> bestVehicleMap = new HashMap<>();
    protected final Graph graph;
    protected final String outputFilePath;

    public AbstractScheduler(ArrayList<Vehicle> vehicles, String requestsFilePath, String outputFilePath, Graph graph, String pathfindingMethod) {
        graph.pathfindingMethod = pathfindingMethod;
        this.requestedTrips = new RequestReader(requestsFilePath, graph).requestedTrips;
        this.graph = graph;
        this.vehicles = vehicles;
        this.outputFilePath = outputFilePath;
    }

    public abstract void run();

    public void saveVehicleTrips(){
        for (Vehicle vehicle : vehicles){

            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"customerID","TripID","bookingTime","vaTime","StartNode", "EndNode", "battery"});
            for (Trip trip : vehicle.takenTrips){
                data.add(new String[]{trip.customerID,
                        trip.TripID,
                        trip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        trip.vaTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        trip.nearestStartNode,
                        trip.nearestEndNode,
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

    public void saveBestVehicleMapping(){
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"customerID","TripID","bookingTime","vaTime","startX","startY","endX","endY", "bestVehicle"});

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

    public void vehicleSummary(){
        System.out.println("\nData Analysis started");
        double summedWaitingTime = 0;
        for (Vehicle vehicle : vehicles){
            ArrayList<Integer> waitingTimes = new ArrayList<>() {
            };
            //iterate over takenTrips of the vehicle
            for (Trip trip : vehicle.takenTrips){
                //and retrieve the waiting time/approach time for all the approach trips
                if (!Objects.equals(trip.customerID, vehicle.name)){
                    int approachTime = (int) Duration.between(trip.bookingTime, trip.vaTime).toSeconds();
                    waitingTimes.add(approachTime);
                    summedWaitingTime += approachTime;
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

        String parsedTotalTime = Duration.ofSeconds((long)summedWaitingTime).toString().replace("PT", "");
        System.out.println("-------------------"+ "Total Waiting Time" + "-------------------");
        System.out.println("Total Waiting Time: " + parsedTotalTime + " sec");
    }

}
