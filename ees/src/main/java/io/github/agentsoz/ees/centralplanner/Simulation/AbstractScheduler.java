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
    public RequestReader requestReader;
    public ArrayList<Trip> allOpenTrips = new ArrayList<>();
    protected final HashMap<String, Integer> bestVehicleMap = new HashMap<>();
    public Graph graph;
    protected String outputFilePath;
    protected HashMap<String, String> configMap;
    public Boolean progressionLogging;

    public AbstractScheduler(HashMap<String, String> configMap) {
        this.configMap = configMap;
    }

    public void init(){
        this.graph = new Graph(configMap);
        requestReader = new RequestReader(configMap, graph);
        this.vehicles = vehicleInit(configMap, graph);
        this.outputFilePath = configMap.get("OUTPUT_PATH") + "/" + this.getClass().getSimpleName();
        initializeOutputFolder(this.outputFilePath);
        progressionLogging = Boolean.parseBoolean(configMap.get("PROGRESSION_LOGGING"));
    }

    public void run(){
        //keep track of the execution time
        long runTimeStart = System.currentTimeMillis();

        //print which scheduler is being used
        if (progressionLogging) {
            System.out.println("\nScheduling using " + this.getClass().getSimpleName());
        }
        //iterate over the grouped trips
        for (int i = 0; i < requestReader.groupedRequestedTrips.size(); i++){
            //get the current group of requests
            ArrayList<Trip> currentTrips = requestReader.groupedRequestedTrips.get(i);
            allOpenTrips.addAll(currentTrips);

            //if no trips arrive in the timeframe, continue with the next iteration
            if (currentTrips.isEmpty()){
                continue;
            }

            //update all vehicles with the time of the last request that came in. Otherwise, the scheduler would use future information that should not exist.
            Trip lastRequestOfGroup = currentTrips.get(currentTrips.size()-1);
            for (Vehicle vehicle : vehicles){
                vehicle.refreshVehicle(lastRequestOfGroup.bookingTime);
            }

            //rescheduling is the ability for trips that got queued to a vehicle but not yet done to be reevaluated
            //if rescheduling is active, append all open trips that have not yet started
            if (Boolean.parseBoolean(configMap.get("RESCHEDULING"))){
                //remove the trips, that got done
                removeTakenTripsFromAllOpenTrips();
                //change currentTrips to include all open trips
                currentTrips = allOpenTrips;
                //remove the trips that got scheduled in a previous loop from the vehicles
                removeOpenTripsFromVehicles();
            }

            //use the index of the group to log the scheduling progression to the console
            if (progressionLogging){
                showProgress(i, requestReader.groupedRequestedTrips.size()-1, " Currently scheduling " + currentTrips.size() + " trips");
            }

            //generate the assignment using the current group of requests
            generateAssignment(currentTrips);
        }

        long runTimeEnd = System.currentTimeMillis();
        System.out.println("\nTotal Runtime: " + (runTimeEnd - runTimeStart) + " ms");
    }

    public abstract void generateAssignment(ArrayList<Trip> requestedTrips);

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

    private void removeTakenTripsFromAllOpenTrips(){
        for (Vehicle vehicle : vehicles){
            for (Trip takenTrip : vehicle.takenTrips){
                if(takenTrip.driveOperationNumber == 2){
                    allOpenTrips.removeIf(openTrip -> openTrip.TripID.equals(takenTrip.TripID));
                }
            }
            if (!(vehicle.queuedTrips.isEmpty()) && vehicle.queuedTrips.get(0).driveOperationNumber == 2){
                allOpenTrips.removeIf(openTrip -> openTrip.TripID.equals(vehicle.queuedTrips.get(0).TripID));
            }
        }
    }

    public void removeOpenTripsFromVehicles(){
        for (Trip trip : allOpenTrips){
            for (Vehicle vehicle : vehicles){
                vehicle.removeQueuedTrip(trip);
//                trip.vaTime = trip.bookingTime;
            }
        }
    }

    public void saveVehicleTrips(){
        for (Vehicle vehicle : vehicles){

            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"CustomerID","tripID", "DriveOperationNumber", "TripType", "bookingTime", "vaTime", "busyUntil", "ArrivalTime", "Distance","StartNode", "EndNode"});
            for (Trip trip : vehicle.takenTrips){
                data.add(new String[]{vehicle.name,
                        trip.TripID,
                        String.valueOf(trip.driveOperationNumber),
                        trip.tripType,
                        trip.bookingTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        trip.vaTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        trip.vehicleBusyTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        String.valueOf(Math.ceil(trip.calculatedPath.travelTime)),
                        String.valueOf(Math.ceil(trip.calculatedPath.distance)),
                        trip.nearestStartNode,
                        trip.nearestEndNode,
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
        data.add(new String[]{"customerID","jobID","bookingTime","vaTime","startX","startY","endX","endY", "bestVehicle"});

        for (Trip trip : requestReader.allRequestedTrips){
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

//        //overwrite graph pathfinding for exact evaluation
//        graph.pathfindingMethod = "dijkstra";
//        //reset Vehicles
//        ArrayList<Vehicle> simulatedVehicles = copyAllVehicles();
//        vehicles = vehicleInit(configMap, graph);
//        //reassign trips with exact pathfinding
//        for (int i = 0; i < simulatedVehicles.size(); i++){
//            Vehicle vehicle = vehicles.get(i);
//            for (Trip simulatedTrip : simulatedVehicles.get(i).takenTrips){
//                simulatedTrip.vaTime = simulatedTrip.bookingTime;
//                //only refresh once, before approach trip is added
//                if (simulatedTrip.driveOperationNumber == 1){
//                    vehicle.refreshVehicle(simulatedTrip.bookingTime);
//                }
//                simulatedTrip.calculateTrip(graph);
//                vehicle.queueTrip(simulatedTrip);
//            }
//            vehicle.refreshVehicle(vehicle.busyUntil);
//        }

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
