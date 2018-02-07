# getMeasuredHeight的相关问题 #

组件获取大小时需要用到`View.getMeasuredHeight()`，而在实际使用中发现，如果在布局文件中写入View，然后通过上述方法获得布局后的长宽。在OnXXX的回调方法有可能会获取不到。具体情况接下来分析。

## 代码结构 ##

activity_main.xml

```
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/activity_main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:paddingBottom="@dimen/activity_vertical_margin"
    android:paddingLeft="@dimen/activity_horizontal_margin"
    android:paddingRight="@dimen/activity_horizontal_margin"
    android:paddingTop="@dimen/activity_vertical_margin"
    tools:context="com.royole.opencvproject.MainActivity">

    <com.royole.opencvproject.MyTextView
        android:id="@+id/text"
        android:visibility="visible"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello World!" />
</RelativeLayout>
```

MainActivity.java

```
public class MainActivity extends Activity {
    MyTextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        textView = (MyTextView)findViewById(R.id.text);
        Log.i(TAG, "onCreate: "+textView.getMeasuredHeight());

        textView.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Runnable: "+textView.getMeasuredHeight());
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.i(TAG, "onPostResume: "+textView.getMeasuredHeight());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: "+textView.getMeasuredHeight());
    }
}

```

MyTextView.java

```
public class MyTextView extends TextView {

    private static final String TAG = "LEZH DEBUG";

    public MyTextView(Context context) {
        super(context);
    }

    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //Log.i(TAG, "onMeasure: ");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        //Log.i(TAG, "onLayout: ");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Log.i(TAG, "onDraw: ");
    }
}
```

## 运行结果分析 ##

运行之后
onCreate() 0
onResume() 0
onPostResume() 0
Runnable 27

特别说明的是第一次启动一个应用的时候，会发生这种情况。而重新唤醒该页面时，onResume及onPostResume都返回的是27，即正常值。

很多文章都说了第一次获取组件长宽应该放在post的runnable中执行可以获取正常值。这是为什么？

而`onDraw()`中也是可以获取 MeasuredHeight 的值的。这又是为什么？

## 源码分析 ##

首先追踪`ViewRootImpl.performTraversals()`相关流程：

第一步，需要分析performTraversals()是如何被调用的。

```
ViewRootImpl.java

void scheduleTraversals() {
        if (!mTraversalScheduled) {
            .......................

			//将mTraversalRunnable调用postCallBack
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);

            .......................
        }
    }

```

```
ViewRootImpl.java

final class TraversalRunnable implements Runnable {
        @Override
        public void run() {
			//执行doTraversal()
            doTraversal();
        }
    }
    final TraversalRunnable mTraversalRunnable = new TraversalRunnable();
```

```
ViewRootImpl.java

 void doTraversal() {
        
				.......................
				//执行performTraversals()
                performTraversals();
	            
				.......................
    }

```

所以实际上performTraversals()是作为runnable被postCallback到mChoreographer中被执行的。接下来我们看看`Choreographer.postCallback()`是如何执行的。

```
Choreographer.java

//postCallback() 最终调用到该函数 action参数即是我们的runnable
private void postCallbackDelayedInternal(int callbackType,
            Object action, Object token, long delayMillis) {
        .......................
        synchronized (mLock) {
            final long now = SystemClock.uptimeMillis();
            final long dueTime = now + delayMillis;
            mCallbackQueues[callbackType].addCallbackLocked(dueTime, action, token);

            if (dueTime <= now) {
                scheduleFrameLocked(now);
            } else {
				//消息是发给mHandler的
                Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_CALLBACK, action);
                msg.arg1 = callbackType;
                msg.setAsynchronous(true);
                mHandler.sendMessageAtTime(msg, dueTime);
            }
        }
    }

```

