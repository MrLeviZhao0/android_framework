# WindowManagerService

WindowManagerService是管理窗口的一个类，但它在android系统中所占据的位置非常重要。

WindowManagerService和ActivityManagerService合作，构建出应用的各种生命周期中复杂有序的显示效果；WindowManagerService和ViewRootImpl合作，绘制出多姿多彩的图形界面；WindowManagerService使用SurfaceFlinger的服务，得以让图形界面高速顺畅的绘制。

可以看到，在显示系统中WindowManagerService是占据了非常核心的一个位置，而接下来，就会逐一分析WindowManagerService的作用与实现。


## 目录 ##


-------------------  施工中  ---------------------------------------