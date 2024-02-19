package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.io.FileWriter;
import java.io.IOException;

public class csvLogger {


    public void logTrip(){
        System.out.println("Logger test");
    }

    //contructor here
/**
    public static void logTripCsv(String newData) {
        try (FileWriter writer = new FileWriter(filePath)) {

            // Write the header
            for (int i = 0; i < header.length; i++) {
                writer.append(header[i]);
                if (i < header.length - 1) {
                    writer.append(";");
                }
            }
            writer.append("\n");

            // Write the data
            for (String[] row : data) {
                for (int i = 0; i < row.length; i++) {
                    writer.append(row[i]);
                    if (i < row.length - 1) {
                        writer.append(";");
                    }
                }
                writer.append("\n");
            }

            System.out.println("CSV file created successfully!");

        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Example usage
        String filePath = "csvLog.csv";
        String[] header = {"AgentID", "TripID", "Drive Operation Number", "TripType",
                "BatteryBefore", "BatteryAfter", "Success", "CustomerMiss", "Distance", "Origin"};
        String[][] data = {
                {"1", "123", "456", "Business", "80%", "70%", "true", "false", "20.5", "City A"},
                {"2", "456", "789", "Personal", "90%", "85%", "true", "true", "15.2", "City B"},
                {"3", "789", "101", "Business", "75%", "60%", "false", "true", "25.0", "City C"}
        };

        writeCsv(filePath, header, data);
    }
**/
}
