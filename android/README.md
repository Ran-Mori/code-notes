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

  * 自动识别**fling、doubleclick、longpress、singletapup**等动作 

* 使用

  * 创建一个`GestureDetector`

  * 将`onTouchEvent`委托给它

    ```kotlin
    override fun onTouchEvent(event: MotionEvent): Boolean {
      return gesturedetector?.onTouchEvent(event) : super.onTouchEvent(event)
    }
    ```

### Fling

* 一种滑动动作，一般是用户快速的滑动
* 从`ACTION_DOWN`开始，到`ACTION_UP`结束。速度越快，`fling`越大

### 插值器

1. `LinearInterpolator`
2. `AccelerateInterpolator`
3. `DecelerateInterpolator`
4. `AnticipateInterpolator()` - starts backward then flings forward
5. `OvershootInterpolator()` - flings forward and overshoots the last value then comes back

### ViewFlipper

* 是什么 -> 一个`FrameLayout`，最多只能显示一个`child`
* 核心api
  1. `public void setDisplayedChild(int whichChild)`
  2. `public void setInAnimation(Animation inAnimation)`
  3. `public void setOutAnimation(Animation outAnimation)`

***

## CBGLSurfaceView

### SurfaceView

* 太复杂了，看不懂，先贴个链接 -> [Graphics architecture](https://source.android.com/docs/core/graphics/architecture)

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

## CBShakeAnimation

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

### View执行动画接口

* `public void startAnimation(Animation animation)`

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

## DoubleClickListener

### 如何实现双击

1. 写一个以`GestureDetector`为成员变量，且实现`View.OnTouchListener`的类

2. `GestureDetector`已经能够区分`onSingleTapUp`和`onDoubleTap`

3. 在`onSingleTapUp`里`postDelay`双击间隔时间的一个单击事件，在`onDoubleTap`中取消事件。如果在双击时间之内则会被取消，负责就会执行

4. 将这个类赋值给`setOnTouchListener(OnTouchListener l)`

***

## ViewCoordinate

### 坐标

* `left, top, right, bottom, elevation, translateX, translateY, translateZ, x, y z `

### translate

* 只改变`draw`渲染，不会重新进行`measure, layout`
* 是一种低代价改变`View`位置的参数，因为不用`layout`，常用来做动画

***

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
