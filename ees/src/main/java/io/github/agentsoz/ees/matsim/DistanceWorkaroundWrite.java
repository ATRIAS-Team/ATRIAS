package io.github.agentsoz.ees.matsim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DistanceWorkaroundWrite {


        //public String filename;
        public String delimiter = ";";

        public boolean created = false;

      //  public static String filename = "DistanceWorkaround_" + agentID + ".csv";

        private static FileWriter writer;

        public DistanceWorkaroundWrite(){};

        public static void addHeader(String agentID){
            FileWriter writer = null;
            try {
                writer = new FileWriter("DistanceWorkaround_" + agentID + ".csv", true);
                writer.append(String.join( ";", "AgentID", "Distance" + "\n"));
                writer.flush();
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static void addLog(String agentID, double distance){

            FileWriter writer = null;
            try {
                writer = new FileWriter("DistanceWorkaround_" + agentID + ".csv", true);
                writer.append(String.join(";", agentID, Double.toString(distance) + "\n"));
                writer.flush();
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }






        public static void totalLinkLengthLog(String agentID, double distance){
            File f = new File("DistanceWorkaround_" + agentID + ".csv");

            // Checking if the specified file exists or not
            if (f.exists()){
                System.out.println("Exists");
                addLog(agentID, distance);
            }
            else {

                // Show if the file does not exists
                System.out.println("Does not Exists");

                FileWriter writer = null;
                try {
                    writer = new FileWriter("DistanceWorkaround_" + agentID + ".csv", false);
                    //writer.append(String.join(delimiter, row) + "\n");//writer.flush();
                    addHeader(agentID);
                    writer.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }






  /*  public DistanceWorkaround(){

        //his.filename = "LogAgent_" + agentID + ".csv";
        //this.delimiter = ";";
        FileWriter writer = null;
        try {
            writer = new FileWriter(filename, false);
            //writer.append(String.join(delimiter, row) + "\n");
            //writer.flush();
            addHeader();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

*/


            //addRow("AgentID", "TripID", "DriveOperationNumber", "TripType",
            //      "BatteryBefore", "BatteryAfter", "ArrivedAtLocation", "Distance", "Origin");
        }


/**
 public static void addRow(String... row){


 FileWriter writer = null;
 try {
 writer = new FileWriter(filename, true);
 writer.append(String.join(delimiter, row) + "\n");
 writer.flush();
 writer.close();
 } catch (IOException e) {
 throw new RuntimeException(e);
 }
 }
 **/
    }
