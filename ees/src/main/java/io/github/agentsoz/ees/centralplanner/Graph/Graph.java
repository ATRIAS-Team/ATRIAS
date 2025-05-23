package io.github.agentsoz.ees.centralplanner.Graph;

import java.util.*;
import java.io.*;
import javax.xml.parsers.*;

import io.github.agentsoz.util.Location;
import org.w3c.dom.*;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;
import static io.github.agentsoz.ees.trikeagent.TrikeConstants.CHARGING_STATION_LIST;

public class Graph {
    public final HashMap<String, Node> nodes; // Maps node ID to Node object
    private final HashMap<Node, List<Edge>> adjacencyList;
    private final ArrayList<String> chargingStations;
    public String pathfindingMethod;
    private final HashMap<String, String> configMap;

    public Graph(HashMap<String, String> configMap) {
        nodes = new HashMap<String, Node>();
        adjacencyList = new HashMap<Node, List<Edge>>();
        chargingStations = new ArrayList<>();
        this.configMap = configMap;
        pathfindingMethod = configMap.get("PATHFINDING_METHOD");
        generateFromXmlFile(configMap.get("inputNetworkFile"));
    }

    // Add a new node to the graph
    public void addNode(String id, String x, String y) {
        Node node = new Node(id, x, y);
        nodes.put(id, node);
        adjacencyList.put(node, new ArrayList<>());
    }

    // Add a new edge to the graph
    public void addEdge(String from, String to, String id, String length, String freespeed, String capacity, String permlanes, String oneway, String modes) {
        Node source = nodes.get(from);
        Node destination = nodes.get(to);

        if (source == null || destination == null) {
            throw new IllegalArgumentException("Invalid node IDs");
        }

        Edge edge = new Edge(source, destination, id, length, freespeed, capacity, permlanes, oneway, modes);
        adjacencyList.get(source).add(edge);
    }

    // Get node id as a String from x and y coordinates
    public String[] getNodeCoordinates(String nodeId) {
        Node node = nodes.get(nodeId);
        return new String[]{String.valueOf(node.x), String.valueOf(node.y)};
    }

