# gl函数相关接口简介 #

注意的是，我们这里主要讲的还是android GLES20的接口，不涉及egl相关（egl主要是在native层创建窗口相关，在上层的GLSurfaceView已经在创建的时候设置好了相应的参数，所以暂时不需要关心）。

gl函数相关接口用的最多的会有以下功能：

1. 生成顶点数组。
2. 读取脚本数据并链接编译。
3. 生成纹理数据。
4. 绘制过程。


## 生成顶点数据 ##

顶点数据最好以byteBuffer压缩之后传入，效率会比较高。示例代码如下：

```
	private FloatBuffer mTexCoorBuffer;
	int maTexCoorHandle; // 顶点颜色属性引用id

	private void initVertex() {
		//生成代码工具类
        float[] vertices = gllineUtils.mVertex;
        //初始化纹理坐标数组 用于和顶点数组对应
        //创建顶点纹理坐标数据缓冲
        ByteBuffer cbb = ByteBuffer.allocateDirect(mTextureVertexData.length * 4);
        cbb.order(ByteOrder.nativeOrder());//设置字节顺序
        mTexCoorBuffer = cbb.asFloatBuffer();//转换为Float型缓冲
        mTexCoorBuffer.put(mTextureVertexData);//向缓冲区中放入顶点着色数据
        mTexCoorBuffer.position(0);//设置缓冲区起始位
    }
	

	public void draw(){
	// 获取Attribute的句柄，参数分别为：(mProgram句柄，Attribute在shader中的变量名)
	maTexCoorHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoor");
	// 赋值Atribute数组，参数分别为：(句柄，向量维数，数据类型，是否归一化，连续顶点的偏移量，数组缓冲区)
	GLES20.glVertexAttribPointer(maTexCoorHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, mTexCoorBuffer);
	// 开启Atribute功能，记得一定要打开，同时也可以禁用
	GLES20.glEnableVertexAttribArray(maTexCoorHandle);
	}

```

## 读取脚本数据并链接编译 ##

```
	//初始化shader
    private void initShader() {
		// 调用加载shader的函数，加载顶点与片元着色器
        String vertex = loadSH("vertex.sh");
        String shader = loadSH("frag.sh");

		// 标记创建的是vertex的顶点着色器
        int verS = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (verS != 0) {
            GLES20.glShaderSource(verS, vertex);
            GLES20.glCompileShader(verS);
        }

		// 标记创建的是fragment的顶点着色器
        int fragS = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        if (fragS != 0) {
			// 输入脚本内容
            GLES20.glShaderSource(fragS, shader);
			//编译脚本
            GLES20.glCompileShader(fragS);
        }

		//创建程序并获得句柄
        mProgram = GLES20.glCreateProgram();
        if (mProgram != 0) {
			// 粘贴着色器
            GLES20.glAttachShader(mProgram, verS);
            GLES20.glAttachShader(mProgram, fragS);
			// 链接程序  注意：到这里还只是把程序链接起来了，并没有glUseProgram使用程序
            GLES20.glLinkProgram(mProgram);
        }

		// 此处可以统一关联各Attribute以及Uniform
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        maTexCoorHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoor");
        maSerialID = GLES20.glGetAttribLocation(mProgram, "aSerial");
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        msTexHandle0 = GLES20.glGetUniformLocation(mProgram, "sTexture0");
        msTexHandle1 = GLES20.glGetUniformLocation(mProgram, "sTexture1");
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

```

## 生成纹理数据 ##

