package io.github.agentsoz.ees.centralplanner.util;

public class ProgressTracker {
    public static void showProgress(int currentIteration, int totalIterations) {
        int barLength = 50; // Length of the progress bar
        int progress = (currentIteration * 100) / totalIterations;
        int filled = (currentIteration * barLength) / totalIterations;

        // Build the progress bar
        String bar = "=".repeat(filled) + " ".repeat(barLength - filled);

        // Print the progress bar and percentage
        System.out.print("\r[" + bar + "] " + progress + "%");
    }
}

