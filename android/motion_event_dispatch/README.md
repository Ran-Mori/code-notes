## Motion相关

* `getX()、getY()`：触摸位置相对于当前View的位置
* `getRawX()、getRawY()`：触摸位置相对于物理屏幕左上角的位置
* `TouchSlop`：被认为是滑动的最小距离。单位是`dp`
* `VelocityTracker`：滑动速度相关
* `GestureDetector`：手势检测相关

## 事件机制三方法

1. `View#dispatchTouchEvent(MotionEvent event)`：Pass the touch screen motion event down to the target view, or this view if it is the target.

   ```kotlin
   //View
   fun dispatchTouchEvent(val event: MotionEvent) {
     if(event.isTargetAccessibilityFocus()) {
       //处理Talkback模式，这个阶段不会消费事件
     }
     //一个变量来记录是否消费事件
     var result: Boolean = false 
     //这货直接响应
     mInputEventConsistencyVerifier?.onTouchEvent(event, 0)
     //这做了一层安全判断，但是大多情况下返回都是true，因此都会执行里面的代码
     if (onFilterTouchEventForSecurity(event)) { 
       //关键点一：首先尝试让onTouchListener消费事件
       if (mListenerInfo?.mOnTouchListener.onTouch(this, event)) {
         result = true
       }
       //关键点二：如果没有onTouchListener，则让onTouchEvent消费事件
       if (!result && onTouchEvent(event)) {
         result = true
       }
     }
     //返回最终是否消费掉事件
     return result
   }
   
   //设置onTouchListener接口
   fun setOnTouchListener(l:OnTouchListener) {
     getListenerInfo().mOnTouchListener = l;
   }
   
   //onTouchEvent是一个已经有默认实现的方法，而且非常复杂
   open fun onTouchEvent(event: MotionEvent): Boolean {
     //先存个变量看这个View是否可以点击
     val clickable = boolean clickable = ((viewFlags & CLICKABLE) == CLICKABLE
                   || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE)
                   || (viewFlags & CONTEXT_CLICKABLE) == CONTEXT_CLICKABLE
     //用户设置的touchDelegate优先级高
     if (mTouchDelegate?.onTouchEvent(event)) {
       return true;
     }
     //真正开始处理点击的地方
     if (clickable || (viewFlags & TOOLTIP) == TOOLTIP) {
       switch (action) {
         //点击肯定是ACTION_UP的时候
         case MotionEvent.ACTION_UP:
         	//如果不可点击，一切都清空
         	if (!clickable) {
             removeTapCallback();
             removeLongPressCallback();
             mInContextButtonPress = false;
             mHasPerformedLongPress = false;
             mIgnoreNextUpEvent = false;
             break;
           }
         	//这个方法内部有调用onClickListener()
         	performClickInternal()
       }
       //只要进到这来就肯定被消费
       return true
     }
     return false
   }
   
   //真正调点击listener
   fun performClick(): Boolean {
     val result: Boolean = false
     final ListenerInfo li = mListenerInfo;
     if (li != null && li.mOnClickListener != null) {
       playSoundEffect(SoundEffectConstants.CLICK)
       //关键代码
       li.mOnClickListener.onClick(this)
       result = true
     } else {
       result = false
     }
     return result;
   }
   ```

   ```kotlin
   // ViewGroup简单版本
   public boolean dispatchTouchEvent(MotionEvent event) {
     var consume = false
     if (allowIntercept() && onInterceptTouchEvent(ev)) {
       consume = onTouchEvent(ev)
     } else {
       comsume = child.dispatchTouchEvent(ev)
     }
     return consume;
   }
   
   // ViewGroup复杂版本
   public boolean dispatchTouchEvent(MotionEvent event) {
     if (actionMasked == MotionEvent.ACTION_DOWN) {
       mFirstTouchTarget = null; // down时置空mFirstTouchTarget
       mGroupFlags &= ~FLAG_DISALLOW_INTERCEPT; // down事件即使child让parent不拦截，parent也强制设为可拦截
     }
     
     final boolean intercepted; // 标志位是否拦截
     if (actionMasked == MotionEvent.ACTION_DOWN || mFirstTouchTarget != null) { // 只有当down事件，或者已经将down事件分给了child的时候才有拦截的机会
       final boolean disallowIntercept = (mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0; // 注意这个变量在上面可能会强制置为false
       if (!disallowIntercept) {
         intercepted = onInterceptTouchEvent(ev); // 看下是否要拦截
       } else {
         intercepted = false; // child让不要拦截，就不拦截
       }
     } else {
       intercepted = true; // 非down事件，之前的down事件又没有child消费，只有自己含泪消费拦截掉
     }
     
     if (!canceled && !intercepted) {
       if (actionMasked == MotionEvent.ACTION_DOWN) {
         if (newTouchTarget == null && childrenCount != 0) {
           for (int i = childrenCount - 1; i >= 0; i--) {
             // 事件响应要在对应的区域内
             if (!child.canReceivePointerEvents()
                 || !isTransformedTouchPointInView(x, y, child, null)) {
               continue;
             }
             // 当某个child的dispatchTouchEvent()返回true，代表child消费掉down事件
             if (dispatchTransformedTouchEvent(ev, false, child)) {
               // 赋值给mFirstTouchTarget
               newTouchTarget = addTouchTarget(child, idBitsToAssign);
               break;
             }
           }
         }
       }
     }
     
     if (mFirstTouchTarget == null) {
       // 没有child响应事件，由parent#onTouchEvent()响应
       handled = dispatchTransformedTouchEvent(ev, canceled, null)
     } else {
       // mFirstTouchTarget不为空时，直接让这个target来消费
       if (dispatchTransformedTouchEvent(ev, cancelChild,
               target.child, target.pointerIdBits)) {
         handled = true;
       }
     }
     return handled;
   }
   ```

