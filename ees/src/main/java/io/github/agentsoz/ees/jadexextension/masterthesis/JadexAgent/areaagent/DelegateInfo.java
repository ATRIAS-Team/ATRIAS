package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Job;
import java.util.HashMap;
import java.util.Map;

public class DelegateInfo{
    public Job job;
    public long timeStamp = -1;
    public Map<String, Long> agentHops = new HashMap<>();

    public DelegateInfo(Job job){
        this.job = job;
    }
}
