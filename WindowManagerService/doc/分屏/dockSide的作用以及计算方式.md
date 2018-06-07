# dockSide的作用以及计算方式

dockSide在分屏计算的过程中是一个比较重要的状态值，会与mBounds等值有很紧密的联系。


## Stack级对dockSide的获取

```java
TaskStack.java

    // 无参同名调用时传入 TaskStack.mBounds
    int getDockSide(Rect bounds) {
        // 非dock stack且不能在dock stack存在的时候resize的 返回invalid
        if (mStackId != DOCKED_STACK_ID && !StackId.isResizeableByDockedStack(mStackId)) {
            return DOCKED_INVALID;
        }
        // 没有关联到Display级的时候
        if (mDisplayContent == null) {
            return DOCKED_INVALID;
        }
        // 获取Display的logical宽高，需要注意的是，其中是以mDisplayInfo.rotation来宽高的
        mDisplayContent.getLogicalDisplayRect(mTmpRect);
        // 获取orientaion，是从mDisplayInfo的父类属性mFullConfiguration取出，那么问题多了一个，这个值和mDisplayInfo.rotation的差异在哪
        final int orientation = mDisplayContent.getConfiguration().orientation;
        return getDockSideUnchecked(bounds, mTmpRect, orientation);
    }

    static int getDockSideUnchecked(Rect bounds, Rect displayRect, int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 竖直模式，dock区域在到top或者bottom
            // 式子可以转换为： 0<=(displayRect.top+displayRect.bottom)/2-(bounds.top+bounds.bottom)/2
            // 即display的中间位置应该是大于bounds的中间位置，则是true
            if (bounds.top - displayRect.top <= displayRect.bottom - bounds.bottom) {
                return DOCKED_TOP;
            } else {
                return DOCKED_BOTTOM;
            }
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 水平模式，dock区域在到left或者right
            if (bounds.left - displayRect.left <= displayRect.right - bounds.right) {
                return DOCKED_LEFT;
            } else {
                return DOCKED_RIGHT;
            }
        } else {
            return DOCKED_INVALID;
        }
    }
```

接下来分析两个config的差异：




## 作用方式

```java
DockedDividerUtils.java

     /**
     * 当前以一个(0,0)-(1080,1920)分辨率的手机为例子
     *
     * @param position 传入的一个bound值，为884
     * @param dockSide dockSide 当竖屏，由分屏切回时，为2 即top
     * @param outRect 最终输出的Rect
     * @param displayWidth display 的宽度 1080
     * @param displayHeight display 高度 1920
     * @param dividerSize 中间那条杠杠的宽度 26xu
     *
     */
    public static void calculateBoundsForPosition(int position, int dockSide, Rect outRect,
            int displayWidth, int displayHeight, int dividerSize) {
        outRect.set(0, 0, displayWidth, displayHeight);
        switch (dockSide) {
            // 横屏，比较常见的状态
            case DOCKED_LEFT:
                outRect.right = position;
                break;
            // 竖屏，比较常见的状态
            case DOCKED_TOP:
                outRect.bottom = position;
                break;
            // 字面意义是和left相反的，实际上很少见
            case DOCKED_RIGHT:
                outRect.left = position + dividerSize;
                break;
            // 字面意义是和top相反的，实际上很少见，在异常情况下见过一次
            case DOCKED_BOTTOM:
                outRect.top = position + dividerSize;
                break;
        }
        // 根据top和left来计算出一个合理的outRect值
        sanitizeStackBounds(outRect, dockSide == DOCKED_LEFT || dockSide == DOCKED_TOP);
    }

```

当dockSide的是left的时候，意味着是横屏的，且在屏幕左边。
当dockSide的是right的时候，意味着是横屏的，且在屏幕右边。
当dockSide的是top的时候，意味着是竖屏的，且在屏幕上边。
当dockSide的是bottom的时候，意味着是竖屏的，且在屏幕下边。

实际上最常见的是left和right。



