package io.github.agentsoz.ees.areaagent;

import java.util.List;

import static io.github.agentsoz.ees.areaagent.AreaConstants.NO_TRIKES_NO_TRIPS_LOAD;
import static io.github.agentsoz.ees.shared.SharedUtils.getSimTime;

/**
 * This class holds important variables and flags to run a multi-AreaAgent setup.
 * */
public class Rebalance {

    public List<String> neighbourIds = null;
    public int MIN_TRIKES = AreaConstants.MIN_TRIKES;

    private double load = NO_TRIKES_NO_TRIPS_LOAD;

    public void setLoad(double value) {
        this.load = value;
    }

    public double getLoad() {
        return this.load;
    }

    public Object loadLock = new Object();
    public long lastDelegateRequestTS = -1;
    public long lastMinTrikesUpdateTS = -1;
    public long rebalanceInitTS = getSimTime() + 720000;
    public long lastLoadUpdateTS = -1;
}
