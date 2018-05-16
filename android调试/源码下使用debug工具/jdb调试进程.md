# jdb调试进程

jdb工具不仅仅能简单的使用run来调试程序，还可以在进程中调试程序，就像AS和eclipse中一样，可以把断点设置到进程中，进行调试。以下将详细介绍相关内容。

* 简单的java进程
* 实际的android进程

## 简单的java进程

想调试一个简单的java进程，流程如下：

1. 编写一个简单的java程序

```java

import java.lang.*;

class A{


public static void main(String[] args){
    A.printInfinite();
}

public static void printInfinite(){

    int i = 10000;

    while(true){
    try{
    Thread.sleep(1000);
    }catch(Exception e){
    }
    System.out.println("i:"+i);

    }
}

}

```

2. 编译时选择 'javac -g A.class'，打开调试选项。

3. 启动程序时，要使用特殊启动指令：

'java -Xdebug -Xrunjdwp:transport=dt_socket,suspend=n,server=y,address=28000 A'

为了允许JDB或者任何调试器连接到一个在运 行的JVM，JVM必须通过许多命令行参数来启动。这些参数促成JVM去加载和初始化调试库，配置传输并打开一个socket，使jdb可以attach到上面。

4. 使用'jdb -attach 28000'绑定端口。注意的是，端口号不是 jps 指令得到的进程号。

5. 开始进行监听。

'threads' 列出线程，选择线程。然后设置断点，可以选择方法断点、函数断点、异常断点等。如果到达断点，则会 'Breakpoint hit:' ，接下来可以和简单的调试使用一样的命令了。

同样也可以先设置断点，再选择线程，再使用run使线程运行，效果一致。


## 实际的android进程

实际调试android进程的情况下会遇到以下问题：

1. android系统中并没有jdb
2. 调试断点时list不能显示源码

### android系统中并没有jdb

1. 通过'adb jdwp'获取可调试的android进程号
2. 通过'adb shell ps -A'查询进程号以及对应的名字，参考jdwp就知道是否可以调试了
3. 通过'adb forward tcp:local jdwp:remote'建立转发关系，把本地的local与remote挂上钩。

通过以上步骤，可以将本地的端口与远端的端口对应起来。然后进行jdb的调试工作。

### 调试断点时list不能显示源码

1. 接着以上步骤，通过'jdb -attach localhost:9000'来进入调试模式
2. 可以不指定关注的线程（因为经常就不知道是什么线程），直接输入'stop at com.android.server.policy.PhoneWindowManager:8737'来打行断点，并且已经开始监听了。
3. 触发断点后，需要注意的是，此时的list不能用上，locals信息也不全。此时报错是'Source file not found: *.java'
4. 我们可以通过两种方式，设置代码，让其找到对应的位置。

* 方法一：在jdb启动时添加参数：'jdb -sourcepath $(pwd)/frameworks/base/services/core/java'
* 方法二：在jdb运行时输入'use /aosp/frameworks/base/services/core/java'
* 路径是java类文件的package的上一级。如：'aosp/frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java' 的包名是'com.android.server.policy'，则最终sourcepath的路径应该如上所述。
* 可以一次输入多个路径，路径之间用 ':'符号隔开

### 杂项

1. 处于断点时，可以使用suspend挂起所有（也可以是单个）线程，以及用resume唤醒。
2. 同样可以用wherei来打印堆信息，以及pc信息。
3. 其他的和简单的调试步骤方法一致。
