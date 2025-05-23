package io.github.agentsoz.ees.centralplanner.Graph;

import java.util.ArrayList;

public class Path {
    public ArrayList<Edge> path = new ArrayList<>();
    public double distance;
    public double travelTime; // in seconds

    public void addEdge(Edge edge) {
        path.add(0, edge);
        distance += edge.length;
        travelTime += edge.travelTime;
    }

    public ArrayList<Node> getNodes(){
        ArrayList<Node> nodes = new ArrayList<>();
        for (Edge edge : path) {
            nodes.add(edge.from);
        }
        nodes.add(path.get(path.size()-1).to);
        return nodes;
    }
}