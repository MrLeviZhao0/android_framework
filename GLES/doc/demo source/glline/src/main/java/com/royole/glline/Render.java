package com.royole.glline;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by lezh on 2017/6/10.
 */

public class Render implements GLSurfaceView.Renderer {
    private int vCount;
    private Bitmap[] bitmapTmps;
    private Context ctx;
    private FloatBuffer fbv;
    private FloatBuffer mTexCoorBuffer;
    private FloatBuffer mSerialBuffer;
    static float[] mMMatrix = new float[16];
    int mProgram;// 自定义渲染管线程序id
    int muMVPMatrixHandle;// 总变换矩阵引用id
    int maPositionHandle; // 顶点位置属性引用id
    int maTexCoorHandle; // 顶点颜色属性引用id
    int maSerialID; // 序列号id

    int msTexHandle0;//纹理引用id
    int msTexHandle1;//纹理引用id
    int msTexHandle2;//纹理引用id
    int msTexHandle3;//纹理引用id
    int msTexHandle4;//纹理引用id

    int mRangeHandle;//波纹振幅 id
    int mPeriodHandle;//波纹周期 id
    int textureId;//系统分配的纹理id

    private float ratio;
    private float near;
    private float far;
    private float dis;

    public static float[] mProjMatrix = new float[16];// 4x4矩阵 投影用
    public static float[] mVMatrix = new float[16];// 摄像机位置朝向9参数矩阵
    public static float[] mMVPMatrix;// 最后起作用的总变换矩阵

    public Render(Context ctx) {
        super();
        this.ctx = ctx;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0, 0, 0, 1.0f);

