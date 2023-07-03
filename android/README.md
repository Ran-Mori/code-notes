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

## Binder

### 好文链接

* [Android框架分析系列](https://mr-cao.gitbooks.io/android/content/)

### aidl

* 是什么: 它只是一个简单的工具，因为直接写一个类过于复杂，`aidl`等于是一个中间过程，我们只需在`aidl`中写一个简单的interface，它就会自动为我们生成一个十分复杂的类

* 简单的interface

  ```java
  interface ICalculator {
    	void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
              double aDouble, String aString);
    	int add(int a, int b)
  }
  ```

* 复杂的类 - 内容非常多，一共200多行

  ```java
  public interface ICalculator extends android.os.IInterface {}
  ```

### 几者关系

* `binder` - It is the core mechanism of the Android IPC system. It allows different processes to communicate with each other by passing messages.
* `IBinder` - It is the interface for the `Binder` object. It defines the methods that can be used to communicate with a remote process.
* `Service` - It is a component in the Android system that runs in the background and provides a set of APIs that can be accessed by other processes.
* `client` - It is the process that uses a service provided by another process.
* `server` - It is the process that provides a service that can be used by other processes.
* `Stub` - It is an object that resides in the process that provides a service, and which can be called by other processes. When a remote process wants to access a service implemented by another process, it sends a request to the stub object. The stub then forwards the request to the actual implementation of the service in the local process.
* `Proxy` - It is an object that resides in the process that needs to access a remote service, and which acts as a surrogate for the stub object in the remote process. The proxy object provides a local API that looks as if it is calling the remote service directly. When a call is made on the proxy object, it is actually sent to the stub object in the remote process.

### Client与Server

* Client
  1. Client需要知道Server端Service的全类名
  2. 知道全类名后，Client直接通过给Intent传入全类名，然后通过`ServiceManager`找到这个Service
  3. bind上Service后，回返回一个`IBinder`，这实际就是远端的实现
* Server
  1. Server侧要将Service启动起来，供Client查询
  2. Server被绑定时，要将实现的`Binder`返回给客户端
* Client调Server
  * Client实际是通过`Proxy`发起的调用，`Proxy`将这个请求转给`Stub.transact()`进行远程调用

### 如何使用

1. 有一个复杂的类`ICalculator.java`

   ```java
   //继承了IInterface，因此需要实现`asBinder`方法
   public interface ICalculator extends android.os.IInterface {
     
     // 默认实现，不用怎么管
     // 一共三个方法，其中basicTypes()和add()都是在aidl文件中定义的方法，而asBinder()是android.os.IInterface中的方法
     public static class Default implements ICalculator {}
     
     // Local-side IPC implementation stub class.
     public static abstract class Stub extends android.os.Binder implements ICalculator {
       // 实现asBinder()，onTransact()
       
       // 将Stub转换成Proxy
       public static ICalculator asInterface(android.os.IBinder obj) {
       
       // 提供一个java方式的调用
       private static class Proxy implements ICalculator {}
     }
   }
   ```

2. Server端实现一个Service

   ```java
   public class CalculatorService extends Service {
   
     private CalculatorImpl mBinder = new CalculatorImpl();
   
     // 返回真正的实现给客户端
     @Override
     public IBinder onBind(Intent intent) { return mBinder; }
   }
   ```

3. Client端bindService()来获取`IBinder`

   ```java
   private ICalculator mCalculator;
   
   private ServiceConnection mConnection = new ServiceConnection() {
     @Override
     public void onServiceConnected(ComponentName name, IBinder service) {
       // 绑定成功时进行赋值
       mCalculator = ICalculator.Stub.asInterface(service);
     }
   };
   ```

4. 一定要记得在`AndroidManifest.xml`中进行注册

   ```xml
   <service
       android:name="com.binder.service.CalculatorService"
       android:enabled="true"
       android:exported="true" />
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

   ```kotlin
   // 写一个Receiver
   private val broadReceiver = object : BroadcastReceiver() {
     override fun onReceive(context: Context?, intent: Intent?) {
       val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
     }
   }
   
   override fun onCreate(savedInstanceState: Bundle?) {
     super.onCreate(savedInstanceState)
     setContentView(R.layout.activity_main)
     // 注册一下监听，声明只监听Intent.ACTION_BATTERY_CHANGED
     registerReceiver(broadReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
   }
   ```

   

***

## CBCompatibility

### compatibility layer

* what it does? - it implements the newer features using the APIs available on the older version. 

* advantages - This allows developers to use the latest features in their apps without having to worry about whether or not the app will work on older devices.

* features

  * The compatibility layer for Android is implemented by the AndroidX libraries themselves, rather than by Android OS or individual applications.
  * The compatibility code in the AndroidX libraries/Android support libraries often uses if statements and other conditional logic to provide different implementations of the same functionality depending on the version of Android that's running the app.

* example

  ```java
  public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
      if (Build.VERSION.SDK_INT >= 23) {
          activity.requestPermissions(permissions, requestCode);
      } else {
          // Code to handle permissions for older versions of Android
      }
  }
  ```

* history

  * The compatibility layer is implemented as a library, it is not a part of OS. 
  * The library that is used to implement the compatibility layer is called the **Android Support Library**. But now it's been replaced by **AndroidX Library**
  * before 2018, you need to add `implementation 'com.android.support:appcompat-v7:28.0.0'`
  * but now, you just need to add `implementation 'androidx.appcompat:appcompat:1.3.0'`

### what include

* AppCompat, Design, RecyclerView, CardView, PercentLayout

  ```xml
  androidx.appcompat:appcompat:1.3.0
  androidx.design:design:1.0.0
  androidx.recyclerview:recyclerview:1.2.0
  androidx.cardview:cardview:1.0.0
  androidx.percentlayout:percentlayout:1.0.0
  ```

***

## CBBootReceiver

### 接收广播

1. 定义一个`BroadcastReceiver()`

2. 在`AndroidManifest.xml`中增加一个`<receiver>`标签

   ```xml
   <receiver android:name="com.cbbootreceiver.MyBootReceiver"
     android:exported="true">
     <intent-filter>
       <!--当收到BOOT_COMPLETED通知时，就会执行Receiver的onReceive()方法-->
       <action android:name="android.intent.action.BOOT_COMPLETED" />
     </intent-filter>
   </receiver>
   ```

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

1. 写一个Service类，继承`Service()`

2. 在`AndroidManifest.xml`中进行声明

   ```xml
   <service
       android:name=".TrackService"
       android:enabled="true"
       android:exported="true" />
   ```

3. 核心实现方法
   * `public abstract IBinder onBind(Intent intent)`
   * `public @StartResult int onStartCommand()`
   * `public boolean onUnbind(Intent intent)`

4. 在`context`中`startService(Intent service)`和`stopService(Intent name)`

   * `public @Nullable ComponentName startService(Intent service)`
   * `public boolean stopService(Intent name)`

***

## CBSharedPreferences

### 实现原理

1. `ContextImpl.java` 创建文件，将文件转换成`SharedPreferencesImpl.java`

   ```java
   public SharedPreferences getSharedPreferences(String name, int mode) {
     File file;
     synchronized (ContextImpl.class) { // 防多线程，上锁
       if (mSharedPrefsPaths == null) {
         mSharedPrefsPaths = new ArrayMap<>(); // 内存友好二分查找的ArrayMap
       }
       file = mSharedPrefsPaths.get(name);
       if (file == null) { // 典型的加缓存思路
         file = makeFilename(getPreferencesDir(), name + ".xml"); // 核心的新建逻辑
         mSharedPrefsPaths.put(name, file);
       }
     }
     return getSharedPreferences(file, mode); // 真正的获取
   }
   
   private File getPreferencesDir() { // 获取此应用SP的路径
       synchronized (mSync) {
         if (mPreferencesDir == null) {
           mPreferencesDir = new File(getDataDir(), "shared_prefs");
         }
         return ensurePrivateDirExists(f(mPreferencesDir, 0771, -1, null); // 读取文件
       }
   }
   
   public File getDataDir() { // dataPath是与包名相关联的路径
     if (isCredentialProtectedStorage()) {
       res = mPackageInfo.getCredentialProtectedDataDirFile();
     } else if (isDeviceProtectedStorage()) {
       res = mPackageInfo.getDeviceProtectedDataDirFile();
     } else {
       res = mPackageInfo.getDataDirFile();
     }
   }
                                       
   static File ensurePrivateDirExists(File file, int mode, int gid, String xattr) {
     try {
       Os.mkdir(path, mode); // 系统调用创文件
       Os.chmod(path, mode); // 系统调用改mode为0771
       if (gid != -1) {
           Os.chown(path, -1, gid);
       }
     }
   }
   
   public SharedPreferences getSharedPreferences(File file, int mode) {
     SharedPreferencesImpl sp;
     synchronized (ContextImpl.class) {
       sp = new SharedPreferencesImpl(file, mode); // 真正的实现类是SharedPreferencesImpl
     }
   }
   ```

2. `SharedPreferencesImpl.java`

   ```java
   SharedPreferencesImpl(File file, int mode) {
     synchronized (mLock) { mLoaded = false; }
     new Thread("SharedPreferencesImpl-load") {
       public void run() { loadFromDisk(); } // 直接新开一个线程去读xml文件到内存
     }.start();
   }
   ```

***

## CBStrictMode

### 是什么

* StrictMode is a developer tool which detects things you might be doing by accident and brings them to your attention so you can fix them.

### 作用

* 为了更方便的检测出`ANR`

### 使用

* `StrictMode.enableDefaults()`

### 示例

* `SharedPreferencesImpl.java`

  ```java
  private void awaitLoadedLocked() {
      if (!mLoaded) {
        // Raise an explicit StrictMode onReadFromDisk for this
        // thread, since the real read will be in a different
        // thread and otherwise ignored by StrictMode.
        // 从磁盘读内容时触发一下StrictMode
        BlockGuard.getThreadPolicy().onReadFromDisk();
      }
  }
  ```

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

* what's for? -> to store raw asset files that will be used by your app, such as fonts, HTML files, JavaScript files, styling files, database files, config files, sound files, and graphic files.

* how to use? -> use apis of `AssetManager.java`

  ```java
  AssetManager assetManager = getAssets();
  InputStream inputStream = assetManager.open("file.txt");
  BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
  StringBuilder builder = new StringBuilder();
  String line;
  while ((line = reader.readLine()) != null) {
      builder.append(line);
  }
  String content = builder.toString();
  inputStream.close();
  ```

* feature

  * The `assets` directory is a directory for arbitrary files that are not compiled. Asset files that are saved in the assets folder are included as-is in the APK file, without any modification, while the files saved in the `res` directory are processed and compiled into optimized binary formats at build time.

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

### 官方文档

* [coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/topics/coroutines-guide.md)

### 是什么

* Coroutines are computations that run on top of threads and can be suspended. When a coroutine is "suspended", the corresponding computation is paused, removed from the thread, and stored in memory. Meanwhile, the thread is free to be occupied with other activities. 

### 执行顺序

* 示例代码

  ```kotlin
  private val scope by lazy { CoroutineScope(EmptyCoroutineContext) }
  
  private suspend fun testBasic() { // line 3
      scope.launch(Dispatchers.IO) { // line 4
          delay(1000L) // line 5
          Log.d(TAG, "World!") // line 6
      } // line 7
      Log.d(TAG, "Hello") // line 8
  }
  
  public suspend fun delay(timeMillis: Long)
  ```

* 顺序解析

  1. line 4 和 line 8是并发执行的，因为 line 4新启动了一个协程，新的协程和其他代码块之间是并发的
  2. line 5 和 line 6是串行的，因为它们在同一个协程的代码块内，同一个协程的代码是串行的
  3. line 5 和 line 8几乎是同时执行的，打印`System.currentTimeMillis()`会发现两者时间一样或者悬殊`1ms`
  4. line 5是不占用线程资源的，因为delay是一个suspend方法，它在这里是会自动挂起释放线程的

### 核心概念

* CoroutineScope
  * `public suspend fun <R> coroutineScope(): R`

    * for - Creates a CoroutineScope and calls the specified suspend block with this scope. The provided scope inherits its coroutineContext from the outer scope, but overrides the context's Job.
    * feature
      * This function is designed for parallel decomposition of work. When any child coroutine in this scope fails, this scope fails and all the rest of the children are cancelled.
      * When we need to start new coroutines in a structured way inside a `suspend` function without access to the outer scope, we can create a new coroutine scope which automatically becomes a child of the outer scope that this `suspend` function is called from. 

  * GlobalScope

    * 定义

      ```kotlin
      public object GlobalScope : CoroutineScope {
          override val coroutineContext: CoroutineContext
              get() = EmptyCoroutineContext
      }
      ```

    * feature

      * The coroutines started from the global scope are all independent; their lifetime is limited only by the lifetime of the whole application. 

    * example

      ```kotlin
      private suspend fun testGlobalScope() {
          val job = scope.launch {
            	// 用GlobalScope启一个协程
              GlobalScope.launch(Dispatchers.IO) {
                  delay(1000L)
                  Log.d(TAG, "GlobalScope.launch finish")
              }
            	// 用inherit scope启一个协程
              launch(Dispatchers.IO) {
                  delay(1000L)
                  Log.d(TAG, "inherit scope launch finish")
              }
              Log.d(TAG, "waiting...")
          }
          delay(500)
          job.cancel() // cancel掉
      }
      ```

      ```bash
      waiting...
      GlobalScope.launch finish
      ```

  * scop buider

    * blocking - *blocks* the current thread for waiting
      1. `fun CoroutineScope.launch(): Job` - Launches a new coroutine without blocking the current thread and returns a reference to the coroutine as a Job.
      2. `fun <T> CoroutineScope.async(): Deferred<T>` - Creates a coroutine and returns its future result as an implementation of Deferred.
    * suspend - suspends, releasing the underlying thread for other usages.
      1. `runBlocking()` - Runs a new coroutine and **blocks** the current thread interruptibly until its completion.
  
* Job
  * what? - A background job. Conceptually, a job is a cancellable thing with a life-cycle that culminates in its completion.
  * feature
    * Jobs can be arranged into parent-child hierarchies where cancellation of a parent leads to immediate cancellation of all its children recursively.
    * Failure of a child with an exception other than CancellationException immediately cancels its parent and, consequently, all its other children.
  * api
    * `public suspend fun join()` - Suspends the coroutine until this job is complete. This invocation resumes normally (without exception) when the job is complete for any reason. 

* CoroutineContext

  * what

    * It is a set of elements that define the behaviour of a coroutine.
    * It is a collection of key-value pairs where keys are instances of  CoroutineContext.Element interface.

  * pre-defined elements

    * CoroutineName
    * CoroutineDispatcher
    * Job

  * feature

    * plus - `myContext = Dispatchers.IO + CoroutineName("my-coroutine")`

    * inherit

      ```kotlin
      // CoroutineScope 的拓展方法 launch
      public fun CoroutineScope.launch(
          context: CoroutineContext = EmptyCoroutineContext,
          block: suspend CoroutineScope.() -> Unit
      ): Job {
        // 新的context是传入的context包了一层
        val newContext = newCoroutineContext(context)
      }
      
      // CoroutineScope 的拓展方法 newCoroutineContext
      public fun CoroutineScope.newCoroutineContext(context: CoroutineContext): CoroutineContext {
        // coroutineContext 是 CoroutineScope的成员变量，context是传入的context
        val combined = coroutineContext + context
        return combined
      }
      ```

* suspend function

  * feature
    * When a suspend function is called, it can be suspended until the result of its operation is available, and it resumes execution when the result is ready.
  * example
    * `public suspend fun delay(timeMillis: Long)`
    * `public suspend fun <T> withContext(): T` - Calls the specified suspending block with a given coroutine context, suspends until it completes, and returns the result.

### Structured concurrency

* what?
  * It is a programming paradigm for writing concurrent code that emphasizes safety, clarity, and predictability. 
* features
  * Coroutines follow a principle of **structured concurrency** which means that new coroutines can be only launched in a specific **CoroutineScope** which delimits the lifetime of the coroutine.
  * An outer scope cannot complete until all its children coroutines complete.

### CPS

* what? - Continuation-passing style (CPS) is a programming technique where functions pass on their results to a callback function instead of directly returning them. The callback function takes the result as an argument and continues the execution of the program. This style is often used in functional programming for tasks such as asynchronous programming and error handling.

* example

  ```python
  function add(a, b, callback) {
    const sum = a + b;
    callback(sum);
  }
  
  // The callback function that prints the result
  function printResult(result) {
    console.log(`The sum is ${result}`);
  }
  
  // Calling the add function with callback function
  add(5, 10, printResult);
  ```

  * We call the `add` function with the two numbers `5` and `10` and the `printResult` function as the callback. When the `add` function completes its calculation, it passes the result to the `printResult` function which outputs `The sum is 15` to the console.

### 协程原理

* 执行顺序

  1. `AbstractCoroutine#start()`

  2. `CoroutineStart#invoke()`

  3. Cancellable.kt

     ```kotlin
     public fun <T> (suspend () -> T).startCoroutineCancellable(Continuation<T>): Unit {
       createCoroutineUnintercepted(completion).intercepted().resumeCancellableWith()
     }
     ```

  4. IntrinsicsJvm.kt

     ```kotlin
     public actual fun <T> (suspend () -> T).createCoroutineUnintercepted(
         completion: Continuation<T>
     ): Continuation<Unit> {
       val probeCompletion = probeCoroutineCreated(completion)
       return if (this is BaseContinuationImpl)
           create(probeCompletion) // 走这里，实际执行BaseContinuationImpl.create()
       else
           createCoroutineFromSuspendFunction(probeCompletion)
     }
     ```

  5. ContinuationImpl.kt

     ```kotlin
     // 上面create创建的就是这个对象
     internal abstract class SuspendLambda(): ContinuationImpl(completion), SuspendFunction
     ```

  6. 继续执行第二步的`intercepted()`

  7. IntrinsicsJvm.kt

     ```kotlin
     // 实际就是执行intercepted()方法, 然后将ContinuationImpl给包一层
     public actual fun <T> Continuation<T>.intercepted(): Continuation<T> =
         (this as? ContinuationImpl)?.intercepted() ?: this
     ```

  8. ContinuationImpl.kt

     ```kotlin
     private var intercepted: Continuation<Any?>? = null
     public fun intercepted(): Continuation<Any?> = 
     	intercepted
           ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)
                 .also { intercepted = it }
     // 实际就是从context中取出ContinuationInterceptor，然后执行interceptContinuation()
     ```

  9. CoroutineDispatcher.kt

     ```kotlin
     public final override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
             DispatchedContinuation(this, continuation) //返回包了一层的Continuation
     ```

  10. 继续执行第三步的`resumeCancellableWith()`

  11. `DispatchedContinuation#resumeWith()`

      ```kotlin
      fun resumeCancellableWith(result: Result<T>) {
        dispatcher.dispatch(context, this)
      }
      ```

  12. 后面就会经过一些队列，线程池的操作，最后调用到`BaseContinuationImpl#resumeWith()`

     ```kotlin
     public final override fun resumeWith(result: Result<Any?>) {
       val outcome = invokeSuspend(param) // 很核心的一句
     }
     ```

* 状态机源代码

  ```kotlin
  private suspend fun firstFunction() {
      delay(1000)
      println( "firstFunction")
  }
  
  private suspend fun secondFunction() {
      firstFunction()
      println( "secondFunction")
      delay(2000)
  }
  ```

* 反编译代码

  ```kotlin
  private static final Object firstFunction(Continuation var0) {
    //... firstFunction代码省略
  }
  
  private static final Object secondFunction(Continuation var0) {
    Object $continuation;
    label27: {
       if (var0 instanceof <undefinedtype>) {
          $continuation = (<undefinedtype>)var0;
          if ((((<undefinedtype>)$continuation).label & Integer.MIN_VALUE) != 0) {
             ((<undefinedtype>)$continuation).label -= Integer.MIN_VALUE;
             break label27;
          }
       }
  
       $continuation = new ContinuationImpl(var0) {
          // $FF: synthetic field
          Object result;
          int label;
  
          @Nullable
          public final Object invokeSuspend(@NotNull Object $result) {
             this.result = $result;
             this.label |= Integer.MIN_VALUE;
             return MainKt.secondFunction(this);
          }
       };
    }
  
    Object $result = ((<undefinedtype>)$continuation).result;
    Object var4 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
    switch (((<undefinedtype>)$continuation).label) {
       case 0:
          ResultKt.throwOnFailure($result);
          ((<undefinedtype>)$continuation).label = 1;
          if (firstFunction((Continuation)$continuation) == var4) {
             return var4;
          }
          break;
       case 1:
          ResultKt.throwOnFailure($result);
          break;
       case 2:
          ResultKt.throwOnFailure($result);
          return Unit.INSTANCE;
       default:
          throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
    }
  
    String var1 = "secondFunction";
    System.out.println(var1);
    
    ((<undefinedtype>)$continuation).label = 2;
    if (DelayKt.delay(2000L, (Continuation)$continuation) == var4) {
       return var4;
    } else {
       return Unit.INSTANCE;
    }
  }
  ```

* 解析

  1. 每一个suspend函数，编译器都会给它末尾加上一个参数`Continuation`，返回值都是`Object`

     * `private static final Object secondFunction(Continuation var0)`
     * `DelayKt.*delay*(2000L, (Continuation)$continuation)`

  2. 第一次进入

     1. 执行label 27代码，给`$continuation`初始化，此时`result = null, label = 0`
     2. 执行switch代码，因为`label = 0`，所以肯定执行case0
     3. case0中，将label置为1，则第二次进入会走case1；
     4. 调用`firstFunction((Continuation)$continuation)`
        1. 如果`firstFunction()` 不挂起，则肯定返回的是`Unit.INSTANCE`，因此直接break执行`System.out.println()`及后面代码，这种情况过于简单我们不考虑
        2. 如果`firstFunction()` 挂起，则肯定返回`IntrinsicsKt.getCOROUTINE_SUSPENDED()`，则`secondFunction()`立即返回，后面的代码全部都没执行

  3. 第二次进入

     1. 当`firstFunction()`执行完成后，会稀奇古怪地调用到`BaseContinuationImpl#resumeWith()`，因此就会调用到下面代码第二次进入

        ```kotlin
        public final Object invokeSuspend(@NotNull Object $result) {
           this.result = $result;
           this.label |= Integer.MIN_VALUE;
           return MainKt.secondFunction(this);
        }
        ```

     2. 此时label = 1，因此检查了下错误就直接break，执行`System.out.println()`和后面的代码

### 线程切换

* 本质: 一个线程池，通过一个队列，将需要run的协程放进去run，需要挂起的就把线程释放出来

* 参考过程 - 看协程原理 6 - 12步

* 实现

  ```kotlin
  class CoroutineScheduler(val corePoolSize: Int, val maxPoolSize: Int): Executor, Closeable {
    val globalCpuQueue = GlobalQueue()
    val globalBlockingQueue = GlobalQueue()
    
    private fun addToGlobalQueue(task: Task): Boolean
    
    override fun execute(command: Runnable) = dispatch(command)
    
    fun dispatch(block: Runnable, taskContext: TaskContext) {}
    
    inner class Worker private constructor() : Thread() {
      val scheduler get() = this@CoroutineScheduler
      override fun run() = runWorker()
      private fun runWorker() {
        while (!isTerminated && state != WorkerState.TERMINATED) {
          val task = findTask(mayHaveLocalTasks)
          if (task != null) {
            executeTask(task)
          }
        }
      }
    }
  }
  ```

* 其实就是队列，handler那一套思想

### Flow

* 创建Flow

  1. 调用Flow buidler

     ```kotlin
     flow {
         delay(1000)
         emit(1)
         delay(1000)
         emit(2)
     }
     ```

  2. Builders.kt

     ```kotlin
     // 调用flow builder实际上只是new了一个对象，其他啥都没有做
     public fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): Flow<T> = SafeFlow(block)
     
     private class SafeFlow<T>(private val block: suspend FlowCollector<T>.() -> Unit) : AbstractFlow<T>() {
       	// 仅仅将block块传入collectSafely的调用
         override suspend fun collectSafely(collector: FlowCollector<T>) {
             collector.block()
         }
     }
     ```

* 消费Flow

  1. 调用`collect()`方法

     ```kotlin
     testFlow().collect {
         Log.d(TAG, "result = $it")
     }
     ```

  2. Collect.kt

     ```kotlin
     public suspend fun <T> Flow<T>.collect(action: suspend (value: T) -> Unit): Unit =
         // 调用Flow#collect方法，并将action包成FlowCollector，emit执行就是action执行
     		this.collect(object : FlowCollector<T> {
             override suspend fun emit(value: T) = action(value)
         })
     ```

  3. Flow.kt

     ```kotlin
     public abstract class AbstractFlow<T> : Flow<T> {
       public final override suspend fun collect(collector: FlowCollector<T>) {
         	// 将collector包成SafeCollector
           val safeCollector = SafeCollector(collector, coroutineContext)
         	// 调用collectSafely
           collectSafely(safeCollector)
       }
       
       public abstract suspend fun collectSafely(collector: FlowCollector<T>)
     }
     ```

  4. Buider.kt

     ```kotlin
     private class SafeFlow<T>(private val block: suspend FlowCollector<T>.() -> Unit) : AbstractFlow<T>() {
         override suspend fun collectSafely(collector: FlowCollector<T>) {
           	// 真正调用到创建Flow时传入的代码
             collector.block()
         }
     }
     ```

* 观察者模式

  * 源代码

    ```kotlin
    private suspend fun testFlow(): Flow<Int> {
        return flow {
            delay(1000)
            emit(1)
            delay(1000)
            emit(2)
        }
    }
    ```

  * 反编译代码

    ```kotlin
    FlowKt.flow((Function2)(new Function2((Continuation)null) {
      private Object L$0; // collector，即消费flow时传入的方法
     	int label; // switch执行时的标志位
      
      public final Object invokeSuspend(@NotNull Object $result) {
        Integer var10001; // emit时的数据
        FlowCollector $this$flow; // collector，即消费flow时传入的方法
        Object var3; // 常量，标记是否有挂起
        label34: {
          label33: {
            var3 = IntrinsicsKt.getCOROUTINE_SUSPENDED(); // 常量固定
            switch(this.label) {
            	case 0:
                $this$flow = (FlowCollector)this.L$0; // collector赋值
                this.label = 1; // 下次进case 1
                if (DelayKt.delay(1000L, this) == var3) {
                  return var3; // 第一次执行时直接返回
                }
                break;
            	case 1:
              	$this$flow = (FlowCollector)this.L$0;
              	break;
            }
            
            var10001 = Boxing.boxInt(1); // 即将emit出的1
            this.label = 2; // 走case2
            if ($this$flow.emit(var10001, this) == var3) { // 执行emit, emit实际执行的是消费时传入的block
               return var3;
            }
          }
        }
      }
    }
    ```

* 操作符如map

  1. 示例代码

     ```kotlin
     private suspend fun testFlow(): Flow<Int> {
         return flow {
             emit(1)
         }
             .map {
                 it + 1
             }
     }
     ```

  2. 调用Flow的拓展函数map()

     ```kotlin
     // 调用transform，将旧Flow包了一层成新Flow
     public fun <T, R> Flow<T>.map(transform: suspend (value: T) -> R): Flow<R> = transform { value ->
        return@transform emit(transform(value))
     }
     ```

  3. 调用Flow的拓展函数unsafeTransform()

     ```kotlin
     // 调用unsafeFlow，将旧Flow包了一层成新Flow
     fun <T, R> Flow<T>.unsafeTransform(
         transform: suspend FlowCollector<R>.(value: T) -> Unit
     ): Flow<R> = unsafeFlow {
         Flow<T>.collect { value ->
             return@collect transform(value)
         }
     }
     ```

  4. 调用unsafeFlow()

     ```kotlin
     // 创建一个Flow，将旧Flow包成新Flow
     fun <T> unsafeFlow(block: suspend FlowCollector<T>.() -> Unit): Flow<T> {
         return object : Flow<T> {
             override suspend fun collect(collector: FlowCollector<T>) {
                 collector.block()
             }
         }
     }
     ```

  5. collect()执行顺序

     * 看了下反编译代码，和Rxjava一样的，就是把flow给包了一层，但是看kotlin各种拓展函数，函数当参数传递没有看明白

***

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

## SurfaceView

### 核心与相关联的类

* Surface

  * `Handle onto a raw buffer that is being managed by the screen compositor.`

  * 继承了`Parcelable`，因此可以跨进程通信，在`WMS`中传递

  * 它持有了`NativeBuffer`的指针，这个`NativieBuffer`指的是用来保存当前窗口屏幕数据的一个`buffer`

  * `ViewRootImpl`持有了这个对象，即一颗`view tree`，一个`window`，共享一个`Surface`

  * java代码

    ```java
    public class Surface implements Parcelable {
      long mNativeObject; // 真正的nativeSurface
      // 持有Canvas, 但Canvas也只是native的一层包装，持有了一个native指针
      private final Canvas mCanvas = new CompatibleCanvas(); 
    }
    ```
  
  
  * native代码
  
    ```c++
    // 构造函数需要传入一个生产者的引用，和BufferQueue的交互均有这个生产者的引用来完成
    Surface::Surface( const sp<GraphicBufferProducer>& bufferProducer, bool controlledByApp) : mGraphicBufferProducer(bufferProducer), mGenerationNumber(0)
    ```
  


* SurfaceHolder

  * 充当`MVC`模式中的`C`，`Surface`是`M`，`SurfaceView`是`V`

  * `SurfaceView`持有`Surface`，但设置为`private`，外部通过`SurfaceHolder`这个`C`去控制`Surface`

    ```java
    public class SurfaceView extends View {
      final Surface mSurface = new Surface(); // 直接new一个Surface
      
      public SurfaceHolder getHolder() { return mSurfaceHolder; }
      
      private final SurfaceHolder mSurfaceHolder = new SurfaceHolder() {
        public boolean isCreating() {
         // 返回成员变量mIsCreating
        }
        
        public void addCallback(Callback callback) {
          // 操作成员变量mCallbacks
        }
        
        public Canvas lockCanvas() {
          // 将操作委托给mSurface
        }
        
        public void unlockCanvasAndPost(Canvas canvas) {
          // 将操作委托给mSurface
        }
        
        public Surface getSurface() {
          // 返回成员变量mSurface
        }
      }
    }
    ```


* SurfaceView

  * 太复杂了，看不懂，先贴个链接 -> [Graphics architecture](https://source.android.com/docs/core/graphics/architecture)

  * `Provides a dedicated drawing surface embedded inside of a view hierarchy.`

  * 是`View`的子类

  * 不与`window`共享`surface`，而是自己持有一个`surface`

  * 为了解决与`window#surface`的重叠问题，`SurfaceView`是在`Z轴`的底部，通过让`window#surface`设置为透明而显示出来

  * `surface`绘制的线程可以自己定，可以不是主线程


* TextureView

  * 继承自`View`，它的表现就像一个普通的`View`一样

  * 它没有自己的`Surface`，而是共享`ViewRootImpl`的`Surface`

  * 由于没有自己的`Surface`，它的理论性能比`SurfaceView`低

  * 显示的内容通过`SurfaceTexture`传递


* SurfaceTexture

  * `Captures frames from an image stream as an OpenGL ES texture.`

  * 可以把`Surface`生成的图像流，转换为纹理`Texture`，供业务方进一步加工使用


### 挖孔论

* 代码实现

  ```java
  // SurfaceView.java
  private OnPreDrawListener mDrawListener = () -> {
      updateSurface(); // onPreDraw()时调用updateSurface()
  };
  
  private OnScrollChangedListener mScrollChangedListener = this::updateSurface; // onScroll时也调updateSurface()
  
  protected void onAttachedToWindow() { // onAttachToWindow()时调用
    // 监听ViewRootImpl对应的Surface发生改变
    getViewRootImpl().addSurfaceChangedCallback(this);
    mAttachedToWindow = true; // 一个标记位
    // 核心实现，开始请求划出一片区域为透明
    mParent.requestTransparentRegion(SurfaceView.this);
    // onPredraw()与onScroll()时调用updateSurface()
    observer.addOnScrollChangedListener(mScrollChangedListener);
    observer.addOnPreDrawListener(mDrawListener);
  }
  
  // ViewGoup.java
  public void requestTransparentRegion(View child) {
    if (child != null) {
      child.mPrivateFlags |= View.PFLAG_REQUEST_TRANSPARENT_REGIONS; // 标记位
      if (mParent != null) {
        mParent.requestTransparentRegion(this); // 一直向上调用，最后调到ViewRootImpl
      }
    }
  }
  
  // ViewRootImpl.java
  private void performTraversals() {
    if (View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {
      params.format = PixelFormat.TRANSLUCENT; // format设置为半透明
    }
    performLayout(lp, mWidth, mHeight); // 执行完了layout
    if (View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {
      mTransparentRegion.set(x, x, x, x) // 规定透明区域的范围
      // 此方法View, ViewGroup, SurfaceView都有实现，会将对应的View的区域设置成透明
      host.gatherTransparentRegion(mTransparentRegion);
    }
  }
  ```

### updateSurface()

* 调用时机

  * `onPreDraw()`,`onSrcoll()`,  `ViewRootSurface#surfaceCreated()`, `ViewRootSurface#surfaceDestroyed()`, `setVisibility()`, `setFrame()`

* 干了什么

  ```java
  private BLASTBufferQueue mBlastBufferQueue; // 一个队列
  
  protected void updateSurface() {
    // 定义一大堆以changed结尾的变量
    final boolean visibleChanged;
    final boolean alphaChanged;
    final boolean creating;
    final boolean sizeChanged;
    
    if(hasChanged) {
      getLocationInWindow(mLocation); // 获取此SurfaceView在window中的位置
      
      mWindowSpaceLeft = mLocation[0]; // 记录位置在mScreenRect成员变量内
      mWindowSpaceTop = mLocation[1];
      mScreenRect.left = mWindowSpaceLeft;
      mScreenRect.top = mWindowSpaceTop;
      mScreenRect.right = mWindowSpaceLeft + getWidth();
      mScreenRect.bottom = mWindowSpaceTop + getHeight();    
    }
    
    if (creating) {
      createBlastSurfaceControls(viewRoot, name, surfaceUpdateTransaction); // 创建 BLASTBufferQueue
    }
    
    // 这一次ViewRootImpl#performDraw()之前是否要增加一些操作(比如draw这个SurfaceView)
    boolean shouldSyncBuffer = redrawNeeded && viewRoot.wasRelayoutRequested()
    if (shouldSyncBuffer) {
      // 队列加一个消息
      mBlastBufferQueue.syncNextTransaction( false, onTransactionReady);
    }
  
    // 里面进行了一大堆操作，最终返回size是否变化
    final boolean realSizeChanged = performSurfaceTransaction(viewRoot, ...);
  
    copySurface(creating, sizeChanged); // 尝试重建Surface
  
    if (!mSurfaceCreated && (surfaceChanged || visibleChanged)) {
      for (SurfaceHolder.Callback c : getSurfaceCallbacks()) {
        c.surfaceCreated(mSurfaceHolder); // 调用SurfaceCreated()
      }
    }
    
    if (creating || formatChanged || sizeChanged || hintChanged) {
      for (SurfaceHolder.Callback c : getSurfaceCallbacks()) {
        c.surfaceChanged(mSurfaceHolder, mFormat, myWidth, myHeight); // 调用SurfaceChanged()
      }
    }
    
    if (redrawNeeded) { // 需要重绘
      callbacks = getSurfaceCallbacks();
      if (shouldSyncBuffer) { // 有额外的操作
        handleSyncBufferCallback(callbacks, syncBufferTransactionCallback);
      } else { // 无额外的操作
        handleSyncNoBuffer(callbacks);
      }
    }
  }
  
  // 这个类感觉自己实现了一个同步机制
  private static class SyncBufferTransactionCallback {
    private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private Transaction mTransaction;
  
    Transaction waitForTransaction() {
     	mCountDownLatch.await();
      return mTransaction;
    }
  
    void onTransactionReady(Transaction t) {
      mTransaction = t;
      mCountDownLatch.countDown();
    }
  }
  
  private void copySurface(boolean surfaceControlCreated, boolean bufferSizeChanged) {
    // 非必要不执行
    if (!surfaceControlCreated && !needsWorkaround) { return; }
    if (surfaceControlCreated) {
      mSurface.copyFrom(mBlastBufferQueue); // surface只是一个java对象，copyFrom重建只需要改下nativeObject的指针就好了
    }
    if (needsWorkaround && mBlastBufferQueue != null) {
      mSurface.transferFrom(mBlastBufferQueue.createSurfaceWithHandle());
    }
  }
  
  private void handleSyncBufferCallback(Callback[]， SyncBufferTransactionCallback) {
    getViewRootImpl().addToSync(syncBufferCallback -> {
      mBlastBufferQueue.stopContinuousSyncTransaction(); // 队列先暂停一下
      t = syncBufferTransactionCallback.waitForTransaction(); // 用自己实现的同步机制获取一个Transaction
      syncBufferCallback.onBufferReady(t); // 调用onBufferReady()准备绘制
      mParent.requestTransparentRegion(SurfaceView.this); // 完成绘制后的操作
      invalidate(); // 完成Surface绘制要求更新UI
    })
  }
  ```


### BufferQueue

* 好文链接 - [深入浅出Android BufferQueue](https://zhuanlan.zhihu.com/p/62813895)

* 模型 - 生产者消费者模式

  * 生产者 - 产生图像源数据，如`Surface`，截图时的`SurfaceFlinger`
  * 消费之 - 消费图像源数据，如`SurfaceFlinger`，截图时另外的一个`BufferQueue`

* BufferState

  * FREE - 所有权归`BufferQueue`
  * DEQUEUED - 所有权归生产者
  * QUEUED - 已填充数据，但未被消费者获取，所有权归`BufferQueue`
  * ACQUIRED - 所有权归消费者

* `Surface`生产者

  ```c++
  // 获取一个Buffer来draw
  static jlong nativeLockCanvas() {
  	//1. 通过Surface::lock方法，获取一个合适的Buffer
    status_t err = surface->lock(&outBuffer, dirtyRectPtr);
    //2. 构造一个Bitmap，地址指向步骤1获取的Buffer的地址，这样在这个Bitmap上绘制的内容，直接绘制到了GraphicBuffer
    SkBitmap bitmap;
    //将GraphicBuffer构造成一个Bitmap，设置给Canvas
    Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
    // canvas始终要调用一次setBitmap()，无论是java调还是native调
    nativeCanvas->setBitmap(bitmap);
  }
  
  static void nativeUnlockCanvasAndPost() {
  	// detach the canvas from the surface
  	Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
  	nativeCanvas->setBitmap(SkBitmap()); // 设置bitmap为另一个对象即与buffer解绑
    err = queueBuffer(mLockedBuffer.get(), fd); // 直接将数据放入BufferQueue
  }
  ```

### SurfaceFlinger

* SurfaceFlinger是一个开机就自动启动的进程

  ![结构图片](https://img2018.cnblogs.com/blog/821933/201907/821933-20190730111306166-2128331293.png)

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

## Video

### VideoView使用

* 自己看代码吧

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
