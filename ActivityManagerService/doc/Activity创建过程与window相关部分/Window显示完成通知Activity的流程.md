# Window显示完成通知Activity的流程 #

在文章 [AMS管理Activity启动流程]() 与 [Activity创建过程与window相关部分]() 提到了，Activity.Idler 其中的代码并不会全部执行，那么接下来我们看为什么会有这样的现象。

## 问题的出现 ##

观察日志的时候发现：

在Idler中的日志会执行，也可以成功调用到ActivityManagerService，最终调用到ActivityStackSupervisor都没有问题。  
但调用到`ActivityStackSupervisor.processStoppingActivitiesLocked`的时候，并没有打印"Ready to Stop:"相关的打印。  
也就是说 Idler中执行到`processStoppingActivitiesLocked`真正去给Activity Stop的时候并没有执行。

那么，为什么没有执行？又是谁保证了正常的Activity可以进入onStop或者onDestroy生命周期。

## 问题的答案 ##

首先先把问题的答案放出来，后面再写分析过程。

1. 为什么执行Idler流程不能让Activity正常进入onStop？
	- 在ActivityStackSupervisor执行stop的时候条件不满足，多数情况下，只有当需要被stop的Activity退出waitingVisible的时候，才会可以进行stop流程。但是当Idler执行的时候，时间会比较靠前，界面没有绘制完成，还是处于waitingVisible状态。

2. 从哪里调起了onStop流程？
	- 当ViewRootImpl进行完performTravesal之后会跨进程访问WMS，WMS进行performLayoutAndPlaceSurfacesLocked流程，在最后会调起`wtoken.appToken.windowsVisible()`，远程访问ActivityRecord，从而进入`ActivityStackSupervisor.processStoppingActivitiesLocked`。

接下来，逐条分析原因。

先放出从handleResumeActivity到processStop流程图：

![handleResumeActivity到processStop流程](handleResumeActivity到processStop流程.png)

### Idler流程不能让Activity正常进入onStop ###

首先看ActivityStackSupervisor的stop流程的值。

```
ActivityStackSupervisor.java

	final ArrayList<ActivityRecord> processStoppingActivitiesLocked(boolean remove) {
        int N = mStoppingActivities.size();
        if (N <= 0) return null;

        ArrayList<ActivityRecord> stops = null;

		//遍历mResumedActivity，看是否有已经处于visible状态的activity
        final boolean nowVisible = allResumedActivitiesVisible();
        for (int i=0; i<N; i++) {
            ActivityRecord s = mStoppingActivities.get(i);

			//s.waitingVisible和nowVisible在一般情况下，是相反的。
			//当不需要等待其他Activty进入Visible的时候，s.waitingVisible是false，nowVisible是true

			//所以这个判断一般情况都不会进入，可以暂时不了解
            if (s.waitingVisible && nowVisible) {
                mWaitingVisibleActivities.remove(s);
                s.waitingVisible = false;
                if (s.finishing) {
                    mWindowManager.setAppVisibility(s.appToken, false);
                }
            }

			//remove是传入值true，所以当 s.waitingVisible 为false，或者isSleepingOrShuttingDown是true
			//s.waitingVisivle 为false时，已经不需要等待有Visible的Activity了，可以进行stop过程了
			//所以，肯定会有一个过程，会有两个activity处于visible状态。
			//
			//isSleepingOrShuttingDown 暂时还没有追，但验证了与一般的熄屏流程没有关系。熄屏时，会走完onCreate->onResume，
			//再进入onPause->onStop。

            if ((!s.waitingVisible || mService.isSleepingOrShuttingDown()) && remove) {
                if (localLOGV) Slog.v(TAG, "Ready to stop: " + s);
                if (stops == null) {
                    stops = new ArrayList<ActivityRecord>();
                }
                stops.add(s);
                mStoppingActivities.remove(i);
                N--;
                i--;
            }
        }

```

根据上述描述是可以发现，waitingVisible影响到是否可以stopActivity。那么waitingVisible是根据什么决定的？

----------------------------施工中---------------------------



### 调起了onStop流程 ###




## 扩展问题--锁屏的时候的生命周期变化 ##

锁屏期间启动新Activity生命周期经过了onCreate - onResume，再经过onPause - onStop。亮屏的时候经过onStart - onResume。