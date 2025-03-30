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

        switch (graph.pathfindingMethod){
            case "euclidean":
                calculatedPath = graph.euclideanDistance(nearestStartNode, nearestEndNode);
                break;
            case "dijkstra":
                calculatedPath = graph.dijkstra(nearestStartNode, nearestEndNode);
                break;
            case "fast_dijkstra":
                calculatedPath = graph.fast_dijkstra(nearestStartNode, nearestEndNode);
                break;
            default:
                throw new IllegalArgumentException("Invalid pathfinding method: " + graph.pathfindingMethod);
        }
    }

    private LocalDateTime timeParser(String time){
        time = time.replace("T", " ");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return LocalDateTime.parse(time, formatter);
    }
}
