package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.JSONParser;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Job;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.GreedyScheduler;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.enums.Strategy;
import io.github.agentsoz.util.Location;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ExperimentalTestCases {

    List<Location> CHARGING_STATION_LIST = Arrays.asList(
            new Location("", 476142.33, 5553197.70),
            new Location("", 476172.65, 5552839.64),
            new Location("", 476482.10, 5552799.06),
            new Location("", 476659.13, 5553054.12),
            new Location("", 476787.10, 5552696.95),
            new Location("", 476689.45, 5552473.11),
            new Location("", 476405.41, 5552489.17),
            new Location("", 476100.86, 5552372.79)
    );

    @Test
    public void measureRuntime() {
        String csvFilePath = "C:\\Users\\timew\\Desktop\\ees - Kopie\\data.csv";
        String jsonFilePath = "output.json";
        char delimiter = ';';

        //parse csv and create json output
        JSONParser.csvToJSON(csvFilePath, jsonFilePath, delimiter);

        System.out.println("parse json from file:");
        List<Job> jobList1 = Job.JSONFileToJobs("output.json");

        for (Job job : jobList1) {
            System.out.println(job.getID());
        }

        // j is equals amount of trips
        for (int j = 4; j < 5; j++) {

            // pick random trips
            int randomInt = new Random().nextInt(jobList1.size() - j);

            List<Trip> tripsToSchedule = new ArrayList<>();
            for (int i = 0; i < j; i++) {
                Job currJob = jobList1.get(randomInt + i);

                Trip newTrip = new Trip(
                        null,
                        currJob.getID(),
                        "CustomerTrip",
                        currJob.getVATime(),
                        currJob.getStartPosition(),
                        currJob.getEndPosition(),
                        "NotStarted",
                        currJob.getbookingTime());
                tripsToSchedule.add(newTrip);
            }

            // instantiate greedy scheduler
            GreedyScheduler scheduler = new GreedyScheduler(
                    CHARGING_STATION_LIST,
                    1.0,
                    tripsToSchedule.get(0).getStartPosition(),
                    jobList1.get(0).getVATime().minusMinutes(1),
                    6.0,
                    900.0,
                    "1",
                    0.0,
                    null
            );

            long startTime = System.currentTimeMillis();
            scheduler.greedySchedule(tripsToSchedule, Strategy.DRIVE_TO_CUSTOMER);
            long endTime = System.currentTimeMillis();

            System.out.println("Measured Time for " + j + " trips: " + (endTime - startTime) / 1000.0);
        }
    }
}
