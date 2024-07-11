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

## binder

### reference

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
* `IBinder` - It is the interface for the `Binder` object. It defines the methods(`transact()`, `isBinderAlive()`) that can be used to communicate with a remote process.
* `Service` - It is a component in the Android system that runs in the background and provides a set of APIs that can be accessed by other processes.
* `client` - It is the process that uses a service provided by another process.
* `server` - It is the process that provides a service that can be used by other processes.
* `Stub` - It is an object that resides in the process that provides a service, and which can be called by other processes. When a remote process wants to access a service implemented by another process, it sends a request to the stub object. The stub then forwards the request to the actual implementation(`CalculatorServer`) of the service in the local process.
* `Proxy` - It is an object that resides in the process that needs to access a remote service, and which acts as a surrogate for the stub object in the remote process. The proxy object provides a local API that looks as if it is calling the remote service directly. When a call is made on the proxy object, it is actually sent to the stub object in the remote process.

### 注意事项

1. 一定要记得server侧在中进行注册


### process

1. server侧写一个`Service`，在`onBind()`方法中返回`server impl`

   ```kotlin
   public IBinder onBind(Intent intent) { return new CalculatorServer(); }
   ```

2. server在`AndroidManifest.xml`将这个`Service`对外暴露，并启动起来

   ```xml
   <!--AndroidManifest.xml-->
   <service
       android:name="com.binder.service.CalculatorService"
       android:enabled="true"
       android:exported="true" />
   ```

3. client通过包名，用启动四大组件的方式连接这个`Service`，`Service`返回`server impl`的内存地址，静态类型为`IBinder`

   ```java
   public void connect(Context context) {
     Intent intent = new Intent();
     // 通过包名的方式启动Service
     intent.setComponent(new ComponentName("com.binder","com.binder.CalculatorService"));
     context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
   }
   
   private ServiceConnection mConnection = new ServiceConnection() {
     @Override
     public void onServiceConnected(ComponentName name, IBinder service) {
       // 这个回调的参数service就是server侧impl的内存地址，静态类型为IBinder
       mCalculator = ICalculator.Stub.asInterface(service);
     }
   };
   ```

4. 将`server impl`进行一层包装，包成了`ICalculator.Stub.Proxy`类。这是`ICalculator.Stub.asInterface(service)`这一行实现的

   ```java
   public static ICalculator asInterface(android.os.IBinder obj) {
       // 先本地查返回，查不到返回Proxy
       return new ICalculator.Stub.Proxy(obj);
   }
   ```

5. client侧调用接口方法，最后走到`mRemote#onTransact()`

   ```java
   private static class Proxy implements ICalculator {
     private android.os.IBinder mRemote; // server impl的内存地址
     public int add(int a, int b) {
       // _data, _reply分别是in和out Parcel
       _status = mRemote.transact(Stub.TRANSACTION_add, _data, _reply, 0);
     }
   }
   ```

6. `server impl` 真正 run方法

   ```java
   abstract class Stub extends android.os.Binder implements ICalculator {
     public boolean onTransact() {
       _arg1 = data.readInt();
       int _result = this.add(_arg0, _arg1); // 真正调用
       reply.writeNoException();
     }
   }
   
   public class CalculatorServer extends ICalculator.Stub {
     public int add(int a, int b) throws RemoteException { return a + b; }
   }
   ```

### features

1. `asInterface()`方法，如果当前进程有就返回当前进程的，就不用跨进程通信了；如果当前进程没有，才返回一个Proxy来跨进程通信

   ```java
   public static ICalculator asInterface(android.os.IBinder obj) {
     if ((obj == null)) {
       return null;
     }
     // 根据DESCRIPTOR key找到
     android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
     if (((iin != null) && (iin instanceof ICalculator))) {
       return ((ICalculator) iin);
     }
     // 返回Proxy
     return new ICalculator.Stub.Proxy(obj);
   }
   ```

3. `onTransact()`方法会运行在服务端的一个线程池中，因此即使它很耗时，也应该用同步的方式去实现；但RPC返回耗时久，客户端不能放在UI线程执行

***

## bitmap_drawable_canvas

### dp -> pix

```kotlin
fun dp2px(context: Context, dp: Int): Float =
	TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics)
```

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

  1. `public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme)` -> 每一个子类`Drawable`都应该去实现，规定了从`xml`如何创建一个对应的`Drawable`

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

  2. `public void setBounds(int left, int top, int right, int bottom)` -> 当在`canvas`进行`draw`时，规定好位置和区域。即决定了此`Drawable`被绘制在`canvas`的那个位置以及绘制多大。注意它不是决定`drawable`那部分被`draw`，而是决定`canvas`那部分来`draw`整个`drawable`

  3. `public abstract void draw(@NonNull Canvas canvas)` -> 如何把这个`drawable`绘制到`convas`上，这依赖每个`Drawable`去自己实现

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

* `draw something`的四个必备要素
  
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

* draw Circle example 

  ```kotlin
  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    val point = dp2px(context, 100)
    val radius = dp2px(context, 50)
    val paint = Paint().apply {
      color = resources.getColor(R.color.green, null)
      isAntiAlias = true
    }
    canvas?.drawCircle(point, point, radius, paint)
  }
  ```

### mutual conversion

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

## boot_receiver

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

## bottom_sheet_dialog

### layout

* design_bottom_sheet_dialog.xml

  ```xml
  <!--design_bottom_sheet_dialog.xml-->
  <FrameLayout android:id="@+id/container">
    <androidx.coordinatorlayout.widget.CoordinatorLayout
        android:id="@+id/coordinator">
      <View android:id="@+id/touch_outside" />
      <FrameLayout android:id="@+id/design_bottom_sheet"/>
    </androidx.coordinatorlayout.widget.CoordinatorLayout>
  </FrameLayout>       
  ```

* 核心是`CoordinatorLayout`，实现在`com.google.android.material.bottomsheet.BottomSheetDialog`

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

## compose

* 不需要笔记，直接看代码吧

***

## constrain_layout

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

## coroutines

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

## drawable_animate

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

## elevation

### 是什么

* 是`View`三维的一个度量

### 注意事项

* 必须要有`background`的前提下设置了`elevation`才有用

### 高度就是高度

* `FrameLayout`中栈底的`View`会因为有`elevation`值而到栈顶

***

## eventbus

* 概述

  * 就是一个耦合度极低的观察者模式的框架

