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

import io.github.agentsoz.ees.util.Parser;
import org.w3c.dom.Element;

import java.io.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Experiment {

    private static String projectRoot = Paths.get("").toAbsolutePath().toString();
    private static String libsPath = Paths.get("", "ees", "target", "external", "eeslib-2.1.1-SNAPSHOT", "libs").toAbsolutePath().toString();
    private static String jarPath = Paths.get("", "out", "artifacts", "eeslib_jar", "eeslib.jar").toAbsolutePath().toString();

    public static void main(String[] args) {
        System.out.println("Project Root: " + projectRoot);

        // List of environment variables to apply for each run
        File[] configFiles = new File("configs").listFiles();
        /*
        // create jar
        String mavenBuildCommand = "./mvnw.cmd clean package -DskipTests";
        System.out.println("Building...");
        int buildResult = runCommand(mavenBuildCommand, new HashMap<>());
        if(buildResult == 0){
            System.out.println("Build successful!");
        }else{
            System.out.println("Build error!");

         */
        runCommand("cd ees", new HashMap<>());
        for (File configFile : configFiles) {
            Element root = Parser.parseXML("configs/" + configFile.getName());
            String runConfig = root.getElementsByTagName("config").item(0).getTextContent();

            String runCommand = "C:\\Program Files\\Java\\jdk-11.0.17\\bin\\java.exe -jar ees/out-fat.jar" +
                    " --config " + runConfig;

            // Run the pre-built application with the environment variables
            System.out.println("Running...");
            Map<String, String> env = new HashMap<>();
            env.put("ConfigFile", configFile.getName());
            int runResult = runCommand(runCommand, env);
            System.out.println("Run finished!");
        }
    }

    private static int runCommand(String javaCommand, Map<String, String> envs){
        ProcessBuilder processBuilder = new ProcessBuilder();
        // Split the command into parts for the ProcessBuilder
        String[] cmd = javaCommand.split(" ");
        processBuilder.command(cmd);


        for (String key: envs.keySet()) {
            processBuilder.environment().put(key, envs.get(key));
        }

        processBuilder.directory(new java.io.File(projectRoot));

        try {
            // Start the process and wait for it to finish
            Process process = processBuilder.start();
            // Capture the output from the process
            // Standard output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("STDOUT: " + line);
            }

            // Capture the error output
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.out.println("STDERR: " + line);
            }

            int exitCode = process.waitFor();
            return exitCode;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return 1;
        }
    }
}