2. `View#onTouchEvent(MotionEvent event)`：Implement this method to handle touch screen motion events.

3. `ViewGroup#onInterceptTouchEvent(MotionEvent ev)`：This allows you to watch events as they are dispatched to your children, and take ownership of the current gesture at any point.

   ```java
   // 是否允许ViewGroup对MotionEvent进行拦截
   public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
     if (disallowIntercept) {
         mGroupFlags |= FLAG_DISALLOW_INTERCEPT;
     } else {
         mGroupFlags &= ~FLAG_DISALLOW_INTERCEPT;
     }
   }
   ```

## 一些结论

* 事件序列 - 从`ACTION_DOWN`开始，到`ACTION_UP`结束，中间有很多的`ACTION_MOVE`
* 一个事件序列正常情况下只能被一个`View`拦截并消耗
* `View`如果要处理事件，就必修消耗`ACTION_DOWN`事件，否则就向上抛；`View`一旦消耗`ACTION_DOWN`事件，即消耗整个事件
* `ViewGroup`默认不拦截任何事件，它的`onInterceptTouchEvent`方法默认返回false

## 分发流程

* `activity` -> `root view` -> `child view`

  ```bash
  MainActivity.dispatchTouchEvent call start, action: DOWN
  ConstraintLayoutA.dispatchTouchEvent call start, action: DOWN
  ConstraintLayoutA.onInterceptTouchEvent call start, action: DOWN
  ConstraintLayoutA.onInterceptTouchEvent call end, result: false, action: DOWN
  ConstraintLayoutB.dispatchTouchEvent call start, action: DOWN
  ConstraintLayoutB.onInterceptTouchEvent call start, action: DOWN
  ConstraintLayoutB.onInterceptTouchEvent call end, result: false, action: DOWN
  ButtonA.dispatchTouchEvent call start, action: DOWN
  ButtonA.OnTouchListener call start, action: DOWN
  ButtonA.OnTouchListener call end, result = false, action: DOWN
  ButtonA.onTouchEvent call start, action: DOWN
  ButtonA.onTouchEvent call end, result: false, action: DOWN
  ButtonA.dispatchTouchEvent call end, result: false, action: DOWN
  ConstraintLayoutB.OnTouchListener call start, action: DOWN
  ConstraintLayoutB.OnTouchListener call end, result = false, action: DOWN
  ConstraintLayoutB.onTouchEvent call start, action: DOWN
  ConstraintLayoutB.onTouchEvent call end, result: false, action: DOWN
  ConstraintLayoutB.dispatchTouchEvent call end, result: false, action: DOWN
  ConstraintLayoutA.OnTouchListener call start, action: DOWN
  ConstraintLayoutA.OnTouchListener call end, result = false, action: DOWN
  ConstraintLayoutA.onTouchEvent call start, action: DOWN
  ConstraintLayoutA.onTouchEvent call end, result: false, action: DOWN
  ConstraintLayoutA.dispatchTouchEvent call end, result: false, action: DOWN
  MainActivity.onTouchEvent call start, action: DOWN
  MainActivity.onTouchEvent call end, result: false, action: DOWN
  MainActivity.dispatchTouchEvent call end, result: false, action: DOWN
  ```

