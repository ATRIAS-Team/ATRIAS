package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.io.FileWriter;
import java.io.IOException;

public class csvLogger {
    public static String filename;
    public static String delimiter = ";";

    public boolean created = false;

    public csvLogger(){}



    public csvLogger(String agentID){
        this.filename = "LogAgent_" + agentID + ".csv";
        //this.delimiter = ";";
        addRow("AgentID", "TripID", "DriveOperationNumber", "TripType",
                "BatteryBefore", "BatteryAfter", "ArrivedAtLocation", "Distance", "Origin");
    }

    public static void addHeader(){
        addRow("AgentID", "TripID", "DriveOperationNumber", "TripType",
                "BatteryBefore", "BatteryAfter", "ArrivedAtLocation", "Distance", "Origin");
    }

    public String getFilname(){return filename;}



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
}