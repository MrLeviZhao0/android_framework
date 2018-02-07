# SurfaceFlinger相关类 #

SurfaceFlinger相关的类，会介绍从Surface及SurfaceControl的native层开始介绍起。

## SurfaceControl Surface ##

这两个类是上层WMS与SurfaceFlinger的接口，封装SurfaceFlinger的服务。实际上不止WMS会使用这两者，包括bootanimation等也会直接使用Native层的SurfaceFlinger服务，获取Bp端的SurfaceControl以及Surface，进行绘制的请求以及控制。

* SurfaceControl ：
	1. 由老版本的Surface中拆分出来，负责管理Surface的属性等。WMS中的窗口动画，多数是通过SurfaceControl调整surface属性实现。
	2. 功能多数是调用SurfaceComposerClient的方法来实现，访问到继承Bn端的Client类，最终访问到SurfaceFlinger得到服务。
	3. 主要意义是拆分Surface类，将多数提供给上层的接口封装在这里。用于控制窗口变化。

* Surface :
	1. SurfaceFlinger主要的客户端实现，多数状态属性都保存在其中。包括dirtyRegion,lockedBuffer,postBuffer,graphicBufferProducer这几个属性。这些属性都直接与图像数据刷新有关系。
	2. 主要提供给上层的接口有lock，unlockAndPost，query等。前两者是获取一个绘制区域，后者是查询一些有关于窗口的数据。
	3. 获取绘制区域的时候，需要从IGraphicBufferProducer中取出buffer用于绘制图像。

## DisplayDevice Layer GraphicBuffer ##

WMS的dump日志中可以看到管理结构有：WMS - Display - Window。

对于多数Window，需要有自己的ViewRootImpl，而在ViewRootImpl中的performDraw的时候，就会去通过lockCanvas获取绘制区域，再将绘制区域给子级组件使用，如TextView与ImageView的绘制等。

* DisplayDevice :
	1. DisplayDevice描述的是显示设备信息，与上层的 Display 是相关的。包括设备显示参数，EGL相关，以及vector<Layer>管理结构。
	2. 会通过DisplayDevice初始化函数，进行一系列属性的赋值，新建一个Surface并通过这个Surface创建一个EGLSurface对象。结束前还会设置Display类型以及Display的透视属性。
	3. 主要通过mVisibleLayersSortedByZ属性来管理Layer。而DisplayDevice在SurfaceFlinger的init中按照NUM_BUILTIN_DISPLAY_TYPES的数值创建，然后将这些DisplayDevice添加到mDisplays这个Map结构中，其中的key成为属性mBuiltinDisplays，来进行多显示设备的操作。

* Layer ：
	1. Layer描述的是每一层的显示区域，与上层的Window对应。包含Geometry和State两个结构体，而很关键的是State的成员z。z描述的是Layer在Z轴上的属性，与Window的type是有关系但不是相等的，与Window的Surface的layer属性相等。
	2. Layer在Bn端的ISurfaceComposerClient的MessageCreateLayer中被申请创建，在SurfaceFlinger中根据类型完成创建，然后更改管理结构的信息。
	3. Layer初始化的时候会默认设置GLES的Mesh以及Texture等属性，紧接着会设置z属性信息。这些信息都可以对应到GLES的接口。
	4. SurfaceFlinger的doComposeSurfaces时，会按照条件看是否使用HwComposer，若不使用，则会调用到Layer的draw。最终会调用接口onDraw去访问GLES接口。

* GraphicBuffer ：
	1. GraphicBuffer是申请的一片内存区域，继承本地实现ANativeWindowBuffer，以及序列化接口Flattenable。目的是