* 优先级

  1. `setOnTouchListener()`
  2. 重写的`onTouchEvent()`，但一般没人会去重写它
  3. `onTouchEvent()`内调用的`setOnClickListener()`

* onTouch和onClick

  1. `onTouchListener()`内有两个参数`View、MotionEvent`，对它的自定义度更高
  2. `onClickListener()`内只有一个参数`View`，对它的自定义度更低

## 分发机制实现

* ViewGoup将down事件给了某一个View，如何确保后续事件都给这个View

  ```java
  //ViewGroup
  public boolean dispatchTouchEvent(MotionEvent ev) {
    // 处理最开始的down事件
    if (actionMasked == MotionEvent.ACTION_DOWN) {
      for (int i = childrenCount - 1; i >= 0; i--) {
        final View child = getAndVerifyPreorderedView(i);
        // 这个child消费了事件
        if (dispatchTransformedTouchEvent(child)) {
          // 调用addTouchTarget()方法
          addTouchTarget(child, idBitsToAssign);
        }
      }
    }
    
    // Dispatch to touch targets.
    if (mFirstTouchTarget == null) {
      // ...
    } else {
      TouchTarget target = mFirstTouchTarget;
      while (target != null) {
        // 将事件直接分发给target对应的child
        if (dispatchTransformedTouchEvent(target.child)) {
            handled = true;
        }
      }
    }
  }
  
  private TouchTarget addTouchTarget(View child, int pointerIdBits) {
    final TouchTarget target = TouchTarget.obtain(child, pointerIdBits);
    target.next = mFirstTouchTarget;
    // 将child包成了TouchTarget，赋值给mFirstTouchTarget
    mFirstTouchTarget = target;
    return target;
  }
  ```

* 当ViewGroup的`onTouchEvent()`已消费了down事件，如何确保后续事件都由`onTouchEvent()`消费

  ```java
  //ViewGroup
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (actionMasked == MotionEvent.ACTION_DOWN || mFirstTouchTarget != null) {
      //...
    } else {
        // There are no touch targets and this action is not an initial down
        // so this view group continues to intercept touches.
        intercepted = true;
    }
  }
  ```

## 手势识别

