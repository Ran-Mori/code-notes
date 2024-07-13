## GestureDetector

* 功能

  * Detects various gestures(fling, doubleclick, longpress, singletapup) and events using the supplied MotionEvents.

* 使用

  * 创建一个`GestureDetector`

  * 将`onTouchEvent`委托给它

    ```kotlin
    override fun onTouchEvent(event: MotionEvent): Boolean {
      return gesturedetector?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }
    ```

## 手势识别

* 双击

  ```java
  // 当前的Down Event
  private MotionEvent mCurrentDownEvent;
  // 外部注入的双击Listener
  private OnDoubleTapListener mDoubleTapListener;
  // 双击时second_down是否生效标记， second_down -> second_up之间为true
  private boolean mIsDoubleTapping;
  
  public boolean onTouchEvent(@NonNull MotionEvent ev) {
    switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
        	// 检查是否间隔小于DOUBLE_TAP_TIMEOUT，小于就移除Tap消息
        	boolean hadTapMessage = mHandler.hasMessages(TAP);
          if (hadTapMessage) mHandler.removeMessages(TAP);
        	// 1. frist_down(虽然名字是current)和first_up不为空
        	// 2. first_down -> second_down时间check
        	// 3. first_up -> second_down时间check，first_down -> second_down距离check
          if ((mCurrentDownEvent != null) && (mPreviousUpEvent != null)
              && hadTapMessage 
              && isConsideredDoubleTap(mCurrentDownEvent, mPreviousUpEvent, ev)) {
            mIsDoubleTapping = true;
            // 执行second_down的回调
            handled |= mDoubleTapListener.onDoubleTap(mCurrentDownEvent);
            // 执行双击中回调
            handled |= mDoubleTapListener.onDoubleTapEvent(ev);
          } else {
            // first_down，间隔DOUBLE_TAP_TIMEOUT(300ms) post一个信息
            mHandler.sendEmptyMessageDelayed(TAP, DOUBLE_TAP_TIMEOUT);
          }
        	
        	// 给mCurrentDownEvent赋值
          mCurrentDownEvent = MotionEvent.obtain(ev);
        case MotionEvent.ACTION_MOVE:
        	if (mIsDoubleTapping) {
            // 执行双击中回调
            handled |= mDoubleTapListener.onDoubleTapEvent(ev);
          }
        case MotionEvent.ACTION_UP:
        	if (mIsDoubleTapping) {
            // 执行双击中回调
            handled |= mDoubleTapListener.onDoubleTapEvent(ev);
          } else if(...) {
            // 单击回调优先级低于双击
            handled = mListener.onSingleTapUp(ev);
          }
        	// 给mPreviousUpEvent赋值
        	mPreviousUpEvent = currentUpEvent;
          // up结束后将双击中标记位清除
        	mIsDoubleTapping = false;
    }
    return handled;
  }
  
  private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp, MotionEvent secondDown) {
    // first_up -> second_down时间check，超过400ms和小于40ms都不行
    long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
    if (deltaTime > DOUBLE_TAP_TIMEOUT || deltaTime < DOUBLE_TAP_MIN_TIME) {
      
      return false;
    }
    
    int deltaX = (int) firstDown.getX() - (int) secondDown.getX();
    int deltaY = (int) firstDown.getY() - (int) secondDown.getY();
    // first_down -> second_down距离别隔太远
    return (deltaX * deltaX + deltaY * deltaY < mDoubleTapSlopSquare)
  }
  
  private class GestureHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
          case TAP:
          	if (mDoubleTapListener != null) {
              // first_down的回调
              mDoubleTapListener.onSingleTapConfirmed(mCurrentDownEvent);
            }
      }
    }
  }
  ```

  * 四个动作
    1. first_down -> `OnDoubleTapListener#onSingleTapConfirmed()` -> 在`GestureHandler`内被执行
    2. first_up -> 
    3. second_down -> `OnDoubleTapListener#onDoubleTap(), OnDoubleTapListener#onDoubleTapEvent() ` -> 在`onTouchEvent()#ACTION_DOWN` 内执行
    4. second_up -> `OnDoubleTapListener#onDoubleTapEvent()` -> 在`onTouchEvent()#ACTION_UP` 内执行
  * 设计思路
    * 双击的真正触发是在`second_down`而不是`second_up`
    * `first_down -> second_down` <= `300ms` && `frist_up -> second_down` <= `400ms`
    * 双击和单击无法做到互斥，即`first_up`的时候肯定会执行单击，有可能整个双击的过程中会有一次单击和双击

* 单击

  ```java
  public boolean onTouchEvent(@NonNull MotionEvent ev) {
    switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_UP:
        	if (mIsDoubleTapping) {
            // 非双击second_down -> second_up之间
          } else if (mInLongPress) {
            // 非长按中
          } else {
            // 执行单击回调
            handled = mListener.onSingleTapUp(ev);
          }
    }
    return handled;
  }
  ```

