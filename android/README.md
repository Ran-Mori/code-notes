# Android

## BaseAndroidProject

### 规定版本

```bash
# AS version ->  Chipmunk | 2021.2.1 Canary 1
# build date -> on October 13, 2021
```

### 设置`compileSdk`

* 规定为`33`，因为google pixel 4的api版本是`33`，方便调试源码

### 依赖`codelocator`

* 方便使用`CodeLocator`

### 基础布局

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout>
    <TextView />
</FrameLayout>
```

***

## BitmapDrawableCanvas

### Bitmap

* 参考博客 -> [Android Bitmap详解](https://www.jianshu.com/p/28c249278c49)

* 本质 -> 是一个位图，很多核心的方法全是`native`的，可以简单的把它理解成像素矩阵

* 创建 -> 一般都是直接或者间接通过`InputStream`或者`Byte[]`来进行创建

  ```java
  public class BitmapFactory {
    public static Bitmap decodeFile(String pathName);
    public static Bitmap decodeResource(Resources res, int id);
    public static Bitmap decodeByteArray(byte[] data, int offset, int length);
    public static Bitmap decodeStream(InputStream is);
  }
  ```

* 重要方法

  1. `recycle()` -> free native object, clear reference to the pixel data.


### Drawable

* 参考博客链接 -> [Android Drawable 详解](https://www.jianshu.com/p/d6c791709949)

* 本质：`something that can be drawn.`表示可以被绘制在`canvas`上的东西

* 重要方法

  1. `inflate()` -> 每一个子类`Drawable`都应该去实现，规定了从`xml`如何创建一个对应的`Drawable`

     ```java
     public final class DrawableInflater {
       Drawable inflateFromXmlForDensity() {
         //通过xml tag的名字确定创建那个drawable
         Drawable drawable = inflateFromTag(name);
         //调用这个drawable的inflate方法
         drawable.inflate(mRes, parser, attrs, theme);
         return drawable;
       }
       
       private Drawable inflateFromTag(String name) {
         //根据tag名称创建对应的drawable
         switch (name) {
                 case "selector":
                     return new StateListDrawable();
                 case "level-list":
                     return new LevelListDrawable();
                 case "layer-list":
                     return new LayerDrawable();
         }
       }
     }
     ```

  2. `setBounds()` -> 当在`canvas`进行`draw`时，规定好位置和区域。即决定了此`Drawable`被绘制在`canvas`的那个位置以及绘制多大。注意它不是决定`drawable`那部分被`draw`，而是决定`canvas`那部分来`draw`整个`drawable`

  3. `draw(Canvas canvas)` -> 如何把这个`drawable`绘制到`convas`上，这依赖每个`Drawable`去自己实现

* 创建

  ```java
  public abstract class Drawable {
    public static Drawable createFromStream(InputStream is);
    public static Drawable createFromResourceStream(Resources res);
    public static Drawable createFromXml(Resources r);
    public static Drawable createFromPath(String pathName);
  }
  ```

### Canvas

* 是什么 -> 提供了`draw`的方法，即暴露了`draw`的能力

* `draw somethind`的四个必备要素
  1. `A Bitmap to hold the pixels `
  2. `a Canvas to host the draw call`
  3. `a drawing primitive (e.g. Rect, Path, text, Bitmap)`
  4. `a paint`

* 四个要素示例

  ```kotlin
  // Drawable -> Bitmap
  val drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.takagi))
  drawable.setBounds(200, 200, 1000 ,1000)
  val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap) //要素1 -> bitmap，要素2 -> canvas，要素3 -> drawable，要素4 -> 默认
  drawable.draw(canvas)
  imageView?.setImageDrawable(BitmapDrawable(resources, bitmap))
  ```

### 相互转换

1. `Bitmap` -> `Drawable`

   ```kotlin
   val bitmap = BitmapFactory.decodeResource(resources, R.drawable.takagi)
   val bitmapDrawable = BitmapDrawable(resources, bitmap)
   ```

2. `Drwable` -> `Bitmap`

   ```kotlin
   val drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.takagi))
               drawable.setBounds(200, 200, 500 ,500)
   val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
   val canvas = Canvas(bitmap)
   drawable.draw(canvas)
   ```

3. View -> Bitmap

   ```kotlin
   val bitmap = Bitmap.createBitmap(textView?.width ?: 100, textView?.height ?: 100, Bitmap.Config.ARGB_8888)
   val canvas = Canvas(bitmap)
   textView?.draw(canvas)
   ```


***

## Canvas

### dp -> pix

```kotlin
fun dp2px(context: Context, dp: Int): Float =
	TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics)
