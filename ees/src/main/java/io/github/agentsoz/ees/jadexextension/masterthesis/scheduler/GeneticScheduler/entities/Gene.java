package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities;

import io.github.agentsoz.util.Location;

import java.time.LocalDateTime;

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


    public double distance(Gene gene) {
        try {
            if (end == null) {
                return Location.distanceBetween(start, gene.start) * config.getDISTANCE_FACTOR();
            } else {
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

    public String getId() {
        return id;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }
}

