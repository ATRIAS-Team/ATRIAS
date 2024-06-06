package io.github.agentsoz.ees.jadexextension.masterthesis.RLDJL;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;

import java.awt.image.BufferedImage;

public class EnvUtil {


    private EnvUtil(){

    }

    /*
    TODO:


    Data InputPath
    Data Preprocessing

     */


    public static int getRandomNumber(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }


}
