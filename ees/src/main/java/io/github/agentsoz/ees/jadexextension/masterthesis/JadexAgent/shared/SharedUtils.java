package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared;

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

import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;


import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared.SharedConstants.*;

public class SharedUtils {
    public static long getSimTime(){
        return initDateTS + (long) (JadexModel.simulationtime * 1000);
    }

    public static long getTimeStamp(LocalDateTime localDateTime){
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public static LocalDateTime getDateTime(long timeStamp){
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStamp), ZoneOffset.UTC);
    }

    public static LocalDateTime getCurrentDateTime(){
        int simTime = (int) JadexModel.simulationtime;
        Duration duration = Duration.ofSeconds(simTime);
       return SIMULATION_START_TIME_DT
               .withHour(duration.toHoursPart())
               .withMinute(duration.toMinutesPart())
               .withSecond(duration.toSecondsPart());
    }
}
