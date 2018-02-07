# Activity创建过程与window相关部分 #

当一个Activity的ActivityThread执行到handleResumeActivity函数的时候，说明当前activity的Create,Start准备工作都完成了。接下来是需要进行Resume准备，显示界面的部分。

整体流程图：
![流程图](启动activity绘制流程.png)

## 具体代码分析 ##

所以我们从ActivityThread的handleResumeActivity开始追踪。

首先ActivityThread在执行handleResumeActivity之前还没有与WindowManagerService有过交互。到了这里主要的工作有：
1. 当activity需要正常显示，不需要启动新activity或被finish时，设置LayoutParams,并将其与decorView通过addView加入到wm中去。
2. 成功添加之后，还发现会有一个updateViewLayout的请求会发送给wm。
3. 当以上两步都完成了之后，会调用`r.activity.makeVisible();`，通知activity可以显示了。


### addView流程 ###

```
ActivityThread.java

	final void handleResumeActivity(IBinder token,
            boolean clearHide, boolean isForward,boolean ignoreCallback, boolean reallyResume) {
        // If we are getting ready to gc after going to the background, well
        // we are back active so skip it.
        unscheduleGcIdler();
        mSomeActivitiesChanged = true;

        ActivityClientRecord r = performResumeActivity(token, clearHide,ignoreCallback);

        if (r != null) {
            final int forwardBit = isForward ?
                    WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION : 0;
			
			//如果这个window还没有被添加到WindowManager，并且这老哥还没finish掉自己或者start另外一个activity，
			//那么接着往下走，并且添加这个window
            boolean willBeVisible = !a.mStartedActivity;
            if (!willBeVisible) {
                try {
					//更新willBeVisible的状态
                    willBeVisible = ActivityManagerNative.getDefault().willActivityBeVisible(
                            a.getActivityToken());
                } catch (RemoteException e) {
                }
            }
            if (r.window == null && !a.mFinished && willBeVisible) {
				//符合上述条件，添加window到WindowManager里面去
                r.window = r.activity.getWindow();
                View decor = r.window.getDecorView();

				//确保当前是invisible的，即需要布局
				//因为当前还需要等待绑定wm端，并等待一些请求
                decor.setVisibility(View.INVISIBLE);
                ViewManager wm = a.getWindowManager();

				//获取window中设置好的LayoutParams
                WindowManager.LayoutParams l = r.window.getAttributes();
				//关联decorView
                a.mDecor = decor;
				//Activity的Type被限定成了TYPE_BASE_APPLICATION
                l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
				//设置软键盘模式
                l.softInputMode |= forwardBit;
                if (a.mVisibleFromClient) {
                    a.mWindowAdded = true;
					
					//最终，我们向wm添加了decor这个view，layoutParams是l
                    wm.addView(decor, l);
                }

			//如果window已经被添加了，但是在resume期间，我们就准备启动另一个activity，那么我们不让这个在resume的activity变的可见
            } else if (!willBeVisible) {
                if (localLOGV) Slog.v(
                    TAG, "Launch " + r + " mStartedActivity set");
                r.hideForNow = true;
            }

            // 防止有遗漏的window
            cleanUpPendingRemoveWindows(r);

			// window这个如果被添加了而不是finish掉了或者启动了另一个activity，那么到这里就是可见的
            if (!r.activity.mFinished && willBeVisible
                    && r.activity.mDecor != null && !r.hideForNow) {
                if (r.newConfig != null) {
                    performConfigurationChanged(r.activity, r.newConfig);
                    freeTextLayoutCachesIfNeeded(r.activity.mCurrentConfig.diff(r.newConfig));
                    r.newConfig = null;
                }
                

                WindowManager.LayoutParams l = r.window.getAttributes();
                if ((l.softInputMode
                        & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION)
                        != forwardBit) {
                    l.softInputMode = (l.softInputMode
                            & (~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION))
                            | forwardBit;
                    if (r.activity.mVisibleFromClient) {
                        ViewManager wm = a.getWindowManager();
                        View decor = r.window.getDecorView();
						//可见之后，会发出updateViewLayout到wm去
                        wm.updateViewLayout(decor, l);
                    }
                }

				// mVisibleFromServer 和 mVisibleFromClient 两者用于描述当前可见性，mVisibleFromClient描述请求状态
				// mVisibleFromServer 是描述是否真正的完成
                r.activity.mVisibleFromServer = true;
                mNumVisibleActivities++;
                if (r.activity.mVisibleFromClient) {
					//调用makeVisivle
                    r.activity.makeVisible();
                }
            }

            if (!r.onlyLocalRequest) {
                r.nextIdle = mNewActivities;
                mNewActivities = r;
                if (localLOGV) Slog.v(
                    TAG, "Scheduling idle handler for " + r);
                Looper.myQueue().addIdleHandler(new Idler());
            }
            r.onlyLocalRequest = false;

            // 告诉AMS我们已经完成了resume
            if (reallyResume) {
                try {
                    ActivityManagerNative.getDefault().activityResumed(token);
                } catch (RemoteException ex) {
                }
            }

        } else {
            //异常处理
        }
    }
```