```

### draw圆流程

1. 创建一个`Paint()`
2. 调用`canvas?.drawCircle(point, point, radius, paint)`方法

***

## CBAnimation

### xml写动画和插值器

* 动画

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <translate
      xmlns:android="http://schemas.android.com/apk/res/android"
      android:fromYDelta="0"
      android:toYDelta="100"
      android:duration="1000"
      android:interpolator="@anim/cycler" />
  ```

* 插值器

  ```xml
  <?xml version="1.0"?>
  <cycleInterpolator
      xmlns:android="http://schemas.android.com/apk/res/android"
      android:cycles="5"/>
  ```

* 以上动画的效果是 -> 在y方向上执行5此平移，达到上下shake的视觉效果

### View#invalidate()

*  可以理解成作用是`强制重绘，调用draw()`

### View执行动画接口

* `public void startAnimation(Animation animation)`
* 最终动画执行在`View#applyLegacyAnimation()`方法内

### 插值器

1. `LinearInterpolator`
2. `AccelerateInterpolator`
3. `DecelerateInterpolator`
4. `CycleInterpolator`
5. `AnticipateInterpolator()` - starts backward then flings forward
6. `OvershootInterpolator()` - flings forward and overshoots the last value then comes back

### 动画与onDraw()方法的关系

* Animations in Android are a way to change the state of a View over time, typically by animating specific properties such as position, size, or opacity.
* When an animation is running, the View's drawing method is called on each frame of the animation to update the View's appearance based on the current state of the animation. 
* However, even when an animation is not running, the View's drawing method may still be called for other reasons, such as when the View is invalidated or when the system needs to redraw the View for some other reason.

***

## CBApplicationSingleton

### 自定义Application

* 继承`Application()`
* 在`AndroidManifest.xml`中修改标签

***

## CBBatteryBroadcast

### 接收广播

1. 定义一个`BroadcastReceiver()`
2. `context`中`registerReceiver`

***

## CBBootReceiver

### 接收广播

1. 定义一个`BroadcastReceiver()`
2. 在`AndroidManifest.xml`中增加一个`<receiver>`标签

***

## CBDrawableAnimate

### `AnimationDrawable`使用

1. 定义一个`xml`

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <animation-list>
       <item android:drawable="@drawable/red" android:duration="2000" />
       <item android:drawable="@drawable/blue" android:duration="2000" />
       <item android:drawable="@drawable/green" android:duration="2000" />
   </animation-list>
   ```

2. 将`xml`赋值给`View.background`

3. 将`background`转成`AnimationDrawable`，然后调用`start()`方法开始动画

***

## CBElevation

### 是什么

* 是`View`三维的一个度量

### 注意事项

* 必须要有`background`的前提下设置了`elevation`才有用

### 高度就是高度

* `FrameLayout`中栈底的`View`会因为有`elevation`值而到栈顶

***

## CBGestureDetector

### GestureDetector

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

### 手势识别

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

### 单双击隔离

* 看源代码`DoubleClickListener`，核心思想还是先`postDelay()`然后再移除

### ViewFlipper

* 是什么 -> 一个`FrameLayout`，最多只能显示一个`child`
* 核心api
  1. `public void setDisplayedChild(int whichChild)`
  2. `public void setInAnimation(Animation inAnimation)`
  3. `public void setOutAnimation(Animation outAnimation)`

***

## CBGLSurfaceView

* 详见`Video`

***

## CBHaptic

### 如何触发震动

1. 在`AndroidManifest.xml`中声明需要震动权限

2. 使用`getSystemSerivce()`

   ```kotlin
   (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createOneShot(300L, 100))
   ```

***

## CBPinch

### matrix

* 是什么

  *  一个`3*3`的矩阵
  * `scaletype`的一种类型

* 模型

  ```bash
  scaleX, skewX, translateX
  skewY, scaleY, translateY
  0, 0, 1
  ```

* 能实现的变换

  1. scale
  2. translate
  3. skew
  4. rotate

* api

  * `public void set(Matrix src)`
  * `public boolean postTranslate(float dx, float dy)`
  * `public boolean postScale(float sx, float sy, float px, float py)`

* 延伸

  * 其他的7种`scaletype`，源码都是通过设置`matrix`来实现的

### 多点触控

* 多指相关Action
  * `ACTION_POINTER_DOWN` -> A non-primary pointer has gone down
  * `ACTION_POINTER_UP` -> A non-primary pointer has gone up
* 一个`MotionEvent`包含多个手指，可以通过传入`pointerIndex`获取
  * `public final float getX(int pointerIndex)`

***

## CBRotateSave

### onSaveInstanceState

1. 只有在意外退出时才会调用此方法，用户强意愿的主动退出不会调用此方法
2. 在`override fun onSaveInstanceState(outState: Bundle)`中写值，在`override fun onCreate(savedInstanceState: Bundle?)`中取值

***

## CBSendEmailAttachments

### 发送邮件并携带附件

```kotlin
val intent = Intent(Intent.ACTION_SEND).apply {
  putExtra(Intent.EXTRA_SUBJECT, "Intent.EXTRA_SUBJECT")
  putExtra(Intent.EXTRA_EMAIL, arrayOf("izumisakai-zy@outlook.com", "izumisakai.zy@gmail.com"))
  putExtra(Intent.EXTRA_TEXT, "Intent.EXTRA_TEXT")
  //把它注释掉，因为'path' 路径会crash
  //putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File("path")))
  type = " text/plain"
}

