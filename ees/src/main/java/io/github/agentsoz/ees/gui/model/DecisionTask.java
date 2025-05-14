package io.github.agentsoz.ees.gui.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionTask {
    Job job;

    long timeStamp;

    long numRequests = 0;
    AtomicLong numResponses = new AtomicLong(0);


     String origin;

     List<UTScore> UTScoreList = Collections.synchronizedList(new ArrayList<>());

     volatile Status status;

     Set<String> agentIds = ConcurrentHashMap.newKeySet();

    boolean isLocal;
    String cell;

     String associatedTrip;

}
