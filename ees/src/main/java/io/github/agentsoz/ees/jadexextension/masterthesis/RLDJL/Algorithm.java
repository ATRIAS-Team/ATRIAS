package io.github.agentsoz.ees.jadexextension.masterthesis.RLDJL;


import ai.djl.modality.rl.agent.QAgent;
import ai.djl.modality.rl.agent.RlAgent;
import ai.djl.modality.rl.agent.EpsilonGreedy;

import ai.djl.modality.rl.env.RlEnv;

import ai.djl.modality.rl.ActionSpace;
import ai.djl.modality.rl.ReplayBuffer;
import ai.djl.modality.rl.LruReplayBuffer;
import ai.djl.ndarray.NDList;

import ai.djl.Model;

import ai.djl.training.TrainingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class Algorithm implements RlAgent{


    private static Logger logger = LoggerFactory.getLogger(Algorithm.class);


    private Algorithm() {};


    public static TrainingResult runExample(String[] args) throws IOException {
        Arguments arguments = new Arguments().parseArgs(args);
        if (arguments == null) {
            return null;
        }



}


