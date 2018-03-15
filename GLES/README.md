# GLES #

android的显示系统有很大的一部分是与GLES相关的。上层有GLSurfaceView提供GLES绘制接口，native层有EGL接口直接提供绘图功能。而实际上，图像内容的很多操作，如贴图、混合等时候会涉及GLES的功能。

GLES会有很多个方面的内容，个人觉得可以分成以下几个模块：
1. EGL（Embedded Graphic Language）接口。
2. [gl函数接口](./doc/gl函数接口简介.md)。
3. [shader script](./doc/shader script简介.md)。

还有一个项目中记录各问题的文档：

1. [测试项目记录文档](./doc/测试项目记录文档.md)

需要注意的是，GLES版本有以下特性：
1. GLES1.X 提供固定管线编程，只需要选择合适的函数调用即可。就像扳开关一样。
2. GLES2.X 提供shader编程，可以在shader中进行编程，语法与C类似。GLES20是最常见的版本，且与GLES1.X版本互相不兼容，接口差异很大。
3. GLES3.X 提供3D纹理等特殊功能，普及率不是特别高。与GLES2.X版本兼容。

因为在Java层，GLES的EGL接口被封装成了GLSurfaceView，所以并不会太多的涉及到。  
我们主要的工作还是熟悉gl函数接口，以及shader script的编写。

