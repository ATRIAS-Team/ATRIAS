package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.JSONParser;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Job;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.GeneticScheduler;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Config;
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
    public void measureRuntimeGreedy() {
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
        for (int j = 1; j < 11; j++) {

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

    Double CHARGING_THRESHHOLD = 0.05;
    // 3,5h bei 400 Watt
    // 12600 seconds for 0% - 100%
    Double COMPLETE_CHARGING_TIME = 12600.0;
    Double MIN_CHARGING_TIME = COMPLETE_CHARGING_TIME * CHARGING_THRESHHOLD;


    @Test
    public void measureRuntimeGenetic() {
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
        for (int j = 1; j < 11; j++) {

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
            LocalDateTime simtimeDate = tripsToSchedule.get(0).bookingTime;
            Double simtime = simtimeDate.getHour() * 3600.0 + simtimeDate.getMinute() * 60.0 + simtimeDate.getSecond();

            Config config = new Config();
            config.setSimulationTime(LocalDateTime.now());
            config.setAgentLocation(tripsToSchedule.get(0).getStartPosition());
            config.setBatteryLevel(0.4);
            config.setDRIVING_SPEED(6.0);
            config.setMIN_CHARGING_TIME(MIN_CHARGING_TIME);
            config.setMAX_CHARGING_TIME(COMPLETE_CHARGING_TIME);
            config.setCOMPLETE_CHARGING_TIME(COMPLETE_CHARGING_TIME);
            config.setTHETA(900.0);
            config.setChargingStations(CHARGING_STATION_LIST);
            config.setDISTANCE_FACTOR(3.0);
            config.setCHARGE_DECREASE(0.0001);
            config.setCHARGE_INCREASE(0.000079);
            config.setCurrentTrip(new ArrayList<>());
            config.setSimtime(simtime);
            config.setBattThreshhold(0.3);



            long startTime = System.currentTimeMillis();
            GeneticScheduler geneticScheduler = new GeneticScheduler(config);
            geneticScheduler.start(tripsToSchedule, 100, 10);
            long endTime = System.currentTimeMillis();

            System.out.println("Measured Time for " + j + " trips: " + (endTime - startTime) / 1000.0);
        }
    }
}
