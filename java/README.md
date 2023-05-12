# Java

## object

### getClass

* signature - `public final native Class<?> getClass();`
* annotation - Returns the runtime class of this Object.

### hashCode

* signature - `public native int hashCode();`

* annotation - Returns a hash code value for the object.

* how hashCode is calculated

  1. by default - It is generated based on the memory address of the object.

  2. overide - You can reimplemented it base on your purpose.

     ```java
     public class MyClass {
         private int myValue;
     
         public MyClass(int value) { this.myValue = value; }
     
         @Override
         public int hashCode() {
             final int prime = 31;
             int result = 1;
             result = prime * result + myValue;
             return result;
         }
     }
     ```

### equal

* signature - `public boolean equals(Object obj)`

* `==` 与 `equal()`

  * `==` operator compares the object references of two objects to check if they refer to the same object in memory. 
  * In the `Object` class, the default implementation of the `equals()` method uses the `==` operator to compare object references.
  *  `equals()` method is always overrided to  check if two objects are equal based on their state and not based on their memory location.

* `==` between primitive data type and Object

  * With primitive data types, such as `int`, `double`, `float`, etc., the `==` operator compares the values of the two operands and returns `true` if they are equal, and `false` otherwise.

    ```java
    int a = 10;
    int b = 10;
    if (a == b) { System.out.println("a and b are equal"); }
    ```

  * With objects, the `==` operator compares the memory locations of the two objects and not the values of the objects themselves.

* `==` and boxing/unboxing

  * When comparing a primitive value with a wrapper object using `==`, Java performs auto-unboxing on the wrapper object to extract the primitive value before comparison. This means that a primitive value and its corresponding wrapper object can be compared using `==` and will be equal to each other if they have the same value.

    ```java
    int x = 10;
    Integer y = 10; // boxing, turn primitive data type int to wrapper type Integer
    System.out.println(x == y); // true (auto-unboxing performed)
    Integer z = new Integer(10);
    System.out.println(y == z); // false (different instances in memory)


### clone

* signature - `protected native Object clone() throws CloneNotSupportedException`

* features

  * If the class of this object does not implement the interface `Cloneable`, then a CloneNotSupportedException is thrown.

    ```java
    // 空接口，什么方法都没有，仅仅是标记
    public interface Cloneable {
    }
    ```

  * The default implementation of the `clone()` method performs a **shallow copy**.
  * It doesn't need to call constructor.

* shallow copy and deep copy

  * A *shallow copy* copies the member variables of an object, but if a member variable is a reference to another object, the copy will have a reference to the same object as the original. In other words, the copy and the original will share the same reference to any objects that are not primitive types or immutable.
  * A *deep copy*, on the other hand, creates new instances of all referenced objects and copies their data. This means that the copy and the original will have separate and distinct copies of any objects that are not primitive types or immutable.

* trun shadow clone to deep clone

  ```java
  // 必须实现Cloneable接口，不然调clone会崩溃
  public class MyClass implements Cloneable {
      private int x;
      private MyCustomObject z;
  
      @Override
      public Object clone() throws CloneNotSupportedException {
          MyClass copy = (MyClass) super.clone();
        	// 对成员也拷贝
          copy.z = (MyCustomObject) z.clone();
          return copy;
      }
  }

### notify

* signature - `public final native void notify()`
* for
  * Wakes up a single thread that is waiting on this object's monitor. If any threads are waiting on this object, one of them is chosen to be awakened.
* requirement
  * This method should only be called by a thread that is **the owner of this object's monitor**.
  * how to be?
    1. By executing a synchronized instance method of that object.
    2. By executing the body of a synchronized statement that synchronizes on the object.
    3. For objects of type Class, by executing a synchronized static method of that class.