接下来我们需要追踪的是wm到底是怎么addView的。

接口ViewManager被WindowManager继承。WindowManager主要定义了WindowManager.LayoutParams这个很重要的状态内部类。

WindowManager接口实际上又被WindowManagerImpl所继承，impl中持有WindowManagerGlobal的实例，实现的多数方法是直接调用mGlobal来实现的，所以接下来直接看WindowManagerGlobal中是如何实现addView的。

```

WindowManagerGlobal.java

	public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {

        //传入参数判空
		................................................

		//注意:调用addView的时候就会重新创建一个ViewRootImpl，比如说dialog其实是有单独的ViewRootImpl的
        ViewRootImpl root;
        View panelParentView = null;

        synchronized (mLock) {

			//预处理部分，暂时不关注
            ................................................
			//创建新的
            root = new ViewRootImpl(view.getContext(), display);

            view.setLayoutParams(wparams);

			//在global中添加数据记录
            mViews.add(view);
            mRoots.add(root);
            mParams.add(wparams);
        }

        // 最后做这个，因为发出了一个请求
        try {
			//调用新创建的ViewRootImpl
            root.setView(view, wparams, panelParentView);
        } catch (RuntimeException e) {
            //异常处理
        }
    }

```

接下来主要分析一下ViewRootImpl，这个类在WindowManager相关中所占的作用非常之大。我会将一些比较关键的属性与方法额外列出来，着重分析。

