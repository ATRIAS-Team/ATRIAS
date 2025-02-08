package io.github.agentsoz.ees.centralplanner.Graph;

public class Node {
    public String id;
    public Double x;
    public Double y;

    Node(String id, String x, String y) {
        this.id = id;
        this.x = Double.parseDouble(x);
        this.y = Double.parseDouble(y);
    }
}