* the monitor of an object
  * what? - It is a mechanism used to ensure that only one thread can access the object's synchronized methods or code blocks at a time. 
  * explanation - When a thread attempts to execute a synchronized method or code block of an object, it must first obtain the monitor of that object. If the monitor is currently owned by another thread, the waiting thread will be blocked until the monitor becomes available. Once the thread obtains the monitor, it can execute the synchronized code and when it completes the execution, it releases the monitor.
  * implementation - It is implemented internally using the object's monitor lock. Every object in Java has its own monitor lock. The monitor lock is implemented by the JVM and is completely transparent to the programmer.
* `synchronized()` and `wait()、notify()`
  * `synchronized()` is a condition for calling `wait()` and `notify()` methods.  Both `wait()` and `notify()` must be called while holding the lock on the object, which is achieved by calling `synchronized()` on the same object. This is because calling `wait()` releases the lock and allows other threads to synchronize on the same object and access its methods and variables.

### wait

* signature - `public final void wait(long timeoutMillis, int nanos) throws InterruptedException`

* annotation - Causes the current thread to wait until it is awakened, typically by being notified or interrupted, or until a certain amount of real time has elapsed.

* what it does

  * It releases the lock on the object and goes to sleep. Other threads that are trying to acquire the lock on the object will not be blocked. 

* summarize

  * `synchronized()` controsl the lock of the object, while `nofity()、wait()` control the state(sleep, wake) of threads.

* 现在有三个线程A、B、C、D，一个对象obj，现进行如下分析

  ```java
  //Thread A
  synchronized(obj) {
   obj.wait()
  }
  //Thread B
  synchronized(obj) {
   obj.wait()
  }
  //Thread C
  synchronized(obj) {
   obj.wait()
  }
  //Thread D
  synchronized(obj) {
   obj.notify();
   obj.notifyAll();
  }
  ```

  1. 线程A执行`synchronized(obj)`时获取了obj的对象锁，执行`obj.wait()`时释放对象锁，让线程A睡眠。
  2. 因为上一步最后释放了对象锁，因此线程B执行`synchronized(obj)`时能获取对象锁，执行`obj.wait()`时释放对象锁，让线程B睡眠。
  3. 因为上一步最后释放了对象锁，因此线程C执行`synchronized(obj)`时能获取对象锁，执行`obj.wait()`时释放对象锁，让线程C睡眠。
  4. 最后当线程D执行`notify()`时，会唤醒A、B、C三者中任意一个线程，这个线程自然而然很容易就会获取到对象锁，然后执行完`synchronized(obj)`语句后，释放对象锁。但另外两个线程会永远睡眠。
  5. 最后当线程D执行`notifyAll()`时，会唤醒A、B、C三个线程，三者都去竞争对象锁。自然而然第一个线程竞争到对象锁，它执行完`synchronized(obj)`后释放对象锁；接着第二个线程竞争到对象锁，执行相同操作；最后一个线程同理，最后释放对象锁，三个线程的`synchronized(obj)`都执行完毕。

### finalize

* signature - `protected void finalize() throws Throwable { }`
* 注释 - Called by the garbage collector on an object when garbage collection determines that there are no more references to the object. A subclass overrides the finalize method to dispose of system resources or to perform other cleanup.
* features
  * 已过时，不再使用
  * The ref.Cleaner and ref.PhantomReference provide more flexible and efficient ways to release resources when an object becomes unreachable.

***

## reference

### PhantomReference

* 类

  ```java
  public class PhantomReference<T> extends Reference<T> {
    public PhantomReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }
  }
  ```

* 注释

  * Phantom reference objects, which are enqueued after the collector determines that their referents may otherwise be reclaimed. Phantom references are most often used to schedule post-mortem cleanup actions.
  * Suppose the garbage collector determines at a certain point in time that an object is phantom reachable. At that time it will atomically clear all phantom references to that object and all phantom references to any other phantom-reachable objects from which that object is reachable. At the same time or at some later time it will enqueue those newly-cleared phantom references that are registered with reference queues.

