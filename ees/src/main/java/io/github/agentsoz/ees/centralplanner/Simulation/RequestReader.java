package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.github.agentsoz.ees.centralplanner.util.Util.timeParser;

public class RequestReader {
    public ArrayList<Map<String, String>> rawData;
    public ArrayList<Trip> allRequestedTrips = new ArrayList<>();
    public ArrayList<ArrayList<Trip>> groupedRequestedTrips = new ArrayList<>();
    protected HashMap<String, String> configMap;


    public RequestReader(HashMap<String, String> configMap, Graph graph) {
        this.configMap = configMap;
        // get raw data from csv file
        generateFromCSVFile(configMap.get("CSV_SOURCE"));
        parseRawDataToRequests(graph);
        groupRequests();

    }

    private void parseRawDataToRequests(Graph graph){
        // parse data to Trip format
        for (Map<String, String> dataRow : rawData) {

            String customerID = dataRow.get("customerID");
            String tripID = dataRow.get("jobID");
            String bookingTime = dataRow.get("bookingTime");
            String vaTime = dataRow.get("vaTime");
            String startX = dataRow.get("startX");
            String startY = dataRow.get("startY");
            String endX = dataRow.get("endX");
            String endY = dataRow.get("endY");

            Trip trip = new Trip(customerID, tripID, 2, "CustomerTrip", bookingTime, vaTime, startX, startY, endX, endY, graph.getNearestNodeID(startX, startY), graph.getNearestNodeID(endX, endY));
            trip.calculateTrip(graph);

            //add fixed delay to mitigate Jadex-MATSim inaccuracies during contract-net-protocol negotiation and customers entering/leaving vehicles
            trip.calculatedPath.travelTime += Double.parseDouble(configMap.get("TRAVELTIME_DELAY"));

            allRequestedTrips.add(trip);
        }
    }

    private void groupRequests(){
        // groups all requests into Arrays of specific time intervals

        LocalDateTime startTime = allRequestedTrips.get(0).bookingTime.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime endTime = allRequestedTrips.get(allRequestedTrips.size()-1).bookingTime.plusHours(1).truncatedTo(ChronoUnit.HOURS);
        System.out.println("\nLoaded " + allRequestedTrips.size() + " requests, between " + startTime + " and " + endTime );

        LocalDateTime intervalStartTime = startTime;
        LocalDateTime intervalEndTime = startTime.plusSeconds(Long.parseLong(configMap.get("TIMEINTERVAL")));
        while (intervalStartTime.isBefore(endTime)) {
            ArrayList<Trip> groupedTrips = new ArrayList<>();
            for (Trip trip : allRequestedTrips) {
                if ((trip.bookingTime.isAfter(intervalStartTime) || trip.bookingTime.isEqual(intervalStartTime)) && trip.bookingTime.isBefore(intervalEndTime)) {
                    groupedTrips.add(trip);
                }
            }
            groupedRequestedTrips.add(groupedTrips);
            intervalStartTime = intervalEndTime;
            intervalEndTime = intervalStartTime.plusSeconds(Long.parseLong(configMap.get("TIMEINTERVAL")));
        }
    }

    private void generateFromCSVFile(String filePath){
        rawData = new ArrayList<>();

        String line;
        String delimiter = ";";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String headerLine = br.readLine();
            String[] headers = headerLine.split(delimiter);

            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);
                Map<String, String> row = new HashMap<>();

                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], values[i]);
                }
                rawData.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}