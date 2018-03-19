package com.royole.glline;

import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by lezh on 3/13/2018.
 */

public class GllineUtils {
    public static final int MAX_X_VALUE = 30;
    public static final int MAX_Y_VALUE = MAX_X_VALUE;
    public static final int SCALE_TIME = 3;

    public static final float PAINT_SIZE = 0.5f;

    //generate random node for test
    public UserPoint[] generateNode(int size){
        Random random = new Random();
        UserPoint[] ret = new UserPoint[size];
        for (int i=0;i<size;i++) {
            UserPoint sinNode = new UserPoint();
            sinNode.x = (float) random.nextInt(MAX_X_VALUE)/SCALE_TIME-MAX_X_VALUE/SCALE_TIME/2;
            sinNode.y = (float) random.nextInt(MAX_Y_VALUE)/SCALE_TIME-MAX_Y_VALUE/SCALE_TIME/2;
            ret[i] = sinNode;
        }
        return ret;
    }

    //generate vectors for shaders
    float[] mVertex;
    float[] textureVertex;
    float[] serialVertex;
    public void generateVectors(UserPoint[] points){
        mVertex = new float[points.length*18];
        textureVertex = new float[points.length*12];
        serialVertex = new float[points.length*6];

        for(int i=0;i<points.length;i++){
            float x = points[i].x;
            float y = points[i].y;

            // xyz for left-top node
            mVertex[i*18] = x - PAINT_SIZE;
            mVertex[i*18+1] = y + PAINT_SIZE;
            mVertex[i*18+2] = 0;

            // xyz for left-bottom node
            mVertex[i*18+3] = x - PAINT_SIZE;
            mVertex[i*18+4] = y - PAINT_SIZE;
            mVertex[i*18+5] = 0;

            // xyz for right-top node
            mVertex[i*18+6] = x + PAINT_SIZE;
            mVertex[i*18+7] = y + PAINT_SIZE;
            mVertex[i*18+8] = 0;

            // xyz for right-top node
            mVertex[i*18+9] = x + PAINT_SIZE;
            mVertex[i*18+10] = y + PAINT_SIZE;
            mVertex[i*18+11] = 0;

            // xyz for left-bottom node
            mVertex[i*18+12] = x - PAINT_SIZE;
            mVertex[i*18+13] = y - PAINT_SIZE;
            mVertex[i*18+14] = 0;

            // xyz for right-bottom node
            mVertex[i*18+15] = x + PAINT_SIZE;
            mVertex[i*18+16] = y - PAINT_SIZE;
            mVertex[i*18+17] = 0;

            //---------------------------------- texture node --------------------------------------
            // xyz for left-top texture node
            textureVertex[i*12] = 1.0f;
            textureVertex[i*12+1] = 0.0f;

            // xyz for left-bottom node
            textureVertex[i*12+2] = 1.0f;
            textureVertex[i*12+3] = 1.0f;

            // xyz for right-top node
            textureVertex[i*12+4] = 0.0f;
            textureVertex[i*12+5] = 0.0f;

            // xyz for right-top node
            textureVertex[i*12+6] = 0.0f;
            textureVertex[i*12+7] = 0.0f;

            // xyz for left-bottom node
            textureVertex[i*12+8] = 1.0f;
            textureVertex[i*12+9] = 1.0f;

            // xyz for right-bottom node
            textureVertex[i*12+10] = 0.0f;
            textureVertex[i*12+11] = 1.0f;

            serialVertex[i*6] = (float) (i*6/6);
            serialVertex[i*6+1] = (float)((i*6+1)/6);
            serialVertex[i*6+2] = (float)((i*6+2)/6);
            serialVertex[i*6+3] = (float)((i*6+3)/6);
            serialVertex[i*6+4] = (float)((i*6+4)/6);
            serialVertex[i*6+5] = (float)((i*6+5)/6);

            //Log.e("LEZH DEBUGGGGG","serial: "+serialVertex[i*6]+" -- "+serialVertex[i*6+1]+" -- "+serialVertex[i*6+2]+" -- "+serialVertex[i*6+3]+" -- "+serialVertex[i*6+4]+" -- "+serialVertex[i*6+5]);
        }

    }

    public void testGenerate(){
        generateVectors(generateNode(2));
    }

    public class UserPoint{
        public float x,y;
    }
}
