package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class SharedPlans {
    public static void cleanupReceivedMessages(Map<UUID, Long> messages){
        Iterator<Long> iterator = messages.values().iterator();
        long currentTimeStamp = Instant.now().toEpochMilli();
        while (iterator.hasNext()){
            long timeStamp = iterator.next();
            if(currentTimeStamp >= timeStamp + 30000){
                iterator.remove();
            }
        }
    }
}