接下来我们要看看这个mHandler是哪个Looper中执行的，即mChoreographer是不是在主线程中运行。
```
ViewRootImpl.java

public ViewRootImpl(Context context, Display display) {
	.......................
	//Choreographer 是使用getInstance获得的
	mChoreographer = Choreographer.getInstance();
	.......................
}
```
```
Choreographer.java

public static Choreographer getInstance() {
        return sThreadInstance.get();
    }
```
```
Choreographer.java

// Thread local storage for the choreographer.
	//ThreadLocal是在libcore目录下的代码，用于实现线程相关的变量，具体实现不在这详细分析
    private static final ThreadLocal<Choreographer> sThreadInstance =
            new ThreadLocal<Choreographer>() {
        @Override
        protected Choreographer initialValue() {
			//获取当前线程的looper 在本文中即是ViewRootImpl的looper
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalStateException("The current thread must have a looper!");
            }
            return new Choreographer(looper);
        }
    };

```
通过以上代码可以知道，performTraversals()是被放在主线程的looper中执行的。

那么接下来我们需要分析`View.post()`的runnable是何时、在哪个线程中被执行的。

```
View.java

public boolean post(Runnable action) {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
			//如果当前View是attch的状态，有attachInfo则会执行到上一级，那么mHandler肯定是也在UI线程，即主线程
            return attachInfo.mHandler.post(action);
        }
        // 如果attachInfo是空的，那么会调用getRunQueue再执行post
        ViewRootImpl.getRunQueue().post(action);
        return true;
    }
```
那么我们看看内部类RunQueue的实现。
```
ViewRootImpl.java

static final class RunQueue {
        private final ArrayList<HandlerAction> mActions = new ArrayList<HandlerAction>();

        void post(Runnable action) {
            postDelayed(action, 0);
        }

        void postDelayed(Runnable action, long delayMillis) {
            HandlerAction handlerAction = new HandlerAction();
            handlerAction.action = action;
            handlerAction.delay = delayMillis;

            synchronized (mActions) {
				//设置好参数后，将action加入到ArrayList中
                mActions.add(handlerAction);
            }
        }

        void executeActions(Handler handler) {
            synchronized (mActions) {
                final ArrayList<HandlerAction> actions = mActions;
                final int count = actions.size();

                for (int i = 0; i < count; i++) {
                    final HandlerAction handlerAction = actions.get(i);
					//执行的时候，从ArrayList中取出一条，并post到handler所在线程执行
                    handler.postDelayed(handlerAction.action, handlerAction.delay);
                }

                actions.clear();
            }
        }

```

那么接下来我们需要知道是何时我们post到Runqueue中的Runnable被处理。  
在整个ViewRootImpl.java中只有一处被调用。
```
private void performTraversals() {
		.......................
		// mAttachInfo初始化时，传入的是ViewRootHandler mHandler作为mAttachInfo.mHandler，这个Handler也在主线程。
        getRunQueue().executeActions(mAttachInfo.mHandler);
		.......................
		performMeasure();
		performLayout();
		performDraw();
}
```

是不是很惊喜，是不是很意外？！现在主脉络已经清晰了。

`onCreate()`的时候把runnable调用`View.post()`。  
这个时候其实是把Runnable托管给RunQueue。  
当`performTraversals()`执行到`performMeasure()`之前，就开始从RunQueue中读取Runnable，并post到主线程执行。  
然而此时`performTraversals()`还未执行完毕，所以runnable尚需等待。  
当`performTraversals()`完全执行完毕之后，我们的runnable会开始执行，这个时候已经经过onMeasure()和onLayout()了。所以可以得到正常值。

这也可以解答结果中的第一个问题。--第一次获取组件长宽应该放在post的runnable中执行可以获取正常值的原因。

而第二个问题会更加复杂，后面一篇文章会讲解。


## 附录 ##

### 线程与Looper与Handler ###

1. `Looper.prepare()`时会根据`if (sThreadLocal.get() != null)`判断当前线程是否已经存在。一个线程中只会存在一个Looper。
2. `Looper.loop()`后会获得属性MessageQueue，然后阻塞线程开始等待消息。在loop后的代码是不会执行的。
3. 一个线程中可以有多个Handler，消息一般是通过Handler发送给MessageQueue进行排队，不同的Handler的Message的target是不一样的。
4. `Handler.sendToTarget()`之类的方法最终都会调到`MessageQueue.enqueMessage()`，而这个方法会检测target是否为空，为空则报错。所以一般来说进入队列(MessageQueue)的消息(Message)都有一个明确的处理者(Handler)。
5. 当有多个Handler在post方法时，会根据发送到Looper中的顺序逐个处理。
