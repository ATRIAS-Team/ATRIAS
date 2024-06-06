package io.github.agentsoz.ees.jadexextension.masterthesis.RLDJL;

public interface ReplayBuffer {

    RlEnv.Step[] getBatch();

    void closeStep();

    void addStep(RlEnv.Step step);


}