* 单击

  1. `setOnClickListener()`置`CLICKABLE` flag生效

     ```java
     //View
     public void setOnClickListener(@Nullable OnClickListener l) {
         if (!isClickable()) { setClickable(true);}
     }
     
     public boolean isClickable() {
         return (mViewFlags & CLICKABLE) == CLICKABLE;
     }
     
     public void setClickable(boolean clickable) {
         setFlags(clickable ? CLICKABLE : 0, CLICKABLE);
     }
     ```

  2. `onTouchEvent()`消费所有事件

     ```java
     public boolean onTouchEvent(MotionEvent event) {
       // 只要setOnClickListener() 调用了，这个clickable变量就肯定是true
       final boolean clickable = (viewFlags & CLICKABLE) == CLICKABLE;
     	
      	if (clickable || (viewFlags & TOOLTIP) == TOOLTIP) {
         switch (action) {
           case MotionEvent.ACTION_UP:
             // 当PFLAG_PRESSED flag生效时
             if ((mPrivateFlags & PFLAG_PRESSED) != 0) {
               // 执行点击事件
               performClickInternal();
             }
           case MotionEvent.ACTION_DOWN:
             // 将PFLAG_PRESSED flag置为生效
             setPressed(true, x, y);
             break;
           case MotionEvent.ACTION_MOVE:
             if (!pointInView(x, y, touchSlop)) {
               // 当已经消费down，但move移出view时，置PFLAG_PRESSED flag为不生效
               setPressed(false);
             }
         }
         return true; //直接返回消费
       }
       return false;
     }
                                  
     public void setPressed(boolean pressed) {
       // 置PFLAG_PRESSED flag
       if (pressed) {
           mPrivateFlags |= PFLAG_PRESSED;
       } else {
           mPrivateFlags &= ~PFLAG_PRESSED;
       }
       dispatchSetPressed(pressed);
     }
     ```

* 长按

  1. `setOnLongClickListener()`置`LONG_CLICKABLE`为生效

     ```java
     public void setOnLongClickListener(@Nullable OnLongClickListener l) {
         if (!isLongClickable()) { setLongClickable(true); }
     }
     public void setLongClickable(boolean longClickable) {
         setFlags(longClickable ? LONG_CLICKABLE : 0, LONG_CLICKABLE);
     }
     ```

  2. `onTouchEvent()`消费所有事件

     ```java
     // 是否已执行长按的临时变量
     private boolean mHasPerformedLongPress;
     // 实现长按的可能会被postDelay()的Runnable
     private CheckForLongPress mPendingCheckForLongPress;
     
     public boolean onTouchEvent(MotionEvent event) {
       // 只要setOnLongClickListener() 调用了，这个clickable变量就肯定是true
       final boolean clickable = (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE;
     	
      	if (clickable) {
         switch (action) {
           case MotionEvent.ACTION_UP:
             // 当PFLAG_PRESSED flag生效时
             if ((mPrivateFlags & PFLAG_PRESSED) != 0) {
               // check是否执行了长按
               if (!mHasPerformedLongPress) {
                 // 执行单击时将长按的Runnable给消除
                 removeLongPressCallback();
                 // 执行单击
                 performClickInternal();
               }
             }
           case MotionEvent.ACTION_DOWN:
             // 直接尝试执行长按操作, 参数delay是400ms
             checkForLongClick(ViewConfiguration.getLongPressTimeout());
             break;
           case MotionEvent.ACTION_MOVE:
             if (!pointInView(x, y, touchSlop)) {
               // 当已经消费down，但move移出view时，置PFLAG_PRESSED flag为不生效
               setPressed(false);
             }
         }
         return true; //直接返回消费
       }
       return false;
     }
     
     private void checkForLongClick(long delay, float x, float y, int classification) {
       // 先check LONG_CLICKABLE flag
       if ((mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE) {
         // 置临时变量为false
         mHasPerformedLongPress = false;
         if (mPendingCheckForLongPress == null) {
     			// 构造一个待post的Runnable
           mPendingCheckForLongPress = new CheckForLongPress();
         }
         // 直接往handler里面post
         postDelayed(mPendingCheckForLongPress, delay);
       }
     }
     
     // 一个用来执行长按的Runnable
     private final class CheckForLongPress implements Runnable {
       @Override
       public void run() {
         // 真正长按执行的地方
         if (performLongClick(mX, mY)) {
           // 置临时变量为true
           mHasPerformedLongPress = true;
         }
       }
     }
     ```

