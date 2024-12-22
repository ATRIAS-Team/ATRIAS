package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Job;

public class DelegateInfo{
    public Job job;
    public long ts = -1;

    public DelegateInfo(Job job){
        this.job = job;
    }
}
