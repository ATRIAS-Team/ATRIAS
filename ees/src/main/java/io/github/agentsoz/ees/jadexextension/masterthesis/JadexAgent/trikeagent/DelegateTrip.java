package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.trikeagent;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;

import java.util.ArrayList;
import java.util.List;

public class DelegateTrip {
    public String tripId;
    public long ts = -1;

    public List<String> offers = new ArrayList<>();

    public DelegateTrip(String tripId){
        this.tripId = tripId;
    }


}