startActivity(intent)
```

***

## CBSerivce

### 自定义一个service

1. 继承`Service()`
2. 核心实现方法
   * `public abstract IBinder onBind(Intent intent)`
   * `public @StartResult int onStartCommand()`
   * `public boolean onUnbind(Intent intent)`
3. `context`中`start`和`stop`
   * `public @Nullable ComponentName startService(Intent service)`
   * `public boolean stopService(Intent name)`

***

## StrictMode

### 作用

* 为了更方便的检测出`ANR`

### 使用

* `StrictMode.enableDefaults()`

***

## CBTextWatcher

### TextChangedListener

* 谁的接口 -> `TextView`

* 长啥样

  ```kotlin
  public interface TextWatcher extends NoCopySpan {
      public void beforeTextChanged(CharSequence s, int start, int count, int after);
      public void onTextChanged(CharSequence s, int start, int before, int count);
      public void afterTextChanged(Editable s);
  }
  ```

* 如何加

  * `public void addTextChangedListener(TextWatcher watcher)`

***

## CBWebView

### WebView

* 如何使用 -> `MainActivity#setContentView(WebView(this))`
* 接口
  * `public void addJavascriptInterface(Object obj, String interfaceName) {}`
  * `public void loadUrl(String url)`

### jsb

* 写一个jsb

  ```kotlin
  class WebInterface(private val mContext: Context) {
  
      @JavascriptInterface
      fun showToast(toast: String) {
          Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
      }
  }
  ```

* 注入WebView

  * `WebView#addJavascriptInterface(interface)`

* html中使用

  ```html
  <script type="text/javascript">
    <!--JNI方法处-->
    function showAndroidToast(toast) {
      WebInterface.showToast(toast);
    }
  </script>
  ```

### Assets

* 位置 -> 在`main`下，和`java`同级
* 注意事项 -> `file:///android_asset/filename.txt`的方式只能访问`assets`下面的资源

***

## CBWidget

### 基本原理

* 将`WidgetProvider`注册为一个`BroadCastReciver`，接收各种广播通知尤其是`android.appwidget.action.APPWIDGET_UPDATE`
* 在初始化或者`update`的时候设置好各个`View`的点击事件，不过要通过`RemoteView`的`api`来设置，和传统的`View#setOnClickListener`有点不同

### 使用过程 

