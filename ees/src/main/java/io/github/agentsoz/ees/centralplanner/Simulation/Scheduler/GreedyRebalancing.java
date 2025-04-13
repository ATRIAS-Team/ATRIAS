package io.github.agentsoz.ees.centralplanner.Simulation.Scheduler;

import com.google.ortools.sat.*;
import io.github.agentsoz.ees.centralplanner.Graph.Path;
import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;
import io.github.agentsoz.ees.centralplanner.Simulation.Rebalancing.ParticleFilter;
import io.github.agentsoz.ees.centralplanner.Simulation.Trip;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

import com.google.ortools.Loader;

import static io.github.agentsoz.ees.centralplanner.util.Util.showProgress;

public class GreedyRebalancing extends AbstractScheduler {
//    public String[] regions = {"61484874", "63571446", "432805679", "61354231", "61359859", "11235155997", "12218206797", "61374815", "61464388"};
    public String[] regions = {"61514870", "61358180", "12352731643", "61355864", "11137326980", "61342086", "61326743", "71921397", "2938306524", "61406534", "73117497", "61346276", "61695020", "1534929960", "61464388"};
    HashMap<String, ParticleFilter> filterMap = new HashMap<>();
    RegionTracker regionTracker = new RegionTracker();

    public GreedyRebalancing(HashMap<String, String> configMap) {
        super(configMap);
        for (String region : regions){
            filterMap.put(region, new ParticleFilter(1000, 0, 32, 0.5));
        }
    }

    public void run(){
        System.out.println("\nGreedyRebalancing started");

        for (int i = 0; i < requestedTrips.size(); i++){
            Trip customerTrip = requestedTrips.get(i);
            //------- schedule request ------
//            BruteForceScheduler sim = new BruteForceScheduler(configMap);
            GreedyScheduler sim = new GreedyScheduler(configMap);
//            sim.SEARCH_DEPTH = 1;
            sim.vehicles = copyAllVehicles();
            ArrayList<Trip> currentTrip = new ArrayList<>();
            currentTrip.add(customerTrip);
            sim.requestedTrips = currentTrip;
            sim.graph = graph;
            sim.run();
            vehicles = sim.vehicles;

            showProgress(i, requestedTrips.size());

            //--------- rebalance ----------

            int idleVehicles = getIdleVehicles().size();
            HashMap<String, ArrayList<Vehicle>> IdleVehicleRegionDistribution = getIdleVehicleRegionDistribution();

            HashMap<String, Integer> neededVehiclesPerRegion = new HashMap<>();
            regionTracker.addRequest(customerTrip);
            double summedDemand = regionTracker.getSummedPredictedDemand();
            for (String region : regions){
                double percentualDemand = regionTracker.getPredictedDemand(region)/summedDemand;
                int neededVehicles = (int) Math.round(idleVehicles * percentualDemand);
                neededVehiclesPerRegion.put(region, neededVehicles);
            }
            // identify surplus
            HashMap<String, Integer> deficitSurplusRegions = new HashMap<>();
            for (String region : regions){
                int delta = neededVehiclesPerRegion.get(region) - IdleVehicleRegionDistribution.get(region).size();
                deficitSurplusRegions.put(region, delta);
            }

            for (String deficitRegion : regions) {
                int deficit = deficitSurplusRegions.get(deficitRegion);

                if (deficit > 0) { // this region needs vehicles
                    // Priority queue to get the closest vehicles first
                    PriorityQueue<VehicleDistance> closestVehicles = new PriorityQueue<>(Comparator.comparingDouble(v -> v.travelTime));

                    for (String surplusRegion : regions) {
                        int surplus = deficitSurplusRegions.get(surplusRegion);

                        if (surplus < 0) { // surplus region
                            List<Vehicle> vehicles = IdleVehicleRegionDistribution.get(surplusRegion);

                            for (Vehicle v : vehicles) {
                                Path path = graph.euclideanDistance(v.currentPosition, deficitRegion);
                                closestVehicles.add(new VehicleDistance(v, surplusRegion, path.travelTime));
                            }
                        }
                    }

                    // Allocate vehicles up to the needed deficit
                    int allocated = 0;
                    while (!closestVehicles.isEmpty() && allocated < deficit) {
                        VehicleDistance vd = closestVehicles.poll();

                        // Reassign vehicle vd.vehicle to deficitRegion
                        vd.vehicle.queueRebalancingTrip(graph, deficitRegion);

                        // Remove from original region's pool
                        IdleVehicleRegionDistribution.get(vd.fromRegion).remove(vd.vehicle);

                        // Track surplus decrement (optional bookkeeping)
                        deficitSurplusRegions.put(vd.fromRegion, deficitSurplusRegions.get(vd.fromRegion) + 1);

                        allocated++;
                    }
                }
            }

        }

    }

