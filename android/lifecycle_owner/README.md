## 概述

* 本质上还是一个观察者模式
* `Observable`是本身拥有生命周期的`Activity`、`Fragment`
* `Observer`是自定义的

## 定义`Observer`

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

## `Observable`订阅`Observer`

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

## 注

* Android手动杀死进程。依旧会执行`onStop()、onDestroy()`方法

## 实现原理

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