* 使用case

  1. for managing native resources in Java. One way to do this is to use a `PhantomReference` to keep track of the object and use a reference queue to receive notifications when the object is garbage collected. This way, you can ensure that native resources are properly released when they are no longer needed.
  2. for implementing caching or resource management systems,  where objects may be removed from memory at any time. By using a `PhantomReference`, you can keep track of when an object is removed from memory and take appropriate action, such as re-creating the object or updating a cache.

* example

  ```java
  public class ResourceHandler {
      private static ResourceHandler instance = new ResourceHandler();
      private ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
      private Thread cleanupThread;
  
      private ResourceHandler() {
          cleanupThread = new Thread(() -> {
              while (true) {
                  try {
                      // Wait for a reference to become available
                      // and remove its corresponding resource
                      PhantomReference<?> reference = (PhantomReference<?>) referenceQueue.remove();
                      Resource resource = (Resource) reference.get();
                      if (resource != null) {
                          resource.close();
                      }
                  } catch (InterruptedException e) {
                      // Handle InterruptedException as needed
                      break;
                  }
              }
          });
          cleanupThread.setDaemon(true);
          cleanupThread.start();
      }
  
      public static ResourceHandler getInstance() {
          return instance;
      }
  
      public void manageResource(Resource resource) {
          PhantomReference<Resource> reference = new PhantomReference<>(resource, referenceQueue);
          // Register the resource with the cleanup thread
          // and manage it using the phantom reference
          // ...
      }
  }
  ```

  * In this example, the `ResourceHandler` class manages resources using a `PhantomReference` and `ReferenceQueue`. When a new resource is created, it is registered with the cleanup thread using a `PhantomReference`. When the resource is no longer needed and is garbage collected, the cleanup thread receives a notification through the `ReferenceQueue`, retrieves the resource using the `PhantomReference`, and releases it using the `close` method.

### WeakReference

* 类

  ```java
  public class WeakReference<T> extends Reference<T> {
    public WeakReference(T referent)
  	// 这个构造方法和功能同PhantomReference一样
    public WeakReference(T referent, ReferenceQueue<? super T> q)
  }
  ```

* for? 

  * It provides a way to reference an object but at the same time not keep it from being garbage collected.

* example

  * One of the common use cases for `WeakReference` is to implement caches. In a caching system, you want to keep frequently used objects in memory to access them quickly but at the same time, you need to remove rarely used objects to reduce the memory footprint. A `WeakReference` can help here because it allows you to hold a reference to an object that may be garbage collected if there are no other strong references to it.

  ```java
  public class Cache<K, V> {
      private Map<K, WeakReference<V>> entries;
      public Cache() { entries = new HashMap<>(); }
      public void put(K key, V value) {
          entries.put(key, new WeakReference<>(value));
      }
      public V get(K key) {
          WeakReference<V> ref = entries.get(key);
          if (ref != null) {
              V value = ref.get();
              if (value != null) {
                  return value;
              } else {
                  entries.remove(key);
              }
          }
          return null;
      }
      public int size() { return entries.size(); }
      public void clear() { entries.clear(); }
  }
  ```

### SoftReference

* 类

  ```java
  public class SoftReference<T> extends Reference<T> {
    // Timestamp clock, updated by the garbage collector
    private static long clock;
    // imestamp updated by each invocation of the get method. The VM may use this field when selecting soft references to be cleared, but it is not required to do so.
    private long timestamp;
    public SoftReference(T referent) {}
    // 这个构造方法和功能同PhantomReference一样
    public SoftReference(T referent, ReferenceQueue<? super T> q) {}
    public T get() {}
  }
  ```

* What?

  * It is another class in Java that allows you to create references to objects that may be garbage collected.
  * A `SoftReference` is similar to a `WeakReference`. 

