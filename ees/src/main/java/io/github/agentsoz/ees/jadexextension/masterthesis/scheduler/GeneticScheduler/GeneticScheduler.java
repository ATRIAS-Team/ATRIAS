package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
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

    // Es sollen Kanten und Punkte miteinander verbunden werden
    // Kanten stellen Kundenfahrten dar, die von einem Start zu einem Endpunkt verlaufen
    // Einzelne Punkte sind Ladestationen

    // Wird ein Trip nicht mit in die Lösung aufgenommen ist das nicht schlimm odr würde steigen?

    // Step 1 Generiere zuerste eine Menge von Chromosomen (mögliche Lösungen)
    // Step 2 Crossover/ Mutation/ Inversion + evtl. weiter k Random erstelle Chromosome
    // Evtl. starten mit Kruskal MST?


    // Zuerst benötigt man eine initiale population (1), dann eine fitness funktion (2) die diese bewertet. Dann werden die
    // besten x Chromosome aus der Population ausgewählt (3) und mit Hilfe von Crossover und Mutation verändert (4)
    // Schritt 3 - 4 werden wiederholt bis eine akzeptabele Lösung gefunden wurde.

    // Eine Aktion ist ein Gen, eine Menge von Genen ein Chromosom
    Population population;
    private int chargingStationsCounter = 0;

    public List<Trip> start(List<Trip> tripsToSchedule, int iterations) {
        try {
            // initial population size in abhängigkeit von der anzahl der trip -> max (2n + 1)! Permutation
            int n = tripsToSchedule.size() * 2 + 1;
            int totalPermutations = 1;
            for (int i = 1; i <= n; i++) {
                totalPermutations *= i;
            }
            // Get 10% of amount or 1
            int initialPopulationSize = (totalPermutations / 10) < 250 ? 250 : Math.min(totalPermutations / 10, 1000);

            System.out.println("Gen Sched Trip to schedule: " + tripsToSchedule.stream().map(t -> t.getTripID()).collect(Collectors.toList()).toString());
            System.out.println("Popsize " + initialPopulationSize);

            population = new Population(initialPopulationSize, tripsToSchedule, this.config);

            for (int i = 0; i < iterations; i++) {
//            System.out.println("Iteration " + i);
                population.update();
            }

            List<Gene> genes = population.getBestChromosome().mergeGenes();
            return mapGenesBackToListOfTrip(genes, tripsToSchedule);
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