    class VehicleDistance {
        Vehicle vehicle;
        String fromRegion;
        double travelTime;

        VehicleDistance(Vehicle vehicle, String fromRegion, double travelTime) {
            this.vehicle = vehicle;
            this.fromRegion = fromRegion;
            this.travelTime = travelTime;
        }
    }

    public class RegionTracker{
        HashMap<String, ArrayList<LocalDateTime>> regionDemandMap = new HashMap<>();

        public void addRequest(Trip request){
            String closestToRegion = getRequestRegion(request);
            ArrayList<LocalDateTime> demandList = regionDemandMap.getOrDefault(closestToRegion, new ArrayList<>());
            demandList.add(request.bookingTime);
            regionDemandMap.put(closestToRegion, demandList);

            LocalDateTime timeDelta = request.bookingTime.minusMinutes(10);
            // Count how many booking times are within the last x minutes
            int count = 0;
            for (LocalDateTime time : demandList) {
                if (!time.isBefore(timeDelta) && !time.isAfter(request.bookingTime)) {
                    count++;
                }
            }
            filterMap.get(closestToRegion).update(count, 1);
        }

        public double getPredictedDemand(String region){
            return filterMap.get(region).estimateRate();
        }

        public double getSummedPredictedDemand(){
            double summedDemand = 0;
            for (String region : regions){
                summedDemand += getPredictedDemand(region);
            }
            return summedDemand;
        }

    }

    public ArrayList<Vehicle> getIdleVehicles() {
        ArrayList<Vehicle> idleVehicles = new ArrayList<>();
        for (Vehicle vehicle : vehicles){
            if (vehicle.queuedTrips.size() == 0){
                idleVehicles.add(vehicle);
            }
        }
        return idleVehicles;
    }

    public String getRequestRegion(Trip requestedTrip){
        String closestToRegion = null;
        double distance = Double.MAX_VALUE;
        for (String center : regions){
            Path path = graph.euclideanDistance(center, requestedTrip.nearestStartNode);
            if (path.distance<distance){
                distance = path.distance;
                closestToRegion = center;
            }
        }
        return closestToRegion;
    }

    public HashMap<String, ArrayList<Vehicle>> getIdleVehicleRegionDistribution(){
        HashMap<String, ArrayList<Vehicle>> vehicleRegionMap = new HashMap<>();
        for (String region : regions){
            vehicleRegionMap.put(region, new ArrayList<>());
        }
//        for (Vehicle vehicle : vehicles){
        for (Vehicle vehicle : getIdleVehicles()){
            String closestToRegion = null;
            double distance = Double.MAX_VALUE;
            for (String center : regions){
                Path path = graph.euclideanDistance(center, vehicle.futurePosition);
                if (path.distance<distance){
                    distance = path.distance;
                    closestToRegion = center;
                }
            }
            ArrayList<Vehicle> vehicleList = vehicleRegionMap.get(closestToRegion);
            vehicleList.add(vehicle);
            vehicleRegionMap.put(closestToRegion, vehicleList);
        }
        return vehicleRegionMap;
    }
}
