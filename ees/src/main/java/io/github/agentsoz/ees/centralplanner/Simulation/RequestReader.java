package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RequestReader {
    public ArrayList<Map<String, String>> rawData;
    public ArrayList<Trip> requestedTrips = new ArrayList<Trip>();

    public RequestReader(String filePath, Graph graph) {
        // get raw data from csv file
        generateFromCSVFile(filePath);
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

            Trip trip = new Trip(customerID, tripID, bookingTime, vaTime, startX, startY, endX, endY);
            trip.calculateTrip(graph);

            requestedTrips.add(trip);
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