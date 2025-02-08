package io.github.agentsoz.ees.centralplanner.Graph;

public class Edge {
    public Node from;
    public Node to;
    public String id;
    public Double length;
    public Float freespeed;
    public Float capacity;
    public Float permlanes;
    public Integer oneway;
    public String modes;
    public Double travelTime;

    Edge(Node from, Node to, String id, String length, String freespeed, String capacity, String permlanes, String oneway, String modes) {
        this.from = from;
        this.to = to;
        this.id = id;
        this.length = Double.parseDouble(length);
        this.freespeed = Float.parseFloat(freespeed);
        this.capacity = Float.parseFloat(capacity);
        this.permlanes = Float.parseFloat(permlanes);
        this.oneway = Integer.parseInt(oneway);
        this.modes = modes;

        travelTime = (this.length / this.freespeed);
    }
}
