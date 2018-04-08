# android studio自带debug工具

从eclipse开发到使用android studio开发Java语言的应用的时候，都可以使用debug工具进行断点调试。

在JVM虚拟机中保留了考察虚拟机运行态的一套工具，这就是JPDA，而在这篇文章中我们更多的是介绍debug工具，原理性的东西可以[参考文章](https://www.ibm.com/developerworks/cn/java/j-lo-jpda1/)。

关于android studio debug工具，[大佬博客](https://blog.csdn.net/jerrywu145/article/details/53892938)中介绍得非常详细，当然也可以直接参考debug工具中的 ? 按钮。


除此之外，还有更多的调试方式：

1. 抛出异常并捕获。
2. monkey。
3. MethodTracing分析函数追踪。
4. DDMS Thread追踪。
5. DDMS Heap分析工具。


--暂时留个坑，先不分析