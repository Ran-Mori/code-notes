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
  1. `A Bitmap to hold the pixels`
  2. `a Canvas to host the draw call`
  3. `a drawing primitive (e.g. Rect, Path, text, Bitmap)`
  4. `a paint`

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

## DoubleClickListener

### 如何实现双击

1. 写一个以`GestureDetector`为成员变量，且实现`View.OnTouchListener`的类

2. `GestureDetector`已经能够区分`onSingleTapUp`和`onDoubleTap`

3. 在`onSingleTapUp`里`postDelay`双击间隔时间的一个单击事件，在`onDoubleTap`中取消事件。如果在双击时间之内则会被取消，负责就会执行

4. 将这个类赋值给`setOnTouchListener(OnTouchListener l)`

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

## CBWidget

### 基本原理

* 将`WidgetProvider`注册为一个`BroadCastReciver`，接收各种广播通知尤其是`android.appwidget.action.APPWIDGET_UPDATE`
* 在初始化或者`update`的时候设置好各个`View`的点击事件，不过要通过`RemoteView`的`api`来设置，和传统的`View#setOnClickListener`有点不同

### 使用过程 

* [Create a simple widget](https://developer.android.com/develop/ui/views/appwidgets)

***

## Compose

* 不需要笔记，直接看代码吧

## Coroutines

* 代码啥都没有，先放这儿吧

## EventBus

### [笔记链接](https://github.com/IzumiSakai-zy/various-kinds-learning/blob/master/daily-android.md)

## EventDispatch

### [笔记链接](https://github.com/IzumiSakai-zy/various-kinds-learning/blob/master/android-develop-explore-art.md)

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

### [笔记链接](https://github.com/IzumiSakai-zy/various-kinds-learning/blob/master/android-develop-explore-art.md)

***

## NegativeMargin

### margin

* `margin`的值可以为负，为负表示画在父View的外部

### clipChildren

* `Defines whether a child is limited to draw inside of its bounds or not.`

***

## Rxjava

### Subject

* `Represents an Observer and an Observable at the same time, allowing multicasting events from a single source to multiple child Observers.`
* 各种`Subject`的区别有实际代码通过log表现

### 执行顺序

* 验证`Observable、Observer、map`等操作的执行顺序
* 验证`observeOn、subscribeOn`线程的区别

***

## ScrollView

### 内容

* `ScrollView#scrollTo()`和`ScrollView#scrollBy()`
* `scrollX、scrollY`

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

### SurfaceView

* 是`View`的子类
* `View`的绘制都是在主线程进行的，如果绘制任务比较复杂，就可以考虑使用`SurfaceView`
* `SurfaceView`的绘制过程是发生在子线程的，使用双缓冲机制

***

## ViewCoordinate

### 坐标

* `left, top, right, bottom, elevation, translateX, translateY, translateZ, x, y z `

### translate

* 设置`translate`后只重新`draw`，不会重新进行`measure, layout`
* 是一种低代价改变`View`位置的参数，因为不用`measure、layout`，常用来做动画
* 设置后重新调用`requestLayout`依然不会使`translate`失效

### scroll

* 当一个View设置`mScrollX/mScrollY`后，会对`children`重新进行`draw`但不会重新进行`measure、layout`
* `scroll`不改变View本身的大小，不改变本身的`backgroundDrawable`，它唯一改变的是`children`的位置，且不重新测量和布局
* 设置后重新调用`requestLayout`会使`mScrollX/mScrollY`失效

### View#scrollTo

* 不调用`measure、layout`，只调用`draw`，不改变自身而改变`children`

### invalidate

* 分为`View#invalidate()`和`View#postInvadiate()`
* 不调用`measure、layout`，只调用`draw`

### View#requestLayout

* 会传递到`ViewRootImpl`
* `measure、layout、draw`都会调用到，成本高

***

## ViewOnPreDraw

* 没啥笔记，自己看吧

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

