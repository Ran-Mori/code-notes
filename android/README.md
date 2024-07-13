# Android

## base_android_project

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

## compatibility

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

## scroll

### View

* mScrollY

  * 计算公式: View位置top边缘的纵坐标(一般是0) - View内容的top边缘的纵坐标(一般为负数)
  * 所以手向上滑动，下面的内容展示出来，这时的`mScrollY`是正数且越来越大
  * 设置`mScrollY`后，会在`draw()`的时候将canvas移动来达到`scroll`的效果

* scrollTo(int, int)

  * 代码实现

    ```java
    public void scrollTo(int x, int y) {
      if (mScrollX != x || mScrollY != y) {
        int oldX = mScrollX;
        int oldY = mScrollY;
        mScrollX = x;
        mScrollY = y; // 赋值
        invalidateParentCaches(); // 清缓存
        onScrollChanged(mScrollX, mScrollY, oldX, oldY); // 执行回调
        if (!awakenScrollBars()) {
          postInvalidateOnAnimation(); // 请求执行draw()
        }
      }
    }
    ```

  * 当一个View设置`mScrollX/mScrollY`后，会进行`draw`但不会重新进行`measure、layout`

  * 这个View本身的宽高、布局、位置全部都不变


### ScrollView

```java
public void scrollTo(int x, int y) {
  // we rely on the fact the View.scrollBy calls scrollTo.
  if (getChildCount() > 0) {
    View child = getChildAt(0);
    x = clamp(x, getWidth() - mPaddingRight - mPaddingLeft, child.getWidth());
    y = clamp(y, getHeight() - mPaddingBottom - mPaddingTop, child.getHeight());
    if (x != mScrollX || y != mScrollY) {
      super.scrollTo(x, y); // 它就包了一层，限定只有一个child，实际还是调用View#scrollTo(x, y)
    }
  }
}

// 重写了onTouchEvent()，手势的时候实现滑动scroll
public boolean onTouchEvent(MotionEvent ev) {
  // 噼里啪啦一堆实现
}
```

### Scroller

* 使用介绍

  ```kotlin
  class MyView: View() {
    val scroller = Scorller(context)
    
    fun smoothScrollTo(int destX, int destY) {
      scroller.startScroll(startX, startY, dx, dy, duration)
      invalidate()
    }
    
    override fun computeScroll() {
      if (scroller.computeScrollOffset()) {
        val currX = scroller.getCurrX()
        val currY = scroller.getCurrY()
        scrollTo(currX, currY)
        // Update the position of the view here
        invalidate()
      }
    }
  }
  ```

  ```java
  public class View {
    boolean draw() {
      computeScroll();
    }
  }
  ```

* 作用

  * 仅仅作为一个计算器，计算了一下应该滑动的值，它本身并不负责滑动

* 过程

  1. 先new了一个`Scroller`对象
  2. 接着调用`scroller.startScroll(startX, startY, dx, dy, duration)`，规定了在指定的duration内滑动多远
  3. 然后马上调用`invalidate()`触发调用`draw()`
  4. 在`draw()`中又调用到了`computeScroll()`，View中本身没有实现这个方法，因此调用到了子类的`computeScroll()`方法
  5. `computeScroll()`中用`scroller`计算出来的应滑动的值来滑动。接着最后又调用了`invalidate()`，回到了步骤3

***

## self_define_view

### xml自定义属性

### 自定义View支持wrap_content

### 如何画一个圆

***

## send_email_attachments

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

## serivce

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

## shadow_background

### svg

* 是一种图片的存储格式，和`jpg、png`一样
* 存储的是点与线的数学关系，而不是位图

### android矢量图

* 矢量图可以理解成是一个接口，而Android的`xml drawable`是对这个接口的实现

### 灰度渐变矢量图如何画

***

## share_animation

### reference

