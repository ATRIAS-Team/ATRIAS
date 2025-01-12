package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent;

import org.w3c.dom.Element;

import static io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig.assignIfNotNull;

public class AreaConstants {
    public static int REQUEST_WAIT_TIME = 20000;

    public static int MIN_TRIKES = 4;

    public static int NEIGHBOURS_WAIT_TIME = 10000;

    public static String CSV_SOURCE = "ees/subsample_2.csv";

    public static void configure(Element classElement) {
        assignIfNotNull(classElement,"CSV_SOURCE", String::toString,
                value -> AreaConstants.CSV_SOURCE = value);
        assignIfNotNull(classElement,"REQUEST_WAIT_TIME", Integer::parseInt,
                value -> AreaConstants.REQUEST_WAIT_TIME = value);
        assignIfNotNull(classElement,"MIN_TRIKES", Integer::parseInt,
                value -> AreaConstants.MIN_TRIKES = value);
        assignIfNotNull(classElement,"NEIGHBOURS_WAIT_TIME", Integer::parseInt,
                value -> AreaConstants.NEIGHBOURS_WAIT_TIME = value);
    }
}
