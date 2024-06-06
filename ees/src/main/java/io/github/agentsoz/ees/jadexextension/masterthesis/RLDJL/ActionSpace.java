package io.github.agentsoz.ees.jadexextension.masterthesis.RLDJL;

import ai.djl.ndarray.NDList;
import ai.djl.util.RandomUtils;

import java.util.ArrayList;

public class ActionSpace extends ArrayList<NDList> {

    private static final long serialVersionUID = 8683452581122892189L;
    public NDList randomAction() {
        return get(RandomUtils.nextInt(size()));
    }

}
