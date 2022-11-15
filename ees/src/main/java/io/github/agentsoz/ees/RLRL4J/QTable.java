package io.github.agentsoz.ees.RLRL4J;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class QTable {
    private Map<String, Map<String, Double>> qTable;
    private final double learningRate;
    private final double rewardDecay;
    private final double epsilon;
    private final String[] actions;

    public QTable(String[] actions, double learningRate, double rewardDecay, double epsilon) {
        this.actions = actions;
        this.learningRate = learningRate;
        this.rewardDecay = rewardDecay;
        this.epsilon = epsilon;
        this.qTable = new HashMap<>();
    }

    public String chooseAction(String observation) {
        checkStateExist(observation);
        if (Math.random() < epsilon) {
            // Choose the best action
            Map<String, Double> stateAction = qTable.get(observation);
            double maxQValue = stateAction.values().stream().max(Double::compareTo).orElse(0.0);
            return stateAction.entrySet().stream()
                    .filter(entry -> entry.getValue() == maxQValue)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(actions[0]);
        } else {
            // Choose a random action
            return actions[new Random().nextInt(actions.length)];
        }
    }

    public void learn(String state, String action, double reward, String nextState) {
        checkStateExist(nextState);
        double qPredict = qTable.get(state).get(action);
        double qTarget = (nextState.equals("terminal")) ? reward : reward + rewardDecay * qTable.get(nextState).values().stream().max(Double::compareTo).orElse(0.0);
        qTable.get(state).put(action, qPredict + learningRate * (qTarget - qPredict));
    }

    private void checkStateExist(String state) {
        if (!qTable.containsKey(state)) {
            Map<String, Double> newActionMap = new HashMap<>();
            for (String action : actions) {
                newActionMap.put(action, 0.0);
            }
            qTable.put(state, newActionMap);
        }
    }

    public void plotTotalReward(double[] totalRewardList) {
        // You can implement your own plotting method for Java, or use a third-party library like JFreeChart.
        // This example doesn't include plotting.
    }

    public static void main(String[] args) {
        // Example usage of the QLearningTable class
        String[] actions = {"action1", "action2", "action3"};
        QTable qLearningTable = new QTable(actions, 0.01, 0.9, 0.9);
        String observation = "state1";
        String action = qLearningTable.chooseAction(observation);
        double reward = 0.5;
        String nextState = "state2";
        qLearningTable.learn(observation, action, reward, nextState);
    }
}