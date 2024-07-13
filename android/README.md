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