    // Get nearest node id as a String, depending on the Euclidean distance of the x and y coordinates
    public String getNearestNodeID(String x, String y) {
        double x2 = Double.parseDouble(x);
        double y2 = Double.parseDouble(y);

        String nearestNodeId = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Node node : nodes.values()) {
            double distance = Math.sqrt(Math.pow(x2 - node.x,2) + Math.pow(y2 - node.y,2));
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestNodeId = node.id;
            }
        }
        return nearestNodeId;
    }

    public String getNearestChargingStation(String currentNode) {
        String nearestStation = null;
        double travelTime = Double.MAX_VALUE;
        for (String chargingStation : chargingStations) {
            Path path = dijkstra(currentNode, chargingStation);
            if (path.travelTime < travelTime) {
                nearestStation = chargingStation;
                travelTime = path.travelTime;
            }
        }
        return nearestStation;
    }

    public void generateFromXmlFile(String path){
        try {
            System.out.println("Generating Graph from XML file: " + path);
            // Parse XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(path));

            // Normalize the document
            document.getDocumentElement().normalize();

            // Read nodes and edges
            NodeList nodeList = document.getElementsByTagName("node");
            NodeList edgeList = document.getElementsByTagName("link");

            // add nodes
            int k = 0;
            int nodeListLength = nodeList.getLength();
            for (int i = 0; i < nodeListLength; i++) {
                Element nodeElement = (Element) nodeList.item(i);
                String nodeId = nodeElement.getAttribute("id");
                String nodeX = nodeElement.getAttribute("x");
                String nodeY = nodeElement.getAttribute("y");
                this.addNode(nodeId, nodeX, nodeY);

                if (i%100 == 0 || i == nodeListLength-1){
                    showProgress(i, nodeListLength-1,  " Nodes added: " + nodes.size());
                }
                k ++;
            }

            // add edges
            int edgeListLength = edgeList.getLength();
            for (int j = 0; j < edgeListLength; j++) {

                Element edgeElement = (Element) edgeList.item(j);
                String from = edgeElement.getAttribute("from");
                String to = edgeElement.getAttribute("to");
                String id = edgeElement.getAttribute("id");
                String length = edgeElement.getAttribute("length");
                String freespeed = edgeElement.getAttribute("freespeed");
                String capacity = edgeElement.getAttribute("capacity");
                String permlanes = edgeElement.getAttribute("permlanes");
                String oneway = edgeElement.getAttribute("oneway");
                String modes = edgeElement.getAttribute("modes");

                this.addEdge(from, to, id, length, freespeed, capacity, permlanes, oneway, modes);
                if (j%100 == 0 || j == edgeListLength-1){
                    showProgress(k, nodeListLength-1,  " Nodes added: " + nodes.size() + ", Edges added: " + j);
                }
            }

            // add charging stations
            for (Location location : CHARGING_STATION_LIST) {
                String station = getNearestNodeID(String.valueOf(location.x), String.valueOf(location.y));
                chargingStations.add(station);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Path euclideanDistance(String startId, String endId) {
        Node startNode = nodes.get(startId);
        Node endNode = nodes.get(endId);

        double distanceFactor = Double.parseDouble(configMap.get("DISTANCE_FACTOR"));

        double distance = Math.sqrt(Math.pow(startNode.x - endNode.x,2) + Math.pow(startNode.y - endNode.y,2)) * distanceFactor;

        Path path = new Path();
        Edge directEdge = new Edge(startNode, endNode, startId+"-"+endId, Double.toString(distance), configMap.get("DRIVING_SPEED"), "600", "1", "1", "car");
        path.addEdge(directEdge);
        return path;
    }

    public Path dijkstra(String startId, String endId) {
        Node startNode = nodes.get(startId);
        Node endNode = nodes.get(endId);

        // Data structures
        Map<Node, Double> distances = new HashMap<>(); // Store shortest distances
        Map<Node, Edge> crossedEdges = new HashMap<>(); // Store the shortest path tree
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));
        Set<Node> visited = new HashSet<>();

        // Initialize start node distance
        distances.put(startNode, 0.0);
        pq.add(startNode);

        // Dijkstra's algorithm
        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();

            // Skip if already visited
            if (!visited.add(currentNode)) continue;

            // If reached the destination node, stop
            if (currentNode.equals(endNode)) break;

            // Relaxation step
            for (Edge edge : adjacencyList.get(currentNode)) {
                Node neighbor = edge.to;
                if (visited.contains(neighbor)) continue;

                //metric for the optimization (length/traveltime), shortest length does not mean fastest route
                //double newDist = distances.get(currentNode) + edge.length;
                double newDist = distances.get(currentNode) + edge.travelTime;

                if (newDist < distances.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    distances.put(neighbor, newDist);
                    crossedEdges.put(neighbor, edge);
                    pq.add(neighbor);
                }
            }
        }

        Path path = new Path();
        for (Edge bestEdge = crossedEdges.get(endNode); bestEdge != null; bestEdge = crossedEdges.get(bestEdge.from)) {
            path.addEdge(bestEdge);
        }

        // If the start node is not in the path, no path was found
        if (!path.path.isEmpty() && !path.path.get(0).from.id.equals(startId)) {
            throw new RuntimeException("No path found from " + startId + " to " + endId);
        }

        return path;
    }

    public Path aStar(String startId, String endId) {
        Node startNode = nodes.get(startId);
        Node endNode = nodes.get(endId);

        // Data structures
        Map<Node, Double> gScore = new HashMap<>();  // the currently known cost of the cheapest path from start to n
        Map<Node, Double> fScore = new HashMap<>();  // current best guess for a path if it goes througn n
        Map<Node, Edge> crossedEdges = new HashMap<>();   // Store the shortest path tree
        Map<Node, NodeDistance> openSetReferences = new HashMap<>();   // Store Node->NodeDistance references
        PriorityQueue<NodeDistance> openSet = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::getDistance));

        NodeDistance startNodeDistance = new NodeDistance(startNode, 0.0);
        openSetReferences.put(startNode, startNodeDistance);
        openSet.add(startNodeDistance);

        // Initialize distances
        for (Node node : nodes.values()) {
            gScore.put(node, Double.MAX_VALUE);
        }
        gScore.put(startNode, 0.0);

        for (Node node : nodes.values()) {
            fScore.put(node, Double.MAX_VALUE);
        }
        fScore.put(startNode, aStarHeuristic(startNode, endNode));

        while (!openSet.isEmpty()) {
            NodeDistance currentNodeDist = openSet.poll();
            Node currentNode = currentNodeDist.getNode();

            // If destination node, stop
            if (currentNode.equals(endNode)) break;

            // Relaxation step
            for (Edge edge : adjacencyList.get(currentNode)) {
                Node neighbor = edge.to;

                //metric for the optimization (length/traveltime), shortest length does not mean fastest route
                //double tentative_gScore = gScore.get(currentNode) + edge.length;
                double tentative_gScore = gScore.get(currentNode) + edge.travelTime;

                if (tentative_gScore < gScore.get(neighbor)) {
                    crossedEdges.put(neighbor, edge);
                    gScore.put(neighbor, tentative_gScore);
                    fScore.put(neighbor, tentative_gScore + aStarHeuristic(neighbor, endNode));
                    NodeDistance neighborDistance = openSetReferences.get(neighbor);
                    if (!openSet.contains(neighborDistance)) {
                        NodeDistance newNeighborDistance = new NodeDistance(neighbor, fScore.get(neighbor));
                        openSet.add(newNeighborDistance);
                        openSetReferences.put(neighbor, newNeighborDistance);
                    }
                }
            }
        }

        Path path = new Path();
        for (Edge bestEdge = crossedEdges.get(endNode); bestEdge != null; bestEdge = crossedEdges.get(bestEdge.from)) {
            path.addEdge(bestEdge);
        }

        return path;
    }

    private double aStarHeuristic(Node startNode, Node endNode) {
        return Math.sqrt(Math.pow(startNode.x - endNode.x,2) + Math.pow(startNode.y - endNode.y,2)) / 14;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes.values()) {
            sb.append(node).append(" -> ").append(adjacencyList.get(node)).append("\n");
        }
        return sb.toString();
    }
}

class NodeDistance {
    private final Node node;
    private final double distance;

    public NodeDistance(Node node, double distance) {
        this.node = node;
        this.distance = distance;
    }

    public Node getNode() {
        return node;
    }

    public double getDistance() {
        return distance;
    }
}