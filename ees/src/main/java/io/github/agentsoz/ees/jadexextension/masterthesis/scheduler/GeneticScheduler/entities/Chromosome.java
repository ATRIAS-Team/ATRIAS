package io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.entities;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.BatteryModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GeneticScheduler.utils.GeneticUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Chromosome {

    private Config config;
    private final List<Gene> customerChromosome;
    private final List<Gene> chargingChromosome;
    private final Random random;
    private final GeneticUtils geneticUtils;

    public Chromosome(List<Gene> customerChromosome, List<Gene> chargingChromosome, Config config) {
        this.customerChromosome = Collections.unmodifiableList(customerChromosome);
        this.chargingChromosome = Collections.unmodifiableList(chargingChromosome);
        this.config = config;
        this.random = new Random();
        this.geneticUtils = new GeneticUtils(config);
    }

    public List<Gene> mergeGenes() {
        List<Gene> resultGene = new ArrayList<>();
        try {
            for (int i = 0; i < customerChromosome.size(); i++) {
                if (chargingChromosome.get(i) != null) {
                    resultGene.add(chargingChromosome.get(i));
                }
                resultGene.add(customerChromosome.get(i));
            }
            // add last charging chromosome if not null
            if (chargingChromosome.get(customerChromosome.size()) != null) {
                resultGene.add(chargingChromosome.get(customerChromosome.size()));
            }
            return resultGene;
        } catch (IndexOutOfBoundsException e) {
            System.out.println("what the...");
        }
        return resultGene;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final Gene gene : mergeGenes()) {
            sb.append(gene.toString()).append(" : ");
        }
        return sb.toString();
    }

    double fitness() {
        // calculate fitness for merged genes
        List<Gene> chromosomeToEvaluate = mergeGenes();

        boolean vehicleBreaksDownRisk = false;
        BatteryModel model = new BatteryModel();
        model.setMyChargestate(config.getBatteryLevel());
        // distance is agentlocation to first gene => create gen for agentLocation
        // if end location is null the start location is used to determine the distance between two genes
        Gene agentGene = new Gene(null, config.getAgentLocation(), null, null, null, config);
        double distance = agentGene.distance(chromosomeToEvaluate.get(0));
        List<Double> waitingTimes = new ArrayList<>();
        for (int i = 0; i < chromosomeToEvaluate.size() - 1; i++) {
            distance += chromosomeToEvaluate.get(i).distance(chromosomeToEvaluate.get(i + 1));
            model.discharge(distance, 0, false);
            // battery level shouldn't get near 20%
            if (model.getMyChargestate() <= 0.2) {
                vehicleBreaksDownRisk = true;
                break;
            }
            waitingTimes.add(
                    chromosomeToEvaluate.get(i).calculateWaitingTime(chromosomeToEvaluate.subList(0, i + 1))
            );
        }
        Double chargingTime = chromosomeToEvaluate.stream()
                .map(g -> g.getChargingTime() == null ? 0.0 : g.getChargingTime())
                .collect(Collectors.summingDouble(Double::doubleValue));
        model.charge(chargingTime);

        // overall waiting time minimieren?
        int odr = waitingTimes.stream().filter(wt -> wt > config.getTHETA()).collect(Collectors.toList()).size();

        if (vehicleBreaksDownRisk) { return 0.0; }

        // Becomes smaller the greater the distance
        Double fitnessFractionDistance = 1 / distance;
        // 0.1 was added, as otherwise the fraction is equal to 1 with an ODR value of 1. However, this should only be
        // 1 if the ODR is 0.
        Double fitnessFractionODR = 1 - (odr / Double.valueOf(this.customerChromosome.size()));

//        int chargingStationSize = chromosomeToEvaluate.stream()
//                .filter(g -> g.getEnd() == null)
//                .collect(Collectors.toList())
//                .size();

        // The higher the fitness, the better it is
        // fitnessFractionDistance, fitnessFractionODR, model.getMyChargeState € [0,1]
        return (fitnessFractionDistance * 100) + fitnessFractionODR  + model.getMyChargestate();
    }

    Chromosome[] crossover(final Chromosome otherChromosome) {
        try {
            // halbiere das Chromosom und füge die zweite hälfte des ersten an die erste hälfte des zweiten ein und umgekehrt
            List<List<Gene>> customerCrossOver = geneticUtils.makeCrossoverCustomer(customerChromosome, otherChromosome.customerChromosome);
            List<List<Gene>> chargingCrossOver = geneticUtils.makeCrossoverCharging(chargingChromosome, otherChromosome.chargingChromosome);

            if (customerCrossOver.get(0).size() != customerChromosome.size()
                    || customerCrossOver.get(1).size() != customerChromosome.size()
                    || chargingCrossOver.get(0).size() != chargingChromosome.size()
                    || chargingCrossOver.get(1).size() != chargingChromosome.size()) {
                System.out.println("Caught Exception in Crossover: \n  this.customer - " + this.customerChromosome
                        + " | this.charging - " + this.chargingChromosome
                        + "\n  Other customer - " + otherChromosome.customerChromosome + " | Other charging - " + otherChromosome.chargingChromosome);
            }

            return new Chromosome[] {
                    new Chromosome(customerCrossOver.get(0), chargingCrossOver.get(0), config),
                    new Chromosome(customerCrossOver.get(1), chargingCrossOver.get(1), config)
            };
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    Chromosome mutate() {
        try {
            // mutate customer trips and insert or delete charging trips

            // case customer trip
            final List<Gene> customerCopy = new ArrayList<>(this.customerChromosome);
            if (this.customerChromosome.size() > 1) {
                int size = this.customerChromosome.size();
                int indexA = random.nextInt(size);
                int indexB = random.nextInt(size);
                while (indexA == indexB) {
                    indexA = random.nextInt(size);
                    indexB = random.nextInt(size);
                }
                Collections.swap(customerCopy, indexA, indexB);
            }

            // case charging trip
            // swap random or change the charging times of random charging trips
            Random rand = new Random();
            boolean doSwap = rand.nextBoolean();

            List<Gene> chargingCopy = new ArrayList<>(this.chargingChromosome);
            if (doSwap) {
                if (chargingCopy.size() == 2) {
                    Collections.swap(chargingCopy, 0, 1);
                } else {
                    int size = this.chargingChromosome.size();
                    int indexACharging = random.nextInt(size);
                    int indexBCharging = random.nextInt(size);
                    while (indexACharging == indexBCharging) {
                        indexACharging = random.nextInt(size);
                        indexBCharging = random.nextInt(size);
                    }
                    Collections.swap(chargingCopy, indexACharging, indexBCharging);
                }
            } else {
                chargingCopy = geneticUtils.mutateChargingTimes(chargingCopy);
            }

            return new Chromosome(customerCopy, chargingCopy, config);
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
            return null;
        }
    }

    public List<Gene> getCustomerChromosome() {
        return customerChromosome;
    }

    public List<Gene> getChargingChromosome() {
        return chargingChromosome;
    }
}