## 点击移开

* parent不拦截时

  ```java
  // View#onTouchEvent()
  public boolean onTouchEvent(MotionEvent event) {
    switch (action) {
      case MotionEvent.ACTION_UP:
        // 当最后的up事件在child范围内时，PFLAG_PRESSED生效，最后执行点击
        // 当最后的up事件在child范围外时，PFLAG_PRESSED不生效，最后的点击不会执行
        if ((mPrivateFlags & PFLAG_PRESSED) != 0) {
          performClickInternal(); // 单击实现
        }
    }
  }
  ```

* parent拦截`MotionEvent.ACTION_UP`事件时，`MotionEvent.ACTION_UP`事件会变`MotionEvent.CANCEL`

  ```bash
  MotionTextView.onTouchEvent call start, action: CANCEL
  MotionTextView.onTouchEvent call end, action: CANCEL, result = true
  ```

  ```java
  // ViewGroup#dispatchTouchEvent
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (mFirstTouchTarget == null) {
      // parent#onTouchEvent()处理的情况
    } else {
      // 如果拦截了，cancelChild置为true
      final boolean cancelChild = intercepted;
      // 将cancelChild作为第二个参数传下去
      if (dispatchTransformedTouchEvent(ev, cancelChild,
              target.child, target.pointerIdBits)) {
        handled = true;
      }
    }
    if (canceled) {
      // 如果事件转成cancel，则将mFirstTouchTarget置空，下一个事件来时分发按正常分发
      resetTouchState();
    }
  }
  
  // ViewGroup#dispatchTransformedTouchEvent
  private boolean dispatchTransformedTouchEvent(MotionEvent event, boolean cancel) {
    final int oldAction = event.getAction();
    if (cancel || oldAction == MotionEvent.ACTION_CANCEL) {
      // 如果cacel了，强设action为MotionEvent.ACTION_CANCEL
      event.setAction(MotionEvent.ACTION_CANCEL);
      handled = child.dispatchTouchEvent(event);
      return handled;
    }
  }
  ```

## 滑动冲突

* parent拦截法

  ```kotlin
  class InnerInterceptViewGroup {
    private var needIntercept: Boolean = false
  
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
      return when (ev?.action) {
        MotionEvent.ACTION_DOWN -> false
        MotionEvent.ACTION_MOVE -> needIntercept
        MotionEvent.ACTION_UP -> false
        else -> false
      }
    }
  }
  ```

  * down事件一定不能拦截，否则child的点击等业务统统失效，不要担心`down`给了child就所有事件给child响应，可以看看上面的`点击移开`
  * move事件就根据需要，如果判断用户是在左右滑动切ViewPager，就拦截，用于执行左右切换的操作
  * up事件就无所必要了。当move事件一旦被拦截，事件分发就和child没关系了，因此它会收到一个cancel事件；如果move事件都没被拦截，那就正常返回false默认值

* child拦截法

  ```kotlin
  // child#dispatchTouchEvent()
  override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
    when (event?.action) {
      // 当down给child时，置为true让parent先没有机会调用onInterceptTouchEvent()
      // 能否调用onInterceptTouchEvent()完全取决于下面的move事件是否放行
      MotionEvent.ACTION_DOWN -> {
        parent?.requestDisallowInterceptTouchEvent(true)
      }
  
      MotionEvent.ACTION_MOVE -> {
        if (needIntercept) {
          // 该处理滑动冲突时，让parent下面的时候有机会拦截
          parent?.requestDisallowInterceptTouchEvent(false)
        }
      }
      // up能传到这里，说明上面的move从来没有让拦截过，正常处理
      MotionEvent.ACTION_UP -> {}
    }
    return super.dispatchTouchEvent(event)
  }
  
  // parent#onIntercepTouchEvent()
  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    // down不拦截，其他事件是否拦截听child通知
    return ev?.action != MotionEvent.ACTION_DOWN
  }
  ```

***

## 