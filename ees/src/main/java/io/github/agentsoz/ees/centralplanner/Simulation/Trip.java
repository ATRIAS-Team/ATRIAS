package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Trip {
    public String customerID;
    public String TripID;
    public LocalDateTime bookingTime;
    public LocalDateTime vaTime;
    public String startX;
    public String startY;
    public String endX;
    public String endY;
    public String nearestStartNode;
    public String nearestEndNode;
    public Path calculatedPath;
    public double batteryLevel;

    public Trip( String customerID, String TripID, String bookingTime, String vaTime, String startX, String startY, String endX, String endY ) {
        this.customerID = customerID;
        this.TripID = TripID;
        this.bookingTime = timeParser(bookingTime);
        this.vaTime = timeParser(vaTime);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public void calculateTrip(Graph graph) {
        nearestStartNode = graph.getNearestNodeID(startX, startY);
        nearestEndNode = graph.getNearestNodeID(endX, endY);

        calculatedPath = graph.fast_dijkstra(nearestStartNode, nearestEndNode);
    }

    private LocalDateTime timeParser(String time){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return LocalDateTime.parse(time, formatter);
    }
}
