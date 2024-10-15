package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.GreedyScheduler;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.enums.Strategy;

import java.util.ArrayList;
import java.util.List;

public class MyRunnable implements Runnable {
    private List<Trip> tripList;
    private List<Trip> unsorted = new ArrayList<>();
    public volatile List<Trip> queue = new ArrayList<>();
    public volatile GreedyScheduler greedyScheduler;

    public volatile boolean finished = false;

    MyRunnable(List<Trip> tripList, GreedyScheduler greedyScheduler){
        this.greedyScheduler = greedyScheduler;
        this.tripList = tripList;
    }

    @Override
    public void run() {
        while (true){
            finished = false;
            unsorted.addAll(queue);
            //greedyScheduler.queue = queue;
            queue = new ArrayList<>();
            if(unsorted.isEmpty()){
                continue;
            }
            List<Trip> sorted = greedyScheduler.greedySchedule(unsorted, Strategy.DRIVE_TO_CUSTOMER);
            if(queue.isEmpty()){
                tripList = sorted;
                finished = true;
                unsorted = new ArrayList<>();
            }
        }
    }
}
