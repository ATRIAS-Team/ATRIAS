package io.github.agentsoz.ees.centralplanner.Simulation;

import java.util.ArrayList;

public interface Simulation {
    void generateAssignment(ArrayList<Trip> requestedTrips);
}
