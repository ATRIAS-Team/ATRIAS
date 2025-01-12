package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared.SharedConstants.CLEANUP_TIMER;

public class SharedPlans {
    public static void cleanupReceivedMessages(Map<UUID, Long> messages){
        Iterator<Long> iterator = messages.values().iterator();
        long currentTimeStamp = SharedUtils.getSimTime();

        while (iterator.hasNext()){
            long timeStamp = iterator.next();
            if(currentTimeStamp >= timeStamp + CLEANUP_TIMER){
                iterator.remove();
            }
        }
    }
}
