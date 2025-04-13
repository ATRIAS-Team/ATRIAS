package io.github.agentsoz.ees.centralplanner.Simulation.Rebalancing;
import java.util.*;

public class ParticleFilter {
    static class Particle {
        double lambda;  // The estimated rate
        double weight;  // The particle weight

        Particle(double lambda, double weight) {
            this.lambda = lambda;
            this.weight = weight;
        }

        Particle copy() {
            return new Particle(this.lambda, this.weight);
        }
    }

    private List<Particle> particles;
    private int numParticles;
    private double volatility;
    private Random rand;

    public ParticleFilter(int numParticles, double lowerBound, double upperBound, double volatility) {
        this.numParticles = numParticles;
        this.volatility = volatility;
        this.rand = new Random();
        this.particles = new ArrayList<>();

        double initWeight = 1.0 / numParticles;
        for (int i = 0; i < numParticles; i++) {
            double lambda = lowerBound + (upperBound - lowerBound) * rand.nextDouble();
            particles.add(new Particle(lambda, initWeight));
        }
    }

    // Main update step
    public void update(int observedCount, double deltaT) {
        // 1. Resample
        List<Particle> resampled = resample();

        // 2. Drift via Wiener process
        for (Particle p : resampled) {
            double noise = rand.nextGaussian() * Math.sqrt(volatility * deltaT);
            p.lambda = Math.max(0, p.lambda + noise); // Keep lambda ≥ 0
        }

        // 3. Weight update based on observed count
        double weightSum = 0;
        for (Particle p : resampled) {
            double lambdaT = p.lambda * deltaT;
            double likelihood = poissonProbability(observedCount, lambdaT);
            p.weight *= likelihood;
            weightSum += p.weight;
        }

        // 4. Normalize weights
        for (Particle p : resampled) {
            p.weight /= weightSum;
        }

        this.particles = resampled;
    }

    // Estimate current rate
    public double estimateRate() {
        return particles.stream().mapToDouble(p -> p.lambda * p.weight).sum();
    }

    // Resample particles based on weights
    private List<Particle> resample() {
        List<Particle> newParticles = new ArrayList<>();
        double[] cdf = new double[particles.size()];
        cdf[0] = particles.get(0).weight;
        for (int i = 1; i < particles.size(); i++) {
            cdf[i] = cdf[i - 1] + particles.get(i).weight;
        }

        for (int i = 0; i < particles.size(); i++) {
            double u = rand.nextDouble();
            int index = Arrays.binarySearch(cdf, u);
            if (index < 0) index = -index - 1;
            newParticles.add(particles.get(index).copy());
        }
        return newParticles;
    }

    // Poisson probability mass function
    private double poissonProbability(int k, double lambda) {
        return Math.pow(lambda, k) * Math.exp(-lambda) / factorial(k);
    }

    private double factorial(int k) {
        double result = 1.0;
        for (int i = 2; i <= k; i++) {
            result *= i;
        }
        return result;
    }
}