```

ViewRootImpl.java

public final class ViewRootImpl implements ViewParent,
        View.AttachInfo.Callbacks, HardwareRenderer.HardwareDrawCallbacks {
	//会新建，但是在setView中会copy传入的Params。
	final WindowManager.LayoutParams mWindowAttributes = new WindowManager.LayoutParams();
	
	//W是IWindow的BN端，用于给WindowState中的BP端对象mClient来调用。
	final W mWindow;

	//Choreographer用于发送消息，控制绘制的节奏
	Choreographer mChoreographer;

	// Surface在这里是可以被任意线程调用的，但是必须被在同步锁里面
	// Surface绝对不可以被重新分配或者清除（调用Surface.clear）
    final Surface mSurface = new Surface();

	// 以下变量会被多个线程访问到
    final Rect mWinFrame; // wm 分配的 frame 
    final Rect mPendingOverscanInsets = new Rect();
    final Rect mPendingVisibleInsets = new Rect();
    final Rect mPendingStableInsets = new Rect();
    final Rect mPendingContentInsets = new Rect();

	//Configuration保存的是与界面全局相关的一些值，比如说mcc/mnc、locale、screenLayout以及如navigation之类的配置
	final Configuration mLastConfiguration = new Configuration();
    final Configuration mPendingConfiguration = new Configuration();

	//投递消息到这里被处理
	final ViewRootHandler mHandler = new ViewRootHandler();

	//构造函数初始化各种属性 传入参数Display中保存的信息说明该ViewRootImpl需要被哪个显示设备输出
	public ViewRootImpl(Context context, Display display) {
        mContext = context;
		mWindowSession = WindowManagerGlobal.getWindowSession();

        //一通初始化
        ................................................
		//获取SystemProperties的一些值
        loadSystemProperties();
        mWindowIsRound = context.getResources().getBoolean(
                com.android.internal.R.bool.config_windowIsRound);
		
    }

	//setView是ActivityThread调用的，目的是设置一些参数到ViewRootImpl里，并且远程调用服务端，获取可用的布局属性等。
	public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
        synchronized (this) {
            if (mView == null) {
                mView = view;

                mAttachInfo.mDisplayState = mDisplay.getState();
                mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);

                mViewLayoutDirectionInitial = mView.getRawLayoutDirection();
                mFallbackEventHandler.setView(view);
                mWindowAttributes.copyFrom(attrs);
                if (mWindowAttributes.packageName == null) {
                    mWindowAttributes.packageName = mBasePackageName;
                }
                attrs = mWindowAttributes;
				//持续追踪客户端实际提供的window flag，在setView的时候去更新这个值
                mClientWindowLayoutFlags = attrs.flags;

                setAccessibilityFocus(null, null);

                if (view instanceof RootViewSurfaceTaker) {
                    mSurfaceHolderCallback =
                            ((RootViewSurfaceTaker)view).willYouTakeTheSurface();
                    if (mSurfaceHolderCallback != null) {
                        mSurfaceHolder = new TakenSurfaceHolder();
                        mSurfaceHolder.setFormat(PixelFormat.UNKNOWN);
                    }
                }

				//计算在某个Z轴上，surface需要绘制的区域
                // TODO: Use real shadow insets for a constant max Z.
                final int surfaceInset = (int) Math.ceil(view.getZ() * 2);
                attrs.surfaceInsets.set(surfaceInset, surfaceInset, surfaceInset, surfaceInset);

                CompatibilityInfo compatibilityInfo = mDisplayAdjustments.getCompatibilityInfo();
                mTranslator = compatibilityInfo.getTranslator();
                mDisplayAdjustments.setActivityToken(attrs.token);

                // 中间一些值的处理，暂时不关注
                ................................................

                int res; /* = WindowManagerImpl.ADD_OKAY; */

                // Schedule the first layout -before- adding to the window
                // manager, to make sure we do the relayout before receiving
                // any other events from the system.
				//在使当前window加入窗口管理器，开始第一次layout，确保我们在接收到其他系统的事件之前已经relayout过了
                requestLayout();
                if ((mWindowAttributes.inputFeatures
                        & WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL) == 0) {
                    mInputChannel = new InputChannel();
                }
                try {
                    mOrigWindowType = mWindowAttributes.type;
                    mAttachInfo.mRecomputeGlobalAttributes = true;
                    collectViewAttributes();
					
					//其实最关键的还是这一句，当ViewRootImpl需要被绑定某个View（即addView传入的view）上，需要addToDisplay
                    res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                            getHostVisibility(), mDisplay.getDisplayId(),
                            mAttachInfo.mContentInsets, mAttachInfo.mStableInsets, mInputChannel)；

				// 异常处理，对res返回值的处理等
                ................................................
            }
        }
    }
}

```

看到上述代码，可以追踪到，ViewRootImpl初始化的时候更多的只是从WindowManagerGlobal中取值等赋初始值等。而与绑定view相关，使ViewRootImpl真正工作的是setView，这个函数会做许多与surface相关的操作，毕竟surface的内容都是由ViewRootImpl来添加的。而最终，会去调用从WindowManagerGlobal中获取的mWindowSession对象的addToDisplay方法。

那么mWindowSession是个什么样的对象呢？