        initShader();

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE,GLES20.GL_ZERO);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        initTexture();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        GLES20.glViewport(0, 0, width, height);
        ratio = (float) width / height;
        Log.e("aaa", "width =" + width + "height = " + height);
        near = 1;
        far = 10;
        dis = 10;

        Matrix.frustumM(
                mProjMatrix, 0,
                -ratio, ratio, -1, 1,
                near, far);
        Matrix.setLookAtM(
                mVMatrix, 0,
                0, 0, dis,
                0f, 0f, 0f,
                0f, 1.0f, 0.0f);

        initVertex();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        draw();
    }

    long time_consume = System.nanoTime();
    public void draw() {
        GLES20.glUseProgram(mProgram);

        //初始化变换矩阵
        Matrix.setRotateM(mMMatrix, 0, 0, 0, 1, 0);

        mMVPMatrix = new float[16];

        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, fbv);
        //TODO 5 将纹理坐标传入glsl
        GLES20.glVertexAttribPointer(maTexCoorHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, mTexCoorBuffer);

        //TODO 6 传入纹理序号
        GLES20.glVertexAttribPointer(maSerialID, 1, GLES20.GL_FLOAT, false, 1 * 4, mSerialBuffer);

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTexCoorHandle);
        GLES20.glEnableVertexAttribArray(maSerialID);

        //TODO 6 绑定纹理
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
//        //如果只有一个纹理 可以不做这一步
        GLES20.glUniform1i(msTexHandle0, 0/*GLES20.GL_TEXTURE0=0  GLES20.GL_TEXTURE1=1*/);
        GLES20.glUniform1i(msTexHandle1, 1/*GLES20.GL_TEXTURE0=0  GLES20.GL_TEXTURE1=1*/);
        GLES20.glUniform1i(msTexHandle2, 2/*GLES20.GL_TEXTURE0=0  GLES20.GL_TEXTURE1=1*/);
        GLES20.glUniform1i(msTexHandle3, 3/*GLES20.GL_TEXTURE0=0  GLES20.GL_TEXTURE1=1*/);
        GLES20.glUniform1i(msTexHandle4, 4/*GLES20.GL_TEXTURE0=0  GLES20.GL_TEXTURE1=1*/);


        time_consume = System.nanoTime();

        //TODO  7 显示
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount);


        time_consume = System.nanoTime()-time_consume;
        Log.e("LEZH DDDDDBUG","draw array spend time(ms):"+time_consume/1000000 + "nano time:"+time_consume);

    }

    //初始化数据
    private void initVertex() {
        GllineUtils gllineUtils = new GllineUtils();
        gllineUtils.testGenerate();
        float[] vertices = gllineUtils.mVertex;
        float[] mTextureVertexData = gllineUtils.textureVertex;
        float[] serialVertex = gllineUtils.serialVertex;

        vCount = vertices.length / 3;
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        fbv = bb.asFloatBuffer();
        fbv.put(vertices);
        fbv.position(0);

        //TODO 4 初始化纹理坐标
        //创建顶点纹理坐标数据缓冲
        ByteBuffer cbb = ByteBuffer.allocateDirect(mTextureVertexData.length * 4);
        cbb.order(ByteOrder.nativeOrder());//设置字节顺序
        mTexCoorBuffer = cbb.asFloatBuffer();//转换为Float型缓冲
        mTexCoorBuffer.put(mTextureVertexData);//向缓冲区中放入顶点着色数据
        mTexCoorBuffer.position(0);//设置缓冲区起始位置


        ByteBuffer srb = ByteBuffer.allocateDirect(serialVertex.length * 4);
        srb.order(ByteOrder.nativeOrder());//设置字节顺序mSerialBuffer = srb.asFloatBuffer();//转换为Float型缓冲
        mSerialBuffer = srb.asFloatBuffer();//转换为Float型缓冲
        mSerialBuffer.put(serialVertex);//向缓冲区中放入顶点着色数据
        mSerialBuffer.position(0);//设置缓冲区起始位置

    }

    //初始化纹理
    public void initTexture() {

        time_consume = System.nanoTime();

        //TODO 2 生成纹理ID
        int texture_size = 5;
        int[] textures = new int[texture_size];
        GLES20.glGenTextures(texture_size,/*产生的纹理id的数量*/textures,/*纹理id的数组*/0/*偏移量*/);
        //循环贴每张图的纹理
        for (int i=0;i<texture_size;i++){
            textureId = textures[i];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            //纹理采样
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //纹理拉伸
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            //通过输入流加载图片
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;

            //TODO 3 实际加载纹理
            GLUtils.texImage2D
                    (
                            GLES20.GL_TEXTURE_2D,   //纹理类型，在OpenGL ES中必须为GL10.GL_TEXTURE_2D
                            0,                    //纹理的层次，0表示基本图像层，可以理解为直接贴图
                            bitmapTmps[i],            //纹理图像
                            0                     //纹理边框尺寸
                    );
            bitmapTmps[i].recycle();          //纹理加载成功后释放图片


            //绑定纹理
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        }

        time_consume = System.nanoTime() - time_consume;
        Log.e("LEZH DDDDDBUG","load texture spend time(ms):"+time_consume/1000000 + "nano time:"+time_consume);
    }

    //初始化shader
    private void initShader() {
        String vertex = loadSH("vertex.sh");
        String shader = loadSH("frag.sh");

        int verS = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (verS != 0) {
            GLES20.glShaderSource(verS, vertex);
            GLES20.glCompileShader(verS);
        }

        int fragS = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        if (fragS != 0) {
            GLES20.glShaderSource(fragS, shader);
            GLES20.glCompileShader(fragS);
        }
        mProgram = GLES20.glCreateProgram();
        if (mProgram != 0) {
            GLES20.glAttachShader(mProgram, verS);
            GLES20.glAttachShader(mProgram, fragS);
            GLES20.glLinkProgram(mProgram);
        }

        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        //TODO 1 关联glsl的纹理坐标
        maTexCoorHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoor");
        maSerialID = GLES20.glGetAttribLocation(mProgram, "aSerial");
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        msTexHandle0 = GLES20.glGetUniformLocation(mProgram, "sTexture0");
        msTexHandle1 = GLES20.glGetUniformLocation(mProgram, "sTexture1");
        msTexHandle2 = GLES20.glGetUniformLocation(mProgram, "sTexture2");
        msTexHandle3 = GLES20.glGetUniformLocation(mProgram, "sTexture3");
        msTexHandle4 = GLES20.glGetUniformLocation(mProgram, "sTexture4");

        mRangeHandle = GLES20.glGetUniformLocation(mProgram, "range");
        mPeriodHandle = GLES20.glGetUniformLocation(mProgram, "period");
    }

    //将sh文件加载进来
    private String loadSH(String fname) {
        String result = null;
        try {
            InputStream in = ctx.getAssets().open(fname);
            int ch = 0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((ch = in.read()) != -1) {
                baos.write(ch);
            }
            byte[] buff = baos.toByteArray();
            baos.close();
            in.close();
            result = new String(buff, "UTF-8");
            result = result.replaceAll("\\r\\n", "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //update data
    public void updateData() {
        initVertex();
    }

    public void setBitmaps(Bitmap[] bitmaps) {
        bitmapTmps = bitmaps;
    }
}