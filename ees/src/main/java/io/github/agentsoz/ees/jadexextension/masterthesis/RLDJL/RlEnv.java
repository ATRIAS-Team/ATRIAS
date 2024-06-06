package io.github.agentsoz.ees.jadexextension.masterthesis.RLDJL;

import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import io.github.agentsoz.ees.jadexextension.masterthesis.RLDJL.ActionSpace;
public interface RlEnv extends AutoCloseable {

    void reset();

    NDList getObservation();

    ActionSpace getActionSpace();

    void step(NDList action, boolean training);

    Step[] runEnvironment(RlAgent agent, boolean training);

    Step[] getBatch();

    @Override
    void close();


    interface Step extends AutoCloseable {

        NDList getPreObservation(NDManager manager);

        NDList getPreObservation();


        NDList getAction();

        NDList getPostObservation(NDManager manager);


        NDList getPostObservation();

        NDManager getManager();


        NDArray getReward();


        boolean isTerminal();

        /**
         * {@inheritDoc}
         */
        @Override
        void close();
    }
}
