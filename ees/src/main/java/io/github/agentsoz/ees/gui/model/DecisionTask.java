package io.github.agentsoz.ees.gui.model;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DecisionTask {
    public Job job;

    public long timeStamp;

    public long numRequests = 0;
    public AtomicLong numResponses = new AtomicLong(0);


    public String origin;

    public List<UTScore> UTScoreList = Collections.synchronizedList(new ArrayList<>());

    public volatile Status status;

    public Set<String> agentIds = ConcurrentHashMap.newKeySet();

    public boolean isLocal;
    public String cell;

    public String associatedTrip;

}