* 长按

  ```java
  // 是否支持长按标志位
  private boolean mIsLongpressEnabled;
  
  // 对外暴露设置的接口
  public boolean isLongpressEnabled() {
    return mIsLongpressEnabled;
  }
  
  private void init(Context context) {
    // 默认值是true
    mIsLongpressEnabled = true;
  }
  
  public boolean onTouchEvent(@NonNull MotionEvent ev) {
    switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
        	// 支持长按
        	if (mIsLongpressEnabled) {
            // 先删除长按消息
            mHandler.removeMessages(LONG_PRESS);
            // 长按生效时刻
            int time = mCurrentDownEvent.getDownTime() + ViewConfiguration.getLongPressTimeout();
            // 直接指定时间(sendMessageAtTime)执行
            mHandler.sendMessageAtTime(LONG_PRESS, time)
          }
        	// 标记位为false
        	mInLongPress = false;
        case MotionEvent.ACTION_MOVE:
        	if (distance > slopSquare) {
            // 移动得太远取消长按
            mHandler.removeMessages(LONG_PRESS);
          }
       	case MotionEvent.ACTION_UP:
        	if (mIsDoubleTapping) {
            // 非双击 second_down -> second_up之间
          } else if (mInLongPress) {
            // 松手时置标记位为false
            mInLongPress = false;
          }
        	// 太早松手的case
        	mHandler.removeMessages(LONG_PRESS);
    }
    return handled;
  }
  
  private class GestureHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
          case LONG_PRESS:
          	// 执行长按
          	dispatchLongPress();
          	break;
      }
    }
  }
  
  private void dispatchLongPress() {
    // 标志位置真
    mInLongPress = true;
    // 真正的执行
    mListener.onLongPress(mCurrentDownEvent);
  }
  ```

* fling

  ```java
  public boolean onTouchEvent(@NonNull MotionEvent ev) {
    switch (action & MotionEvent.ACTION_MASK) {
       	case MotionEvent.ACTION_UP:
        	if (mIsDoubleTapping) {
            // 非双击 second_down -> second_up之间
          } else if (mInLongPress) {
            // 非长按中
          } else if (mAlwaysInTapRegion) {
            // 非在点击 down -> up 的规定距离内
          } else {
            // x/y 方向的加速度足够
            if ((Math.abs(velocityY) > mMinimumFlingVelocity) 
                || (Math.abs(velocityX) > mMinimumFlingVelocity)) {
              // 执行fling回调
              handled = mListener.onFling(mCurrentDownEvent, ev, velocityX, velocityY);
            }
          }
    }
    return handled;
  }
  ```

  * 什么是fling? -> 一种滑动动作，一般是用户快速的滑动
  * 特点？ -> 从`ACTION_DOWN`开始，中间一系列`ACTION_MOVE`，到`ACTION_UP`结束。速度越快，`fling`越大
  * 接口 -> `boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);`    
    * e1 -> The first down motion event
    * e2 -> The last up motion event
    * velocityX/Y -> The velocity of this fling measured in pixels per second along the x/y axis.  

* scroll

  ```java
  // 用于计算scroll的临时变量
  private float mLastFocusX;
  private float mLastFocusY;
  
  public boolean onTouchEvent(@NonNull MotionEvent ev) {
    // Determine focal point
    float sumX = 0, sumY = 0;
    final int count = ev.getPointerCount();
    for (int i = 0; i < count; i++) {
      if (skipIndex == i) continue;
      sumX += ev.getX(i);
      sumY += ev.getY(i);
    }
    final int div = pointerUp ? count - 1 : count;
    // 得到focusX, focusY。其实就是所有手指的平均数
    final float focusX = sumX / div;
    final float focusY = sumY / div;
    
    switch (action & MotionEvent.ACTION_MASK) {
       	case MotionEvent.ACTION_MOVE:
        	float scrollX = mLastFocusX - focusX;
          float scrollY = mLastFocusY - focusY;
        	// 距离大于单击的范围
        	if (distance > slopSquare) {
            // 执行onScroll回调
            handled = mListener.onScroll(mCurrentDownEvent, ev, scrollX, scrollY);
            // 更新临时变量
            mLastFocusX = focusX;	
            mLastFocusY = focusY;
          }
    }
    return handled;
  }
  ```

  * 接口 -> `boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);`
    1. e1 -> The first down motion event
    2. e2 -> The move motion event
    3. distanceX/Y -> The distance along the X/Y axis that has been scrolled since the last call to onScroll. This is NOT the distance between e1 and e2.

## 单双击隔离

* 看源代码`DoubleClickListener`，核心思想还是先`postDelay()`然后再移除

## ViewFlipper

* 是什么 -> 一个`FrameLayout`，最多只能显示一个`child`
* 核心api
  1. `public void setDisplayedChild(int whichChild)`
  2. `public void setInAnimation(Animation inAnimation)`
  3. `public void setOutAnimation(Animation outAnimation)`