* 和WeakRefrence的区别

  * In contrast to a weak reference, which is always a candidate for garbage collection, a `SoftReference` can stay in memory longer as long as the JVM still has enough memory. If the JVM starts to run out of memory, the `SoftReference` objects may be garbage collected to free up memory.
  * The main difference between `WeakReference` and `SoftReference` is the likelihood of being garbage collected.

* 问答

  * Q: if there is both a strong reference and a soft reference to an object when the memory is under pressure, will the object be reclaimed by gc?
  * A: No. The JVM will try to free up memory by first collecting only weak references and, if necessary, then collecting soft references. However, if the object still has a strong reference, it will not be garbage collected, even if there is memory pressure.

### StrongReferences

* `StrongReference` is not a separate class in Java because it is simply the default type of reference that objects have in Java. When you create an object and assign it to a variable, the variable holds a strong reference to the object.

***

## rxjava

> 以下代码基于 'io.reactivex.rxjava3:rxjava:3.1.6'

### callback style

1. Callback hell
2. Error handling - Error handling can be a challenge since error handling logic needs to be included in each callback. It can make it difficult to maintain and understand the code.
3. Code duplication
4. Debugging - Debugging with callbacks can be more challenging than with synchronous code
5. Bad Performance - It is due to the overhead of setting up the callbacks and managing the event loop.
6. Context loss

### four key roles
1. `Observable`：`produce event`

   ```java
   public abstract class Observable<T> implements ObservableSource<T> {
     // 一大堆static方法，比如map、zip……
   }
   
   // 实际这才是真正的Observable，只有一个方法，类似于addObserver()
   public interface ObservableSource<T> {
       void subscribe(Observer<? super T> observer);
   }
   ```

2. `Observer`：`consume event`

   ```java
   public interface Observer<T> {
     void onSubscribe(Disposable d);
     void onNext(T t); //对应onNext()
     void onError(Throwable e); //对应throw Exception
     void onComplete();//对应!hasNext()
   }
   ```

3. `Subscribe`：`a connection between observalbe and observer`

4. `Event`：`carry message`

### monad

* 按照道理是不能函数式的，因为有异常有副作用

* 原理是使用了`monad`。即`Observable`是`Monad`，rxjava中操作符起了`monad`的作用

  ```kotlin
  class ObservableMap<T, U> (
    private val source: ObservableSource<T>, 
    private val mapper: (T) -> U,
  ) : Observable<U>() {} //两个入参一个返回值的结构完全符合monad
  ```

* 由于monad的函数组合能力，最终成为一个简洁明了的链式调用

### simple process

1. 创建`Observable`并待产生事件

   * Observable的子类有很多，不同的静态方法会创建不同子类

   * `ObservableZip, Subject, ObservableAmb, ObservableCreate, ObservableJust, ……`

   * 举例

     ```java
     // Observable.java
     public static <@NonNull T> Observable<T> just(@NonNull T item) {
         Objects.requireNonNull(item, "item is null");
       	// 返回一个ObservableJust
         return RxJavaPlugins.onAssembly(new ObservableJust<>(item)); 
     }
     ```

2. 创建`Observer`定义消费事件行为

   * 一般是`subscribe`的时候才创建`Observer`，将其作为参数传入

3. 通过`Subscribe`连接`Observable`和`Observer`

   * Observable.java

     ```java
     public Disposable subscribe(onNext, onError, onComplete) {
       // 将各个回调封成一个LambdaObserver
       LambdaObserver<T> ls = new LambdaObserver<>(onNext, onError, onComplete);
       // 调subscribe方法
       subscribe(ls);
       // 返回disposable
       return ls;
     }
     
     public final void subscribe(Observer<? super T> observer) {
       // 一个hook，一般是啥都不做，返回源observer
       observer = RxJavaPlugins.onSubscribe(this, observer);
       // 这个方法不同的子类实现会不一样
       subscribeActual(observer);
     }
     ```

   * ObservableCreate.java

     ```java
     protected void subscribeActual(Observer<? super T> observer) {
       // 将observer和observerable关联上，一个onNext另一也onNext
       CreateEmitter<T> parent = new CreateEmitter<>(observer);
       // 直接调用oberver的4个回调之一
       observer.onSubscribe(parent);
       // 真正的observable添加observer, 执行到这一行时流才真正开始对外吐数据
       source.subscribe(parent);
     }
     ```

