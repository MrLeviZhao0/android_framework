# SurfaceFlinger相关类 #

SurfaceFlinger相关的类，会介绍从Surface及SurfaceControl的native层开始介绍起。

## SurfaceControl Surface ##

这两个类是上层WMS与SurfaceFlinger的接口，封装SurfaceFlinger的服务。实际上不止WMS会使用这两者，包括bootanimation等也会直接使用Native层的SurfaceFlinger服务，获取Bp端的SurfaceControl以及Surface，进行绘制的请求以及控制。

* SurfaceControl ：
	1. 由老版本的Surface中拆分出来，负责管理Surface的属性等。WMS中的窗口动画，多数是通过SurfaceControl调整surface属性实现。
	2. 功能多数是调用SurfaceComposerClient的方法来实现，访问到继承Bn端的Client类，最终访问到SurfaceFlinger得到服务。
	3. 主要意义是拆分Surface类，将多数提供给上层的接口封装在这里。用于控制窗口变化。
	4. 通过SurfaceControl的getSurface来获得根据mGraphicBufferProducer新建的Surface。

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
	1. GraphicBuffer是申请的一片内存区域，继承本地实现ANativeWindowBuffer，以及序列化接口Flattenable。是承载图像内容的最基本最直接的类。
	2. 通过lock系列函数获取空间，lock函数调用GraphicBufferMapper的同名函数，最终会调用到gralloc的结构体gralloc_module_t的方法。调用到gralloc硬件抽象层，分配空间给GraphicBuffer。
	3. gralloc驱动中的`gralloc_alloc_buffer`负责分配空间，在`fd = ashmem_create_region("gralloc-buffer", size);`中分配一个匿名共享内存用于存放图像数据，最后通过mapBuffer来关联`private_handle_t`和`gralloc_module_t`。
	4. 需要注意的是GraphicBuffer可以序列化，从server端传到client端，但开辟的匿名共享内存内容不会被序列化，而是通过记录地址偏移的方式，需要的时候按照地址进行强制转型获取内容。具体实现见flatten和unflatten函数。

## SurfaceComposerClient Client ComposerService SurfaceFlinger ##

SurfaceComposerClient与ComposerService是两个Bp端的方法类，相应的Bn端是Client以及SurfaceFlinger。

* SurfaceComposerClient :
  1. 提供SurfaceControl的一些功能，如设置缩放可见等属性、创建销毁Surface、查询Display属性等。
  2. Client的Bp端，关于Layer的值是放在layer_state_t指针中，修改后记录同步锁，更新Layer属性。
  3. 通过ComposerService以及本身的Binder机制两种方式与SurfaceFlinger端通信。
  4. 内部类Composer用于具体实现逻辑，ScreenShotClient用于截屏功能的实现。
* Client :
  1. SurfaceComposerClient的Bn端实现，实现创建销毁Surface，清除获取Layer状态的功能。
  2. 持有Layer和SurfaceFlinger的引用，用于实现逻辑。
* ComposerService :
  1. SurfaceFlinger服务的Bp端，头文件单独列出，实现代码在SurfaceComposerClient中。
  2. 使用getInstance调用connect函数，获取注册过的SurfaceFlinger服务。
* SurfaceFlinger :
  1. SurfaceFinger是最核心的类，管理framebuffer相关硬件抽象层的使用，为上层WindowManagerService进行服务。
  2. SurfaceFlinger模块可以使用dumpsys来输出日志，可以分为Layer处理，图像数据混合，同步等步骤。
  3. 在此处要体现的是，为上层可以直接提供服务，如bootanimation就没有经过WindowManagerService等，直接调用SurfaceFlinger相关服务可以实现。


## Layer BufferQueue BufferQueueProducer BufferQueueConsumer BufferSlot ##

图像的实际数据的生成到填充，最后到绘制出来的过程会比较复杂。  
比如简单的在OnDraw中绘制一张Bitmap的时候，

## HWComposer ##

## VSync ##