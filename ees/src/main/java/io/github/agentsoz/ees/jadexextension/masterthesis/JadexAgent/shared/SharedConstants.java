package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared;

import io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig;
import org.w3c.dom.Element;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig.assignIfNotNull;

public class SharedConstants {
    public static boolean FIREBASE_ENABLED = false;
    public static int CLEANUP_TIMER = 30000;

    private static String SIMULATION_START_TIME = "01.12.2019 00:00";

    public static LocalDateTime SIMULATION_START_TIME_DT;

    public static long initDateTS = 0;

    public static void configure(){
        Element classElement = XMLConfig.getClassElement("TrikeAgent.java");
        assignIfNotNull(classElement,"FIREBASE_ENABLED", Boolean::parseBoolean,
                value -> SharedConstants.FIREBASE_ENABLED = value);
        assignIfNotNull(classElement,"SIMULATION_START_TIME", String::toString,
                value -> SharedConstants.SIMULATION_START_TIME = value);
        assignIfNotNull(classElement,"CLEANUP_TIMER", Integer::parseInt,
                value -> SharedConstants.CLEANUP_TIMER = value);

        SIMULATION_START_TIME_DT = LocalDateTime
                .parse(SIMULATION_START_TIME, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm[:ss]"));

        initDateTS = SIMULATION_START_TIME_DT.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
