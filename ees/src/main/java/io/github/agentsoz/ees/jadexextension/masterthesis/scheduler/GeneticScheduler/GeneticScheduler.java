package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Chromosome;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Config;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Gene;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Population;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GeneticScheduler {

    private final Config config;

    public GeneticScheduler(Config config) {
        this.config = config;
    }

    Population population;
    private int chargingStationsCounter = 0;

    public List<Trip> start(List<Trip> tripsToSchedule, int iterations, int runs) {
        try {
//            if (tripsToSchedule.size() == 1 && config.getBatteryLevel() >= 0.90) {
//                return tripsToSchedule;
//            }

            List<Double> scores = new ArrayList<>();
            List<Chromosome> chromosomes = new ArrayList<>();
            List<Integer> stagnationIndex = new ArrayList<>();
            for (int i = 0; i < runs; i++) {
                // initial population size in abhängigkeit von der anzahl der trip -> max (2n + 1)! Permutation
                // oder feste populationsgröße
                int n = tripsToSchedule.size() * 2 + 1;
                int totalPermutations = 1;
                for (int k = 1; k <= n; k++) {
                    totalPermutations *= k;
                }
                // Get 10% of amount but maximum of 500
//                int initialPopulationSize = (totalPermutations / 10) < 100 ? 100 : Math.min(totalPermutations / 10, 300);
                int initialPopulationSize = Integer.parseInt(System.getenv("popsize"));

                System.out.println("Gen Sched Trip to schedule: " + tripsToSchedule.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
                System.out.println("Popsize " + initialPopulationSize);

                tripsToSchedule = tripsToSchedule.stream()
                        .filter(t -> t.getTripType().equals("CustomerTrip"))
                        .collect(Collectors.toList());

                population = new Population(initialPopulationSize, tripsToSchedule, this.config);

                List<Double> scoresArchive = new ArrayList<>();
                for (int j = 0; j < iterations; j++) {
                    population.update();
                    scoresArchive.add(population.getBestChromosome().fitnessOld());
                }

//                int idx = scoresArchive.indexOf(scoresArchive.get(scoresArchive.size() - 1));
//                stagnationIndex.add(idx);
//                int stagnationIndex
//                System.out.println("Scores Archive of Run " + i + ": " + scoresArchive);

                writeScoresArchiveIntoFile(scoresArchive, tripsToSchedule.size());

                Chromosome best = population.getBestChromosome();
                scores.add(best.fitnessOld());
                chromosomes.add(best);
            }

            int index = 0;
            double maxVal = 0.0;
            System.out.println("Best Scores - " + scores);
            for (int i = 0; i < scores.size(); i++) {
                if (scores.get(i) > maxVal) {
                    index = i;
                    maxVal = scores.get(i);
                }
            }
            System.out.println("Stagnation List: " + stagnationIndex);
            return mapGenesBackToListOfTrip(chromosomes.get(index).mergeGenes(), tripsToSchedule);
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }


    private void writeScoresArchiveIntoFile(List<Double> scoresArchive, int amountOfTrip) {
        try {
            String popsize = System.getenv("popsize");
            String filePath = "C:\\Users\\timew\\Desktop\\Experimente Thesis\\H5\\archive- " + popsize + ".txt";
            File file = new File(filePath);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(amountOfTrip + "," + scoresArchive);
                writer.newLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private List<Trip> mapGenesBackToListOfTrip(List<Gene> genes, List<Trip> trips) {
        try {

            List<Trip> result = new ArrayList<>();
            for (Gene gene : genes) {
                // charging gene
                if (gene.getEnd() == null) {
                    Trip trip = new Trip(
                            "CH" + (chargingStationsCounter),
                            "ChargingTrip",
                            gene.getStart(),
                            "NotStarted",
                            gene.getChargingTime());
                    chargingStationsCounter++;
                    result.add(trip);
                } else {
                    // customer trip
                    Trip trip = trips.stream().filter(t -> t.getTripID() == gene.getId()).collect(Collectors.toList()).get(0);
                    result.add(trip);
                }
            }

            // set chargingTime to -1 if lastTrip ist chargingTrip
            Trip lastTrip = result.get(result.size() - 1);
            if (lastTrip.getTripType().equals("ChargingTrip")) {
                lastTrip.setChargingTime(-1.0);
            }

            System.out.println("Result of Scheduler " + result.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
            System.out.println("ChargingTime " + result.stream().map(t -> t.getChargingTime() == null ? 0 : t.getChargingTime()).collect(Collectors.toList()));
            return result;
        } catch (Exception e) {
            System.out.println("Caught exception when creating result");
            e.printStackTrace();
            return null;
        }
    }
}