首先先放一张大佬总结的，WindowManagerService相关结构的神图:
![WMS架构](WindowManagerService整体.png)

我们关注的mWindowSession在放大图中：
![局部](WindowManagerService局部0.png)

我们可以很明显可以看到继承IWindowManager的服务端提供了接口openSession。那么为什么又要单独给出一个IWindowSession呢？

接下来我们继续按照线索追踪，看看这个架构的原因。

```

WindowManagerGlobal.java

	public static IWindowSession getWindowSession() {
        synchronized (WindowManagerGlobal.class) {
            if (sWindowSession == null) {
                try {
                    InputMethodManager imm = InputMethodManager.getInstance();
					//通过ServiceManager获取IWindowManager
                    IWindowManager windowManager = getWindowManagerService();
					//调用IWindowManager的openSession,会调用到WindowManagerService的相应函数
                    sWindowSession = windowManager.openSession(
                            new IWindowSessionCallback.Stub() {
                                @Override
                                public void onAnimatorScaleChanged(float scale) {
                                    ValueAnimator.setDurationScale(scale);
                                }
                            },
                            imm.getClient(), imm.getInputContext());
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to open window session", e);
                }
            }
            return sWindowSession;
        }
    }

```

```

WindowManagerService.java

	public IWindowSession openSession(IWindowSessionCallback callback, IInputMethodClient client,
            IInputContext inputContext) {
        if (client == null) throw new IllegalArgumentException("null client");
        if (inputContext == null) throw new IllegalArgumentException("null inputContext");
        Session session = new Session(this, callback, client, inputContext);
		//返回根据传入参数创建的IWindowSession的BN端
        return session;
    }

```

```

Session.java


	public int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs,
            int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets,
            InputChannel outInputChannel) {
		//直接调用服务端的属性mService所对应的方法，mService是初始化时传入的属性 
        return mService.addWindow(this, window, seq, attrs, viewVisibility, displayId,
                outContentInsets, outStableInsets, outInputChannel);
    }
```

在WindowManagerGlobal中，调用的是WindowManagerGlobal的getWindowSession。而这个方法会调用到服务端的Session类的addToDisplay方法。最终还是由mService来处理addWindow。

WMS向外提供openSession，从而返回另一个Binder对象，使外界通过另一条路进行通信。而Session也会与Surface的创建与回收相关。所以会单独的将Session拆开，同时也将很多与WMS相关的操作直接进行调用。

Session与WMS的关系是多对一的关系，Session在WindowState中保存，储存在WMS的数据结构中。

那么接下来主要关注WindowManagerService的addWindow。

