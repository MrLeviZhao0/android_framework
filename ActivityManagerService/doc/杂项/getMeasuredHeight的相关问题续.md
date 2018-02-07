# getMeasuredHeight的相关问题续 #

上一篇文章留下了一个问题：  

而`onDraw()`中也是可以获取 MeasuredHeight 的值的。这又是为什么？

前一篇文章不是说了么，`performTraversals()`执行之后才会执行runnable，那为什么performDraw是在performTraversals中的，它为什么没有在runnable之前执行？

接下来会继续详细分析执行过程。

## 代码分析 ##

接下来关注点主要放在performTraversals与draw相关的函数上。

```
ViewRootImpl.java

private void performTraversals() {
	.......................
	//cancelDraw 是判断是否取消这次绘制 newSurface 是标记当前是否刚创建新的Surface
	if (!cancelDraw && !newSurface) {
			//当前这次的draw事件不需要被省略
	        if (!skipDraw || mReportNextDraw) {
					//还有未执行完的Transitions，执行完其动画
	                if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
	                    for (int i = 0; i < mPendingTransitions.size(); ++i) {
	                        mPendingTransitions.get(i).startChangingAnimations();
	                    }
	                    mPendingTransitions.clear();
	                }
					//开始执行performDraw方法
	                performDraw();
	        }
	} else {
			//当前根组件是否是可见的
	        if (viewVisibility == View.VISIBLE) {
	            // Try again
	            scheduleTraversals();
			//同上 执行未执行完的Transitions
	        } else if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
	            for (int i = 0; i < mPendingTransitions.size(); ++i) {
	                mPendingTransitions.get(i).endChangingAnimations();
	            }
	            mPendingTransitions.clear();
	        }
			//如果不可见 且不需要执行Transitions 则什么都不做
	}
	
	.......................	
}
```

第一次执行的时候cancelDraw = false，newSurface = true。所以并不会执行`performDraw()`。  
而是直接进入判断 `if(viewVisibility == View.VISIBLE)`	，当前组件是VISIBLE的。  
所以立马进入`scheduleTraversals()`。需要注意的是，scheduleTraversals是将TraversalRunnable post到Choreographer中执行。  
所以第二次`scheduleTraversals()`不会马上执行，而是等post方法执行完后执行。  
在第二次执行的时候会设置newSurface的值。代码如下：  

```

private void performTraversals() {
	.......................
	if (!hadSurface) {
    	if (mSurface.isValid()) {
	        // If we are creating a new surface, then we need to
	        // completely redraw it.  Also, when we get to the
	        // point of drawing it we will hold off and schedule
	        // a new traversal instead.  This is so we can tell the
	        // window manager about all of the windows being displayed
	        // before actually drawing them, so it can display then
	        // all at once.
    		newSurface = true;
			.......................
}
```
所以第二次会执行`performDraw()`。  
所以`View.onDraw()`第一次执行的时候就可以获得组件的长宽了。


## 系统日志 ##

日志是在ViewRootImpl的performXXX，以及View相关的XXX函数中打印系统日志，编译系统源码得到的日志。  
在原本的日志上，添加了STEP标签，还标记了日志是出自什么函数。


```
------------------------    STEP 0    ------------------------
I/LEZH DEBUG( 1448): onCreate: 0  -------- Activity.onCreate
I/LEZH DEBUG( 1448): onResume: 0  -------- Activity.onResume
I/LEZH DEBUG( 1448): onPostResume: 0  -------- Activity.onPostResume
------------------------    STEP 1    ------------------------
I/LEZH DEBUG( 1448):  performMeasure:  -------- ViewRootImpl.performMeasure
I/LEZH DEBUG( 1448):   view measure:  -------- View.measure
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448): onMeasure:  -------- MyTextView.onMeasure
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448): onMeasure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448):  performLayout:  -------- ViewRootImpl.performLayout
I/LEZH DEBUG( 1448):   view layout:  -------- View.layout
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448): onLayout:  -------- MyTextView.onCreate
I/LEZH DEBUG( 1448):   view layout:
------------------------    STEP 2    ------------------------
I/LEZH DEBUG( 1448): Runnable: 27  -------- Runnable.run
------------------------    STEP 3    ------------------------
I/LEZH DEBUG( 1448):  performMeasure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448): onMeasure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448): onMeasure:
I/LEZH DEBUG( 1448):   view measure:
I/LEZH DEBUG( 1448):  performLayout:
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448): onLayout:
I/LEZH DEBUG( 1448):   view layout:
I/LEZH DEBUG( 1448):  performDraw:-------- ViewRootImpl.performDraw
I/LEZH DEBUG( 1448):   view draw: canvas-------- View.draw
I/LEZH DEBUG( 1448):   view draw: canvas,parent,drawing
I/LEZH DEBUG( 1448):   view draw: canvas,parent,drawing
I/LEZH DEBUG( 1448):   view draw: canvas,parent,drawing
I/LEZH DEBUG( 1448):   view draw: canvas,parent,drawing
I/LEZH DEBUG( 1448):   view draw: canvas
I/LEZH DEBUG( 1448): onDraw:-------- Activity.onDraw
I/LEZH DEBUG( 1448):   view draw: canvas,parent,drawing
I/LEZH DEBUG( 1448):   view draw: canvas

```


### STEP0 ###

首先是启动一个Activity的生命周期，由ActivityManagerService负责调度，并与Instrumentation等类合作。但这些细节暂时不必太过关注，
只需要知道在onCreate和onResume等回调函数时并不会对界面进行刷新，只有这些回调函数被调用之后才会进行界面的刷新。

STEP0还未涉及界面的测量、布局和绘制，只负责初始化Activity相关的数据结构。

### STEP1 ###

整个STEP1是执行了一遍`ViewRootImpl.performTraversals()`。没有执行`performDraw()`是因为新建Surface，不进行子组件的draw，而是立马请求进入下一次`ViewRootImpl.performTraversals()`。

### STEP2 ###

STEP2是post到View的Runnable执行的过程，这里出现View的runnable原因是runnable被加入了ActivityThread的Looper中，在performTraversals被执行之后，轮到各个子级的runnable被执行。之所以能得到getMeasuredHeight，是因为在runnable之前Measure和Layout都被执行过一遍。

### STEP3 ###

STEP3是一次新的`ViewRootImpl.performTraversals()`，而且在这次调用中才首次调用`onDraw()`。所以`onDraw()`是可以获取到正常的getMeasuredHeight的。