* [Analysis of EventBus principle](https://programmer.group/analysis-of-eventbus-principle.html)

* 四种threadMode模式

  * `POSTING`：在`Observable`的线程执行
  * `Main`：`Observer`方法在`Main`(UI线程)执行
    * 如果`Observable`是在`Main`线程发出post。那么`Observer`立即执行，导致`Observerable`被阻塞
    * 如果`Observable`不在`Main`线程发出post，那么所有post构成一个队列，依次执行，`Observable`不会被阻塞
  * `MAIN_ORDERED`：post总是在一个队列里，`Observable`永远不会被阻塞
  * `BACKGROUND`
    * 如果`Observable`是在`Main`线程发出post。那么事件被**队列化**安排到一条固定的`Backgroud`线程执行，有可能会阻塞`backgroud`线程
    * 如果被`Observable`不是在`Main`线程发出post。那么任务队列就直接在发出post的那条线程执行
  * `ASYNC`：既不在`Main`线程执行，也不在`Observable`的post线程执行。EventBus有一个线程池

* 源码解析

  ```java
  public class EventBus {
    public void register(Object subscriber) {
      //查找订阅方法
      List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
      //将方法进行订阅
      synchronized (this) {
        for (SubscriberMethod subscriberMethod : subscriberMethods) {
          subscribe(subscriber, subscriberMethod);
        }
      }
    }
    
    public void post(Object event) {
      //将事件抛出，调用postSingleEvent方法
      postSingleEvent(eventQueue.remove(0), postingState);
    }
    
    private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
      synchronized (this) {
        //通过抛出的事件找到订阅方法
        subscriptions = subscriptionsByEventType.get(eventClass);
      }
      for (Subscription subscription : subscriptions) {
        //将事件处理传递给postToSubscription方法
        postToSubscription(subscription, event, postingState.isMainThread); 
      }
    }
    
    private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
      switch (subscription.subscriberMethod.threadMode) {
        //区分注解threadMode
        case POSTING:
          invokeSubscriber(subscription, event);
          break;
        case MAIN:
          //将事件处理传递给invokeSubscriber
          invokeSubscriber(subscription, event);
          break;
        case ASYNC:
          asyncPoster.enqueue(subscription, event);
          break;
      }
    }
    
    void invokeSubscriber(Subscription subscription, Object event) {
      //直接反射开始执行
      subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
    }
  }
  ```

* `sticky event`

  1. 参考链接：[Sticky Events](https://greenrobot.org/eventbus/documentation/configuration/sticky-events/)
  2. 概念：当使用`EventBus.getDefault().postSticky()`抛出一个事件时，这个事件就是`sticky event`。内存中始终会存储最近抛出的一个`sticky event`
  3. 作用：一旦注册了EventBus，即`EventBus.getDefault().register(this)`，它`@Subscribe(sticky = true)`的方法就会看是否有这个已经抛出过被存储在内存中的`sticky event`，一旦有则立刻执行`@Subscribe`的方法体
  4. 关键API
     * EventBus.getDefault().postSticky(stickyEvent)

***

## fresco

* 参考文档

  * [Fresco架构设计赏析](https://juejin.cn/post/6844903784460582926)

* 重要构成

  * `DraweeView`

    1. 官方解释 -> `View that displays a DraweeHierarchy.`

    2. 继承`ImageView`，但它的接口别用，未来会考虑直接继承`View`。唯一交集: 利用`ImageView`来显示`Drawable`

    3. 持有`DraweeHolder`对象

       ```java
       public class DraweeView extends ImageView {
         private DraweeHolder<DH> mDraweeHolder;
       }
       ```

  * `DraweeHolder`

    * 官方解释 -> `A holder class for Drawee controller and hierarchy.`

    * 持有`DraweeController`和`DraweeHierarchy`

      ```java
      public class DraweeHolder {
        @Nullable private DH mHierarchy;
        private DraweeController mController = null;
      }
      ```

  * `DraweeHierachy`

    * `Draweable`的容器，从`BACKGROUND -> OVERLAY`一共包含7层`Drawable`

      ```java
      public class GenericDraweeHierarchy {
        private static final int BACKGROUND_IMAGE_INDEX = 0;
        private static final int PLACEHOLDER_IMAGE_INDEX = 1;
        private static final int ACTUAL_IMAGE_INDEX = 2;
        private static final int PROGRESS_BAR_IMAGE_INDEX = 3;
        private static final int RETRY_IMAGE_INDEX = 4;
        private static final int FAILURE_IMAGE_INDEX = 5;
        private static final int OVERLAY_IMAGES_INDEX = 6;
      }
      ```

  * `DraweeController` 

    * 控制图片的加载，请求，并根据不同事件控制`Hierarchy`

    * 持有`DraweeHierarchy`

      ```java
      public abstract class AbstractDraweeController {
        private SettableDraweeHierarchy mSettableDraweeHierarchy;
      }
      ```

  * `ImagePipline` - 顾名思义

* 图片加载简要流程

  1. `Controller`将请求任务委托给`DataSource`，在`DataSource`内注册一个请求结果的回调 -> `DataSubscriber`

  2. `DataSource`通过经过一系列`Producer`委托责任链处理最终获得`result`，调用到`DataSubscriber`的方法

  3. 将`result`传递给`Hierachy`

  4. `DraweeView`将`Hierachy`的`topLevelDrawable`取出来展示

* 与加载相关的一些关键接口

  1. `DataSource`

     ```java
     public interface DataSource<T> {
       //获取结果
       T getResult();
       //查询状态
       boolean isFinished();
       boolean hasFailed();
       //注册回调，类似于RxJava
       void subscribe(DataSubscriber<T> dataSubscriber, Executor executor);
     }
     ```

  2. `DataSubscirber`

     ```java
     public interface DataSubscriber<T> {
       //成功回调
       void onNewResult(@Nonnull DataSource<T> dataSource);
       //失败回调
       void onFailure(@Nonnull DataSource<T> dataSource);
       //取消回调
       void onFailure(@Nonnull DataSource<T> dataSource);
     }
     ```

  3. `Producer`

     ```java
     public interface Producer<T> {
       // 产生数据，并通知consumer消费
       void produceResults(Consumer<T> consumer, ProducerContext context);
     }
     ```

  4. `Consumer`

     ```java
     public interface Consumer<T> {
       //实际上就是一个回调
       void onNewResult(@Nullable T newResult, @Status int status);
       void onFailure(Throwable t);
       void onCancellation();
     }
     ```

* 图片加载详细流程

  1. 设置`controller`，订阅`dataSourceSubsciber`

     ```java
     //DraweeView#setController
     public void setController(draweeController) {
       //将设置controller委托给mDraweeHolder
       mDraweeHolder.setController(draweeController);
     }
     
     //DraweeHolder#setController
     public void setController(draweeController) {
       mController = draweeController;
       if (wasAttached) {
         //尝试进行attach
         attachController();
       }
     }
     private void attachController() {
       //调用controller的attach
       mController.onAttach();
     }
     
     //AbstractDraweeController
     public void onAttach() {
       if (!mIsRequestSubmitted) {
         submitRequest();
       }
     }
     protected void submitRequest() {
       final T closeableImage = getCachedImage();
       
       if (closeableImage != null) {
         //有缓存(代码只找了内存缓存)，根本不用请求，直接return
         return
       }
       
       //内存缓存没有就通过mDataSource进行请求
       DataSubscriber<T> dataSubscriber = new BaseDataSubscriber<T>() {
         public void onNewResultImpl(DataSource<T> dataSource) {
           //...
         }
         public void onFailureImpl(DataSource<T> dataSource) {
           //...
         }
         public void onProgressUpdate(DataSource<T> dataSource) {
           //...
         }
       }
       mDataSource.subscribe(dataSubscriber, mUiThreadImmediateExecutor);
     }
     ```

     * `controller`进行`attach`有两条路径
       1. 当进行赋值设置`controller`时会把`controller`给`attach`
       2. 当`DraweeView#onAttachedToWindow()`时也会尝试将当前已赋值的`controller`进行`attach`
       3. `detach`同理

  2. `DataSource`获取、首个`Producer`获取并注入`DataSource`

     ```java
     //AbstractDraweeController
     protected void submitRequest() {
       //非常核心的一行，去获取dataSource
     	mDataSource = getDataSource();
     }
     
     //从AbstractDraweeController#getDataSource()开始调用，会调到ImagePipeline#fetchDecodedImage()
     //ImagePipeline
     public DataSource<CloseableReference<CloseableImage>> fetchDecodedImage() {
       //首个Producer获取，一般是BitmapMemoryCacheProducer
       Producer<CloseableReference<CloseableImage>> producerSequence =
               mProducerSequenceFactory.getDecodedImageProducerSequence(imageRequest);
       return submitFetchRequest(producerSequence, ...)
     }
     
     private <T> DataSource<CloseableReference<T>> submitFetchRequest() {
       //将首个Producer给传进去，new一个CloseableProducerToDataSourceAdapter
       return CloseableProducerToDataSourceAdapter.create(producerSequence, settableProducerContext);
     }
     ```

  3. 首个`Producer`开始执行`produceResults`，并注册`Consumer`

     ```java
     //CloseableProducerToDataSourceAdapter
     private CloseableProducerToDataSourceAdapter() {
       //构造函数直接走到super，super是AbstractProducerToDataSourceAdapter
       super(producer, settableProducerContext, listener);
     }
     
     //AbstractProducerToDataSourceAdapter
     protected AbstractProducerToDataSourceAdapter() {
       //开始调用producer.produceResults了，并且注册了回调createConsumer()
       //这里的producer是BitmapMemoryCacheProducer
       producer.produceResults(createConsumer(), settableProducerContext);
     }
     
     private Consumer<T> createConsumer() {
       return new BaseConsumer<T>() {
         protected void onNewResultImpl(T newResult) {
           //注册的回调就是给dataSource的实现类result赋值，这样接口方法`T getResult();`才能返回
           AbstractProducerToDataSourceAdapter.this.onNewResultImpl(newResult);
         }
       }
     }
     ```

* 各种各种的`Producer`

  1. `BitmapMemoryCacheProducer`

     * 尝试从内存缓存中找`Bitmap`

     * 包装`consumer`，将`result`存入`mMemoryCache`

       ```java
       //内存缓存容器
       private final MemoryCache<CacheKey, CloseableImage> mMemoryCache;
       //cacheKey容器
       private final CacheKeyFactory mCacheKeyFactory;
       //下一个Producer -> ThreadHandoffProducer
       private final Producer<CloseableReference<CloseableImage>> mInputProducer;
       
       public void produceResults(Consumer<CloseableReference<CloseableImage>> consumer, ProducerContext context) {
         //...代码太多了，讲一下代码的逻辑
         
         //看下isBitmapCacheEnabled，开启就从根据key从内存缓存map里找，cacheKey默认是图片url，找到的话就执行回调然后返回，如下图
         if(cachedReference != null) {
           consumer.onNewResult(cachedReference);
           return;
         }
         
         //没找到就将请求委托给mInputProducer
         mInputProducer.produceResults(wrappedConsumer, producerContext);
       }
       
       //包装consumer
       protected Consumer<CloseableReference<CloseableImage>> wrapConsumer() {
         return new DelegatingConsumer {
           public void onNewResultImpl() {
             //是否支持写入缓存
             if (isBitmapCacheEnabledForWrite) {
               //写入缓存
               newCachedResult = mMemoryCache.cache(cacheKey, newResult);
             }
             //写入缓存后才执行原来的consumer的onNewResult()
             getConsumer().onNewResult()
           }
         }
       }
       ```

  2. `ThreadHandoffProducer` -> 不找，将任务委托到非UI线程

     ```java
     //下一个Producer -> BitmapMemoryCacheKeyMultiplexProducer(父类是MultiplexProducer)
     private final Producer<T> mInputProducer;
     //一个queue，里面有ThreadPoolExecutor
     private final ThreadHandoffProducerQueue mThreadHandoffProducerQueue;
     
     public void produceResults(Consumer<T> consumer, ProducerContext context) {
       // new了一个runnable出来，在这个runnable内会将图片请求委托给mInputProducer
     	Runnable<T> runnable = new StatefulProducerRunnable {
         protected void onSuccess(@Nullable T ignored) {
           mInputProducer.produceResults(consumer, context);
         }
       }
       //将runnable添加到异步线程池里面等待执行
       mThreadHandoffProducerQueue.addToQueueOrExecute(runnable);
     }
     ```

  3. `MultiplexProducer` -> 不找，Producer for combining multiple identical requests into a single request.

     ```java
     //下一个Producer -> BitmapMemoryCacheProducer
     private final Producer<T> mInputProducer;
     
     public void produceResults(Consumer<T> consumer, ProducerContext context) {
       //组合的过程比较复杂，与请求过程关系不大，先跳过不看了
       mInputProducer.produceResults(forwardingConsumer, multiplexProducerContext);
     }
     ```

  4. `BitmapMemoryCacheProducer` -> 又找了一次`Bitmap`内存缓存，简直离谱

     ```java
     //下一个Producer -> DecodeProducer
     private final Producer<CloseableReference<CloseableImage>> mInputProducer;
     
     public void produceResults(Consumer<CloseableReference<CloseableImage>> consumer, ProducerContext context) {
       //没找到就将请求委托给mInputProducer
       mInputProducer.produceResults(wrappedConsumer, producerContext);
     }
     ```

  5. `DecodeProducer` -> 将解码任务封在`consumer`里往下传递

     ```java
     public class DecodeProducer {
       public void produceResults() {
         ProgressiveJpegParser jpegParser = new ProgressiveJpegParser(mByteArrayPool);
         progressiveDecoder =
                 new NetworkImagesProgressiveDecoder(consumer,jpegParser);
         mInputProducer.produceResults(progressiveDecoder)
       }
     }
     ```

  6. `ResizeAndRotateProducer` 

     * Resizes and rotates images according to the EXIF orientation data or a specified rotation angle. 

     * 包一层`consumer`进行`resize、rotate`

       ```java
       public class ResizeAndRotateProducer {
         public void produceResults(final Consumer<EncodedImage> consumer, final ProducerContext context) {
           mInputProducer.produceResults(
               new TransformingConsumer(consumer, context, mIsResizingEnabled, mImageTranscoderFactory),
               context);
         }
       }
       ```

  7. `AddImageTransformMetaDataProducer` ->

     * Add image transform meta data producer. 

     * 包一层`consumer`

       ```java
       public class AddImageTransformMetaDataProducer {
         public void produceResults(Consumer<EncodedImage> consumer, ProducerContext context) {
           mInputProducer.produceResults(new AddImageTransformMetaDataConsumer(consumer), context);
         }
       }
       ```

  8. `EncodedMemoryCacheProducer` 

     * 从内存缓存中找`encoded image`
     * 包一层`consumer`将结果写入`mMemoryCache`

  9. `DiskCacheReadProducer` -> 从磁盘中找

  10. `DiskCacheWriteProducer` -> 仅仅是包了个`consumer`用于存磁盘缓存

  11. `NetworkFetchProducer`

* 网络数据转成图片

  1. 把网络流转换为`EncodeImage`

     ```java
     public class NetworkFetchProducer {
       protected static void notifyConsumer(PooledByteBufferOutputStream pooledOutputStream) {
         encodedImage = new EncodedImage(result);
       }
     }
     ```

  2. 决定图片的类型

     ```java
     public class DefaultImageFormatChecker {
       public final ImageFormat determineFormat(byte[] headerBytes, int headerSize) {
         if (isJpegHeader(headerBytes, headerSize)) {
           return DefaultImageFormats.JPEG;
         }
     
         if (isPngHeader(headerBytes, headerSize)) {
           return DefaultImageFormats.PNG;
         }
       }
     }
     ```

  3. 将`EncodeImage`变为`Bitmap`

     ```java
     public abstract class DefaultDecoder {
       private CloseableReference<Bitmap> decodeFromStream() {
         //EncodedImage只是包含所有信息，没有被解码。可以从中获取流
         InputStream in = encodedImage.getInputStream();
         //调用系统的方法，将流变成Bitmap。系统方法会调用到native
         Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream, null, options);
       }
     }
     ```

  4. 将`Bitmap`变成`CloseableStaticBitmap`

     ```java
     public class DefaultImageDecoder {
       public CloseableStaticBitmap decodeJpeg() {
         //先从`EncodeImage`变为`Bitmap`
         CloseableReference<Bitmap> bitmapReference = ...;
         //在从`Bitmap`变成`CloseableStaticBitmap`
          CloseableStaticBitmap closeableStaticBitmap = new CloseableStaticBitmap(bitmapReference);
       }
     }
     ```

  5. 将`CloseableStaticBitmap`变成`BitmapDrawable`

     ```java
     public class DefaultDrawableFactory {
       public Drawable createDrawable(CloseableImage closeableImage) {
         Drawable bitmapDrawable =
                 new BitmapDrawable(mResources, closeableStaticBitmap.getUnderlyingBitmap());
       }
     }
     ```

  6. 不同的中间物意义

    7. `PooledByteBufferOutputStream` - 这是网络流

    8. `EncodeImage` 

       * it is implemented a lightweight wrapper around an encoded byte stream.
       * Encoded image data is a compressed representation of an image that has been prepared for storage or transmission. 
       * Encoded images are generally smaller in size than decoded images because they have been compressed to reduce their file size. 
       * it cannot be directly displayed on a screen without first being decoded.
       * it takes up very little memory compared to the decoded image data. This allows Fresco to load and cache multiple images at once without using excessive memory.

    9. `DecodeImage`

       * Decoded image data  is the uncompressed representation of an image that can be directly displayed on a screen.
       * Decoded images are generally larger in size than their encoded counterparts because they contain all of the image data necessary for display, such as pixel values and color information.
       * Decoding an encoded image involves unpacking the compressed data and reconstructing the original image.
       * The process of decoding an encoded image typically involves reading the encoded image data from disk or network, decompressing the data, and constructing a DecodedImage object that represents the resulting bitmap or other format suitable for display.

    10. `Bitmap`

        * DecodedImage is a higher-level abstraction used by Fresco to manipulate and manage the image data, while Bitmap is a lower-level representation of the actual pixel data that can be displayed on a screen.

* 不同level drawable显示原理

  * `DraweeView`始终都只设置了一个`Drawable`

    ```java
    public void setHierarchy(DH hierarchy) {
      mDraweeHolder.setHierarchy(hierarchy);
      //设置drawable
      super.setImageDrawable(mDraweeHolder.getTopLevelDrawable());
    }
    
    public void setController(@Nullable DraweeController draweeController) {
      mDraweeHolder.setController(draweeController);
      //设置drawable
      super.setImageDrawable(mDraweeHolder.getTopLevelDrawable());
    }
    
    @Deprecated
    public void setImageDrawable(@Nullable Drawable drawable) {
      init(getContext());
      mDraweeHolder.setController(null);
      //过期的方式，别去用
      super.setImageDrawable(drawable);
    }
    ```

  * `ImageView#Drawable` = `getTopLevelDrawable()` = `RootDrawable` = `FadeDrawable`

    ```java
    public class GenericDraweeHierarchy {
      private final RootDrawable mTopLevelDrawable;
      
      GenericDraweeHierarchy() {
        mTopLevelDrawable = new RootDrawable(mFadeDrawable); //将fadeDrawable给包了一层
      }
      
      public Drawable getTopLevelDrawable() {
        return mTopLevelDrawable; //对外暴露的方法
      }
    }
    ```

  * `FadeDrawble`原理

    ```java
    //一个drawable容器
    private final Drawable[] mLayers;
    //动画时间
    int mDurationMs;
    //每个level(index)的透明度
    int[] mAlphas;
    //Determines whether to fade-out a layer to zero opacity (false) or to fade-in to the full opacity (true)
    boolean[] mIsLayerOn;
    //The index of the layer that contains the actual image 
    private final int mActualImageLayer;
    
    public FadeDrawable(Drawable[] layers, int actualImageLayer) {
      //赋一个值
      mLayers = layers;
      //初始化每一层的alpha
      mAlphas = new int[layers.length];
      //把真正显示的层给设置好
      mActualImageLayer = actualImageLayer;
    }
    
    //设置某一层的drawable可见
    public void fadeInLayer(int index) {
      mTransitionState = TRANSITION_STARTING; //设置drawing的state
      mIsLayerOn[index] = true; //将isOn设置成true
      invalidateSelf(); //请求redraw
    }
    
    //原理同上
    public void fadeOutLayer(int index) {
      mTransitionState = TRANSITION_STARTING;
      mIsLayerOn[index] = false;
      invalidateSelf();
    }
    
    //专门只显示某一层
    public void fadeToLayer(int index) {
      mTransitionState = TRANSITION_STARTING;
      Arrays.fill(mIsLayerOn, false);
      mIsLayerOn[index] = true;
      invalidateSelf();
    }
    
    //核心的draw
    public void draw(Canvas canvas) {
      switch (mTransitionState) {
        case TRANSITION_RUNNING:
          done = updateAlphas(ratio); //核心语句，draw时更新alpha
          //更新状态
          mTransitionState = done ? TRANSITION_NONE : TRANSITION_RUNNING;
          break;
      }
      for (int i = 0; i < mLayers.length; i++) {
        //上面alpha数组更新好后，遍历更新每一层drawable的alpha
        drawDrawableWithAlpha(canvas, mLayers[i], (int) Math.ceil(mAlphas[i] * mAlpha / 255.0));
      }
    }
    
    private boolean updateAlphas(float ratio) {
      for (int i = 0; i < mLayers.length; i++) {
        //更新数组内的alpha
        mAlphas[i] = (int) (mStartAlphas[i] + dir * 255 * ratio);
      }
    }
    ```

  * 总结

    1. 只有一个`RootDrawable`，但这个`RootDrawable`包装了`FadeDrawable`，`FadeDrawable`是一个`Drawable`数组容器
    2. 显示不同的层是通过设置`alpha`来控制`draw`实现的

* `PostProcessor`原理

  1. `new`一个 `PostprocessorProducer`，并将`BaseProducer`传进去当作下一个`inputProducer`

     ```java
     //ImagePipeline
     public DataSource<CloseableReference<CloseableImage>> fetchDecodedImage() {
       //这个入口会去获取首个producer
       Producer<CloseableReference<CloseableImage>> producerSequence =
               mProducerSequenceFactory.getDecodedImageProducerSequence(imageRequest);
       return submitFetchRequest(...)
     }
     
     //ProducerSequenceFactory
     public Producer getDecodedImageProducerSequence(imageRequest) {
       //基础的Producer
       Producer<> pipelineSequence = getBasicDecodedImageSequence(imageRequest);
       //如果有Postprocessor，就将producer包一层，将pipelineSequence作为下一个Producer
       if (imageRequest.getPostprocessor() != null) {
         //开始包一层
         pipelineSequence = getPostprocessorSequence(pipelineSequence);
       }
       //返回包了一层的Producer
       return pipelineSequence;
     }
     private Producer getPostprocessorSequence() {
       //将刚才的Producer当成inputProducer传进去
       PostprocessorProducer postprocessorProducer = mProducerFactory.newPostprocessorProducer(inputProducer);
     }
     ```

  2. `PostprocessorProducer`的处理实际上是把`Consumer`给包一层

     ```java
     //PostprocessorProducer
     public void produceResults() {
       //从ImageRequest中获取Processor
       Postprocessor postprocessor = context.getImageRequest().getPostprocessor();
       //将原来的Consumer用PostprocessorConsumer包一层
       PostprocessorConsumer postprocessorConsumer = new PostprocessorConsumer(consumer, listener, postprocessor, context);
       //啥都不干，直接让mInputProducer去produce图片。把包好的consumer给传进去
       mInputProducer.produceResults(postprocessorConsumer, context);
     }
     ```

  3. `PostprocessorConsumer`处理

     1. `DelegatingConsumer`

        * 以下代码示例如何将`Consumer`给包一层

        * 泛型表示这个`DelegateConsumer`的`输入`和`输出`

          ```java
          public abstract class DelegatingConsumer<I, O> extends BaseConsumer<I> {
            private final Consumer<O> mConsumer;
            public DelegatingConsumer(Consumer<O> consumer) {
              mConsumer = consumer;
            }
            public Consumer<O> getConsumer() {
              return mConsumer;
            }
            
            protected void onFailureImpl(Throwable t) { mConsumer.onFailure(t);}
          }
          ```

     2. `PostprocessorConsumer` -> 将内层`Consumer`给拦截住，处理结束后在通知内层`Consumer`

        ```java
        //PostprocessorConsumer
        private final Postprocessor mPostprocessor;
        //涉及异步线程操作了
        private final Executor mExecutor;
        
        //缓存、网络等producer返回结果了
        protected void onNewResultImpl(CloseableReference<CloseableImage> newResult, @Status int status) {
          //开始准备Postprocessing
          submitPostprocessing();
        }
        
        private void submitPostprocessing() {
          mExecutor.execute(
            new Runnable() {
              @Override
              public void run() {
                //在异步线程中执行Postprocessing
                doPostprocessing(closeableImageRef, status);
              }
            }
        }
        
        private void doPostprocessing(CloseableReference<CloseableImage> sourceImageRef) {
          //核心处理的三行代码
          CloseableStaticBitmap staticBitmap = (CloseableStaticBitmap) sourceImage;
          Bitmap sourceBitmap = staticBitmap.getUnderlyingBitmap();
          CloseableReference<Bitmap> bitmapRef = mPostprocessor.process(sourceBitmap, mBitmapFactory);
          //处理后的Bitmap
          destImageRef = new CloseableStaticBitmap(bitmapRef);
          //获取内容的consumer，然后将新图片通知给内层consumer
          getConsumer().onNewResult(destImageRef, status);
        }
        ```

***

## fresco_mask

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

## gesture_detector

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

## gl_surface_view

* 详见`Video`

***

## gson

### RFC

* [RFC7159 - The JavaScript Object Notation (JSON) Data Interchange Format](https://www.ietf.org/rfc/rfc7159.txt)

### JsonReader

* what for? - reads a json encoded value as a stream of tokens.

* feature 

  * the tokens are traversed in deepth-first order.
  * it is a recursive descent parser.

* code

  ```java
  public class JsonReader implements Closeable {
    public void beginArray() {}
    public void endObject() {}
    public boolean hasNext() {}
    public JsonToken peek() {}
    public String nextName() {}
    public String nextString() {}
  }
  ```

### TypeAdapter

* what for? - Converts Java objects to and from JSON.

* code

  ```java
  public abstract class TypeAdapter<T> {
    // Writes one JSON value (an array, object, string, number, boolean or null) for value.
    public abstract void write(JsonWriter out, T value);
    
    // Reads one JSON value and converts it to a Java object. Returns the converted object.
    public abstract T read(JsonReader in);
  }
  ```

* NumberTypeAdapter.java

  ```java
  public final class NumberTypeAdapter extends TypeAdapter<Number> {
    public Number read(JsonReader in) {
      JsonToken jsonToken = in.peek();
      switch (jsonToken) {
      case NUMBER:
      case STRING:
        return toNumberStrategy.readNumber(in);
      default:
        throw new JsonSyntaxException("Expecting number, got: ");
      }
    }
    
    public void write(JsonWriter out, Number value) {
      out.value(value);
    }
  }
  ```

* CollectionTypeAdapter.java

  ```java
  class Adapter<E> extends TypeAdapter<Collection<E>> {
    // adapter for handling element from/to json
    private final TypeAdapter<E> elementTypeAdapter;
    // use this to new a Collection
    private final ObjectConstructor<? extends Collection<E>> constructor;
    
    public Collection<E> read(JsonReader in) throws IOException {
      Collection<E> collection = constructor.construct();
      in.beginArray();
      while (in.hasNext()) {
        E instance = elementTypeAdapter.read(in);
        collection.add(instance);
      }
      in.endArray();
      return collection;
    }
    
    public void write(JsonWriter out, Collection<E> collection) {
      out.beginArray();
      for (E element : collection) {
        elementTypeAdapter.write(out, element);
      }
      out.endArray();
    }
  }
  ```

* FieldReflectionAdapter.java

  ```java
  FieldReflectionAdapter
    - ObjectConstructor: com.gson.Staff
  	- boundFields:
  		"age" to TypeAdapter,
  		"name" to TypeAdapter,
  		"position" to TypeAdapter,
  		"salary" to TypeAdapter,
  		"skills" to TypeAdapter,
  ```

### ConstructorConstructor

* code

  ```java
  public <T> ObjectConstructor<T> get(TypeToken<T> typeToken) {
    // user inject
    InstanceCreator<T> typeCreator = (InstanceCreator<T>) instanceCreators.get(type);
    // for specical collections
    ObjectConstructor<T> specialConstructor = newSpecialCollectionConstructor(type, rawType);
    // default constructor
    ObjectConstructor<T> defaultConstructor = newDefaultConstructor(rawType, filterResult);
    // for special collections
    ObjectConstructor<T> defaultImplementation = newDefaultImplementationConstructor(type, rawType);
    // use unsafe
    newUnsafeAllocator(rawType);
  }
  ```

***

## haptic

### 如何触发震动

1. 在`AndroidManifest.xml`中声明需要震动权限

2. 使用`getSystemSerivce()`

   ```kotlin
   (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createOneShot(300L, 100))
   ```

***

## life_cycle_owner

* 概述

  * 本质上还是一个观察者模式
  * `Observable`是本身拥有生命周期的`Activity`、`Fragment`
  * `Observer`是自定义的

* 定义`Observer`

  ```kotlin
  class MyLifecycleObserver: LifecycleObserver {    
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)    
    fun onStart(){        
      Log.d("LifecycleObserver","onStart")    
    }    
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)    
    fun onStop(){        
      Log.d("LifecycleObserver","onStop")    
    }    
    
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)    
    fun onDestroy(){        
      Log.d("LifecycleObserver","onDestroy")    
    }
    
  }
  ```

  * `LifecycleObserver`接口没有任何方法
  * 使用`@OnLifecycleEvent`注解

* `Observable`订阅`Observer`

  ```kotlin
  class FirstFragment : Fragment() {    
    private lateinit var myLifecycleObserver: MyLifecycleObserver  
    
    override fun onCreate(savedInstanceState: Bundle?) {        
      super.onCreate(savedInstanceState)        
      myLifecycleObserver = MyLifecycleObserver(this)        
      lifecycle.addObserver(myLifecycleObserver)    
    }
  }
  ```

  * 首先实例化出一个`Observer`对象
  * 再将`Observer`对象添加进去

* 注

  * Android手动杀死进程。依旧会执行`onStop()、onDestroy()`方法

* 实现原理

  * `LifecycleOwner`接口的实现类在`androidx.activity.ComponentActivity`

    ```java
    public class ComponentActivity implements LifecycleOwner {
      //声明一个LifecycleRegistry
      private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
      
      protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //将生命周期的变化委托到ReportFragment
        ReportFragment.injectIfNeededIn(this);
      }
    }
    ```

  * `ReportFragment`生命周期依附`Activity`，这样可以无侵入`Activity`即可观察`Activity`的生命周期

    ```java
    public class ReportFragment extends android.app.Fragment {
      public static void injectIfNeededIn(Activity activity) {
        LifecycleCallbacks.registerIn(activity);
      }
      
      static class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        static void registerIn(Activity activity) {
          //给Activity注册一个ActivityLifecycleCallback，而这个Callback的实现如下所示
          activity.registerActivityLifecycleCallbacks(new LifecycleCallbacks());
        }
        
        @Override
        public void onActivityPostStarted(@NonNull Activity activity) {
          //将start事件diapatch出去
          dispatch(activity, Lifecycle.Event.ON_START);
        }
        
        @Override
        public void onActivityPostResumed(@NonNull Activity activity) {
          dispatch(activity, Lifecycle.Event.ON_RESUME);
        }
        
        @Override
        public void onActivityPreDestroyed(@NonNull Activity activity) {
          dispatch(activity, Lifecycle.Event.ON_DESTROY);
        }
      }
      
      static void dispatch(@NonNull Activity activity, @NonNull Lifecycle.Event event) {
        Lifecycle lifecycle = ((LifecycleOwner) activity).getLifecycle();
        //将事件交给了LifecycleRegistry，调用handleLifecycleEvent方法来处理
        ((LifecycleRegistry) lifecycle).handleLifecycleEvent(event);
      }
    }
    ```

    * 综上`ReportFragment`只是一个没有界面的`Fragment`，它的作用就是将`Activity`的生命周期引借出来。通过注册一个监听器来监听`Activity`的生命周期，在监听的处理中又将生命周期处理交还给了`Activity.mLifecycleRegistry`

  * `LifecycleRegistry`

    ```java
    public class LifecycleRegistry extends Lifecycle { 
      
      //mLifecycleOwner在这里是一个Activity
      private final WeakReference<LifecycleOwner> mLifecycleOwner;
      private FastSafeIterableMap<LifecycleObserver, ObserverWithState> mObserverMap = new FastSafeIterableMap<>();
      
      public void handleLifecycleEvent(@NonNull Lifecycle.Event event) {
        //处理生命周期只是调用了一下moveToState
        moveToState(event.getTargetState());
      }
      
      private void moveToState(State next) {
        mState = next;
        mHandlingEvent = true;
        //调用核心的sync方法
        sync();
        mHandlingEvent = false;
      }
      
      private void sync() {
        LifecycleOwner lifecycleOwner = mLifecycleOwner.get();
        backwardPass(lifecycleOwner);
      }
      
      private void backwardPass(LifecycleOwner lifecycleOwner) {
        //observer is get from `mObserverMap`, and it is an ObserverWithState
        Event event = Event.downFrom(observer.mState);
        observer.dispatchEvent(lifecycleOwner, event);
      }
      
      static class ObserverWithState {
        State mState;
        LifecycleEventObserver mLifecycleObserver;
        
        ObserverWithState(LifecycleObserver observer, State initialState) {
          //通过Lifecycling.lifecycleEventObserver()将LifecycleObserver转换为ReflectiveGenericLifecycleObserver
          mLifecycleObserver = Lifecycling.lifecycleEventObserver(observer);
          mState = initialState;
        }
        
        void dispatchEvent(LifecycleOwner owner, Event event) {
          mLifecycleObserver.onStateChanged(owner, event);
        }
      }
    }
    ```

  * `ReflectiveGenericLifecycleObserver`

    ```java
    //public interface LifecycleEventObserver extends LifecycleObserver
    class ReflectiveGenericLifecycleObserver implements LifecycleEventObserver {
      //这个mWrapped是LifecycleObserver
      private final Object mWrapped;
      private final CallbackInfo mInfo;
    
      ReflectiveGenericLifecycleObserver(Object wrapped) {
        mWrapped = wrapped;
        mInfo = ClassesInfoCache.sInstance.getInfo(mWrapped.getClass());
      }
    
      @Override
      public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Event event) {
        //这一行会通过反射执行目标方法
        mInfo.invokeCallbacks(source, event, mWrapped);
      }
    }
    ```

    * 还是同样一个思路，尽量不侵入用户自定义的`class MyLifeCycle: LifecycleObserver`类。而是通过继承的方式将方法引出来。

***

## live_data

* 概述

  * 也是一种观察者模式
  * 除了`观察者、被观察者`外多了一个第三者——`LifeCycleOwner`，用于控制观察者模式的时间域范围

* 特点

  * `LifecycleOwner`，一般就只能是`Activity`、`Fragment`，必须有`start、resume、stop`等方法
  * `Observer`必须实现`public interface Observer<T>`接口的`onChanged(T t)`方法
  * 不用手动处理生命周期，默认方式封装了只会在活跃生命周期内观察
  * 如果在不正常生命周期漏观察了变化，则在进入正常生命周期时刻会立即更新
  * 总是就是很好用很方便

* 观察

  ```java
  //LiveData.observe()
  public void observe(LifecycleOwner owner, Observer<? super T> observer) {}
  ```

* `LiveData`

  ```java
  public abstract class LiveData<T> {
    
    //observer容器
    private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers = new SafeIterableMap<>();
    
    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
  
      //包一层，将LifecycleOwner生命周期观察引借出来
      LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
      //存放入容器
      ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
      //LifecycleOwner观察wrapper
      owner.getLifecycle().addObserver(wrapper);
    }
    
    @MainThread
    protected void setValue(T value) {
      mVersion++;
      mData = value;
      //setValue调用到dispatchValue()
      dispatchingValue(null);
    }
    
    void dispatchingValue(@Nullable ObserverWrapper initiator) {
      for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =
           mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
        //调用considerNotify
        considerNotify(iterator.next().getValue());
      }
    }
    
    private void considerNotify(ObserverWrapper observer) {
      //调用observer的方法
      observer.mObserver.onChanged((T) mData);
    }
    
    class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
      //在lifecycleOwner的生命周期前提下进行观察者模式
      @Override
      public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        Lifecycle.State currentState = mOwner.getLifecycle().getCurrentState();
        if (currentState == DESTROYED) {
          removeObserver(mObserver);
          return;
        }
        Lifecycle.State prevState = null;
        while (prevState != currentState) {
          prevState = currentState;
          //因lifecycle生命周期发生改变而进行观察改变,实际调用ObserverWrapper.activeStateChanged
          activeStateChanged(shouldBeActive());
          currentState = mOwner.getLifecycle().getCurrentState();
        }
      }
    }
    
    private abstract class ObserverWrapper {
      void activeStateChanged(boolean newActive) {
        //调用dispatchValue
        dispatchingValue(this);
      }
    }
  }
  ```

***

## measure_layout_draw

### purpose

* `measure`：determine the size requirements of a view before it is drawn on the screen. 

* `layout`：position the view on the screen.

* `draw`：draw the view on the screen.

### core method

* `void setMeasuredDimension(int measuredWidth, int measuredHeight)`
  *  This method must be called by onMeasure(int, int) to store the measured width and measured height.
* `boolean setFrame(int left, int top, int right, int bottom)` 
  * Assign a size and position to this view.
  * This is called from layout.
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

## motion_event_dispatch

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

### 点击移开

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

### 滑动冲突

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

## negative_margin

### margin

* `margin`的值可以为负，为负表示画在父View的外部

### clipChildren

* `Defines whether a child is limited to draw inside of its bounds or not.`

***

## notification

### 基础知识

* 管理和通信
  * Notice由NotificationManager管理，Widget由AppWidgetManager管理
  * NotificationManager和AppWidgetManager通过Binder和SysterServer进程中的NotificationService和AppWidgetManagerService通信
  * 视图是在SystemServer进程的xxxService中被加载的
* 采用Action
  * 理论上RemoteView可以支持View的所有操作，但那样跨进程开销太大，而且设计复杂
  * 因此目前采用Action来跨进程更新UI

### PendingIntent

* 源码

  ```java
  public final class PendingIntent implements Parcelable {
    private final IIntentSender mTarget;
    
    public static PendingIntent getActivityAsUser() {
      IIntentSender target = ActivityManager.getService().getIntentSenderWithFeature();
      eturn target != null ? new PendingIntent(target) : null;
    }
    
    public PendingIntent(IIntentSender target) {
      mTarget = Objects.requireNonNull(target);
    }
    
    public PendingIntent(IBinder target, Object cookie) {
      mTarget = IIntentSender.Stub.asInterface(target);
      mWhitelistToken = (IBinder)cookie;
    }
  }
  ```

  ```java
  // IIntentSender.aidl
  interface IIntentSender {
      void send(int code, in Intent intent, String resolvedType, in IBinder whitelistToken,
              IIntentReceiver finishedReceiver, String requiredPermission, in Bundle options);
  }
  ```

  * PendingIntent 仅仅是持有`IIntentSender`引用而已
  * 实际的`IItentSender`实例是由操作系统控制的
  * `IIntentSender`是通过`AMS`获取的

* 注释

  * A PendingIntent itself is simply a reference to a token maintained by the system describing the original data used to retrieve it.
  * If the creating application later re-retrieves the same kind of PendingIntent， it will receive a PendingIntent representing the same token if that is still valid, and can thus call cancel to remove it.

* [stack over flow](https://stackoverflow.com/questions/9583230/what-is-the-purpose-of-intentsender)

  * Instances of `IIntentSender` can not be made directly, but rather must be created from an existing `PendingIntent` with `PendingIntent.getIntentSender()`.
  * As for a `PendingIntent`, it's basically a token that you give to another application which allows that application to use your app's permissions to execute a specific piece of your app's code.

### RemoteView

* 跨进程通信

  ```java
  public class RemoteViews implements Parcelable {
    public ApplicationInfo mApplication;
    private int mLayoutId;
    
    public RemoteViews(String packageName, int layoutId) {
      // 构造参数核心就是给两个变量赋值
      mApplication = application;
      mLayoutId = layoutId;
    }
    
    public void writeToParcel(Parcel dest, int flags) {
      // 写入Parcel传递给远端进程
      mApplication.writeToParcel(dest, flags);
      dest.writeInt(mLayoutId);
    }
    
    public static final Parcelable.Creator<RemoteViews> CREATOR = new Parcelable.Creator<RemoteViews>() {
      public RemoteViews createFromParcel(Parcel parcel) {
        // 远端进程从Parcel读构造RemoteView
        return new RemoteViews(parcel);
      }
    }
    
    public RemoteViews(Parcel parcel) {
      // 远端进程从Parcel读构造RemoteView
      mApplication = ApplicationInfo.CREATOR.createFromParcel(parcel);
      mViewId = parcel.readInt();
    }
  }
  ```

* 远端构造View

  ```java
  // 加载布局并更新界面
  public View apply(Context context, ViewGroup parent) {
    RemoteViews rvToApply = getRemoteViewsToApply(context, size);
  	// 通过LayoutInflater.from直接常规构造一个View
    View result = inflateView(context, rvToApply, parent);
    // 开始performApply
    rvToApply.performApply(result, parent, handler, null);
    return result;
  }
  
  // 只更新界面
  private void performApply(View v, ViewGroup parent) {
    for (int i = 0; i < count; i++) {
      Action a = mActions.get(i);
      // apply就是执行所有反射执行所有更新的方法
      a.apply(v, parent, handler, colorResources);
    }
  }
  
  private void reapply(Context context, View v) {
    RemoteViews rvToApply = getRemoteViewsToReapply(context, v, size);
    rvToApply.performApply(v, (ViewGroup) v.getParent());
  }
  ```

  ```java
  // Widget的parent容器，运行在远端进程
  public class AppWidgetHostView extends FrameLayout {
    // 更新Widget界面
    public void updateAppWidget(RemoteViews remoteViews) {
      RemoteViews rvToApply = remoteViews.getRemoteViewsToApply(mContext, mCurrentSize);
      // 调用reapply
      rvToApply.reapply(mContext, mView);
    }
  }
  ```

* 更新View

  ```java
  public void setImageViewResource(int viewId, int srcId) {
    setInt(viewId, "setImageResource", srcId);
  }
  
  public void setInt(int viewId, String methodName, int value) {
    // 仅仅是加了一个ReflectionAction
    addAction(new ReflectionAction(viewId, methodName, BaseReflectionAction.INT, value));
  }
  
  private void addAction(Action a) {
    mActions.add(a);
  }
  
  // 一个Action是可以跨进程通信的
  private abstract static class Action implements Parcelable {}
  
  abstract class BaseReflectionAction extends Action {
    // 真正改视图的地方，在远端执行
    public final void apply(View root, ViewGroup rootParent) {
      // 远端进程就是一个普通的View，直接findViewById找到这个View
      final View view = root.findViewById(viewId);
      // 然后反射执行方法改变视图
      getMethod(view, this.methodName, param, false).invoke(view, value);
    }
  }
  
  private final class ReflectionAction extends BaseReflectionAction {
    public void writeToParcel(Parcel out, int flags) {
      // 一样的，写入Parcel传给远端进程
    }
    ReflectionAction(Parcel in) {
      // 远端进程通过Parcel构造Action
    }
  }
  ```

* 点击实现

  ```java
  public void setOnClickPendingIntent(int viewId, PendingIntent pendingIntent) {
    // 将pendingIntent包成了RemoteResponse
    // 将response包成SetOnClickResponse
    RemoteResponse resp = RemoteResponse.fromPendingIntent(pendingIntent);
    addAction(new SetOnClickResponse(viewId, response));
  }
  
  public static class RemoteResponse {
    private PendingIntent mPendingIntent;
    // 封装了PendingIntent跨进程通信
    private void writeToParcel(Parcel dest, int flags) {
      PendingIntent.writePendingIntentOrNullToParcel(mPendingIntent, dest);
    }
    private void readFromParcel(Parcel parcel) {
      mPendingIntent = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
    }
  }
  
  private class SetOnClickResponse extends Action {
    public void apply(View root, ViewGroup rootParent) {
      final View target = root.findViewById(viewId);
      // 调用setOnClickListener
      target.setOnClickListener(v -> mResponse.handleViewInteraction(v, handler));
    }
  }
  ```

### RemoteView IPC

* put into intent

  ```java
  public class Intent implements Parcelable, Cloneable {
    // as RemoteView has implmented Parcelable interface, so it can be passed as the second parameter
    public Intent putExtra(String name, Parcelable value) {
      mExtras.putParcelable(name, value);
      return this;
    }
  }
  ```

* get from intent

  ```java
  public class Intent implements Parcelable, Cloneable {
    public <T> T getParcelableExtra(String name, Class<T> clazz) {
      return mExtras == null ? null : mExtras.getParcelable(name, clazz);
    }
  }
  ```

* apply

  ```java
  public class RemoteViews implements Parcelable {
    // Inflates the view hierarchy represented by this object and applies all of the actions.
    public View apply(Context context, ViewGroup parent) {
      View result = inflateView(context, rvToApply, parent);
      rvToApply.performApply(result, parent, handler, null);
      return result;
    }
  }
  ```


***

## okhttp

### Request

* An HTTP request.

  ```kotlin
  class Request internal constructor(
    @get:JvmName("url") val url: HttpUrl,
    @get:JvmName("method") val method: String,
    @get:JvmName("headers") val headers: Headers,
    @get:JvmName("body") val body: RequestBody?,
    internal val tags: Map<Class<*>, Any>
  )
  ```

### OkHttpClients

* what is? -> Factory for calls, which can be used to send HTTP requests and read their responses.

* features

  1. OkHttpClients Should Be Shared. 
  2. each client holds its own connection pool and thread pools. Reusing connections and threads reduces latency and saves memory.

* code

  ```kotlin
  open class OkHttpClient internal constructor(
    builder: Builder
  ) {
    // specifies the thread of execution
    val dispatcher: Dispatcher
    // HTTP requests that share the same Address may share a Connection.
    val connectionPool: ConnectionPool
    // observe the full span of each call
    val interceptors: List<Interceptor>
    // Listener for metrics events. Extend this class to monitor the quantity, size, and duration of your application's HTTP calls.
    val eventListenerFactory: EventListener.Factory
    // Caches HTTP and HTTPS responses to the filesystem so they may be reused, saving time and bandwidth.
    val cache: Cache?
    // This class represents a proxy setting, typically a type (http, socks) and a socket address. 
    val proxy: Proxy?
  }
  ```

### Dispatcher

* comment -> Policy on when async requests are executed. Each dispatcher uses an ExecutorService to run calls internally.

* code

  ```kotlin
  class Dispatcher constructor() {
    val executorService: ExecutorService
      get() {
        if (executorServiceOrNull == null) {
          // 创建线程池
          executorServiceOrNull = ThreadPoolExecutor(0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
              SynchronousQueue(), threadFactory("$okHttpName Dispatcher", false))
        }
        return executorServiceOrNull!!
      }
  }
  ```

### RealCall

* Call

  ```kotlin
  package okhttp3
  
  interface Call : Cloneable {
    fun request(): Request
    fun execute(): Response
    fun enqueue(responseCallback: Callback)
  }
  ```

* RealCall

  ```kotlin
  class RealCall(
    val client: OkHttpClient,
    val originalRequest: Request,
    val forWebSocket: Boolean
  ) : Call {
    private val executed = AtomicBoolean()
    
    override fun enqueue(responseCallback: Callback) {
      // atomic operation
      check(executed.compareAndSet(false, true))
      // call dispatch.enqueue(AsyncCall)
      client.dispatcher.enqueue(AsyncCall(responseCallback))
    }
    
    internal inner class AsyncCall(
      private val responseCallback: Callback
    ) : Runnable {
      // Dispather.enqueue() will call this
      fun executeOn(executorService: ExecutorService) {
        executorService.execute(this)
      }
      // implements Runnable
      override fun run() {
        // try get response
        val response = getResponseWithInterceptorChain()
        // call callback
        responseCallback.onResponse(this@RealCall, response)
      }
    }
    
    internal fun getResponseWithInterceptorChain(): Response {
      // add all kinds of Interceptors
      // use chain of responsibility pattern to get response
      val interceptors = mutableListOf<Interceptor>()
      interceptors += client.interceptors
      interceptors += RetryAndFollowUpInterceptor(client)
      interceptors += BridgeInterceptor(client.cookieJar)
      interceptors += CacheInterceptor(client.cache)
      interceptors += ConnectInterceptor
      if (!forWebSocket) {
        interceptors += client.networkInterceptors
      }
      interceptors += CallServerInterceptor(forWebSocket)
      
      val chain = RealInterceptorChain(interceptors = interceptors)
      return chain.proceed(originalRequest)
    }
  }
  ```

### Intercepter

* RetryAndFollowUpInterceptor

  ```kotlin
  class RetryAndFollowUpInterceptor(private val client: OkHttpClient) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      while (true) {
        try {
          response = realChain.proceed(request) // try get response
          newExchangeFinder = true
        } catch (e: RouteException) {
          // try connect via a route failed, want to retry
          if (!recover(e.lastConnectException, call, request, requestSendStarted = false)) {
            // not meet retry condition, fail
            throw e.firstConnectException.withSuppressed(recoveredFailures)
          } else {
            continue // start to retry
          }
        } catch (e: IOException) {
          // An attempt to communicate with a server failed. want to retry
          if (!recover(e, call, request, requestSendStarted = e !is ConnectionShutdownException)) {
            // not meet retry condition, fail
            throw e.withSuppressed(recoveredFailures)
          } else {
            continue // start to retry
          }
        }
        // this method will check if status_code indicates you need to redirect
        val followUp = followUpRequest(response, exchange)
        if (++followUpCount > MAX_FOLLOW_UPS) {
          // when redirect counts overlimit, throw exception
          throw ProtocolException("Too many follow-up requests: $followUpCount")
        }
        // redirect, rerun while true
        request = followUp
        priorResponse = response
      }
    }
    
    private fun recover(call: RealCall, userRequest: Request, requestSendStarted: Boolean): Boolean {
      // The application layer has forbidden retries.
      if (!client.retryOnConnectionFailure) return false
      // We can't send the request body again.
      if (requestSendStarted && requestIsOneShot(e, userRequest)) return false
      // This exception is fatal.
      if (!isRecoverable(e, requestSendStarted)) return false
      // No more routes to attempt.
      if (!call.retryAfterFailure()) return false
      // For failure recovery, use the same route selector with a new connection.
      return true
    }
    
    // get redirect url
    private fun buildRedirectRequest(userResponse: Response, method: String): Request? {
      val location = userResponse.header("Location") ?: return null
      val url = userResponse.request.url.resolve(location) ?: return null
      return requestBuilder.url(url).build()
    }
  }
  ```

* BridgeInterceptor

  ```kotlin
  class BridgeInterceptor(private val cookieJar: CookieJar) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val userRequest = chain.request()
      val requestBuilder = userRequest.newBuilder()
      requestBuilder.header("Content-Type", contentType.toString())
      requestBuilder.header("Content-Length", contentLength.toString())
      requestBuilder.header("Connection", "Keep-Alive")
      requestBuilder.header("Accept-Encoding", "gzip") // use gzip algorithm to reduce response size
      
      val responseBuilder = networkResponse.newBuilder()
          .request(userRequest)
      // when you use gzip to compress response, it's your duty to decompress it
      if ("gzip".equals(networkResponse.header("Content-Encoding")) {
        val gzipSource = GzipSource(responseBody.source())
        responseBuilder.body(RealResponseBody(contentType, -1L, gzipSource.buffer()))
      }
      return responseBuilder.build()
    }
  }
  ```

* CacheInterceptor

  ```kotlin
  class CacheInterceptor(internal val cache: Cache?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      // If we don't need the network, we're done.
      if (networkRequest == null) {
        return cacheResponse
      }
      // do real request
      networkResponse = chain.proceed(networkRequest)
      // when not modified, use cache
      if (networkResponse?.code == HTTP_NOT_MODIFIED) {
        val response = cacheResponse
        cache.update(cacheResponse, response)
        return response
      }
      // not hit cache
      val response = networkResponse
      cache.put(response) // put into cache
      return response
    }
  }
  ```

* ConnectionInterceptor

  ```kotlin
  object ConnectInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val realChain = chain as RealInterceptorChain
      // code are very little, but inner it is complicated
      val exchange = realChain.call.initExchange(chain)
      val connectedChain = realChain.copy(exchange = exchange)
      return connectedChain.proceed(realChain.request)
    }
  }
  ```

  ```kotlin
  class ExchangeFinder(
    private val connectionPool: RealConnectionPool, // pool
    internal val address: Address, // address
    private val call: RealCall) {
    
    private fun findConnection(connectTimeout: Int, readTimeout: Int, writeTimeout: Int, pingIntervalMillis: Int, onnectionRetryEnabled: Boolean ): RealConnection {
      // Attempt to reuse the connection from the call.
      // If the call's connection wasn't released, reuse it. We don't call connectionAcquired() here
      // because we already acquired it.
      if (call.connection != null) { return callConnection }
      
      // Attempt to get a connection from the pool.
      if (connectionPool.callAcquirePooledConnection(address, call)) {
        return call.connection!!
      }
      
      // Nothing in the pool. Figure out what route we'll try next.
      val routes: List<Route>?
      val route: Route
      
      // Connect. Tell the call about the connecting call so async cancels work.
      newConnection.connect()
      return newConnection
    }
  }
  ```

  ```kotlin
  class RealConnection(
    val connectionPool: RealConnectionPool,
    private val route: Route
  ) : Http2Connection.Listener(), Connection {
    fun connect() {
      if (route.requiresTunnel()) {
        connectTunnel(connectTimeout)
      } else {
        connectSocket(connectTimeout)
      }
      establishProtocol()
    }
    
    private fun establishProtocol() {
      if (route.address.sslSocketFactory == null) { //https
        if (Protocol.H2_PRIOR_KNOWLEDGE in route.address.protocols) {
          protocol = Protocol.H2_PRIOR_KNOWLEDGE
          startHttp2(pingIntervalMillis) //http2
          return
        }
        socket = rawSocket
        protocol = Protocol.HTTP_1_1
        return
      }
      if (protocol === Protocol.HTTP_2) {
        startHttp2(pingIntervalMillis) // http2
      }
    }
  }
  ```

* CallServerInterceptor

  ```kotlin
  class CallServerInterceptor(private val forWebSocket: Boolean) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      // write request header to server 
      exchange.writeRequestHeaders(request)
      // write request body to server
      requestBody.writeTo(bufferedRequestBody)
      // read response header from server
      responseBuilder = exchange.readResponseHeaders(expectContinue = true)
      // read response body from server
      exchange.openResponseBody(response)
      return response
    }
  }
  ```

***

## pinch

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

## proguard

### reference

* [google - Shrink, obfuscate, and optimize your app](https://developer.android.com/build/shrink-code)
* [Understanding ProGuard](https://medium.com/@dugguRK/understanding-proguard-a23bbac14863)

### features

* four functions

  1. Code shrinking - detects and safely removes unused classes, fields, methods, and attributes from your app and its library dependencies.
  2. Resource shrinking - removes unused resources from your packaged app, including unused resources in your app’s library dependencies. 
  3. Obfuscation - shortens the name of classes and members, which results in reduced DEX file sizes.
  4. Optimization - inspects and rewrites your code to further reduce the size of your app’s DEX files. For example, if R8 detects that the `else {}` branch for a given if/else statement is never taken, R8 removes the code for the `else {}` branch.

* R8 - it is the default compiler that converts your project’s Java bytecode into the DEX format that runs on the Android platform.

* how to use

  ```groovy
  android {
      buildTypes {
          release {
              minifyEnabled true // Enables code shrinking, obfuscation, and optimization for only your project's release build type.
              shrinkResources true // Enables resource shrinking
              proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),'proguard-rules.pro'
          }
      }
      ...
  }
  ```

### Shrink your code

* how to do it? - R8 inspects your app’s code to build a graph of all methods, member variables, and other classes that your app might access at runtime. Code that is not connected to that graph is considered *unreachable* and may be removed from the app.

* config

  * add a `-keep` line in the ProGuard rules file.

    ```bash
    -keep public class MyClass
    ```

  * use `@Keep` annotation

### mapping.txt

* locatioin - `app/build/outputs/mapping/realease/mapping.txt`

* content

  ```bash
  com.proguard.MainActivity -> com.proguard.MainActivity:
      int $r8$clinit -> o
      kotlin.Lazy textView$delegate -> n
      1:1:kotlin.Lazy kotlin.LazyKt__LazyJVMKt.lazy(kotlin.jvm.functions.Function0):0:0 -> <init>
      1:1:void <init>():0 -> <init>
      2:2:void <init>():0:0 -> <init>
      1:1:android.widget.TextView getTextView():0:0 -> onCreate
      1:1:void onCreate(android.os.Bundle):0 -> onCreate
      2:2:void onCreate(android.os.Bundle):0:0 -> onCreate
  ```

***

## render

### vsync

* reference

  * [“终于懂了” 系列：Android屏幕刷新机制—VSync、Choreographer 全面理解！](https://juejin.cn/post/6863756420380196877)

* how it is generated: 

  * The VSync signal is a physical signal generated by the display hardware itself. It's not a software construct within the Android framework or kernel. 
  * The Android kernel receives the VSync signal from the display hardware through specific display controller registers or interrupt mechanisms. 

* Functionality

  1. Display Hardware: Generates the VSync signal at its refresh rate.

  2. Kernel: Receives the VSync signal and makes it available to the system.

  3. Hardware Composer (HWC): Utilizes the VSync signal for scheduling display updates.

  4. SurfaceFlinger: Relies on VSync for display composition synchronization.

  5. Applications: Can leverage the Choreographer class to schedule rendering in sync with VSync.

* how generate

  ```java
  // frameworks/native/libs/gui/DisplayEventDispatcher.cpp
  int DisplayEventDispatcher::handleEvent(int, int events, void*) {
    if (processPendingEvents(&vsyncTimestamp, &vsyncDisplayId, &vsyncCount, &vsyncEventData)) {
      // call dispatchVsync
      dispatchVsync(vsyncTimestamp, vsyncDisplayId, vsyncCount, vsyncEventData);
    }
  }
  
  // frameworks/base/core/jni/android_view_DisplayEventReceiver.cpp
  void NativeDisplayEventReceiver::dispatchVsync(nsecs_t timestamp, PhysicalDisplayId displayId, uint32_t count, VsyncEventData vsyncEventData) {
    // call jni method
    env->CallVoidMethod(receiverObj.get(), gDisplayEventReceiverClassInfo.dispatchVsync, timestamp, displayId.value, count);
  }
  
  // android.view.DisplayEventReceiver#dispatchVsync
  private void dispatchVsync(long timestampNanos, long physicalDisplayId, int frame, VsyncEventData vsyncEventData) {
    onVsync(timestampNanos, physicalDisplayId, frame, vsyncEventData);
  }
  
  // android.view.Choreographer.FrameDisplayEventReceiver
  @Override
  public void onVsync(long timestampNanos, long physicalDisplayId, int frame, VsyncEventData vsyncEventData) {
    Message msg = Message.obtain(mHandler, this);
    msg.setAsynchronous(true);
    // sene a message
    mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
  }
  
  @Override
  public void run() {
    // call doFrame
    doFrame(mTimestampNanos, mFrame, mLastVsyncEventData);
  }
  
  // android.view.Choreographer
  void doFrame(long frameTimeNanos, int frame, DisplayEventReceiver.VsyncEventData vsyncEventData) {
    doCallbacks(Choreographer.CALLBACK_INPUT, frameData, frameIntervalNanos);
    doCallbacks(Choreographer.CALLBACK_ANIMATION, frameData, frameIntervalNanos);
    doCallbacks(Choreographer.CALLBACK_INSETS_ANIMATION, frameData, frameIntervalNanos);
    doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameData, frameIntervalNanos);
    doCallbacks(Choreographer.CALLBACK_COMMIT, frameData, frameIntervalNanos);
  }
  
  void doCallbacks(int callbackType, FrameData frameData, long frameIntervalNanos) {
    CallbackRecord callbacks = mCallbackQueues[callbackType].extractDueCallbacksLocked(now / TimeUtils.NANOS_PER_MS);
    for (CallbackRecord c = callbacks; c != null; c = c.next) {
      // do run
      c.run(frameData);
    }
  }
  
  // android.view.Choreographer.CallbackRecord
  public void run(long frameTimeNanos) {
    if (token == FRAME_CALLBACK_TOKEN) {
      ((FrameCallback)action).doFrame(frameTimeNanos);
    } else {
      ((Runnable)action).run();
    }
  }
  
  // process of add callback
  // android.view.ViewRootImpl#scheduleTraversals
  void scheduleTraversals() {
    if (!mTraversalScheduled) {
      mTraversalScheduled = true;
      // set SyncBarrier
      mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
      // post a callback to mChoreographer
      mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
    }
  }
  
  // android.view.Choreographer
  public static final int CALLBACK_INPUT = 0;
  public static final int CALLBACK_ANIMATION = 1;
  public static final int CALLBACK_INSETS_ANIMATION = 2;
  public static final int CALLBACK_TRAVERSAL = 3; // Handles layout and draw. Runs after all other asynchronous messages have been handled.
  public static final int CALLBACK_COMMIT = 4;
  
  private void postCallbackDelayedInternal(int callbackType, Object action, Object token, long delayMillis) {
    // add a callBack to mCallbackQueues
    mCallbackQueues[callbackType].addCallbackLocked(dueTime, action, token);
    if (dueTime <= now) {
      scheduleFrameLocked(now);
    } else {
      Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_CALLBACK, action);
      msg.arg1 = callbackType;
      msg.setAsynchronous(true);
      mHandler.sendMessageAtTime(msg, dueTime);
    }
  }
  ```

### create RenderThread

```java
// android.view.ViewRootImpl
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
  enableHardwareAcceleration(attrs);
}
private void enableHardwareAcceleration(WindowManager.LayoutParams attrs) {
  mAttachInfo.mThreadedRenderer = ThreadedRenderer.create(mContext, translucent, attrs.getTitle().toString());
}

// android.graphics.HardwareRenderer
private final long mNativeProxy;

public HardwareRenderer() {
  // create a RenderNode
  mRootNode = RenderNode.adopt(nCreateRootRenderNode());
	// create a proxy, it is frameworks/base/libs/hwui/renderthread/RenderProxy.cpp
  mNativeProxy = nCreateProxy(!mOpaque, mRootNode.mNativeRenderNode);
}

// frameworks/base/libs/hwui/jni/android_graphics_HardwareRenderer.cpp
static jlong android_view_ThreadedRenderer_createRootRenderNode(JNIEnv* env, jobject clazz) {
  // RenderNode of ThreadedRenderer is RootRenderNode
  // RootRenderNode extends to RenderNode
  RootRenderNode* node = new RootRenderNode(std::make_unique<JvmErrorReporter>(env));
  return reinterpret_cast<jlong>(node);
}

// frameworks/base/libs/hwui/renderthread/RenderProxy.cpp
RenderProxy::RenderProxy(RenderNode* rootRenderNode,)
        : mRenderThread(RenderThread::getInstance()) { // call RenderThread::getInstance()
  mDrawFrameTask.setContext(&mRenderThread, mContext, rootRenderNode);
}

// frameworks/base/libs/hwui/thread/ThreadBase.h
// it is a looper queue runnable pattern
class ThreadBase : public Thread {
public:
  ThreadBase()
    : Thread(false)
    , mLooper(new Looper(false))
    , mQueue([this]() { mLooper->wake(); }, mLock) {}

// frameworks/base/libs/hwui/renderthread/RenderThread.cpp
RenderThread& RenderThread::getInstance() {
  // real create RenderThread
  [[clang::no_destroy]] static sp<RenderThread> sInstance = []() {
      sp<RenderThread> thread = sp<RenderThread>::make();
      thread->start("RenderThread");
      return thread;
  }();
  return *sInstance;
}
// while true
bool RenderThread::threadLoop() {
  initThreadLocals();
	while (true) {
    waitForWork();
    processQueue();
  }
}
// init Choreographer, EglManager
void RenderThread::initThreadLocals() {
  setupFrameInterval();
  initializeChoreographer();
  mEglManager = new EglManager();
  mRenderState = new RenderState(*this);
  mVkManager = VulkanManager::getInstance();
  mCacheManager = new CacheManager(*this);
}
  
void RenderThread::initializeChoreographer() {
  // when sync comes, it will call choreographerCallback
  mLooper->addFd(AChoreographer_getFd(mChoreographer), 0, Looper::EVENT_INPUT,
                     RenderThread::choreographerCallback, this);
  mVsyncSource = new ChoreographerSource(this);
}
```

### createCanvas

```c++
// android.graphics.RecordingCanvas
public final class RecordingCanvas extends BaseRecordingCanvas {
	// constructor
  private RecordingCanvas(RenderNode node, int width, int height) {
    super(nCreateDisplayListCanvas(node.mNativeRenderNode, width, height));
  }
  // jni create a canvas, and it is associated with RenderNode
  private static native long nCreateDisplayListCanvas(long node, int width, int height);
}

// frameworks/base/libs/hwui/jni/android_graphics_DisplayListCanvas.cpp
static jlong android_view_DisplayListCanvas_createDisplayListCanvas() {
  // create the RenderNode in cpp
  RenderNode* renderNode = reinterpret_cast<RenderNode*>(renderNodePtr);
  // it calls Canvas::create_recording_canvas
  return reinterpret_cast<jlong>(Canvas::create_recording_canvas(width, height, renderNode));
}

// frameworks/base/libs/hwui/hwui/Canvas.cpp
Canvas* Canvas::create_recording_canvas(int width, int height, uirenderer::RenderNode* renderNode) {
  // so the realy Canvas in a instance of SkiaRecordingCanvas
  return new uirenderer::skiapipeline::SkiaRecordingCanvas(renderNode, width, height);
}

// frameworks/base/libs/hwui/pipeline/skia/SkiaRecordingCanvas.cpp
void SkiaRecordingCanvas::initDisplayList(uirenderer::RenderNode* renderNode, int width, int height) {
  // get mDisplayList from RenderNode
  mDisplayList = renderNode->detachAvailableList();
  mDisplayList->attachRecorder(&mRecorder, SkIRect::MakeWH(width, height));
}
```

### drawCircle

```c++
// android.graphics.BaseCanvas#nDrawCircle
public abstract class BaseCanvas {
  private static native void nDrawCircle(long nativeCanvas, float cx, float cy, float radius, long nativePaint);
}

//frameworks/base/libs/hwui/jni/android_graphics_Canvas.cpp
static void drawCircle(JNIEnv* env, jobject, jlong canvasHandle, jfloat cx, jfloat cy) {
  // from above, we know that get_canvas returns SkiaRecordingCanvas
  get_canvas(canvasHandle)->drawCircle(cx, cy, radius, *paint);
}

// frameworks/base/libs/hwui/pipeline/skia/SkiaRecordingCanvas.cpp
void SkiaRecordingCanvas::drawCircle(uirenderer::CanvasPropertyPrimitive* x) {
  // it will only save infomation in the mDisplayList，and it will not directly calculate the frame buffer
  drawDrawable(mDisplayList->allocateDrawable<AnimatedCircle>(x, y, radius, paint));
}
```

### SurfaceFlinger

* structure

  ![结构图片](https://img2018.cnblogs.com/blog/821933/201907/821933-20190730111306166-2128331293.png)

* features
  1. it is a daemon process in android.
  2. It is not a part of Android SDK, but a part of AOSP.

### draw process

```java
// android.view.ViewRootImpl#draw
private boolean draw(boolean fullRedrawNeeded, boolean forceDraw) {
  // use gpu instead of cpu
  if (isHardwareEnabled()) {
    mAttachInfo.mThreadedRenderer.draw(mView, mAttachInfo, this);
  }
}

// android.view.ThreadedRenderer#draw
void draw(View view, AttachInfo attachInfo, DrawCallbacks callbacks) {
  updateRootDisplayList(view, callbacks);
}
private void updateRootDisplayList(View view, DrawCallbacks callbacks) {
  updateViewTreeDisplayList(view);
}
private void updateViewTreeDisplayList(View view) {
  view.updateDisplayListIfDirty();
}

// android.view.View
// hold a mRenderNode
final RenderNode mRenderNode;
public View(Context context) {
  mRenderNode = RenderNode.create(getClass().getName(), new ViewAnimationHostBridge(this));
}

// frameworks/base/libs/hwui/jni/android_graphics_RenderNode.cpp
static jlong android_view_RenderNode_create(JNIEnv* env, jobject, jstring name) {
  // RenderNode of View is RenderNode
  RenderNode* renderNode = new RenderNode();
  return reinterpret_cast<jlong>(renderNode);
}

// android.view.View
public RenderNode updateDisplayListIfDirty() {
  final RecordingCanvas canvas = renderNode.beginRecording(width, height);
  try {
    // save draw infomation in the displayList of canvas
    draw(canvas);
  } finally {
    renderNode.endRecording();
  }
  return renderNode;
}

// android.view.ThreadedRenderer#draw
void draw(View view, AttachInfo attachInfo, DrawCallbacks callbacks) {
  updateRootDisplayList(view, callbacks);
  // finish saveing all information in displayList
  // this method is a sync method, and it will wait for a return
  int syncResult = syncAndDrawFrame(frameInfo);
}

static int android_view_ThreadedRenderer_syncAndDrawFrame(JNIEnv* env, jobject clazz, jlong proxyPtr, jlongArray frameInfo, jint frameInfoSize) {
  RenderProxy* proxy = reinterpret_cast<RenderProxy*>(proxyPtr);
  // let proxy run syncAndDrawFrame
  return proxy->syncAndDrawFrame();
}

// frameworks/base/libs/hwui/renderthread/RenderProxy.cpp
int RenderProxy::syncAndDrawFrame() {
  return mDrawFrameTask.drawFrame();
}

// frameworks/base/libs/hwui/renderthread/DrawFrameTask.cpp
int DrawFrameTask::drawFrame() {
  mSyncResult = SyncResult::OK;
  mSyncQueued = systemTime(SYSTEM_TIME_MONOTONIC);
  postAndWait(); // post and wait
  return mSyncResult;
}
void DrawFrameTask::postAndWait() {
  AutoMutex _lock(mLock); // lock 
  // queue a runnable to mRenderThread, it will do run() method when being scheduled
  mRenderThread->queue().post([this]() { run(); }); 
  mSignal.wait(mLock); // wait
}

void DrawFrameTask::run() {
  CanvasContext* context = mContext;
  // let context run draw
  context->draw(solelyTextureViewUpdates);
}

// frameworks/base/libs/hwui/renderthread/CanvasContext.cpp
void CanvasContext::draw(bool solelyTextureViewUpdates) {
  // it will call EglManager::beginFrame to get a buffer from SurfaceFlinger
  Frame frame = getFrame();
  // real draw
  drawResult = mRenderPipeline->draw(
                frame, windowDirty, dirty, mLightGeometry, &mLayerUpdateQueue, mContentDrawBounds,
                mOpaque, mLightInfo, mRenderNodes, &(profiler()), mBufferParams, profilerLock());
  // swapBuffer and ready to display. it will call EglManager::swapBuffers, it will give buffer back to SurfaceFlinger
	bool didSwap = mRenderPipeline->swapBuffers(frame, drawResult, windowDirty);
}

// frameworks/base/libs/hwui/renderthread/EglManager.cpp
Frame EglManager::beginFrame(EGLSurface surface) {
  makeCurrent(surface);
  Frame frame;
  frame.mSurface = surface;
  // get a buffer to draw
  frame.mBufferAge = queryBufferAge(surface);
  eglBeginFrame(mEglDisplay, surface);
  return frame;
}

// frameworks/base/libs/hwui/pipeline/skia/SkiaOpenGLPipeline.cpp
IRenderPipeline::DrawResult SkiaOpenGLPipeline::draw(const Frame& frame, const SkRect& screenDirty, const SkRect& dirty) {
  renderFrame(*layerUpdateQueue, dirty, renderNodes, opaque, contentDrawBounds, surface);
}

// frameworks/base/libs/hwui/pipeline/skia/SkiaPipeline.cpp
void SkiaPipeline::renderFrame(const LayerUpdateQueue& layers, const std::vector<sp<RenderNode>>& nodes, sk_sp<SkSurface> surface) {
  SkCanvas* canvas = tryCapture(surface.get(), nodes[0].get(), layers);
  renderLayersImpl(layers, opaque);
  renderFrameImpl(clip, nodes, opaque, contentDrawBounds, canvas, preTransform);
  endCapture(surface.get());
}

void SkiaPipeline::renderFrameImpl(const std::vector<sp<RenderNode>>& nodes, SkCanvas* canvas) {
  SkCanvas* layerCanvas = layerNode->getLayerSurface()->getCanvas();
  // send instructions to gpu
  cachedContext->flushAndSubmit();
}

// external/skia/include/gpu/GrDirectContext.h
void flushAndSubmit(GrSyncCpu sync = GrSyncCpu::kNo) {
  // send instructions to gpu
  this->flush(GrFlushInfo());
  this->submit(sync);
}

// android.view.ThreadedRenderer#draw
void draw(View view, AttachInfo attachInfo, DrawCallbacks callbacks) {
  int syncResult = syncAndDrawFrame(frameInfo);
  // gpu draw return
}
```

### BufferQueue

* 好文链接 - [深入浅出Android BufferQueue](https://zhuanlan.zhihu.com/p/62813895)

* 模型 - 生产者消费者模式

  * 生产者 - 产生图像源数据，如`Surface`，截图时的`SurfaceFlinger`
  * 消费者 - 消费图像源数据，如`SurfaceFlinger`，截图时另外的一个`BufferQueue`

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

## retrofit

### path与query

* 示例

  ```kotlin
  @GET("/posts/{id}")
  fun getRespById(@Path("id") id: Int): Observable<DataResponse>
  ```

  ```kotlin
  @GET("/comments")
  fun getComment(@Query("postId") postId: Int): Observable<List<DataComment>>
  ```

### basic

* Converter

  ```java
  // Convert objects to and from their representation in HTTP.
  // okhttp3.ResponseBody -> SomeOtherType
  // such as GsonConverter
  public interface Converter<F, T> {
    T convert(F value);
  }
  ```

* CallAdapter

  ```java
  // Adapts a Call with response type R into the type of T.
  // retrofit2.Call -> Observable
  // such as RxJava3CallAdapter
  public interface CallAdapter<R, T> {
    Type responseType();
    T adapt(Call<R> call);
  }
  ```

### process

1. generate a proxy class

   ```java
   // service -> com.retrofit.ApiService
   public <T> T create(final Class<T> service) {
     // loader -> dalvik.system.PathClassLoader
     // interfaces -> [com.retrofit.ApiService]
     return (T)
       Proxy.newProxyInstance(
           service.getClassLoader(),
           new Class<?>[] {service},
           new InvocationHandler() {
   					public Object invoke(Object proxy, Method method, @Nullable Object[] args) {
               return loadServiceMethod(method).invoke(args);
             }
           }
       );
     // className -> $Proxy2
   }
   ```

2. find the right platform

   ```java
   class Platform {
     private static Platform findPlatform() {
       // 直接根据虚拟机来判断是不是安卓
       return "Dalvik".equals(System.getProperty("java.vm.name"))
         ? new Android()
         : new Platform(true);
     }
     
     static final class Android extends Platform {
       public Executor defaultCallbackExecutor() {
         return new MainThreadExecutor();
       }
     }
     
     static final class MainThreadExecutor implements Executor {
       private final Handler handler = new Handler(Looper.getMainLooper());
       // 一个Executor的实现居然就是单纯地向MainLooper抛东西
       public void execute(Runnable r) {
         handler.post(r);
       }
     }
   }
   ```

3. ready to call `InvocationHandler#invoke()`

   ```java
   public final class $Proxy0 extends Proxy implements ApiService {
     private static final Method m3;
     static {
       // method是从这里来的
       m3 = Class.forName("com.retrofit.api.ApiService").getMethod("getRespById", int.class);
     }
   }
   
   // method相关关键参数
   // annotations -> [@retrofit2.http.GET(value=/comments)]
   // parameterTypes -> [class(int)]
   // parameterAnnotationsArray -> [@retrofit2.http.Query(encoded=false, value=postId)]
   ```

4. parse method annotation

   ```java
   // RequestFactory.java
   private void parseMethodAnnotation(Annotation annotation) {
   	if (annotation instanceof GET) {
       parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
     }
   }
   
   // parse method annotation
   // httpMethod -> GET
   // value -> /comments
   private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
     int question = value.indexOf('?');
   }
   
   // parse parameter annotation
   // parameterType -> Class(int)
   // annotations -> [@retrofit2.http.Query(encoded=false, value=postId)]
   private ParameterHandler<?> parseParameter(Type parameterType, @Nullable Annotation[] annotations) {
     // set variable to be true if the last param is kotlin.coroutine.Continuation
     if (Utils.getRawType(parameterType) == Continuation.class) {
       isKotlinSuspendFunction = true;
     }
   }
   ```

5. create RequestFactory

   ```java
   final class RequestFactory {
     private final Method method; // public abstract io.reactivex.rxjava3.core.Observable com.retrofit.api.ApiService.getComment(int)
     private final HttpUrl baseUrl; // https://jsonplaceholder.typicode.com/
     final String httpMethod; // GET
     private final String relativeUrl; // /comments
     private final ParameterHandler<?>[] parameterHandlers; // [ParameterHandler$Query]
     final boolean isKotlinSuspendFunction; // false
   }
   ```

6. find CallAdapter

   ```java
   class Retrofit {
     // [Rxjava3CallAdapterFactory, CompeletableFutureCallAdapterFactory, DefaultCallAdapterFactory]
     final List<CallAdapter.Factory> callAdapterFactories;
     
     // Retrofit.java
     // returnType -> io.reactivex.rxjava3.core.Observable<java.util.List<com.retrofit.resp.DataComment>>
     public CallAdapter<?, ?> nextCallAdapter(Type returnType) {}
   }
   
   final class RxJava3CallAdapter<R> implements CallAdapter<R, Object> {
     private final Type responseType; // java.util.List<com.retrofit.resp.DataComment>
   }
   ```

7. find ResponseConvertor

   ```java
   class Retrofit {
     // [BuiltinConverters, GsonConverterFactory, OptionalConverterFactory]
     final List<Converter.Factory> converterFactories;
     
     // Retrofit.java
     // returnType -> java.util.List<com.retrofit.resp.DataComment>
     public <T> Converter<ResponseBody, T> nextResponseBodyConverter(Type returnType) {}
   }
   
   public final class GsonConverterFactory extends Converter.Factory {
     // type -> java.util.List<com.retrofit.resp.DataComment>
     public Converter<ResponseBody, ?> responseBodyConverter(Type type) {
       TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
       return new GsonResponseBodyConverter<>(gson, adapter); // 返回GsonConverter
     }
   }
   // 其他的几个Converter都返回null不会走到
   ```

8. get HttpServiceMethod

   ```java
   HttpServiceMethod<ResponseT, ReturnT> parseAnnotations() {
     // omit the process of finding RespConverter and CallConverter
     if (!isKotlinSuspendFunction) { // adapt for kotlin coroutine
       return new CallAdapted<>() // it will return here if not kotlin suspend
     } else {
       return new new SuspendForBody<>()
     }
   }
   ```

9. run `CallAdapter.adapt()`

   ```java
   final class RxJava3CallAdapter<R> implements CallAdapter<R, Object> {
     private final Type responseType; // java.util.List<com.retrofit.resp.DataComment>
     
     // call -> retrofit2.OkHttpCall implements retrofit2.Call
     // R -> Integer
     public Object adapt(Call<R> call) {
       Observable<Response<R>> responseObservable = new CallEnqueueObservable<>(call);
       Observable<?> observable = new BodyObservable<>(responseObservable);
       return RxJavaPlugins.onAssembly(observable);
     }
   }
   ```

10. make a call by okhttp

    ```java
    class CallEnqueueObservable<T> extends Observable<Response<T>> {
      protected void subscribeActual(Observer<? super Response<T>> observer) {
        CallCallback<T> callback = new CallCallback<>(call, observer); // observer是向下的回调
        call.enqueue(callback); // 调用retrofit2.OkHttpCall#enqueue(retrofit2.Callback)
      }
    }
    
    
    final class OkHttpCall<T> implements Call<T> {
      public void enqueue(final Callback<T> callback) {
        call.enqueue(
        	new okhttp3.Callback() {
            public void onResponse() {
              response = parseResponse(rawResponse); //parse the returned response
              callback.onResponse(OkHttpCall.this, response); // call callBack
            }
          }
        )
      }
    }
    ```

11. convert resp

    ```java
    Response<T> parseResponse(okhttp3.Response rawResponse) {
      // responseConverter -> retrofit2.converter.gson.GsonResponseBodyConverter
      T body = responseConverter.convert(catchingBody);
    }
    ```

12. final

    ```java
    class CallEnqueueObservable<T> extends Observable<Response<T>> {
      final class CallCallback<T> {
        public void onResponse(Call<T> call, Response<T> response) {
          // onNext()
          observer.onNext(response);
        }
      }
    }
    ```

### extra knowledge

1. factory pattern
2. adapter pattern
3. dynamic proxy
4. static proxy
5. httpmethod cache

***

## rotate_save

### onSaveInstanceState

1. 只有在意外退出时才会调用此方法，用户强意愿的主动退出不会调用此方法
2. 在`override fun onSaveInstanceState(outState: Bundle)`中写值，在`override fun onCreate(savedInstanceState: Bundle?)`中取值

***

## rxjava

### flatMap使用

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

## surface_view

### create a surface

```java
// android.view.SurfaceControl
// Handle to an on-screen Surface managed by the system compositor.
public final class SurfaceControl implements Parcelable {
  
}

// android.view.ViewRootImpl
class ViewRootImpl {
  // hold a SurfaceControl
  private final SurfaceControl mSurfaceControl = new SurfaceControl();
  
  private void performTraversals() {
    relayoutResult = relayoutWindow(params, viewVisibility, insetsPending);
  }
  private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility, boolean insetsPending) {
    // pass the surfaceControl as a parameter
    relayoutResult = mWindowSession.relayout(mWindow, params, requestedWidth, requestedHeight, viewVisibility, mSurfaceControl)
  }
}

// com.android.server.wm.Session
public int relayout(IWindow window, WindowManager.LayoutParams attrs, SurfaceControl outSurfaceControl) {
  // mService is WindowManagerService
  // pass the surfaceControl as a parameter
  int res = mService.relayoutWindow(this, window, attrs, outSurfaceControl)
}

// com.android.server.wm.WindowManagerService
public int relayoutWindow(Session session, IWindow client, LayoutParams attrs, SurfaceControl outSurfaceControl) {
  result = createSurfaceControl(outSurfaceControl, result, win, winAnimator);
}
private int createSurfaceControl(SurfaceControl outSurfaceControl, int result) {
  WindowSurfaceController surfaceController;
  // try to create a SurfaceController
  surfaceController = winAnimator.createSurfaceLocked();
  if (surfaceController != null) {
    surfaceController.getSurfaceControl(outSurfaceControl);
  } else {outSurfaceControl.release();
  }

  return result;
}

// com.android.server.wm.WindowStateAnimator
WindowSurfaceController createSurfaceLocked() {
  // create an instance of WindowSurfaceController
  mSurfaceController = new WindowSurfaceController(attrs.getTitle().toString(), format, flags, this, attrs.type);
}

// com.android.server.wm.WindowSurfaceController
WindowSurfaceController(String name, int format, int flags, WindowStateAnimator animator, int windowType) {
  final SurfaceControl.Builder b = win.makeSurface()
      .setParent(win.getSurfaceControl())
      .setName(name)
      .setFormat(format)
      .setFlags(flags)
      .setMetadata(METADATA_WINDOW_TYPE, windowType)
      .setMetadata(METADATA_OWNER_UID, mWindowSession.mUid)
      .setMetadata(METADATA_OWNER_PID, mWindowSession.mPid)
      .setCallsite("WindowSurfaceController");
  // use build pattern to create a WindowSurfaceController
  mSurfaceControl = b.build();
}

// android.view.SurfaceControl
private SurfaceControl(SurfaceSession session, String name) {
  Parcel metaParcel = Parcel.obtain();
  // jni create, pass metaParcel as a parameter
  mNativeObject = nativeCreate(session, name, w, h, format, flags, metaParcel);
  mNativeHandle = nativeGetHandle(mNativeObject);
}

// frameworks/base/core/jni/android_view_SurfaceControl.cpp
static jlong nativeCreate(JNIEnv* env, jclass clazz, jobject sessionObj, jobject metadataParcel) {
  // get data from Parcel
  Parcel* parcel = parcelForJavaObject(env, metadataParcel);
  // call createSurfaceChecked
  status_t err = client->createSurfaceChecked(String8(name.c_str()), w, h, format, &surface, flags, parentHandle, std::move(metadata));
}

// frameworks/native/libs/gui/SurfaceComposerClient.cpp
status_t SurfaceComposerClient::createSurfaceChecked(const String8& name, uint32_t w, uint32_t h) {
  // call createSurface
  binder::Status status = mClient->createSurface(std::string(name.c_str()), std::move(metadata));
}

// gen/aidl_library/android/gui/ISurfaceComposerClient.cpp
BpSurfaceComposerClient::createSurface(const ::android::gui::LayerMetadata& metadata, ::android::gui::CreateSurfaceResult* _aidl_return) {
  // use binder rpc to get a surface from SurfaceFlinger
  _aidl_ret_status = remote()->transact(BnSurfaceComposerClient::TRANSACTION_createSurface, _aidl_data, &_aidl_reply, 0);
  if (_aidl_ret_status == ::android::UNKNOWN_TRANSACTION && ISurfaceComposerClient::getDefaultImpl()) [[unlikely]] {
     return ISurfaceComposerClient::getDefaultImpl()->createSurface(name, flags, parent, metadata, _aidl_return);
  }
}

// frameworks/native/services/surfaceflinger/Client.cpp
binder::Status Client::createSurface(const std::string& name,  const gui::LayerMetadata& metadata, gui::CreateSurfaceResult* outResult) {
  // creat a layer from SurfaceFlinger
  const status_t status = mFlinger->createLayer(args, *outResult);
  return binderStatusFromStatusT(status);
}

// com.android.server.wm.WindowManagerService
private int createSurfaceControl(SurfaceControl outSurfaceControl, int result) {
  WindowSurfaceController surfaceController;
  // surfaceController is returned by SurfaceFlinger
  surfaceController = winAnimator.createSurfaceLocked();
  if (surfaceController != null) {
    // get a surface
    surfaceController.getSurfaceControl(outSurfaceControl);
  } else {
    outSurfaceControl.release();
  }
  return result;
}

// com.android.server.wm.WindowSurfaceController
void getSurfaceControl(SurfaceControl outSurfaceControl) {
  // use copyFrom to get the Surface
  outSurfaceControl.copyFrom(mSurfaceControl, "WindowSurfaceController.getSurfaceControl");
}

// frameworks/base/core/jni/android_view_SurfaceControl.cpp
static jlong nativeCopyFromSurfaceControl(JNIEnv* env, jclass clazz, jlong surfaceControlNativeObj) {
 	// create a surface from surfaceControlNativeObj
  sp<SurfaceControl> surface(reinterpret_cast<SurfaceControl *>(surfaceControlNativeObj));
  sp<SurfaceControl> newSurface = new SurfaceControl(surface);
  newSurface->incStrong((void *)nativeCreate);
  return reinterpret_cast<jlong>(newSurface.get());
}

// android.view.ViewRootImpl
class ViewRootImpl {
  // hold a SurfaceControl
  private final SurfaceControl mSurfaceControl = new SurfaceControl();
  // hold a Surface
  public final Surface mSurface = new Surface();
  
  private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility, boolean insetsPending) {
    // pass the surfaceControl as a parameter
    relayoutResult = mWindowSession.relayout(mWindow, params, requestedWidth, requestedHeight, viewVisibility, mSurfaceControl);
    // now mSurfaceControl is modified by SurfaceFlinger
    // use mSurfaceControl to create a Surface.
    mSurface.copyFrom(mSurfaceControl);
  }
  private void performTraversals() {
    // mSurface is passed to mThreadedRenderer
    wInitialized = mAttachInfo.mThreadedRenderer.initialize(mSurface);
  }
}
```

### 核心与相关联的类

* Surface

  * `Handle onto a raw buffer that is being managed by the screen compositor.`

  * 继承了`Parcelable`，因此可以跨进程通信，在`WMS`中传递

  * 它持有了`NativeBuffer`的指针，这个`NativieBuffer`指的是用来保存当前窗口屏幕数据的一个`buffer`

  * `ViewRootImpl`持有了这个对象，即一颗`view tree`，一个`window`，共享一个`Surface`

    ```java
    public final class ViewRootImpl implements ViewParent {
      BaseSurfaceHolder mSurfaceHolder;
    	public final Surface mSurface = new Surface(); 
    }
    ```
  
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

  * 是`View`的子类；不与`window`共享`surface`，而是自己持有一个`surface`；未重写`onDraw()`方法，即不参与绘制

    ```java
    public class SurfaceView extends View {
      final Surface mSurface = new Surface(); // Current surface in use
    }
    ```
  
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
    
    if (hasChanged) {
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
      mBlastBufferQueue.syncNextTransaction(false, onTransactionReady);
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