```

	//初始化纹理
    public void initTexture() {
        int texture_size = 5;
        int[] textures = new int[texture_size];

		// 生成5个纹理ID，可以批量生成
        GLES20.glGenTextures(texture_size,/*产生的纹理id的数量*/textures,/*纹理id的数组*/0/*偏移量*/);
        //循环贴每张图的纹理
        for (int i=0;i<texture_size;i++){
            textureId = textures[i];
			// 根据ID绑定纹理，后续关于纹理的操作都是操作这个
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            //  设置纹理采样
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //  设置纹理拉伸
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            //通过输入流加载图片
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;

            // 实际加载纹理，将bitmap加载到对应的纹理句柄中
            GLUtils.texImage2D
                    (
                            GLES20.GL_TEXTURE_2D,   //纹理类型，在OpenGL ES中必须为GL10.GL_TEXTURE_2D
                            0,                    //纹理的层次，0表示基本图像层，可以理解为直接贴图
                            bitmapTmps[i],            //纹理图像
                            0                     //纹理边框尺寸
                    );
            bitmapTmps[i].recycle();          //纹理加载成功后释放图片


            // 激活纹理，参数是TEXTURE序号，最大激活数量8-22都有可能，由硬件决定，这个是加载在显存（？）中的纹理。高于这个数量就需要替换纹理了。
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
        }	

```

## GL控制语句 ##

```
GLSurfaceView.Render的接口：

// 以RGBA颜色来覆盖底色
GLES20.glClearColor(0, 0, 0, 1.0f);
// 擦除信息，当前是擦除深度以及颜色信息。不擦除并不能保留上一帧图像，而是出现奇怪的画面。
GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
// 开启混合模式的功能
GLES20.glEnable(GLES20.GL_BLEND);
// 混合模式设置
GLES20.glBlendFunc(GLES20.GL_ONE,GLES20.GL_ZERO);
// 关闭深度测试，深度测试开启后可以前面遮挡后面的元素
GLES20.glDisable(GLES20.GL_DEPTH_TEST);

GLSurfaceView的接口：
// 设置使用GLES的版本
this.setEGLContextClientVersion(2);
// 设置渲染模式，当前是脏绘制，即有更新才会绘制。也可以使用一直绘制的模式。
this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
// 请求绘制下一帧，会调用Render的onDrawFrame函数
requestRender();
```
关于混合模式可以参考一个[实例](./实例2-混合模式.md)


## 绘制过程 ##

```
	public void draw() {
		// 擦除信息
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		// 在绘制之前调用useProgram
        GLES20.glUseProgram(mProgram);

        //初始化变换矩阵
        Matrix.setRotateM(mMMatrix, 0, 0, 0, 1, 0);
		//初始化主矩阵，这个值要传到vertex shader中对顶点进行变换
        mMVPMatrix = new float[16];

		//进行透视变化
		Matrix.frustumM(
                mProjMatrix, 0,
                -ratio, ratio, -1, 1,
                near, far);
		//进行视角变化
        Matrix.setLookAtM(
                mVMatrix, 0,
                0, 0, dis,
                0f, 0f, 0f,
                0f, 1.0f, 0.0f);
		//进行矩阵运算
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

		// 主变换矩阵值作为Uniform值 传入vertex shader
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		//传入顶点数组
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, fbv);
        //将纹理坐标传入glsl
        GLES20.glVertexAttribPointer(maTexCoorHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, mTexCoorBuffer)；

		//启用数组
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTexCoorHandle);

        //如果只有一个纹理 可以不做这一步
        GLES20.glUniform1i(msTexHandle0, 0);
        GLES20.glUniform1i(msTexHandle1, 1);
        GLES20.glUniform1i(msTexHandle2, 2);

        // 以TRIANGLES模式显示，绘制图像
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount);
    }
```

关于透视以及视角变化的参数的意义可以参考：

Matrix.frustumM(float[] m, int offset, //m是矩阵输出  offset是m矩阵的偏移量
float left, float right, float bottom, float top, //near面的 left,right,bottom.top
float near, float far)//near面的距离  far面的距离

Matrix.setLookAtM(mViewMatrix, 0, 
eyeX, eyeY, eyeZ, //相机坐标 
lookX, lookY, lookZ, //目标坐标，与gl中的VPN是不一样的 
upX, upY, upZ); //相机正上方向量VUV
