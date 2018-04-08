# WindowManagerService

WindowManagerService和ActivityManagerService合作，构建出应用的各种生命周期中复杂有序的显示效果；WindowManagerService和ViewRootImpl合作，绘制出多姿多彩的图形界面；WindowManagerService使用SurfaceFlinger的服务，得以让图形界面高速顺畅的绘制。
可以看到，在显示系统中WindowManagerService是占据了非常核心的一个位置，而接下来，就会逐一分析WindowManagerService的作用与实现。

以下主要从大致的架构、WMS自身的启动、WMS的三个子系统（布局，动画，事件）来了解WMS。

1. [类图以及数据结构分析](./doc/概述管理范围/类图与数据结构分析.md)
2. 流程分析1-WMS启动流程
3. 流程分析2-WMS管理addView流程
4. 流程分析3-WMS管理Animation子系统
5. 流程分析4-WMS管理输入事件分发

以下是一些碰到与WMS相关的问题记录：

1. [GPU性能测试](./doc/杂项/GPU性能测试.md)

-------------------  施工中  ---------------------------------------