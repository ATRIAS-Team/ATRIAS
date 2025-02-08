package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MyLogger {
    private BufferedWriter writer;
    private DateTimeFormatter dtf;
    private Status status;

    public enum Status {
        DEBUG, INFO, ERROR
    }

    public MyLogger(String logFilePath, Status status) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(logFilePath, true)); // Append mode
        this.dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.status = status;
    }

    // Log a message
    public void log(Status logLevel, String message) {
        if (logLevel.ordinal() >= this.status.ordinal()) {
            try {
                String timestamp = LocalDateTime.now().format(dtf);
                writer.write(String.format("[%s] [%s]: %s", timestamp, logLevel, message));
                writer.newLine();
                writer.flush(); // Flush to ensure the message is written immediately
            } catch (IOException e) {
                System.err.println("Error writing to log file: " + e.getMessage());
            }
        }
    }

    public void newLine() throws IOException {
        writer.newLine();
    }
    // Log different levels
    public void info(String message) {
        log(Status.INFO, message);
    }

    public void debug(String message) {
        log(Status.DEBUG, message);
    }

    public void error(String message) {
        log(Status.ERROR, message);
    }

    // Close the writer when done
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing log file: " + e.getMessage());
        }
    }
}
