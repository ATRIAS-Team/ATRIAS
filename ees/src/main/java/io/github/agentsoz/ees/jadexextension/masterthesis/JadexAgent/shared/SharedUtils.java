package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared;

import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;


import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared.SharedConstants.initDateTS;

public class SharedUtils {
    public static long getSimTime(){
        return initDateTS + (long) (JadexModel.simulationtime * 1000);
    }

    public static long getTimeStamp(LocalDateTime localDateTime){
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
