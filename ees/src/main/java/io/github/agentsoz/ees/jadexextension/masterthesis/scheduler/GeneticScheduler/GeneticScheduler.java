package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Chromosome;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Config;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Gene;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities.Population;

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
            if (tripsToSchedule.size() == 1 && config.getBatteryLevel() >= 0.9) {
                return tripsToSchedule;
            }

            List<Double> scores = new ArrayList<>();
            List<Chromosome> chromosomes = new ArrayList<>();
            for (int i = 0; i < runs; i++) {
                // initial population size in abhängigkeit von der anzahl der trip -> max (2n + 1)! Permutation
                // oder feste populationsgröße
                int n = tripsToSchedule.size() * 2 + 1;
                int totalPermutations = 1;
                for (int k = 1; k <= n; k++) {
                    totalPermutations *= k;
                }
                // Get 10% of amount but maximum of 500
                int initialPopulationSize = (totalPermutations / 10) < 100 ? 100 : Math.min(totalPermutations / 10, 300);

                System.out.println("Gen Sched Trip to schedule: " + tripsToSchedule.stream().map(t -> t.getTripID()).collect(Collectors.toList()).toString());
                System.out.println("Popsize " + initialPopulationSize);

                population = new Population(initialPopulationSize, tripsToSchedule, this.config);

                for (int j = 0; j < iterations; j++) {
                    population.update();
                }

                Chromosome best = population.getBestChromosome();
                scores.add(best.fitnessOld());
                chromosomes.add(best);
            }

            int index = 0;
            double maxVal = 0.0;
            for (int i = 0; i < scores.size(); i++) {
                if (scores.get(i) > maxVal) {
                    index = i;
                    maxVal = scores.get(i);
                }
            }
            return mapGenesBackToListOfTrip(chromosomes.get(index).mergeGenes(), tripsToSchedule);
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
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
