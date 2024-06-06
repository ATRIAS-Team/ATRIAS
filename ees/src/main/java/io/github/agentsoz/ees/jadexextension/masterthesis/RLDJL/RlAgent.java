package io.github.agentsoz.ees.jadexextension.masterthesis.RLDJL;

import io.github.agentsoz.ees.jadexextension.masterthesis.RLDJL.RlEnv;
import ai.djl.ndarray.NDList;

public interface RlAgent {


    NDList chooseAction(RlEnv env, boolean training);
    void trainBatch(RlEnv.Step[] batchSteps);

}
