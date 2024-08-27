package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities;

import io.github.agentsoz.util.Location;
import static io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.SchedulerUtils.calculateTravelTime;

import java.time.LocalDateTime;
import java.util.List;

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

    public double distance(Gene gene) {
        try {
            if (end == null) {
//                    Double test = getDrivingDistanceBetweenToNodes(start, gene.start, JadexModel.simulationtime);
                return Location.distanceBetween(start, gene.start) * config.getDISTANCE_FACTOR();
            } else {
//                    Double test = getDrivingDistanceBetweenToNodes(end, gene.start, JadexModel.simulationtime);
                return Location.distanceBetween(end, gene.start) * config.getDISTANCE_FACTOR();
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return 0.0;
        }
    }

    public Gene createDeepCopy() {
        return new Gene(
                id,
                start,
                end,
                bookingTime,
                chargingTime,
                config
        );
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

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }
}

