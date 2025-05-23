package io.github.agentsoz.ees.centralplanner.Simulation;

import io.github.agentsoz.ees.centralplanner.Graph.*;

import java.time.LocalDateTime;

import static io.github.agentsoz.ees.centralplanner.util.Util.timeParser;

public class Trip {
    public String customerID;
    public String TripID;
    public int driveOperationNumber;
    public String tripType;
    public LocalDateTime bookingTime;
    public LocalDateTime vaTime;
    public String startX;
    public String startY;
    public String endX;
    public String endY;
    public String nearestStartNode;
    public String nearestEndNode;
    public Path calculatedPath;
    public LocalDateTime vehicleBusyTime;

    public Trip( String customerID, String TripID, int driveOperationNumber, String tripType, String bookingTime, String vaTime, String startX, String startY, String endX, String endY, String nearestStartNode, String nearestEndNode ) {
        this.customerID = customerID;
        this.TripID = TripID;
        this.driveOperationNumber = driveOperationNumber;
        this.tripType = tripType;
        this.bookingTime = timeParser(bookingTime);
        this.vaTime = timeParser(vaTime);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.nearestStartNode = nearestStartNode;
        this.nearestEndNode = nearestEndNode;
    }

    public Trip(Trip other){
        //for copying the trip
        this.customerID = other.customerID;
        this.TripID = other.TripID;
        this.driveOperationNumber = other.driveOperationNumber;
        this.tripType = other.tripType;
        this.bookingTime = other.bookingTime;
        this.vaTime = other.vaTime;
        this.startX = other.startX;
        this.startY = other.startY;
        this.endX = other.endX;
        this.endY = other.endY;
        this.nearestStartNode = other.nearestStartNode;
        this.nearestEndNode = other.nearestEndNode;
        this.calculatedPath = other.calculatedPath;
    }

    public void calculateTrip(Graph graph) {
        switch (graph.pathfindingMethod){
            case "euclidean":
                calculatedPath = graph.euclideanDistance(nearestStartNode, nearestEndNode);
                break;
            case "dijkstra":
                calculatedPath = graph.dijkstra(nearestStartNode, nearestEndNode);
                break;
            case "aStar":
                calculatedPath = graph.aStar(nearestStartNode, nearestEndNode);
                break;
            default:
                throw new IllegalArgumentException("Invalid pathfinding method: Please choose euclidean, dijkstra or aStar");
        }
    }
}
