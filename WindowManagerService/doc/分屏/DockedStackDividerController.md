# DockedStackDividerController

DockedStackDividerController是管理分屏的Controller类，分屏主要的操作都会经过这里。

以下主要分析一下DockedStackDividerController被谁调用，有什么属性以及方法。

## 调用方式

DockedStackDividerController（DSDC）被DisplayContent持有实例，再通过DisplayContent被外界调用。

通过这种方式，在StackWindowController、TaskStack、以及WMS等等数据结构都可以取到DSDC。

与分屏相关的属性有：

```java
DockedStackDividerController.java

    // 用于计算动画时长的参数，在getClipRevealMeetFraction中使用
    private static final float CLIP_REVEAL_MEET_EARLIEST = 0.6f;
    private static final float CLIP_REVEAL_MEET_LAST = 1f;
    private static final float CLIP_REVEAL_MEET_FRACTION_MIN = 0.4f;
    private static final float CLIP_REVEAL_MEET_FRACTION_MAX = 0.8f;

    // ime动画插值器，计算动画时间t的
    private static final Interpolator IME_ADJUST_ENTRY_INTERPOLATOR =
            new PathInterpolator(0.2f, 0f, 0.1f, 1f);

    // ime动画时长，以及延迟时间
    private static final long IME_ADJUST_ANIM_DURATION = 280;
    private static final long IME_ADJUST_DRAWN_TIMEOUT = 200;

    // Divider不点击的时候的宽度(dp)
    private static final int DIVIDER_WIDTH_INACTIVE_DP = 4;

    // 初始化的会赋值mService用于取mPolicy等数据结构以及通过mH发送消息。DisplayContent创建该类，并将自身复制给mDisplayContent。
    private final WindowManagerService mService;
    private final DisplayContent mDisplayContent;

    // 从resource中取值赋值到这里，都是一些与DividerView宽高相关的内容
    private int mDividerWindowWidth;
    private int mDividerWindowWidthInactive;
    private int mDividerInsets;

    // setResizing的flag
    private boolean mResizing;
    // 即DividerView的窗口，在addWindow中判断并单独添加。
    private WindowState mWindow;
    // TaskStack计算最小的Bounds宽度所用的中间变量 其中mTmpRect是Stack的bounds
    private final Rect mTmpRect = new Rect();
    private final Rect mTmpRect2 = new Rect();
    private final Rect mTmpRect3 = new Rect();
    private final Rect mLastRect = new Rect();

    // 可见性，如果minimize之后，Divider也是不可见的
    private boolean mLastVisibility = false;
    // 提供给外部的监听器接口，可以监听如ime动画，可见性变化等等事件。
    private final RemoteCallbackList<IDockedStackListener> mDockedStackListeners
            = new RemoteCallbackList<>();
    // DSDC本身就是DimLayerUser，在构造函数中创建DimLayer实例，用于ime动画、拖拽时的dim效果。
    private final DimLayer mDimLayer;

    
    
    private boolean mMinimizedDock;// 当前的dock stack是否被minimized
    private boolean mAnimatingForMinimizedDockedStack;// 是否是minimized动画
    private boolean mAnimationStarted;// 标记动画是否已经启动，如果已经启动则不会进入动画代码
    private long mAnimationStartTime;// 根据mAnimationStarted判断第一帧，将传入的时间赋值到这里，用于计算动画
    private float mAnimationStart;// 和mAnimationTarget成对赋值与使用，minimize的起始比例和终止比例，计算出mLastAnimationProgress
    private float mAnimationTarget;
    private long mAnimationDuration;// 计算动画的总耗时时长
    private boolean mAnimationStartDelayed;// ime动画delay标记，在当WMS回调通知完成之后delay解除，可以进行ime动画
    private final Interpolator mMinimizedDockInterpolator;// minimize插值器，fast_out_slow_in
    private float mMaximizeMeetFraction;//  TODO：查询clipRevealAnimation，看这个值的作用
    private final Rect mTouchRegion = new Rect();// 与触摸以及focus相关
    private boolean mAnimatingForIme;// 标记ime正在动画中，不会重复启动ime动画
    private boolean mAdjustedForIme;// 是否根据ime动画进行过调整
    private int mImeHeight;// ime动画调整的距离
    private WindowState mDelayedImeWin;// ime的WindowState，在DSDC中只用于设置ime的WindowStateAnimation值
    private boolean mAdjustedForDivider;// ime动画的时候，dividerView是否已经调整过位置了
    private float mDividerAnimationStart;// 与mLastAnimationProgress一起计算mLastDividerProgress，调用stack.updateAdjustForIme来调整bounds
    private float mDividerAnimationTarget;
    private float mLastAnimationProgress;// 见mAnimationStart
    private float mLastDividerProgress;
    private final DividerSnapAlgorithm[] mSnapAlgorithmForRotation = new DividerSnapAlgorithm[4];// 使用四个方向，最终计算最小bounds宽度，提供给StackWindowContainer
    private boolean mImeHideRequested;// 是否需要隐藏ime窗口，如果这里是true，会影响到DisplayContent中adjustForImeIfNeeded计算imeVisible，从而影响是否需要进行ime动画等

```

DockedStackDividerController主要管理的内容有：两个动画MinimizeDock以及ime，在此期间会有dim的效果。