* [Android Activity共享元素动画分析](https://juejin.cn/post/7144621475503276045#heading-4)

### view transfer

* ActivityA generate an ActivityOptions

  ```java
  public class ActivityOptions {
    static ExitTransitionCoordinator makeSceneTransitionAnimation() {
      ExitTransitionCoordinator exit = new ExitTransitionCoordinator();
      opts.mTransitionReceiver = exit; // pass ExitTransitionCoordinator to ActivityB
      opts.mSharedElementNames = names; // pass shareElements names to ActivityB
      opts.mIsReturning = false; // pass not return to ActivityB
      return exit;
    }
  }
  ```

* ActivityA start ActivityB with a bundle

  ```java
  val bundle = ActivityOptions.makeSceneTransitionAnimation(this, iv,"shareElement").toBundle()
  val intent = Intent(this, ActivityB::class.java)
  startActivity(intent, bundle) // start with a bundle
  ```

* ActivityB receive the bundle

  ````java
  public class LaunchActivityItem {
    private LaunchActivityItem(Parcel in) {
      // from bundle to new an ActivityOptions
      ActivityOptions.fromBundle(in.readBundle())
    }
  }
  ````

* use the bundle to generate an ActivityOptions

  ```java
  public class ActivityOptions {
    public static ActivityOptions fromBundle(Bundle bOptions) {
      return bOptions != null ? new ActivityOptions(bOptions) : null;
    }
  }
  ```


***

## shared_preferences

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

### 特点

* 理论上sp实现原理是读写文件，可以用于IPC。但实际上最好别用，因为系统对它的读写有一定的缓存的策略，在内存中存在一个副本，所以多进程它是读写不可靠的。

***

## strict_mode

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

## system_service

### reference

* [理解 Context.getSystemService 原理](https://juejin.cn/post/6844903812159815687)

### process

* call getService()

  ```java
  // android.content.Context#getSystemService(java.lang.String)
  public abstract @Nullable Object getSystemService(@ServiceName @NonNull String name);
  
  // android.app.ContextImpl#getSystemService
  public Object getSystemService(String name) {
    return SystemServiceRegistry.getSystemService(this, name);
  }
  
  
  ```

* how does `SystemServiceRegistry` work.

  ```java
  // android.app.SystemServiceRegistry#getSystemService
  public final class SystemServiceRegistry {
    // store ServiceFetcher
    private static final Map<String, ServiceFetcher<?>> SYSTEM_SERVICE_FETCHERS = new ArrayMap();
    
    // static block code
    static {
      // there are many calls of registerService() method, and it will register to SYSTEM_SERVICE_FETCHERS.
      registerService(Context.LAYOUT_INFLATER_SERVICE, LayoutInflater.class, new CachedServiceFetcher<LayoutInflater>() {
      @Override
      public LayoutInflater createService(ContextImpl ctx) {
        return new PhoneLayoutInflater(ctx.getOuterContext());
      }});
    }
    
    public static Object getSystemService(ContextImpl ctx, String name) {
      // query ServiceFetcher by a string name.
      final ServiceFetcher<?> fetcher = SYSTEM_SERVICE_FETCHERS.get(name);
      // delegate the action to ServiceFetcher
      return fetcher.getService(ctx);
    }
  }
  ```

* How does ServiceFetcher works. 

  ```java
  // android.app.SystemServiceRegistry.ServiceFetcher
  static abstract interface ServiceFetcher<T> {
    T getService(ContextImpl ctx); // only a single method
  }
  
  // android.app.SystemServiceRegistry.CachedServiceFetcher
  // it use infinite loop mechanism to make soure that there is only one instance of this service in case of mullti threads
  // the instance will differ in ContextImpl, so it can't be accessed across process.
  
  // android.app.SystemServiceRegistry.StaticServiceFetcher
  // it is a more simple way to implement ServiceFetcher
  // the instance will not differ in ContextImpl, so it can be accessed across process.
  ```

* how really to create a service.

  ```java
  // Context.WINDOW_SERVICE
  @Override
  public WindowManager createService(ContextImpl ctx) {
    return new WindowManagerImpl(ctx);
  }
  
  // Context.ACTIVITY_SERVICE
  @Override
  public ActivityManager createService(ContextImpl ctx) {
    return new ActivityManager(ctx.getOuterContext(), ctx.mMainThread.getHandler());
  }
  
  // Context.LOCATION_SERVICE
  @Override
  public LocationManager createService(ContextImpl ctx) throws ServiceNotFoundException {
    // use Binder and ServiceManager
    IBinder b = ServiceManager.getServiceOrThrow(Context.LOCATION_SERVICE);
    return new LocationManager(ctx, ILocationManager.Stub.asInterface(b));
  }
  ```

### ServiceManager



## text_watcher

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

## video

### VideoView使用

* 自己看代码吧

***

## view_coordinate

### 坐标

* `left, top, right, bottom, elevation` - The distance in pixels from the xxx edge of this view's parent
* `translateX, translateY, translateZ` - The x/y/z location of this view relative to its left/top/elevation position
* `x/y/z = mLeft/mTop/elevation + translationX/translationY/translationZ` - The visual x/y/z position of this view, in pixels

### translate value

* 设置`translate`后只重新`draw`，不会重新进行`measure, layout`
* 是一种低代价改变`View`位置的参数，因为不用`measure、layout`，常用来做动画
* 设置后重新调用`requestLayout`依然不会使`translate`失效

***

## view_stub

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

## view_tree_observer

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

## web_view

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

## widget

### 基本原理

* 将`WidgetProvider`注册为一个`BroadCastReciver`，接收各种广播通知尤其是`android.appwidget.action.APPWIDGET_UPDATE`
* 在初始化或者`update`的时候设置好各个`View`的点击事件，不过要通过`RemoteView`的`api`来设置，和传统的`View#setOnClickListener`有点不同

### 使用过程 

* [Create a simple widget](https://developer.android.com/develop/ui/views/appwidgets)

***

## window_manager

### links

* [Android绘制流程 —— View、Window、SurfaceFlinger](https://juejin.cn/post/6899010578145411085)
* [Android全面解析之Window机制](https://juejin.cn/post/6888688477714841608)

### 几个关键类

1. ViewManager

   * source code

     ```java
     public interface ViewManager{
         public void addView(View view, ViewGroup.LayoutParams params);
         public void updateViewLayout(View view, ViewGroup.LayoutParams params);
         public void removeView(View view);
     }
     ```

   * `ViewManager`接口定义得很纯净，就是cud三个操作

2. WindowManagerImpl

   * definition

     ```java
     // public interface WindowManager extends ViewManager
     public final class WindowManagerImpl implements WindowManager {
       private final WindowManagerGlobal mGlobal = WindowManagerGlobal.getInstance();
       public void addView(View view, ViewGroup.LayoutParams params) { mGlobal.addView(); }
     }
     ```

   * 最终`WindowManagerImpl`是实现了`ViewManager`接口

   * 将所有的操作全都委托给了`WindowManagerGlobal`

   * 创建

     ```java
     // window.java
     if (wm == null) {
       // 获取到系统WindowManagerService
       wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
     }
     // 创建一个本地使用的WindowManagerImpl对象
     mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);
     ```

3. WindowManagerGlobal

   * definition

     ```java
     public final class WindowManagerGlobal {
       private ArrayList<View> mViews = new ArrayList<View>(); // 存所有DecorView
       private ArrayList<ViewRootImpl> mRoots = new ArrayList<ViewRootImpl>(); // 存所有ViewRootImpl
       private static IWindowManager sWindowManagerService; // static全局唯一
       
       public static IWindowManager getWindowManagerService() {
         // 通过Binder获取WindowManagerService
         sWindowManagerService = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
       }
     }
     ```

### setContentView流程

1. 收到`EXECUTE_TRANSACTION`159类型的message，调用到创建window

   ```java
   public final class ActivityThread {
     class H extends Handler {
       public void handleMessage(Message msg) {
         switch (msg.what) {
           case EXECUTE_TRANSACTION:
             ClientTransaction transaction = (ClientTransaction) msg.obj;
             // 执行这个transaction，虽然不知道这个transaction到底是啥，只知道和启动Activity有关
             mTransactionExecutor.execute(transaction);
         }
       }
     }
   }
   
   class Activity {
     private Window mWindow;
     final void attach(...) {
       // 创建window
       mWindow = new PhoneWindow(this, window, activityConfigCallback);
     }
   }
   ```

2. `PhoneWindow`创建`DecorView`，并设置`DecorView`的布局

   ```java
   public class PhoneWindow extends Window {
     private DecorView mDecor; // 持有DecorView
     ViewGroup mContentParent;
     
     protected DecorView generateDecor(int featureId) {
       // new一个DecorView
       return new DecorView(context, featureId, this, getAttributes());
     }
     
     private void installDecor() {
       mDecor = generateDecor(-1);
       mContentParent = generateLayout(mDecor);
     }
     
     protected ViewGroup generateLayout(DecorView decor) {
       int layoutResource = R.layout.screen_simple;
       // 将R.layout.screen_simple传给DecorView
       mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);
       // 从DecorView中找到了R.id.content，如下xml所示
       ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT); // 从DecorView
       return contentParent;
     }
   }
   ```

   ```xml
   <!--screen_simple.xml-->
   <LinearLayout>
   	<ViewStub 
       android:id="@+id/action_mode_bar_stub"
       android:inflatedId="@+id/action_mode_bar"
       android:layout="@layout/action_mode_bar" />
   	<FrameLayout
        android:id="@android:id/content" /> <!--id为content-->
   </LinearLayout>
   ```

3. `DecorView`设置View

   ```java
   public class DecorView extends FrameLayout {
     // 第二步会调到
     void onResourcesLoaded(LayoutInflater inflater, int layoutResource) {
       // 构造这个View，布局如上xml所示
       final View root = inflater.inflate(layoutResource, null);
       // 直接作为DecorView的根子View
       addView(root, 0, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
       mContentRoot = (ViewGroup) root;
     }
   }
   ```

4. `AppCompatDelegateImpl`强行魔改`R.id.content`

   ```java
   class AppCompatDelegateImpl extends AppCompatDelegate {
     ViewGroup subDecor = null;
     
     private ViewGroup createSubDecor() {
       // 直接构造一个View，布局如下所示
       subDecor = (ViewGroup) LayoutInflater.from(themedContext).inflate(R.layout.abc_screen_toolbar, null);
     	// 从新构建的subDecor里找到一个View作为contentView
       FrameLayout contentView = subDecor.findViewById(R.id.action_bar_activity_content);
       // 获取DecorView的R.id.content
       ViewGroup windowContentView = (ViewGroup) mWindow.findViewById(android.R.id.content);
       while (windowContentView.getChildCount() > 0) {
         // 将R.id.content内的View全部移动到新的contentView中
         final View child = windowContentView.getChildAt(0);
         windowContentView.removeViewAt(0);
         contentView.addView(child);
       }
       windowContentView.setId(View.NO_ID); // 将原来的R.id.content设置为NO_ID
       contentView.setId(android.R.id.content); // 将新的contentView强设成R.id.content
     }
   }
   ```

   ```xml
   <android.support.v7.widget.ActionBarOverlayLayout
           android:id="@+id/decor_content_parent">
       <include layout="@layout/abc_screen_content_include"/>
       <android.support.v7.widget.ActionBarContainer
               android:id="@+id/action_bar_container">
           <android.support.v7.widget.Toolbar
                   android:id="@+id/action_bar"/>
           <android.support.v7.widget.ActionBarContextView
                   android:id="@+id/action_context_bar"/>
       </android.support.v7.widget.ActionBarContainer>
   </android.support.v7.widget.ActionBarOverlayLayout>
   ```

5. 执行`setContentView()`

   ```java
   class AppCompatDelegateImpl {
     ViewGroup subDecor = null;
     public void setContentView(int resId) {
       // 找到强行改后的R.id.content
       ViewGroup contentParent = mSubDecor.findViewById(android.R.id.content);
       // 清空然后强加
       contentParent.removeAllViews();
       LayoutInflater.from(mContext).inflate(resId, contentParent);
     }
   }
   ```


### 添加View进Window

1. Activity#onResume()

   ```java
   public final class ActivityThread {
     public void handleResumeActivity() {
       View decor = r.window.getDecorView(); // 获取DecorView
       decor.setVisibility(View.INVISIBLE); // 设置为不可见
       ViewManager wm = a.getWindowManager(); // 从Activity获取wm
       WindowManager.LayoutParams l = r.window.getAttributes(); // 获取LayoutParams
       wm.addView(decor, l); // 尝试把整个DecorView添加进Window
       r.activity.makeVisible(); //添加结束后最后设置为可见
     }
   }
   ```
   
2. WindowManagerGloabal创建ViewRootImpl

   ```java
   public final class WindowManagerGlobal {
     // 上面的addView最终会调用到这个方法，view是DecorView
     public void addView(View view, ViewGroup.LayoutParams params) {
       ViewRootImpl root = new ViewRootImpl(view.getContext(), display);
       // 把DecorView和ViewRootImpl都存一下
       mViews.add(view); 
       mRoots.add(root);
       // 调用ViewRootImpl的setView() 方法
       root.setView(view, wparams, panelParentView, userId);
     }
   }
   ```

3. ViewRootImpl#setView

   ```java
   public final class ViewRootImpl {
     View mView;
     
     public ViewRootImpl(Context context, Display display) {
       // getWindowSession()是单例，即一个应用对应一个Session
       this(context, display, WindowManagerGlobal.getWindowSession(), false);
     }
     // 上面会调到这个方法
     public void setView(View view, WindowManager.LayoutParams attrs) {
       mView = view; // 把DecorView赋值给mView
       requestLayout(); // 调一下requestLayout()
       // 跨进程Binder代理调用，通过系统进程的WindowManagerService建立连接
       res = mWindowSession.addToDisplayAsUser(mWindow, mWindowAttributes)
     }
   }
   ```
   

### window添加进WMS

1. com.android.server.wm.Session#addToDisplayAsUser()

   ```java
   // 继承Stub，是Binder IPC的server侧
   // There is generally one Session object per process 
   class Session extends IWindowSession.Stub {
     final WindowManagerService mService; // 持有WMS
     
     @Override // Override，说明是IWindowSession接口定义的方法
     public int addToDisplayAsUser(IWindow window, WindowManager.LayoutParams attrs) {
       return mService.addWindow(this, window);
     }
   }
   ```

2. WindowManagerService#addWindow()

   ```java
   // 继承Stub，是Binder IPC的server侧
   public class WindowManagerService extends IWindowManager.Stub {
     public int addWindow(Session session, IWindow client) {
       // 代码太复杂，就不贴了
     }
   }
   ```

### 删除View过程

1. WindowManagerGlobal#removeView()

   ```java
   public final class WindowManagerGlobal {
     public void removeView(View view, boolean immediate) {
       removeViewLocked(index, immediate);
     }
     private void removeViewLocked(int index, boolean immediate) {
       ViewRootImpl root = mRoots.get(index);
       View view = root.getView();
       boolean deferred = root.die(immediate); // 将操作委托给ViewRootImpl, 返回是否异步
       if (deferred) {
         mDyingViews.add(view); // 如果是同步，立即就删除，如果是异步，添进mDyingViews
       }
     }
   }
   ```

2. ViewRootImpl#die()

   ```java
   public final class ViewRootImpl implements ViewParent {
     boolean die(boolean immediate) {
       if (immediate && !mIsInTraversal) {
         doDie(); // 同步，直接doDie
         return false;
       }
       mHandler.sendEmptyMessage(MSG_DIE); // 异步，抛个事件，收到事件后也是执行doDie()
       return true;
     }
     void doDie() {
       dispatchDetachedFromWindow();
     }
     
     void dispatchDetachedFromWindow() {
       mView.dispatchDetachedFromWindow(); // 分发detachedFromWindow事件
       mView = null;
       mAttachInfo.mRootView = null; // 持有元素置空
       mWindowSession.remove(mWindow); //最后调用到WMS来remove
     }
   }
   ```