### decorator & operator 

* AbstractObservableWithUpstream.java

  * T -> the input source type
  * U -> the output type

  ```java
  abstract class AbstractObservableWithUpstream<T, U> extends Observable<U> {
    protected final ObservableSource<T> source;
    AbstractObservableWithUpstream(ObservableSource<T> source) {
        this.source = source;
    }
  }
  ```

* Observable#observeOn()

  ```java
  public class ObservableObserveOn<T> extends AbstractObservableWithUpstream<T, T> {
    final Scheduler scheduler;
    
    public ObservableObserveOn(ObservableSource<T> source, Scheduler scheduler) {
      // 父类持有源source
      super(source);
      this.scheduler = scheduler;
    }
    
    protected void subscribeActual(Observer<? super T> observer) {
      Scheduler.Worker w = scheduler.createWorker();
      // 将observer给包了一层，然后在调用作为构造参数传进来的源source.subscibe()
      source.subscribe(new ObserveOnObserver<>(observer, w));
    }
  }
  ```

* Observable#map()

  ```java
  public class ObservableMap<T, U> extends AbstractObservableWithUpstream<T, U> {
    Function<? super T, ? extends U> function;
    
    public ObservableMap(ObservableSource<T> source, Function function) {
      // 父类持有源source
      super(source);
      this.function = function;
    }
    
    public void subscribeActual(Observer<? super U> t) {
      // 将observer给包了一层，然后在调用作为构造参数传进来的源source.subscibe()
      source.subscribe(new MapObserver<T, U>(t, function));
    }
    
    static class MapObserver {
      Function mapper; // 映射函数
      Observer downstream; // 源(老)observer
      
      @Override
      public void onNext(T t) {
        // 先自己处理
        U v = mapper.apply(t);
        // 然后再将处理好的结果交给源
        downstream.onNext(v);
      }
    }
  }
  ```

* 总结

  1. 设计模式很像Fresco的consumer与producer
  2. subscibe顺序 -> **从最右到最左**
  3. onNext顺序 -> **从最左到最右**

### Disposable

* [When and How to Use RxJava Disposable](https://cupsofcode.com/post/when_how_use_rxjava_disposable_serialdisposable_compositedisposable/)
* 为什么建议要在`onDestory`进行`dispose`？因为流的发射通常是网络请求，耗时事件很长，有时结果还没有返回但页面已经退出了，此时应该终止流的进行与订阅，否则可能出现内存泄漏问题

### other

* 绑定时机性
  * 真正开始观察与被观察一定是在`observeralbe.subscible()`后才开始
  * 因为只有`observable.subscibe()` 调用到 `source.subscribe(parent)`才会真正地生成数据流
* 队列性

  * `Observable`发射的是一串事件，而不是一个事件。整串事件被抽象成一个队列
  * 事件流未开始时观察者调用`onSubscribe()`
  * 事件流中每一个事件观察者调用`onNext()`
  * 事件流发生错误时观察者调用`onError()`
  * 事件流结束时观察者调用`onComplete()`
  * 互斥性：`onError()`和`onComplete()`是互斥的。两者之一必被调用一次

## rxjava-subject

* `Represents an Observer and an Observable at the same time, allowing multicasting events from a single source to multiple child Observers.`
* 各种`Subject`的区别有实际代码通过log表现

### 执行顺序

* 验证`Observable、Observer、map`等操作的执行顺序
* 验证`observeOn、subscribeOn`线程的区别

***

## 