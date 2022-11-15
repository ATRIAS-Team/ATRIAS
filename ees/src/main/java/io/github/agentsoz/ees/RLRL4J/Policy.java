package io.github.agentsoz.ees.RLRL4J;

import java.util.Random;

public class Policy {
    private double[][] qTable;
    private double explorationRate; // Epsilon for epsilon-greedy strategy
    private Random random;

    public Policy(int numStates, int numActions, double initialQValue, double explorationRate) {
        this.qTable = new double[numStates][numActions];
        this.explorationRate = explorationRate;
        this.random = new Random();

        // Initialize the Q-table with initial Q-values
        for (int state = 0; state < numStates; state++) {
            for (int action = 0; action < numActions; action++) {
                qTable[state][action] = initialQValue;
            }
        }
    }

    public int selectAction(int state) {
        // Epsilon-greedy strategy: Explore with probability explorationRate
        if (random.nextDouble() < explorationRate) {
            // Explore: Choose a random action
            return random.nextInt(qTable[state].length);
        } else {
            // Exploit: Choose the action with the highest Q-value
            return getBestAction(state);
        }
    }

    public void updateQValue(int state, int action, double newValue) {
        // Update the Q-value for the given state-action pair
        qTable[state][action] = newValue;
    }

    public double getQValue(int state, int action) {
        // Get the Q-value for the given state-action pair
        return qTable[state][action];
    }

    public int getBestAction(int state) {
        // Find the action with the highest Q-value in the given state
        int bestAction = 0;
        double bestQValue = qTable[state][0];

        for (int action = 1; action < qTable[state].length; action++) {
            if (qTable[state][action] > bestQValue) {
                bestAction = action;
                bestQValue = qTable[state][action];
            }
        }

        return bestAction;
    }
}