package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared;

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