```

WindowManagerService.java

	public int addWindow(Session session, IWindow client, int seq,
            WindowManager.LayoutParams attrs, int viewVisibility, int displayId,
            Rect outContentInsets, Rect outStableInsets, InputChannel outInputChannel) {
        int[] appOp = new int[1];
        int res = mPolicy.checkAddPermission(attrs, appOp);
        if (res != WindowManagerGlobal.ADD_OKAY) {
            return res;
        }

        boolean reportNewConfig = false;
        WindowState attachedWindow = null;
        WindowState win = null;
        long origId;
        final int type = attrs.type;

        synchronized(mWindowMap) {
            

            boolean addToken = false;
            WindowToken token = mTokenMap.get(attrs.token);

			//异常处理
			................................................
				//新建一个与attrs.token相关的token值
                token = new WindowToken(this, attrs.token, -1, false);
                addToken = true;
           
			//很长的一段异常处理
			................................................

			//使用前面的一些属性来新建一个WindowState对象
            win = new WindowState(this, session, client, token,
                    attachedWindow, appOp[0], seq, attrs, viewVisibility, displayContent);
            
			//对于win返回值的异常处理
			................................................

			//WindowManagerPolicy实际上是PhoneWindowManager实现
			//根据当前窗口状态（是否需要状态栏等），来调整mAttrs.flags的属性
            mPolicy.adjustWindowParamsLw(win.mAttrs);
            win.setShowToOwnerOnlyLocked(mPolicy.checkShowToOwnerOnly(attrs));

			//根据 attrs.type 设置一些操作之前需要的值，只要执行完都会return ADD_OKAY
            res = mPolicy.prepareAddWindowLw(win, attrs);
            if (res != WindowManagerGlobal.ADD_OKAY) {
                return res;
            }

			//设置跟InputChannel相关的初始化，与InputDispatcher相关
            if (outInputChannel != null && (attrs.inputFeatures
                    & WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL) == 0) {
                String name = win.makeInputChannelName();
                InputChannel[] inputChannels = InputChannel.openInputChannelPair(name);
                win.setInputChannel(inputChannels[0]);
                inputChannels[1].transferTo(outInputChannel);

                mInputManager.registerInputChannel(win.mInputChannel, win.mInputWindowHandle);
            }

            // 从现在开始，不能出现异常之类的

            res = WindowManagerGlobal.ADD_OKAY;

            origId = Binder.clearCallingIdentity();

            if (addToken) {
                mTokenMap.put(attrs.token, token);
            }

			//实际上调用mSession.windowAddedLocked  这一步是对于session的绑定
            win.attach();
			//将WindowState添加到mWindowMap中
            mWindowMap.put(client.asBinder(), win);

            ................................................

            boolean imMayMove = true;
			//这一段判断，主要是处理掉type是InputMethod相关的步骤，将windowState放入不同的数据结构
            if (type == TYPE_INPUT_METHOD) {
                win.mGivenInsetsPending = true;
                mInputMethodWindow = win;
                addInputMethodWindowToListLocked(win);
                imMayMove = false;
            } else if (type == TYPE_INPUT_METHOD_DIALOG) {
                mInputMethodDialogs.add(win);
                addWindowToListInOrderLocked(win, true);
                moveInputMethodDialogsLocked(findDesiredInputMethodWindowIndexLocked(true));
                imMayMove = false;
            } else {
                addWindowToListInOrderLocked(win, true);
				
				getDefaultDisplayContentLocked().pendingLayoutChanges|= WindowManagerPolicy.FINISH_LAYOUT_REDO_WALLPAPER;

				//处理掉type与wallPaper相关的逻辑
                if (type == TYPE_WALLPAPER) {
                    mLastWallpaperTimeoutTime = 0;
                    displayContent.pendingLayoutChanges |= FINISH_LAYOUT_REDO_WALLPAPER;
                } else if ((attrs.flags&FLAG_SHOW_WALLPAPER) != 0) {
                    displayContent.pendingLayoutChanges |= FINISH_LAYOUT_REDO_WALLPAPER;
                } else if (mWallpaperTarget != null
                        && mWallpaperTarget.mLayer >= win.mBaseLayer) {
                    // If there is currently a wallpaper being shown, and
                    // the base layer of the new window is below the current
                    // layer of the target window, then adjust the wallpaper.
                    // This is to avoid a new window being placed between the
                    // wallpaper and its target.
                    displayContent.pendingLayoutChanges |= FINISH_LAYOUT_REDO_WALLPAPER;
                }
            }
			
			//设置winAnimator的值，表示当前动画处于何种状态，当前正在进入Animation
            final WindowStateAnimator winAnimator = win.mWinAnimator;
            winAnimator.mEnterAnimationPending = true;
            winAnimator.mEnteringAnimation = true;

			//根据是不是default的display来设置outContentInsets以及outStableInsets
            if (displayContent.isDefaultDisplay) {
                mPolicy.getInsetHintLw(win.mAttrs, outContentInsets, outStableInsets);
            } else {
                outContentInsets.setEmpty();
                outStableInsets.setEmpty();
            }

            if (mInTouchMode) {
                res |= WindowManagerGlobal.ADD_FLAG_IN_TOUCH_MODE;
            }
            if (win.mAppToken == null || !win.mAppToken.clientHidden) {
                res |= WindowManagerGlobal.ADD_FLAG_APP_VISIBLE;
            }

			//mInputMonitor是输入事件以及注释区域管理器
            mInputMonitor.setUpdateInputWindowsNeededLw();

			//检测foucus是否改变，这里的代码暂时没有继续往后追
            boolean focusChanged = false;
            if (win.canReceiveKeys()) {
                focusChanged = updateFocusedWindowLocked(UPDATE_FOCUS_WILL_ASSIGN_LAYERS,
                        false /*updateInputWindows*/);
                if (focusChanged) {
                    imMayMove = false;
                }
            }

            if (imMayMove) {
                moveInputMethodWindowsIfNeededLocked(false);
            }


			//这里重新计算window的层次
            assignLayersLocked(displayContent.getWindowList());
			//注意，在这里不要进行布局计算，window必须要重新布局之后才能显示，所以我们接下来会完成这件事

			//focus改变了才会执行这里
            if (focusChanged) {
                mInputMonitor.setInputFocusLw(mCurrentFocus, false /*updateInputWindows*/);
            }

			//更新由input dispatcher提供的已缓存的信息
            mInputMonitor.updateInputWindowsLw(false /*force*/);

            ................................................

        Binder.restoreCallingIdentity(origId);

        return res;
    }

```

