package io.github.agentsoz.ees.Run;

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

import java.io.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Experiment {

    private static String projectRoot = Paths.get("").toAbsolutePath().toString();

    public static void main(String[] args) {
        System.out.println("Project Root: " + projectRoot);
        // List of environment variables to apply for each run
        File[] configFiles = new File("configs").listFiles();

        // create jar
        String[] mavenBuildCommand = {"./mvnw.cmd", "clean", "package", "-DskipTests"};
        System.out.println("Building...");
        runCommand(mavenBuildCommand, new HashMap<>());

        for (File configFile : configFiles) {
            String[] cmd = {
                    "C:\\Program Files\\Java\\jdk-11.0.17\\bin\\java.exe",
                    "-jar",
                    "ees/out-fat.jar"
            };

            // Run the pre-built application with the environment variables
            System.out.println("Running...");
            Map<String, String> env = new HashMap<>();
            env.put("ConfigFile", configFile.getName());
            int runResult = runCommand(cmd, env);
            System.out.println("Run finished!" + "Code: " + runResult);
        }
    }

    private static int runCommand(String[] cmd, Map<String, String> envs){
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(cmd);

        for (String key: envs.keySet()) {
            processBuilder.environment().put(key, envs.get(key));
        }
        processBuilder.directory(new java.io.File(projectRoot));

        try {
            // Start the process and wait for it to finish
            Process process = processBuilder.start();

            // Capture standard output and error output in separate threads
            new Thread(() -> readStream(process.getInputStream(), "STDOUT")).start();
            new Thread(() -> readStream(process.getErrorStream(), "STDERR")).start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down... Stopping JAR process.");
                stopJar(process);
            }));

            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return 1;
        }
    }

    public static void stopJar(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
            }
        }
    }

    private static void readStream(InputStream stream, String type) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[" + type + "]: " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
