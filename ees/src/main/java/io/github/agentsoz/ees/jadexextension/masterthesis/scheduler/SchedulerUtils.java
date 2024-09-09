package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;

// Contains functions that both schedulers use
public class SchedulerUtils {

    public static long calculateWaitingTime(LocalDateTime bookingTime, LocalDateTime simulationTime) {
        return Duration
                .between(bookingTime, simulationTime)
                .getSeconds();
    }

    public static double calculateTravelTime(Double distance, Double DRIVING_SPEED) {
        return ((distance / 1000) / DRIVING_SPEED) * 60 * 60;
    }
}
