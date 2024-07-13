## 概述

* 也是一种观察者模式
* 除了`观察者、被观察者`外多了一个第三者——`LifeCycleOwner`，用于控制观察者模式的时间域范围

## 特点

* `LifecycleOwner`，一般就只能是`Activity`、`Fragment`，必须有`start、resume、stop`等方法
* `Observer`必须实现`public interface Observer<T>`接口的`onChanged(T t)`方法
* 不用手动处理生命周期，默认方式封装了只会在活跃生命周期内观察
* 如果在不正常生命周期漏观察了变化，则在进入正常生命周期时刻会立即更新
* 总是就是很好用很方便

## 观察

```java
//LiveData.observe()
public void observe(LifecycleOwner owner, Observer<? super T> observer) {}
```

## `LiveData`

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