流程到这里，ActivityThread的handleResumeActivity与WMS相关的第一个步骤就结束了。

这个流程中首先会创建ViewRootImpl，然后在setView中openSession。通过Session向WMS请求addWindow。addWindow中会根据type值，设置一些变量，然后对InputDispatcher，Animation这些Window相关模块的初始化设置。

### updateViewLayout流程 ###

第二步，在符合判断的情况会进入updateViewLayout。和addView的调用流程很相似，追踪到WindowManagerGlobal：

```

WindowManagerGlobal.java

public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
		//判空处理
        ................................................

		WindowManager.LayoutParams wparams = (WindowManager.LayoutParams)params;
        
		//设置新的params
        view.setLayoutParams(wparams);

        synchronized (mLock) {
            int index = findViewLocked(view, true);
			//更新Layout
            wparams = mMultiWindowGlobal.updateViewLayout(view,wparams,mParams,index);
            ViewRootImpl root = mRoots.get(index);
			//删除了再重新添加一遍
            mParams.remove(index);
            mParams.add(index, wparams);
			//更新一遍LayoutParams，最终调用scheduleTraversals
            root.setLayoutParams(wparams, false);
        }
    }

```

实际上，这里没有太多意外的东西。首先把各数据结构中的params更新成新的，最后在进行一次ViewRootImpl的scheduleTraversals进行重新performMeasure、performLayout、performDraw的过程。

performDraw的最后会通知WMS进行重新布局。重新布局的重点会通过AppWindowToken进行IPC调用AMS,从而通知Activity进入onStop等。

[这一块的东西比较复杂，重新写了一篇文档记录该过程](Window显示完成通知Activity的流程.md)。


### makeVisible流程 ###

直接从Activity相关代码追起：

```
Activity.java

	void makeVisible() {
        if (!mWindowAdded) {
            ViewManager wm = getWindowManager();
            wm.addView(mDecor, getWindow().getAttributes());
            mWindowAdded = true;
        }
        mDecor.setVisibility(View.VISIBLE);
    }

```

由于mWindowAdded已经添加过了，所以我们直接看DecorView的setVisibility。