* [Create a simple widget](https://developer.android.com/develop/ui/views/appwidgets)

***

## Compose

* 不需要笔记，直接看代码吧

***

## ConstrainLayout

### bias

* 计算公式: `bias = layout_constraintTop_toTopOf / (layout_constraintBottom_toBottomOf + layout_constraintBottom_toBottomOf)`
* 默认值是0.5，即此时`layout_constraintTop_toTopOf` = `layout_constraintBottom_toBottomOf`

### chainStyle

* 设置在什么地方: on the first element of a chain
* behavior
  1. spread - 默认值，全部平分
  2. spread_inside - 第一个`layout_constraintTop_toTopOf`和最后一个`layout_constraintBottom_toBottomOf` 为零，其余平分
  3. packed - 第一个`layout_constraintTop_toTopOf`和最后一个`layout_constraintBottom_toBottomOf` 平均，其余为零

***

## Coroutines

* 代码啥都没有，先放这儿吧

## EventBus

### [笔记链接](https://github.com/IzumiSakai-zy/various-kinds-learning/blob/master/daily-android.md)

## Fresco

### [笔记链接](https://github.com/IzumiSakai-zy/various-kinds-learning/blob/master/daily-android.md)

## FrescoMask

### url -> 上屏粗流程

1. fresco进行网络请求
2. 将网络请求的`jpg`图片封装成位图`BitMap`
3. `BitMap`封装成`BitMapDrawable`
4. `BitMapDrawable`在屏幕上显示出来

### Process

* 是什么 -> fresco提供的一个API
* 作用 -> 在上述过程的 2~3之间提供一个hook，对`BitMap`做一些自定义的处理

### FILTER_BITMAP_FLAG

* 是什么 -> `Paint`的一个常量
* 作用 -> 当对`BitMap`进行缩放时，决定像素的采样。采用双线性采样或最邻近采样

***

## LifeCycleOwner

### [笔记链接](https://github.com/IzumiSakai-zy/various-kinds-learning/blob/master/daily-android.md)

***

## LiveData

### [笔记链接](https://github.com/IzumiSakai-zy/various-kinds-learning/blob/master/daily-android.md)

***

## MeasureLayoutDraw

### purpose

* `measure`：determine the size requirements of a view before it is drawn on the screen. 

* `layout`：position the view on the screen.

* `draw`：draw the view on the screen.

### core method

* `void setMeasuredDimension(int measuredWidth, int measuredHeight)`
  *  This method must be called by onMeasure(int, int) to store the measured width and measured height.
* `boolean setFrame(int left, int top, int right, int bottom)` 
  * Assign a size and position to this view.
  *  This is called from layout.
* `convas?.drawxxx()`

### recursive？

* measure - yes

  ```java
  //RelativeLayout#onMeasure()
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int count = views.length;
    for (int i = 0; i < count; i++) {
      final View child = views[i];
      if (child.getVisibility() != GONE) {
        // 先遍历所有children，依次调用measure完每一个child
        measureChild(child, params, myWidth, myHeight);
      }
    }
    //最后设置自己measure的宽高结果
    setMeasuredDimension(width, height);
  }
  ```

* layout - no

  ```java
  //View#layout()
  public void layout(int l, int t, int r, int b) {
    //已经调用setFrame()把自己放好了
    boolean changed = isLayoutModeOptical(mParent) ? setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);
    
    if (changed || condition.isOk()) {
      // 然后再尝试layout children
      onLayout(changed, l, t, r, b);
    }
  }
  ```

* draw - no

  ```java
  //View#draw()
  public void draw(Canvas canvas) {
    // Step 3, draw the content. 把自己draw完了
    onDraw(canvas);
    // Step 4, draw the children. 然后再draw children
    dispatchDraw(canvas);
  }
  ```

### MeasureSpec

1. 结构

   1. 前2位表示mode，后30位表示实际大小

2. 三种mode

   * `UNSPECIFIED`：父对子无任何限制，要多大给多大。一般不用

   * `EXACTLY`：父已经检测出了子的精确大小，就给那么大。对应`dp、match_parent`

   * `AT_MOST`：父指定了一个最大值，最大不能超过这个值。对应`wrap_content`

3. 如何获得

   * 公式 -> `子MeasureSpec = 父MeasureSpec + 子LayoutParams`

   * 代码实现 ->`ViewGroup#measureChild(View, int, int)`、 `ViewGoup#getChildMeasureSpec(int, int ,int)`

     ```java
     protected void measureChild(View child, int parentWidthMeasureSpec,
                 int parentHeightMeasureSpec) {
       // 获取子View的LayoutPrarams
       final LayoutParams lp = child.getLayoutParams();
     
       // 通过父View的MeasureSpec + 子View的LayoutPrarams获取子View的MeasureSpec
       final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,mPaddingLeft + mPaddingRight, lp.width);
       final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,mPaddingTop + mPaddingBottom, lp.height);
     
       // 调用子View的measure()进行测量
       child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
     }
     ```

   * 图示: 横轴为parent，众轴为child

     |              | EXACTLY | AT_MOST |
     | ------------ | ------- | ------- |
     | dp           | EXACTLY | EXACTLY |
     | match_parent | EXACTLY | AT_MOST |
     | wrap_content | AT_MOST | AT_MOST |

### measure相关方法

* `View#measure()`

  * 签名 -> `public final void measure(int widthMeasureSpec, int heightMeasureSpec)`

  * 特点 -> `final`不可被重写，此方法内会调用`onMeasure`

  * 源码

    ```java
    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
      // 调用onMeasure()
      onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    ```

* `View#onMeasure()`

  * 签名 -> `protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)`

  * 作用 -> 确定这个View的测量宽和测量高

  * 特点 -> `View`中有一个基础的默认实现，但子`View`和子`ViewGroup`都会进行复写

  * 源码

    ```java
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      //默认的实现，直接获取默认宽高调用setMeasuredDimension()
      setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                           getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
    ```

* `ViewGroup#measureChild()`

  * 作用 -> 构造子View的MeasureSpec，调用`child.measure()`

  * 源码

    ```java
    protected void measureChild(View child, int parentWidthMeasureSpec,
                int parentHeightMeasureSpec) {
      // 获取子View的LayoutPrarams
      final LayoutParams lp = child.getLayoutParams();
    
      // 通过父View的MeasureSpec + 子View的LayoutPrarams获取子View的MeasureSpec
      final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,mPaddingLeft + mPaddingRight, lp.width);
      final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,mPaddingTop + mPaddingBottom, lp.height);
    
      // 调用子View的measure()进行测量
      child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
    ```

* `ViewGroup#measureChildren()`

  * 作用 -> 遍历children并调用`measureChild(child, xx, xx)`

  * 源码

    ```java
    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
      final int size = mChildrenCount;
      final View[] children = mChildren;
      for (int i = 0; i < size; ++i) {
        final View child = children[i];
        if ((child.mViewFlags & VISIBILITY_MASK) != GONE) {
          // 循环遍历measure每一个child
          measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
      }
    }
    ```

### measure流程

1. 叶子节点`View`

   * `View#measure()` -> `View#onMeasure()`来确定自己的宽高
   * `View#onMeasure()`的默认实现 -> `View#getDefaultSize()`
   * 现状 -> 除了`View`类外根本不会有其他子类使用，子类都是根据自身的内容来决定自己的宽高，而不是仅仅根据一个`MeasureSpec`
2. 非叶子节点`ViewGroup`

   * `View#measure()` -> `XXXLayout#onMeasure()`调用来确定自己的高
   * 每个`XXXLayout`会重写`View#onMeasure()`，一般的业务逻辑是创建两个临时变量来记录自己宽高，遍历测量所有子View的宽高，每测量一个子View就更新临时变量值(比如LinearLayout的高由各个View累加)
   * `ViewGroup#measureChildren、ViewGroup#measureChild`一般没人用。这两个方法的逻辑都包含在`XXXLayout#onMeasure()`内

### self defined view wrap_content ?

* `View#onMeasure()`获取宽高默认的实现

  ```java
  //View#getDefaultSize
  public static int getDefaultSize(int size, int measureSpec) {
    //size是getSuggestedMinimumWidth()，返回mMinWidth或mBackground.getMinimumWidth()
    int result = size;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);
  
    switch (specMode) {
      case MeasureSpec.UNSPECIFIED:
        result = size;
        break;
      case MeasureSpec.AT_MOST:
      case MeasureSpec.EXACTLY:
        result = specSize;
        break;
    }
    return result;
  }
  ```

* 当`specMode`是`EXACTILY`时，`result = specSize`是无问题的；

* 当`specMode`是`AT_MOST`，此`View`设置为`match_parent`时无问题；当`specMode`是`AT_MOST`，此`View`设置为`wrap_content`就有大问题。因为它的实际效果和`match_parent`一样了，因此自定义View必须重写`onMeasure`，否则不支持`wrap_content`

### layout相关方法

1. `View#layout()`

   * 签名 -> `public void layout(int l, int t, int r, int b)`

   * 作用 -> 确定`View`本身的位置

   * 工作 -> 调用`setFrame()`方法来确定4个点的位置(相对于父容器)；如果布局有改变，则调用`View#onLayout()`方法来递归布局children

   * 源码

     ```java
     public void layout(int l, int t, int r, int b) {
       if (changed || conditon.isOk) {
         onLayout(changed, l, t, r, b);
       }
     }
     ```

2. `View#onLayout()`

   * 签名 -> `protected void onLayout(boolean changed, int left, int top, int right, int bottom)`

   * 特点 -> 在`View`中是个空方法，需要子`View`和`ViewGroup`去自己实现

   * 源码

     ```java
     protected void onLayout(boolean changed, int left, int top, int right, int bottom) { 
       //空实现 
     }
     ```

3. `ViewGroup#layout()`

   * 什么都没做，就是调用父类`View#layout()`

   * 源码

     ```java
     public final void layout(int l, int t, int r, int b) {
       super.layout(l, t, r, b)
     }
     ```

4. `ViewGroup#onLayout()`

   * 必须让子类重写

     ```java
     protected abstract void onLayout(boolean changed, int l, int t, int r, int b);
     ```

### layout流程

1. 叶子节点
   * `View#layout()` 把自己放好
2. 非叶子节点
   * `ViewGroup#layout()`将自己放好，会调用到`XXXLayout#onLayout()`
   * `XXXLayout#onLayout()` 将children放好

### draw相关

1. `View.draw()`

   * 这个方法一般不会`override`，一般只会重写`onDraw()`

   * 四个步骤

     * Step 1, draw the background, if need -  `(drawBackground(canvas))`
     * Step 3, draw the content - (`onDraw(canvas)`)
     * Step 4, draw the children - (`dispatchDraw(canvas)`)
     * Step 6, draw decorations (foreground, scrollbars) - (`onDrawForeground(canvas)`)

   * 实现

     ```java
     public void draw(Canvas canvas) {
       // Step 1, draw the background, if needed
       drawBackground(canvas);
       // Step 3, draw the content
       onDraw(canvas);
       // Step 4, draw the children
       dispatchDraw(canvas);
       // Step 6, draw decorations (foreground, scrollbars)
       onDrawForeground(canvas);
       // Step 7, draw the default focus highlight
       drawDefaultFocusHighlight(canvas);
     }
     ```

2. `View#onDraw()`

   * 特点 ->  在`View`中是一个空方法，需要不同的`View`和`ViewGroup`来自己实现

   * 作用 -> 负责如何`draw`自己

3. `View#dispatchDraw()`

   * 特点 -> 在`View`中是一个空方法，需要不同的`ViewGroup`来自己实现

4. `ViewGroup#drawChild`

   * 作用 -> 调用`child.draw()`

### draw()流程

1. 叶子节点
   * 直接调用`draw()`方法，通过`draw()`方法调`onDraw()`把自己draw出来
2. 非叶子节点
   1. 直接调用`draw()`方法，先通过`draw()`方法调`onDraw()`把自己draw出来，接着调用`dispatchDraw()`，复写的`dispatchDraw`会通过`drawChild()`最后调用到`child.draw()`把children给draw出来

### onXXX()与XXX()方法

* `onMeasure()、onLayout()、onDraw()` -> 代表这个View如何实现`measure、layout、draw`
* `measure()、layout()、draw()` -> 手动强项执行(或者系统某处需调用)`measure、layout、draw`
* 执行`draw()`前请确保`measure()和layout()`执行过

### where to start

* `ViewRootImpl#performTraversals()`

  ```java
  private void performTraversals() {
    // do measure
    measureHierarchy(host, lp, mView.getContext().getResources(),
                      desiredWindowWidth, desiredWindowHeight);
    // do layout
    performLayout(lp, mWidth, mHeight);
    
    // do draw
    if (!performDraw())
  }
  ```

### View#invalidate()

* 标记为`PFLAG_INVALIDATED`和`~PFLAG_DRAWING_CACHE_VALID`

  ```java
  void invalidateInternal() {
    // 置换标记位
    mPrivateFlags |= PFLAG_INVALIDATED;
  	mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
  }
  ```

* 下一次系统自动执行`ViewRootImpl#performTraversals()`时，会通过变量控制，达到只执行`performDraw()`而不执行`measureHierarchy()、performLayout()`的效果

### View#requestLayout()

* 标记为`PFLAG_FORCE_LAYOUT`和`PFLAG_INVALIDATED`。向上递归

  ```java
  public void requestLayout() {
    if (viewRoot != null && viewRoot.isInLayout()) {
      if (!viewRoot.requestLayoutDuringLayout(this)) {
        return; // 防止多次requestLayout
      }
    }
  	// 置标记位
    mPrivateFlags |= PFLAG_FORCE_LAYOUT;
    mPrivateFlags |= PFLAG_INVALIDATED;
    
    if (mParent != null && !mParent.isLayoutRequested()) {
      //递归向上调用做标记
      mParent.requestLayout();
    }
  }
  ```

* the view's requestLayout method almost does nothing except puts the flag to be 'invalidated', and the system will automatically handle it.

* 下一次系统自动执行`ViewRootImpl#performTraversals()`时，`measureHierarchy()、performLayout()、performDraw()`全都会执行

***

## MotionEventDispatch

### Motion相关

* `getX()、getY()`：触摸位置相对于当前View的位置
* `getRawX()、getRawY()`：触摸位置相对于物理屏幕左上角的位置
* `TouchSlop`：被认为是滑动的最小距离。单位是`dp`
* `VelocityTracker`：滑动速度相关
* `GestureDetector`：手势检测相关

### 事件机制三方法

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
   //ViewGroup版本
   fun dispatchTouchEvent(val ev:MotionEvent){
     var consume = false
     if (allowIntercept() && onInterceptTouchEvent(ev)) {
       consume = onTouchEvent(ev)
     } else {
       comsume = child.dispatchTouchEvent(ev)
     }
     return consume;
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

### 一些结论

* 事件序列 - 从`ACTION_DOWN`开始，到`ACTION_UP`结束，中间有很多的`ACTION_MOVE`
* 一个事件序列正常情况下只能被一个`View`拦截并消耗
* `View`如果要处理事件，就必修消耗`ACTION_DOWN`事件，否则就向上抛；`View`一旦消耗`ACTION_DOWN`事件，即消耗整个事件
* `ViewGroup`默认不拦截任何事件，它的`onInterceptTouchEvent`方法默认返回false

### 分发流程

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

### 分发机制实现

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

### 手势识别

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

***

## NegativeMargin

### margin

* `margin`的值可以为负，为负表示画在父View的外部

### clipChildren

* `Defines whether a child is limited to draw inside of its bounds or not.`

***

## ScrollView

### 内容

* `ScrollView#scrollTo()`和`ScrollView#scrollBy()`
* `scrollX、scrollY`

### View#mScrollY

* 计算公式: 屏幕top边缘的纵坐标(一般是0) - view的top边缘的纵坐标(一般为负数)
* 所以手向上滑动，下面的内容展示出来，这时的`mScrollY`是正数且越来越大

### View#scrollTo(int, int)

* 当一个View设置`mScrollX/mScrollY`后，会对`children`重新进行`draw`但不会重新进行`measure、layout`
* 这个View本身的宽高、布局、位置全部都不变，只有`children`才会变
* `scroll()`不改变View本身的大小，不改变本身的`backgroundDrawable`，它唯一改变的是`children`的位置，且不重新测量和布局
* 设置后重新调用`requestLayout`会使`mScrollX/mScrollY`失效

***

## SelfDefineView

### xml自定义属性

### 自定义View支持wrap_content

### 如何画一个圆

***

## ShadowBackground

### svg

* 是一种图片的存储格式，和`jpg、png`一样
* 存储的是点与线的数学关系，而不是位图

### android矢量图

* 矢量图可以理解成是一个接口，而Android的`xml drawable`是对这个接口的实现

### 灰度渐变矢量图如何画

***

## ShareElement

* 代码有点复杂，自己看代码吧

***

## Video

### Surface

* `Handle onto a raw buffer that is being managed by the screen compositor.`
* 继承了`Parcelable`，因此可以跨进程通信，在`WMS`中传递
* 它持有了`NativeBuffer`的指针，这个`NativieBuffer`指的是用来保存当前窗口屏幕数据的一个`buffer`
* `ViewRootImpl`持有了这个对象，即一颗`view tree`，一个`window`，共享一个`Surface`

### SurfaceHolder

* 充当`MVC`模式中的`C`，`Surface`是`M`，`SurfaceView`是`V`

### SurfaceView

* 太复杂了，看不懂，先贴个链接 -> [Graphics architecture](https://source.android.com/docs/core/graphics/architecture)
* `Provides a dedicated drawing surface embedded inside of a view hierarchy.`
* 是`View`的子类
* 不与`window`共享`surface`，而是自己持有一个`surface`
* 为了解决与`window#surface`的重叠问题，`SurfaceView`是在`Z轴`的底部，通过让`window#surface`设置为透明而显示出来
* `surface`绘制的线程可以自己定，可以不是主线程

### TextureView

* 继承自`View`，它的表现就像一个普通的`View`一样
* 它没有自己的`Surface`，而是共享`ViewRootImpl`的`Surface`
* 由于没有自己的`Surface`，它的理论性能比`SurfaceView`低
* 显示的内容通过`SurfaceTexture`传递

### SurfaceTexture

* `Captures frames from an image stream as an OpenGL ES texture.`
* 可以把`Surface`生成的图像流，转换为纹理`Texture`，供业务方进一步加工使用

***

## ViewCoordinate

### 坐标

* `left, top, right, bottom, elevation` - The distance in pixels from the xxx edge of this view's parent
* `translateX, translateY, translateZ` - The x/y/z location of this view relative to its left/top/elevation position
* `x/y/z = mLeft/mTop/elevation + translationX/translationY/translationZ` - The visual x/y/z position of this view, in pixels

### translate value

* 设置`translate`后只重新`draw`，不会重新进行`measure, layout`
* 是一种低代价改变`View`位置的参数，因为不用`measure、layout`，常用来做动画
* 设置后重新调用`requestLayout`依然不会使`translate`失效

***

## ViewStub

### xml写法

```xml
<ViewStub
    android:id="@+id/stub_view"
    android:inflatedId="@+id/fl_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 代码写法

```kotlin
class MainActivity : AppCompatActivity() {
  
    private var viewStub: ViewStub? = null
    private var flContainer: ViewGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
      	//通过android:id找到ViewStub
        viewStub = findViewById(R.id.stub_view)
        viewStub?.apply {
          	//给layoutResource赋值
            layoutResource = R.layout.stub_view_layout
          	//执行inflate()
            inflate()
        }
      	//ViewStub已经被替换，直接通过inflatedId即可找到View
        flContainer = findViewById(R.id.fl_container)
    }
}
```

***

## ViewTreeObserver

### 类关系

* View.java

  ```java
  // 每个View都有一个获取ViewTreeObserver的接口，且都是从mAttachInfo获取
  public ViewTreeObserver getViewTreeObserver() {
      if (mAttachInfo != null) {
          return mAttachInfo.mTreeObserver;
      }
      return mFloatingTreeObserver;
  }
  
  void dispatchAttachedToWindow(AttachInfo info, int visibility) {
    	// View attach的时候赋值
      mAttachInfo = info;
  }
  
  // 是一个静态类
  final static class AttachInfo {
   	IWindow mWindow;
    IBinder mWindowToken;
    WindowId mWindowId;
    View mRootView;
    ViewTreeObserver mTreeObserver; //持有ViewTreeObserver
    AttachInfo() {
      // 构造函数时new一个
      mTreeObserver = new ViewTreeObserver(context);
    }
  }
  ```

* ViewRootImpl.java

  ```java
  // 持有AttachInfo
  final View.AttachInfo mAttachInfo;
  
  public ViewRootImpl() {
    // 构造函数时赋值
    mAttachInfo = new View.AttachInfo()
  }
  
  private void performTraversals() {
    // WindowAttachedChange回调
    mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true);
    
    if (didLayout) {
      // 先做layout
      performLayout(lp, mWidth, mHeight);
      // layout结束后调GlobalLayout回调
      mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
    }
    
    // 先调OnPreDraw，且记录是否cancel
    boolean cancelAndRedraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw();
    if (cancelAndRedraw) {
      // Try again. 直接重试整个traversal，即重measure、layout、draw
      scheduleTraversals();
    } else {
      // 未取消才真正的draw
      performDraw()
    }
  }
  ```

### ViewOnGlobalLayout

* 触发时机: layout结束后，preDraw之前
* 用处，可以在这时候获取View的宽高

### ViewOnPreDraw

* 触发时机: layout结束后，draw之前
* 返回值: bool表示是否取消这次draw
* 用处
  1. 获取View的宽高
  2. 取消draw重走`traversal`，这样UI就不会跳变

### ViewOnDraw

* 触发时机: draw之后
* 用户: 暂时没有想到

***
