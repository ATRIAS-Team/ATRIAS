package io.github.agentsoz.ees.centralplanner.Simulation.Rebalancing;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import io.github.agentsoz.ees.centralplanner.Graph.Path;
import io.github.agentsoz.ees.centralplanner.Simulation.AbstractScheduler;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

public class CreateSupportingMatrices extends AbstractScheduler {
    public CreateSupportingMatrices(HashMap<String, String> configMap) {
        super(configMap);
    }


    public void run(){
        System.out.println("\nTravel Time Matrix Calculator started");

//        double[][] travelTimeMatrix = loadTravelTimeMatrix("travelTimeMatrix" + configMap.get("THETA") + ".dat");
//        if (travelTimeMatrix == null) {
//            travelTimeMatrix = createTravelTimeMatrix();
//            saveTravelTimeMatrix(travelTimeMatrix);
//        }

        int[][] reachabilityMatrix = loadReachabilityMatrix("reachabilityMatrix" + configMap.get("THETA") + ".dat");
        if (reachabilityMatrix == null) {
            reachabilityMatrix = createReachabilityMatrix();
            saveReachabilityMatrix(reachabilityMatrix);
        }
//
        ArrayList<String> centers = runILPSolverForRegionCenters(reachabilityMatrix);
        System.out.println(centers);
    }

    public ArrayList<String> runILPSolverForRegionCenters(int[][] reachabilityMatrix){
        System.out.println("ILP Solver for Region Centers started");
        Loader.loadNativeLibraries();
        CpModel model = new CpModel();

        System.out.println("ILP Solver: Adding Constraints");
        // Decision variables: x[i] ∈ {0,1}
        IntVar[] x = new IntVar[graph.nodes.size()];
        for (int i = 0; i < graph.nodes.size(); i++) {
            x[i] = model.newBoolVar("x_" + i);
        }

        // Constraints: for all j: sum over i of x[i] * R[i][j] ≥ 1
        for (int j = 0; j < graph.nodes.size(); j++) {
            List<Literal> coveringLiterals = new ArrayList<>();
            for (int i = 0; i < graph.nodes.size(); i++) {
                if (reachabilityMatrix[i][j] == 1) {
                    coveringLiterals.add((Literal) x[i]);
                }
            }
            model.addAtLeastOne(coveringLiterals);
        }

        // Objective: minimize sum of x[i]
        model.minimize(LinearExpr.sum(x));

        // Solve
        System.out.println("ILP Solver: Solving Problem");
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(1200.0);
        CpSolverStatus status = solver.solve(model);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("Minimum set size: " + solver.objectiveValue());
            ArrayList<String> centerNodes = new ArrayList<>();
            for (int i = 0; i < graph.nodes.size(); i++) {
                if (solver.booleanValue((Literal)x[i])) {
                    String center = new ArrayList<>(graph.nodes.keySet()).get(i);
                    centerNodes.add(center);
                    System.out.println("Select node: " + center);
                }
            }
            return centerNodes;
        } else {
            System.out.println("No feasible solution found.");
            return null;
        }
    }

    public int[][] convertTravelTimeToReachabilityMatrix(double[][] travelTimeMatrix) {
        System.out.println("Converting Travel Time Matrix to Reachability Matrix");
        int[][] reachabilityMatrix = new int[travelTimeMatrix.length][travelTimeMatrix[0].length];
        for (int i = 0; i < travelTimeMatrix.length; i++) {
            for (int j = 0; j < travelTimeMatrix[i].length; j++) {
                if (travelTimeMatrix[i][j] < Double.parseDouble(configMap.get("THETA"))) {
                    reachabilityMatrix[i][j] = 1;
                } else {
                    reachabilityMatrix[i][j] = 0;
                }
            }
        }
        return reachabilityMatrix;
    }

    public double[][] createTravelTimeMatrix() {
        System.out.println("Creating new Travel Time Matrix");
        double[][] travelTimeMatrix = new double[graph.nodes.size()][graph.nodes.size()];
        List<String> nodeList = new ArrayList<>(graph.nodes.keySet()); // avoid recomputing in loop
        int nodeListSize = nodeList.size();

        IntStream.range(0, nodeListSize).parallel().forEach(row -> {
            String rowNode = nodeList.get(row);
            for (int col = 0; col < nodeListSize; col++) {
                String colNode = nodeList.get(col);
                Path path = graph.euclideanDistance(rowNode, colNode);
                travelTimeMatrix[row][col] = path.travelTime;
            }
        });

        return travelTimeMatrix;
    }

    public int[][] createReachabilityMatrix() {
        System.out.println("Creating new Reachability Matrix");
        int[][] reachabilityMatrix = new int[graph.nodes.size()][graph.nodes.size()];
        List<String> nodeList = new ArrayList<>(graph.nodes.keySet()); // avoid recomputing in loop
        int nodeListSize = nodeList.size();

        IntStream.range(0, nodeListSize).parallel().forEach(row -> {
            String rowNode = nodeList.get(row);
            for (int col = 0; col < nodeListSize; col++) {
                String colNode = nodeList.get(col);
                Path path = graph.euclideanDistance(rowNode, colNode);
                if (path.travelTime < Double.parseDouble(configMap.get("THETA"))) {
                    reachabilityMatrix[row][col] = 1;
                } else {
                    reachabilityMatrix[row][col] = 0;
                }
            }
        });

        return reachabilityMatrix;
    }

    public double[][] loadTravelTimeMatrix(String path){
        double[][] loadedMatrix;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            loadedMatrix = (double[][]) ois.readObject();
            return loadedMatrix;
        } catch (IOException | ClassNotFoundException e) {
//            e.printStackTrace();
            System.out.println("Matrix file not found/not yet created.");
            return null;
        }
    }

    public int[][] loadReachabilityMatrix(String path){
        int[][] loadedMatrix;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            loadedMatrix = (int[][]) ois.readObject();
            return loadedMatrix;
        } catch (IOException | ClassNotFoundException e) {
//            e.printStackTrace();
            System.out.println("Matrix file not found/not yet created.");
            return null;
        }
    }

    public void saveTravelTimeMatrix(double[][] travelTimeMatrix) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("travelTimeMatrix"+ configMap.get("THETA") + ".dat"))) {
            oos.writeObject(travelTimeMatrix);
            System.out.println("Saved Travel Time Matrix to disk.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveReachabilityMatrix(int[][] reachabilityMatrix) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("reachabilityMatrix"+ configMap.get("THETA") + ".dat"))) {
            oos.writeObject(reachabilityMatrix);
            System.out.println("Saved Reachability Matrix to disk.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
