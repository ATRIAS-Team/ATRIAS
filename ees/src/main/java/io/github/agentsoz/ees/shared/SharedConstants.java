package io.github.agentsoz.ees.shared;

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

import io.github.agentsoz.ees.Run.XMLConfig;
import io.github.agentsoz.ees.firebase.FirebaseHandler;
import org.w3c.dom.Element;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static io.github.agentsoz.ees.Run.XMLConfig.assignIfNotNull;


public class SharedConstants {
    public static boolean FIREBASE_ENABLED = false;
    public static int CLEANUP_TIMER = 30000;

    private static String SIMULATION_START_TIME = "01.12.2019T00:00";

    public static LocalDateTime SIMULATION_START_TIME_DT;

    public static long initDateTS = 0;

    public static void configure(){
        Element classElement = XMLConfig.getClassElement("SharedConstants.java");
        assignIfNotNull(classElement,"FIREBASE_ENABLED", Boolean::parseBoolean,
                value -> SharedConstants.FIREBASE_ENABLED = value);
        assignIfNotNull(classElement,"SIMULATION_START_TIME", String::toString,
                value -> SharedConstants.SIMULATION_START_TIME = value);
        assignIfNotNull(classElement,"CLEANUP_TIMER", Integer::parseInt,
                value -> SharedConstants.CLEANUP_TIMER = value);

        if(!FIREBASE_ENABLED){
            SIMULATION_START_TIME_DT = LocalDateTime
                    .parse(SIMULATION_START_TIME, dateTimeFormatter);
        }else{
            SIMULATION_START_TIME_DT = LocalDateTime.now();
            FirebaseHandler.init();
        }
        initDateTS = SIMULATION_START_TIME_DT.toInstant(ZoneOffset.UTC).toEpochMilli();

    }

    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy'T'HH:mm");
}