```
View.java

	public void setVisibility(@Visibility int visibility) {
        setFlags(visibility, VISIBILITY_MASK);
        if (mBackground != null) mBackground.setVisible(visibility == VISIBLE, false);
    }

	//flag是一个32bit的值，mask是用于和flag进行位与操作，得到各种标签值，通过这种机制可以压缩标志位长度
	void setFlags(int flags, int mask) {
        final boolean accessibilityEnabled =
                AccessibilityManager.getInstance(mContext).isEnabled();
        final boolean oldIncludeForAccessibility = accessibilityEnabled && includeForAccessibility();

        int old = mViewFlags;
        mViewFlags = (mViewFlags & ~mask) | (flags & mask);

		//没有值改变，则直接返回
        int changed = mViewFlags ^ old;
        if (changed == 0) {
            return;
        }
        int privateFlags = mPrivateFlags;

/* --------------------------------检查FOCUSABLE bit位是否改变了---------------------------- */
        ................................................

/* --------------------------------检查GONE bit位是否改变了---------------------------- */
        ................................................

/* --------------------------------检查INVISIBLE bit位是否改变了---------------------------- */
        if ((changed & INVISIBLE) != 0) {
            needGlobalAttributesUpdate(false);
            /*
             * If this view is becoming invisible, set the DRAWN flag so that
             * the next invalidate() will not be skipped.
             */
            mPrivateFlags |= PFLAG_DRAWN;

            if (((mViewFlags & VISIBILITY_MASK) == INVISIBLE)) {
                // root view becoming invisible shouldn't clear focus and accessibility focus
                if (getRootView() != this) {
                    if (hasFocus()) clearFocus();
                    clearAccessibilityFocus();
                }
            }
            if (mAttachInfo != null) {
                mAttachInfo.mViewVisibilityChanged = true;
            }
        }

        if ((changed & VISIBILITY_MASK) != 0) {
            // If the view is invisible, cleanup its display list to free up resources
            if (newVisibility != VISIBLE && mAttachInfo != null) {
                cleanupDraw();
            }

            if (mParent instanceof ViewGroup) {
                ((ViewGroup) mParent).onChildVisibilityChanged(this,
                        (changed & VISIBILITY_MASK), newVisibility);
                ((View) mParent).invalidate(true);
            } else if (mParent != null) {
                mParent.invalidateChild(this, null);
            }
            dispatchVisibilityChanged(this, newVisibility);

            notifySubtreeAccessibilityStateChangedIfNeeded();
        }

        if ((changed & WILL_NOT_CACHE_DRAWING) != 0) {
            destroyDrawingCache();
        }

        if ((changed & DRAWING_CACHE_ENABLED) != 0) {
            destroyDrawingCache();
            mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
            invalidateParentCaches();
        }

        if ((changed & DRAWING_CACHE_QUALITY_MASK) != 0) {
            destroyDrawingCache();
            mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
        }

        if ((changed & DRAW_MASK) != 0) {
            if ((mViewFlags & WILL_NOT_DRAW) != 0) {
                if (mBackground != null) {
                    mPrivateFlags &= ~PFLAG_SKIP_DRAW;
                    mPrivateFlags |= PFLAG_ONLY_DRAWS_BACKGROUND;
                } else {
                    mPrivateFlags |= PFLAG_SKIP_DRAW;
                }
            } else {
                mPrivateFlags &= ~PFLAG_SKIP_DRAW;
            }
            requestLayout();
            invalidate(true);
        }

        if ((changed & KEEP_SCREEN_ON) != 0) {
            if (mParent != null && mAttachInfo != null && !mAttachInfo.mRecomputeGlobalAttributes) {
                mParent.recomputeViewAttributes(this);
            }
        }

        if (accessibilityEnabled) {
            if ((changed & FOCUSABLE_MASK) != 0 || (changed & VISIBILITY_MASK) != 0
                    || (changed & CLICKABLE) != 0 || (changed & LONG_CLICKABLE) != 0) {
                if (oldIncludeForAccessibility != includeForAccessibility()) {
                    notifySubtreeAccessibilityStateChangedIfNeeded();
                } else {
                    notifyViewAccessibilityStateChangedIfNeeded(
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
                }
            } else if ((changed & ENABLED_MASK) != 0) {
                notifyViewAccessibilityStateChangedIfNeeded(
                        AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
            }
        }
    }

```


----------------------   施工中，暂时还没分析清楚这里是怎么样的流程  ----------------------------




