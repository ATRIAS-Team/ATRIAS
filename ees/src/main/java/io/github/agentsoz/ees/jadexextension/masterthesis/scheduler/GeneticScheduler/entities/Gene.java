package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.SchedulerUtils;
import io.github.agentsoz.util.Location;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Gene {

    private final String id;
    // represents a trip (customer or charging trip)
    private final Location start;
    // end is null in case of charging trip
    private final Location end;
    private final LocalDateTime bookingTime;
    private Double chargingTime = null;
    private Config config;

    public Gene(String id, Location start, Location end, LocalDateTime bookingTime, Double chargingTime, Config config) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.bookingTime = bookingTime;
        this.chargingTime = chargingTime;
        this.config = config;
    }

    // take type of trip into account

    double distance(Gene gene) {
        try {
            if (end == null) {
                return Location.distanceBetween(start, gene.start);
            } else {
                return Location.distanceBetween(end, gene.start);
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return 0.0;
        }
    }
    @Override
    public String toString() {
        if (end == null) {
            return "Gene [start=" + start.getCoordinates() + "]";
        } else {
            return "Gene [start=" + start.getCoordinates() + ", end=" + end.getCoordinates() + "]";
        }
    }

    public Location getStart() {
        return start;
    }

    public Location getEnd() {
        return end;
    }

    public Double getChargingTime() {
        return chargingTime;
    }

    public void setChargingTime(Double chargingTime) {
        this.chargingTime = chargingTime;
    }

    public Double calculateWaitingTime(List<Gene> otherGenes) {
        try {
            Gene agentGene = new Gene(null, config.getAgentLocation(), null, null, null, config);
            Double waitingTime = calculateTravelTime(distance(agentGene), config.getDRIVING_SPEED());;
            if (chargingTime == null) { return waitingTime; }
            // travel time
            for (Gene gene : otherGenes) {
                waitingTime += calculateTravelTime(distance(gene), config.getDRIVING_SPEED());
                if (gene.getChargingTime() != null) {
                    waitingTime = gene.getChargingTime();
                }
            }
            return waitingTime;
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return 0.0;
        }
    }

    public String getId() {
        return id;
    }

    public double calculateTravelTime(Double distance, Double DRIVING_SPEED) {
        return ((distance / 1000) / DRIVING_SPEED) * 60 * 60;
    }
}

