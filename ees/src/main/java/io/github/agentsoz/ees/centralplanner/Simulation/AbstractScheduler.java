package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.Graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.github.agentsoz.ees.centralplanner.util.Util.*;

public abstract class AbstractScheduler implements Simulation {
    public ArrayList<Vehicle> vehicles;
    public ArrayList<Trip> requestedTrips;
    protected final HashMap<String, Integer> bestVehicleMap = new HashMap<>();
    public Graph graph;
    protected String outputFilePath;
    protected HashMap<String, String> configMap;
    public Boolean progressionLogging = false;

    public AbstractScheduler(HashMap<String, String> configMap) {
        this.configMap = configMap;
    }

    public void init(){
        this.graph = new Graph(configMap);
        this.requestedTrips = new RequestReader(configMap.get("CSV_SOURCE"), graph).requestedTrips;
        this.vehicles = vehicleInit(configMap, graph);
        this.outputFilePath = configMap.get("OUTPUT_PATH") + "/" + this.getClass().getSimpleName();
        initializeOutputFolder(this.outputFilePath);
        progressionLogging = true;
    }

    public abstract void run();

    public void updateWithLastArrivalTime(){
        //update rest of queued trips from vehicles, even after last booking came in
        for (Vehicle vehicle : vehicles){
            if (!vehicle.queuedTrips.isEmpty()){
                vehicle.refreshVehicle(vehicle.busyUntil);
            }
            for (Trip customerTrip : vehicle.takenTrips){
                bestVehicleMap.put(customerTrip.TripID+"-"+customerTrip.driveOperationNumber, vehicle.id);
            }
        }
    }

    public ArrayList<Vehicle> copyAllVehicles(){
        ArrayList<Vehicle> copiedVehicles = new ArrayList<>();
        for (Vehicle vehicle : vehicles){
            copiedVehicles.add(new Vehicle(vehicle));
        }
        return copiedVehicles;
    }

    public void removeTripsFromVehicles(HashMap<Integer, ArrayList<Trip>> vehicleTripMap){
        for (int vehicleId: vehicleTripMap.keySet()){
            Vehicle vehicle = vehicles.get(vehicleId);
            for (Trip trip: vehicleTripMap.get(vehicleId)){
                vehicle.removeQueuedTrip(trip);
            }
        }
    }

    public void saveVehicleTrips(){
        for (Vehicle vehicle : vehicles){

            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"CustomerID","TripID", "DriveOperationNumber", "TripType", "bookingTime", "vaTime", "ArrivalTime", "Distance","StartNode", "EndNode", "battery"});
            for (Trip trip : vehicle.takenTrips){
                data.add(new String[]{trip.customerID,
                        trip.TripID,
                        String.valueOf(trip.driveOperationNumber),
                        trip.tripType,
                        trip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        trip.vaTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        String.valueOf(Math.ceil(trip.calculatedPath.travelTime)),
                        String.valueOf(Math.ceil(trip.calculatedPath.distance)),
                        trip.nearestStartNode,
                        trip.nearestEndNode,
                        String.valueOf(trip.batteryBefore)
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
                    trip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy'T'HH:mm")),
                    trip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy'T'HH:mm")),
                    trip.startX,
                    trip.startY,
                    trip.endX,
                    trip.endY,
                    String.valueOf(bestVehicleMap.get(trip.TripID+"-"+trip.driveOperationNumber)),
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

        //overwrite graph pathfinding for exact evaluation
        graph.pathfindingMethod = "fast_dijkstra";
        //reset Vehicles
        ArrayList<Vehicle> simulatedVehicles = copyAllVehicles();
        vehicles = vehicleInit(configMap, graph);
        //reassign trips with exact pathfinding
        for (int i = 0; i < simulatedVehicles.size(); i++){
            Vehicle vehicle = vehicles.get(i);
            for (Trip simulatedTrip : simulatedVehicles.get(i).takenTrips){
                simulatedTrip.vaTime = simulatedTrip.bookingTime;
                //only refresh once, before approach trip is added
                if (simulatedTrip.driveOperationNumber == 1){
                    vehicle.refreshVehicle(simulatedTrip.bookingTime);
                }
                simulatedTrip.calculateTrip(graph);
                vehicle.queueTrip(simulatedTrip);
            }
            vehicle.refreshVehicle(vehicle.busyUntil);
        }

        double summedWaitingTime = 0;
        int summedMissedTrips = 0;
        for (Vehicle vehicle : vehicles){
            ArrayList<Integer> waitingTimes = new ArrayList<>();
            int missedTrips = vehicle.getMissedTrips();
            //iterate over takenTrips of the vehicle
            for (Trip trip : vehicle.takenTrips){
                //and retrieve the waiting time/approach time for all the approach trips
                if (trip.driveOperationNumber == 2){
                    int approachTime = (int) Duration.between(trip.bookingTime, trip.vaTime).toSeconds();
                    waitingTimes.add(approachTime);
                    summedWaitingTime += approachTime;
                }
            }

            summedMissedTrips += missedTrips;

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
            System.out.println("Taken Trips: " + vehicle.takenTrips.size() + ", Queued Trips: " + vehicle.queuedTrips.size() + ", Missed Trips: " + missedTrips);
            System.out.println("Minimum Waiting Time: " + min + " sec");
            System.out.println("Maximum Waiting Time: " + max + " sec");
            System.out.println("Average Waiting Time: " + average + " sec");
        }

        String parsedTotalTime = Duration.ofSeconds((long)summedWaitingTime).toString().replace("PT", "");
        System.out.println("-------------------"+ "Vehicle Summary" + "-------------------");
        System.out.println("Total Waiting Time: " + parsedTotalTime + " sec");
        System.out.println("Total Missed Trips: " + summedMissedTrips);
    }

}
