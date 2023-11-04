# Java

## by-lazy

### code

* kotlin source code

  ```kotlin
  class Tester {
      private val value: Int by lazy { 200 }
  }
  ```

* decompiled java code

  ```java
  public final class Tester {
     private final Lazy value$delegate;
  
     private final int getValue() {
        Lazy var1 = this.value$delegate;
        Object var3 = null;
        return ((Number)var1.getValue()).intValue();
     }
  
     public Tester() {
        this.value$delegate = LazyKt.lazy((Function0)null.INSTANCE);
     }
  }
  ```

### implementation

* generate a virable `value#delegate` whose type is `Lazy` class
* init `value#delegate` virable value at constructor
* calling `getValue()` is equal to calling `value#delegate.getValue()`

### design

* interface `Lazy`

  ```kotlin
  public interface Lazy<out T> {
      public val value: T // most important, value T
      public fun isInitialized(): Boolean
  }
  ```

* get instance by static method

  ```kotlin
  // pass an initializer
  public actual fun <T> lazy(initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer)
  ```

* SynchronizedLazyImpl - use single instance to get value

  ```kotlin
  override val value: T
      get() {
          val _v1 = _value
          if (_v1 !== UNINITIALIZED_VALUE) { return _v1 as T}
          return synchronized(lock) {
              val _v2 = _value
              if (_v2 !== UNINITIALIZED_VALUE) {
                  _v2 as T
              } else {
                  val typedValue = initializer!!()
                  _value = typedValue
                  initializer = null
                  typedValue
              }
          }
      }
  ```

* UnsafeLazyImpl - it's not thread safe

  ```kotlin
  override val value: T
      get() {
          if (_value === UNINITIALIZED_VALUE) {
              _value = initializer!!()
              initializer = null
          }
          return _value as T
      }
  ```

***

## class-loader & class

### class feature

* The primitive Java types (boolean, byte, char, short, int, long, float, and double), and the keyword void are also represented as Class objects.
* Class has no public constructor. Instead a Class object is constructed automatically by the Java Virtual Machine when a class is derived from the bytes of a class file through the invocation of one of three methods.

### BootStrapClassLoader

* The *Bootstrap Classloader*, being a classloader and all, is actually a part of the JVM Core and it is written in native code.
* All classloaders, with the exception of the bootstrap classloader, are implemented as Java classes. 
* BootstrapClassloader is not a subclass of `java.lang.ClassLoader`.
* The bootstrap classloader is platform specific machine instructions that kick off the whole classloading process.

### class loader feature

* Every Class object contains a reference to the `ClassLoader` that defined it.

  ```java
  // Some implementations may use null to represent the bootstrap class loader.
  public final class Class<T> {
    public ClassLoader getClassLoader() {}
  }
  ```

### loadClass

```java
protected Class<?> loadClass(String name, boolean resolve) {
  // First, check if the class has already been loaded
  Class<?> c = findLoadedClass(name); // implemented in native code
  if (c == null) {
    try {
      if (parent != null) {
        c = parent.loadClass(name, false); // delegate to parent
      } else {
        c = findBootstrapClassOrNull(name);
      }
    } catch (ClassNotFoundException e) {}

    if (c == null) {
      // If still not found, then invoke findClass in order
      // to find the class.
      c = findClass(name);
    }
  }
  return c;
}
```

***

## collection

### inheritance relationship

```bash
- Iterable
	- Collection
		- List
			- ArrayList
			- Vector
			- LinkedList
			- Stack
			- CopyOnWriteArrayList
		- Queue
			- Deque
				- ArrayDeque
				- LinkedBlockingDeque
				- ConcurrentLinkedDeque
				- LinkedList
      - ArrayBlockingQueue
      - ConcurrentLinkedQueue
      - SynchronousQueue
      - PriorityQueue
      - DelayQueue
- Map
	- ConcurrentHashMap
	- HashMap
		- LinkedHashMap
	- Dictionary
		- Hashtable
  - SortedMap
  	- TreeMap
```

### List

* ArrayList

  * what? - It is a dynamic, resizable array that can grow or shrink at runtime.

  * feature - It is not thread-safe.

  * code

    ```java
    public class ArrayList<E> extends AbstractList<E> implements List<E> {
      transient Object[] elementData;
      
      public boolean add(E e) {
        modCount++; // modCount自增，用于抛ConcurrentModifyException
        if (s == elementData.length)
            elementData = grow(); // 扩容
        elementData[s] = e; // 赋值
        size = s + 1; // size + 1
        return true;
      }
      
      private Object[] grow(int minCapacity) {
        // 通过Arrays.copy执行扩容
        return elementData = Arrays.copyOf(elementData, newCapacity);
      }
      
      private void checkForComodification(final int expectedModCount) {
        // modCount不符合预期，直接抛并发异常
        if (modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
      }
    }
    ```

* Vector

  * It is almost the same as ArrayList, but it is thread-safe.
  * 线程安全的原因是很多方法都带了`synchronized`关键字，就很蠢

* LinkedList

  ```java
  public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E> {
    // 核心成员，双向链表的首尾节点
    transient Node<E> first;
    transient Node<E> last;
    
    // 双向链表的节点
    private static class Node<E> {
      E item;
      Node<E> next;
      Node<E> prev;
  		
      // 构造方法顺序很好 -> 前中后
      Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
      }
    }
    
    public void addFirst(E e) {
      final Node<E> f = first;
      final Node<E> newNode = new Node<>(null, e, f);
      first = newNode;
      if (f == null)
          last = newNode;
      else
          f.prev = newNode;
      size++;
      modCount++; // 增加modCount用于ConcurrentModificationException
    }
  }
  ```

* Stack

  * feature
    * signature - `public class Stack<E> extends Vector<E>`
    * 本质还是使用ArrayList扩容数组的思路实现的
  * 一般不直接用这个类，而是`Deque<Object> stack/queue = new LinkedList<>();`

* CopyOnWriteArrayList

  * what? - It is a thread-safe variant of the standard `ArrayList`. 

  * code

    ```java
    public class CopyOnWriteArrayList<E> implements List<E> {
      // 用一个对象来当同步块的锁
      final transient Object lock = new Object();
      // 存放数据的数组，声明为了volatile不缓存
      private transient volatile Object[] array;
      
      public boolean add(E e) {
        synchronized (lock) { // write的时候直接上锁
          Object[] es = getArray();
          int len = es.length;
          es = Arrays.copyOf(es, len + 1); // 直接十分粗暴的复制一个数组
          es[len] = e; // 数组末尾赋值
          setArray(es);
          return true;
        }
      }
      
      public E set(int index, E element) {
        synchronized (lock) {
          Object[] es = getArray();
          E oldValue = elementAt(es, index);
          if (oldValue != element) {
              es = es.clone(); // 方法基本同上，暴力复制一个数组，然后再改
              es[index] = element;
          }
          // Ensure volatile write semantics even when oldvalue == element
          setArray(es);
          return oldValue;
        }
      }
      
      public E get(int index) {
          return array[index]; // 读时不加任何锁，直接暴力读
      }
    }

### Deque

* ArrayDeque

  * feature

    1. 顾名思义，用数组来实现队列
    2. 设计思路还挺重要的

  * code

    ```java
    public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E> {
      transient Object[] elements;
      transient int head; // 当elements不为空时，elements[head]肯定不为空
      transient int tail; // 无论elements是否为空，elements[tail]肯定为空
      
      public ArrayDeque() {
        elements = new Object[16 + 1]; // 构造函数，初始17个元素
      }
      
      static final int inc(int i, int modulus) {
        // 自增，如果自增后 == length，说明到头了，index变为最小值0
        if (++i >= modulus) i = 0;
        return i;
      }
    
      static final int dec(int i, int modulus) {
        // 自减，如果自减后 == -1，说明到头了，index变为最大值 length - 1
        if (--i < 0) i = modulus - 1;
        return i;
      }
      
      public void addFirst(E e) {
        final Object[] es = elements;
        es[head = dec(head, es.length)] = e; // head自减
        if (head == tail)
          grow(1); // 扩容
      }
      
      public void addLast(E e) {
        final Object[] es = elements;
        es[tail] = e; // tail位肯定没元素，直接赋值
        if (head == (tail = inc(tail, es.length))) // tail自增
          grow(1);
      }
    }
    ```

* LinkedBlockingDeque

  ```java
  public class LinkedBlockingDeque<E> extends AbstractQueue<E> implements BlockingDeque<E> {
    static final class Node<E> { // 双向链表的节点
      E item;
      Node<E> prev;
      Node<E> next;
      Node(E x) { item = x; }
    }
    
    transient Node<E> first; // 首尾节点
    transient Node<E> last;
    
    final ReentrantLock lock = new ReentrantLock(); // 可重入锁
    private final Condition notEmpty = lock.newCondition(); // 用来takeFirst()阻塞用的
    
    public boolean offerFirst(E e) {
      Node<E> node = new Node<E>(e);
      final ReentrantLock lock = this.lock;
      lock.lock(); // 可重入锁上锁
      try {
        Node<E> f = first;
        node.next = f;
        first = node;
        if (last == null) 
          last = node; // 空链表情况
        else
          f.prev = node;
        ++count;
        notEmpty.signal(); // 很重要，当有takeFirst()阻塞时，通过这里唤醒
        return true;
      } finally {
        lock.unlock(); // 解可重入锁
      }
    }
    
    public E takeFirst() throws InterruptedException {
      final ReentrantLock lock = this.lock;
      lock.lock(); // 可重入锁上锁
      try {
        E x;
        while ( (x = unlinkFirst()) == null)
          notEmpty.await(); // 当链表为空，在这里阻塞住等待唤醒
        return x;
      } finally {
        lock.unlock(); // 解可重入锁
      }
    }
  }
  ```

* ConcurrentLinkedDeque
  * 有点复杂，没看懂，总之是用的`volatile + CAS`来确保线程安全

### Queue

* ArrayBlockingQueue

  * 声明 - `public class ArrayBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>`
  * 特点
    1. 使用数组来实现`Queue`，详看`ArrayDeque`
    2. `dequeue()`时没有元素会阻塞，通过`Condition`来实现，详看`LinkedBlockingDeque`

* ConcurrentLinkedQueue

  * 特点 - 也很复杂，实现方式类似于`ConcurrentLinkedDeque`，总之是用的`volatile + CAS`来确保线程安全

* SynchronousQueue

  * 特点 - 很复杂，使用了一层代理 + `CAS`来实现

* PriorityQueue

  ```java
  public class PriorityQueue<E> extends AbstractQueue<E> {
    // the two children of queue[n] are queue[2*n+1] and queue[2*(n+1)]
    transient Object[] queue; // 看似数组，实际是平衡二叉树
    private final Comparator<? super E> comparator; // 用于排序的Comparator
    
    public boolean offer(E e) {
      modCount++; // 用于抛 ConcurrentModificationException
      int i = size;
      if (i >= queue.length)
        grow(i + 1); // 容量不够了扩容
      siftUpUsingComparator(i, e, queue, comparator); // 进行排序
      size = i + 1;
      return true;
    }
    
    private static <T> void siftUpUsingComparator(
          int k, T x, Object[] es, Comparator<? super T> cmp) {
        while (k > 0) { // 排序算法有点迷惑，看不明白
          int parent = (k - 1) >>> 1;
          Object e = es[parent];
          if (cmp.compare(x, (T) e) >= 0)
            break;
          es[k] = e;
          k = parent;
        }
        es[k] = x;
    }
  }
  ```

* DelayQueue

  * annotation - An unbounded blocking queue of Delayed elements, in which an element can only be taken when its **delay has expired**.

  * code

    ```java
    public class DelayQueue<E extends Delayed> extends AbstractQueue<E> {
      private final transient ReentrantLock lock = new ReentrantLock(); // 可重入锁
      private final PriorityQueue<E> q = new PriorityQueue<E>(); // 将队列操作委托给它
      private final Condition available = lock.newCondition(); // condition
      
      public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
          q.offer(e);
          if (q.peek() == e) {
            leader = null;
            available.signal(); // 插入成功，进行notify()
          }
          return true;
        } finally {
          lock.unlock();
        }
      }
      
      public E take() {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
          available.await(); // 先await一下
          return q.poll(); // 然后才允许poll获取元素
        } finally {
          lock.unlock();
        }
      }
    }
    ```


### Map

* HashMap

  ```java
  public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V> {
    
    static final int TREEIFY_THRESHOLD = 8; // 桶中元素超过8个就转为红黑树
  
    static class Node<K,V> implements Map.Entry<K,V> {
      final int hash; // hash值
      final K key; // key不可变
      V value; // value可变
      Node<K,V> next; // 单向链表的下一个节点
    }
    // 根据继承关系，它继承自HashMap.Node
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {}
    
    transient Node<K,V>[] table; // table的元素既可以是单向链表HashMap.Node，也能是红黑树HashMap.TreeNode
    int threshold; // 总数达到这个就扩容
    final float loadFactor; // 用来计算threshold = table.length * loadFactor
    
    public HashMap() {
      // 空构造函数只是赋loadFactor为0.75
      this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }
    
    static final int hash(Object key) {
        int h;
      	// 计算hash值，将高16位与低16位做异或
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
    
    final Node<K,V>[] resize() {
      Node<K,V>[] oldTab = table; // 旧的table
      int oldCap = (oldTab == null) ? 0 : oldTab.length; // 旧的table容量
      int oldThr = threshold;
      int newCap, newThr = 0;
      if (oldCap > 0) {
        if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && // 新容量直接翻倍double
               oldCap >= DEFAULT_INITIAL_CAPACITY)
          newThr = oldThr << 1; // 新阈值翻倍double
      } else if (oldThr > 0) { // initial capacity was placed in threshold
        newCap = oldThr; // 构造函数内对threshold进行了赋值
      } else {
        // 完全第一次初始化，容量为16，阈值为(16 * 加载因子)
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
      }
      threshold = newThr; // 赋值给threshold
      Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
      table = newTab; //创建新容量的table，并赋给table成员变量
      if (oldTab != null) {
        // 进行换桶操作
        if (e.next == null) {
          newTab[e.hash & (newCap - 1)] = e; // hash取与方法
        } else if (e instanceof TreeNode) {
          ((TreeNode<K,V>)e).split(this, newTab, j, oldCap); // 执行红黑树的相关操作
        } else { 
          // 执行二倍扩容基础的链表换桶操作，基本是原桶元素一半在原桶，一半在新桶
        }
      }
    }
    
    // put都会调到这个方法
    V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
      Node<K,V>[] tab; Node<K,V> p; int n, i; // tab -> 当前桶，p -> 桶中当前hash的元素, n -> 桶长
      if ((tab = table) == null || (n = tab.length) == 0) // 赋值tab，赋值length
        	n = (tab = resize()).length; // 执行resize()扩容，赋值length
      if ((p = tab[i = (n - 1) & hash]) == null)  // 当前这个桶为空
        tab[i] = newNode(hash, key, value, null); // 为空简单，直接放进桶中当第一个元素
      else {
        Node<K,V> e; K k; // k -> 桶中当前hash元素的key
        if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) // 桶中元素key相同case，即put的key相同但value一般不同的情况
          e = p;
        else if (p instanceof TreeNode) // 红黑树的情况
          e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value); // 红黑树插入，太复杂了
        else {
          for (int binCount = 0; ; ++binCount) {
            if ((e = p.next) == null) { // 单向链表向下遍历，找到链表尾
              p.next = newNode(hash, key, value, null); // 插入到链表尾部
              if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st 桶元素大于8直接链表转红黑树
                treeifyBin(tab, hash); // 把链表转成红黑树
              break;
            }
            if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) // Key相同case
              break;
            p = e;
          }
        }
        if (e != null) { // existing mapping for key
          V oldValue = e.value; 
          if (!onlyIfAbsent || oldValue == null)
            e.value = value; // 相同key修改值
          afterNodeAccess(e);
          return oldValue;
        }
      }
      if (++size > threshold)
      	resize(); // 达到扩容条件，直接扩容
    } 
  }
  ```

* LinkedHashMap

  * signature - `public class LinkedHashMap<K,V> extends HashMap<K,V> implements Map<K,V>`, 继承自map

  * code

    ```java
    public class HashMap<K,V> {
      
      // Callbacks to allow LinkedHashMap post-actions
      void afterNodeAccess(Node<K,V> p) { } // 暴露了几个hook给LinkedHashMap
      void afterNodeInsertion(boolean evict) { }
      void afterNodeRemoval(Node<K,V> p) { }
      
      final V putVal() {
        afterNodeAccess(e); // 删改的某些时机调用这些hook
        afterNodeInsertion(evict);
      }
    }
    
    public class LinkedHashMap<K,V> extends HashMap<K,V> { // 继承自HashMap
    
      transient LinkedHashMap.Entry<K,V> head; // The head (eldest) of the doubly linked list.
      transient LinkedHashMap.Entry<K,V> tail; // The head (eldest) of the doubly linked list.
      
    }
    ```

  * feature

    * `HashMap` does not guarantee any specific order of the entries. It uses the hash code of the keys to store and retrieve the values in the map, so the order is determined by the hash codes. 
    * `LinkedHashMap` maintains the order in which the entries were inserted into the map. It uses a doubly linked list to maintain the order of the keys and values. This means that iterating over a `LinkedHashMap` returns the entries in the same order in which they were added to the map.
    * In summary, if you require a map that maintains the order of the entries, you should use `LinkedHashMap`. Otherwise, use `HashMap`.

* Hashtable

  * 实验原理和`HashMap`一样
  * 线程安全，但实现很蠢，是方法全部加`synchronized`关键字实现的，性能弱于`HashMap`

* ConcurrentHashMap

  * feature

    1. 实现原理和`HashMap`基本类似，桶 + 链表或者红黑树，伺机进行桶扩容
    2. 并发实现 -> `volatile, CAS, synchronized()`
    3. 能用CAS尽量用CAS，要上锁也是锁一个桶而不是锁整个`HashMap`，最大的锁粒度是一个桶

  * code

    ```java
    V putVal(K key, V value, boolean onlyIfAbsent) {
      Node<K,V> f
      if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
        // no lock when adding to empty bin, 该用CAS的时候就用CAS
        if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value))) 
      }
      
      synchronized (f) {
        // 是在不行要上锁，但也只是锁一个桶而不是把整个HashMap都锁住，将锁的粒度降到最低
      }
    }
    ```

* TreeMap

  * feature

    1. The underlying data structure of `TreeMap` is a red-black tree. A red-black tree is a self-balancing binary search tree.

  * code

    ```java
    public class TreeMap<K,V> extends AbstractMap<K,V> {
      private final Comparator<? super K> comparator; // 排序的比较器
      private transient Entry<K,V> root; // 红黑树的根节点
      private static final boolean RED   = false; // 红黑树用到的一些值
      private static final boolean BLACK = true;
      
      private V put(K key, V value, boolean replaceOld) {
        Entry<K,V> t = root; // 树的根节点
        int cmp;
        Entry<K,V> parent;
        Comparator<? super K> cpr = comparator;
        do {
          parent = t;
          cmp = cpr.compare(key, t.key);
          if (cmp < 0) // 根据cmp来取左还是取右
            t = t.left;
          else if (cmp > 0)
            t = t.right;
          else {
            V oldValue = t.value;
            if (replaceOld || oldValue == null) {
              t.value = value;
            }
            return oldValue;
          }
        } while (t != null); // 循环找到parent
        // 经过上面的操作，找到了应该插入的parent
        Entry<K,V> e = new Entry<>(key, value, parent);
        if (addToLeft) / 执行插入节点操作
          parent.left = e;
        else
          parent.right = e;
        fixAfterInsertion(e); // 执行红黑树平衡
      }
      
      final Entry<K,V> getEntryUsingComparator(Object key) {
        K k = (K) key;
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {
          Entry<K,V> p = root;
          while (p != null) { // 因为是红黑树，所以直接比较
            int cmp = cpr.compare(k, p.key);
            if (cmp < 0) // 小于取走
              p = p.left;
            else if (cmp > 0) // 大于取右
              p = p.right;
            else
              return p; // 相等直接返回
          }
        }
        return null;
      }
      
    }
    ```


***

## concurrency

### AtomicInteger

* class code

  ```java
  public class AtomicInteger extends Number {
    // 一个Unsafe
    private static final Unsafe U = Unsafe.getUnsafe();
    // 一个volatile
    private volatile int value;
    // 这个操作不是线程安全的
    public final void set(int newValue) { value = newValue; }
    
    public final boolean compareAndSet(int expectedValue, int newValue) {
      //调用U提供的CAS api，这是线程安全的
      return U.compareAndSetInt(this, VALUE, expectedValue, newValue);
    }
    
    public final int getAndIncrement() {
        return U.getAndAddInt(this, VALUE, 1);
    }
  }
  ```

* example

  ```java
  private void testAtomicInteger() {
      AtomicInteger ai = new AtomicInteger();
      ai.set(10); // 这一步不保证thread-safe，因为它仅仅是给一个volatile的变量赋值
      System.out.println(ai.incrementAndGet()); // 这一步是thread-safe的，因此它底层是CAS
  }

### Lock

* AbstractQueuedSynchronizer

  * It implements most of the synchronization mechanics that are needed by a concurrent synchronizer, including a wait queue, thread acquisition and release, and reentrant support.

  * It is a basic framework for implementing locks, semaphores, and similar kinds of thread synchronization constructs in Java.

  * code

    ```java
    public abstract class AbstractQueuedSynchronizer
        extends AbstractOwnableSynchronizer {
      
      abstract static class Node {
        volatile Node prev;
        volatile Node next;
        Thread waiter; // 队列节点，存放线程
        volatile int status;
      }
      
      int acquire(Node node, int arg, boolean shared, boolean interruptible, boolean timed, long time) {
        LockSupport.park(this); // 挂起线程，暂停调度
      }
      
      public final void acquire(int arg) {
        // 当锁没获得成功时，就调动方法挂起当前线程
        if (!tryAcquire(arg)) {
          acquire(null, arg, false, false, false, 0L);
        }
      }
      
      public final boolean release(int arg) {
        if (tryRelease(arg)) {
          LockSupport.unpark(s.waiter); // 线程可以继续调度
          return true;
        }
        return false;
      }
    }

* How to implement a lock by AbstractQueuedSynchronizer

  ```java
  class Mutex implements Lock {
    
    private static class Sync extends AbstractQueuedSynchronizer {
      
      public boolean tryAcquire(int acquires) {
        if (compareAndSetState(0, 1)) {
          // CAS上锁成功，当前线程持有锁，返回true
          setExclusiveOwnerThread(Thread.currentThread());
          return true;
        }
        // 没上锁成功，让抽象Sync去挂起当前线程
        return false;
      }
      
      protected boolean tryRelease(int releases) {
        if (getExclusiveOwnerThread() != Thread.currentThread()) {
          // 当前线程都没持有锁，肯定不能进行释放操作
          throw new IllegalMonitorStateException();
        }
        // 清空线程，设置状态为0
        setExclusiveOwnerThread(null);
        setState(0);
        return true;
      }
      
      public boolean isLocked() { return getState() != 0; }
    }
    
    // 把所有活儿都委托给这个sync
    private final Sync sync = new Sync();
    
    public void lock() { sync.acquire(1); } // acquire() 会先调到tryAcquire()
    public boolean tryLock()  { return sync.tryAcquire(1); }    
    public void unlock() { sync.release(1); }
  }
  ```

* fair lock and unfair lock 

  ```java
  //see Semaphore.java
  
  class NonfairSync extends AbstractQueuedSynchronizer {
    
  	int tryAcquireShared(int acquires) {
      for (;;) {
        int available = getState();
        int remaining = available - acquires;
  			// 非公平锁上来就直接看是否还有剩余，有剩余管都不管队列里面的直接尝试获取锁
        if (remaining < 0 ||
          compareAndSetState(available, remaining))
          return remaining;
      }
    }
  }
  
  class FairSync extends AbstractQueuedSynchronizer {
    int tryAcquireShared(int acquires) {
      for (;;) {
        // 公平锁会先处理队列里面已经排队久等了的线程
        if (hasQueuedPredecessors())
          return -1;
        int available = getState();
        int remaining = available - acquires;
        // 然后在去尝试获取锁
        if (remaining < 0 ||
          compareAndSetState(available, remaining))
          return remaining;
      }
    }
  }
  ```

* How to implement a ReentrantLock

  ```java
  public class ReentrantLock implements Lock {
    abstract static class Sync extends AbstractQueuedSynchronizer {
      
      // 上锁
      final boolean tryLock() {
        Thread current = Thread.currentThread();
          int c = getState();
        	// 因为可重入锁只能允许一个线程拥有，所以直接判断 c==0
          if (c == 0) {
            if (compareAndSetState(0, 1)) {
              // 上锁成功
              setExclusiveOwnerThread(current);
              return true;
            }
          } else if (getExclusiveOwnerThread() == current) {
            // 可重入锁，线程一样，就直接获得锁并把state++
            if (++c < 0) // overflow
              throw new Error("Maximum lock count exceeded");
            setState(c); // 因为是同线程操作，所以虽然没CAS，但这里改volatile的值是线程安全的
            return true;
          }
          return false;
        }
      }
    
    	final boolean tryRelease(int releases) {
        int c = getState() - releases;
        // 线程不一样，没可能释放，直接抛异常
        if (getExclusiveOwnerThread() != Thread.currentThread())
          throw new IllegalMonitorStateException();
        boolean free = (c == 0);
        if (free) {
          // 这个线程不拥有锁了，把owner线程置空
          setExclusiveOwnerThread(null);
        } else {
          // 还拥有锁，修改state的值。因为是同线程操作，所以虽然没CAS，但这里改volatile的值是线程安全的
          setState(c);
        }
        return free; // 返回此线程是否彻底释放
      }
    }
  }
  ```

* How to implement ReentrantReadWriteLock

  ```java
  public class ReentrantReadWriteLock implements ReadWriteLock {
    
    private final ReentrantReadWriteLock.ReadLock readerLock;
    private final ReentrantReadWriteLock.WriteLock writerLock;
    
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this); // 读锁和写锁是分开的
        writerLock = new WriteLock(this); // 读锁和写锁是分开的
    }
    
    abstract static class Sync extends AbstractQueuedSynchronizer {
      // state一共32位，高16位用作读锁，低16位用作写锁
      static final int SHARED_SHIFT   = 16; 
      static final int SHARED_UNIT    = (1 << SHARED_SHIFT); // 读锁+1时实际加的值
      static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1; // 锁最大值，低16位全是1
      static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1; // 用于&，高16位是0，低16位是1
      
      static int sharedCount(int c) { return c >>> SHARED_SHIFT; } // 读锁，取高16位
      static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; } // 写锁，取低16位
      
      // 上写锁
      boolean tryWriteLock() {
        Thread current = Thread.currentThread();
        int c = getState();
        if (c != 0) { // c != 0 代表有读||有写
          int w = exclusiveCount(c); // w代表写的数量
          if (w == 0 || current != getExclusiveOwnerThread())
            // 此分支代表当前有读，无写，但其他线程尝试上写锁，return false
            return false;
          if (w == MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        }
        // 执行到这里，已经确定了 c == 0 || ownerThread == currentThread
        // 当前无读无写，或者当前线程无读有写，尝试上第一次写锁或者写锁加一
        if (!compareAndSetState(c, c + 1)) {
          return false;
        } else {
          setExclusiveOwnerThread(current);
        	return true;
        }
      }
      
      // 上读锁
      final boolean tryReadLock() {
        Thread current = Thread.currentThread();
        for (;;) {
          int c = getState();
  				// 已有写锁，且不是同一个线程，肯定return false
          if (exclusiveCount(c) != 0 &&
            getExclusiveOwnerThread() != current)
            return false;
          int r = sharedCount(c);
          // 读线程数量最大了，抛异常
          if (r == MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
          // 无写锁，同一个线程，则可以继续加读锁
          compareAndSetState(c, c + SHARED_UNIT)
          return true;
        }
      }
    }
  }
  ```

### Executor

* ThreadPoolExecutor

  ```java
  public class ThreadPoolExecutor extends AbstractExecutorService {
    
    // 一个32的int，表示两个数值 - workerCount和runState
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    
    private static final int RUNNING    = -1 << COUNT_BITS; // 各种状态runState
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;
    
    private static int runStateOf(int c)     { return c & ~COUNT_MASK; } // 从ctl取runState
    private static int workerCountOf(int c)  { return c & COUNT_MASK; } // 从ctl取workerCount
    private static int ctlOf(int rs, int wc) { return rs | wc; } 
    
    // Core pool size is the minimum number of workers to keep alive (and not allow to time out etc) unless allowCoreThreadTimeOut is set, in which case the minimum is zero.
    private volatile int corePoolSize; 
    private volatile int maximumPoolSize; // Maximum pool size.
    // Timeout in nanoseconds for idle threads waiting for work. Threads use this timeout when there are more than corePoolSize present or if allowCoreThreadTimeOut. Otherwise they wait forever for new work.
    private volatile long keepAliveTime;
    
    // The queue used for holding tasks and handing off to worker threads.
    private final BlockingQueue<Runnable> workQueue; // 待运行的Runnable，注意用的是Blocking的数据结构
    // Set containing all worker threads in pool. Accessed only when holding mainLock.
    private final HashSet<Worker> workers = new HashSet<>(); // 真正的线程池
    
    // 将线程给包一层，它继承了AbstractQueuedSynchronizer，说明访问它是互斥的
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
      final Thread thread; // 对应被包装的线程
      Runnable firstTask; // 构建时首次的Runnable，真正运行Runnable从队列里面取
      volatile long completedTasks; // 这个线程总共执行了多少个Runnable
      
      Worker(Runnable firstTask) {
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this); // 创建新的线程
      }
      
      
      public void run() { runWorker(this); } //** Delegates main run loop to outer runWorker. */
    }
    
    public void execute(Runnable command) {
      int c = ctl.get();
      if (workerCountOf(c) < corePoolSize) { // 核心线程数量不够，直接创建新线程
        if (addWorker(command, true))
          return;
        c = ctl.get();
      }
      if (isRunning(c) && workQueue.offer(command)) { // 正在运行且加入待运行队列成功
        int recheck = ctl.get();
        if (!isRunning(recheck) && remove(command))
          reject(command);
        else if (workerCountOf(recheck) == 0)
          addWorker(null, false);
      }
      else if (!addWorker(command, false))
        reject(command);
    }
    
    private boolean addWorker(Runnable firstTask, boolean core) {
      workerCountOf(c) >= (core ? corePoolSize : maximumPoolSize); // 根据是否core判断数量
      Worker w = new Worker(firstTask);
      workers.add(w); // 创建一个worker，然后加到set里面
      Thread t = w.thread;
      if (workerAdded) {
        container.start(t); // 把worker里面的线程取出来直接run
        workerStarted = true;
      }
    }
    
    private Runnable getTask() {
      for (;;) {
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize; // 看是否要减少worker数量
      	if ((wc > maximumPoolSize || (timed && timedOut))
          && (wc > 1 || workQueue.isEmpty())) {
          if (compareAndDecrementWorkerCount(c))
            return null; // 判断应该减少worker时，不阻塞地尝试获取Runnable，而是直接 return null 
          continue;
        }
        compareAndDecrementWorkerCount(c)
        Runnable r = timed ?
          workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
          workQueue.take(); // 阻塞式地从workQueue中取Runable出来
        return r;
      }
    }
    
    final void runWorker(Worker w) {
      Thread wt = Thread.currentThread(); // 当前线程
      Runnable task = w.firstTask;
      w.firstTask = null; // 清空首次的Runable
      while (task != null || (task = getTask()) != null) { // 从BlockingQueue中取Runable
        w.lock(); // 此worker(线程)上锁
        beforeExecute(wt, task); // hook
        task.run(); // 直接执行Runnable
  			afterExecute(task, null); // hook
        w.completedTasks++; // 总数++
        w.unlock();
      }
      processWorkerExit(w, completedAbruptly); // 如果task是空，就退出while了，将此worker删除
    }
  }
  ```

  * 执行顺序
    1. `execute()`如果 worker 不够就新建worker，然后把 Runnable给添加进 BlockingQueue
    2. 上条新建worker完成后，会把worker放进一个set里，并在 `addWorker()` 末尾让 worker 对应的线程直接 start 起来
    3. 由于 worker 实现了 Runnable 接口，因此直接调用到 run() 方法，而 run() 方法又直接调用 `ThreadPoolExecutor#runWorker(Worker w)` 方法
    4. 因此可以假设有10个线程都在同时调用 `runWorker(Worker w)`方法
    5. 因此这个方法里面有一个`while (task != null || (task = getTask()) != null)`，且用了BlockingQueue，因此实现了事件循环。当 task 返回为空时，就自然执行结束，删除worker

### ThreadLocal

* usage example

  * Thread-local variables can be useful when you need to store data that is specific to a particular execution context, such as the current user session or request being processed by a web server. Using `ThreadLocal` can help you avoid synchronization problems that might otherwise occur if multiple threads were accessing the same shared resource at the same time. 

* code

  ```java
  public class ThreadLocal<T> {
    
    static class ThreadLocalMap {
      Object value;
      Entry(ThreadLocal<?> k, Object v) {
        value = v; // key是ThreadLocal，value是Object
      }
    }
    public void set(T value) {
      // set方法直接第一个参数传currentThread()
      set(Thread.currentThread(), value);
    }
    
    private void set(Thread t, T value) {
      ThreadLocalMap map = getMap(t); // 获取Thread的成员变量ThreadLocals
      if (map != null) {
        map.set(this, value);
      } else {
        createMap(t, value);
      }
    }
    
    ThreadLocalMap getMap(Thread t) {
      return t.threadLocals; //Thread类有一个成员变量threadLocals
    }
    
    void createMap(Thread t, T firstValue) {
      // createMap实际就是给Thread的成员变量new一个对象赋值
      t.threadLocals = new ThreadLocalMap(this, firstValue);
    }
  }
  ```

* relation

  * 1 thread - 1 ThreadLocalMap
  * 1 ThreadLocalMap - { n TheadLocal, n value }
  * 即每一个Thread都有一个map实例，而`ThreadLocal`变量仅仅起到一个`Key`的作用

* example - [see project source code](https://github.com/IzumiSakai-zy/self-private/blob/main/java/concurrency/src/main/java/ThreadLocalTest.java)

***

## dynamic-proxy

### version control

* all the analysis below is based on `jdk 20`

  ```
  openjdk version "20.0.1" 2023-04-18
  OpenJDK Runtime Environment Homebrew (build 20.0.1)
  OpenJDK 64-Bit Server VM Homebrew (build 20.0.1, mixed mode, sharing)
  ```

### comments

* how to use

  ```java
  InvocationHandler handler = new MyInvocationHandler(...);
  Foo f = (Foo) Proxy.newProxyInstance(Foo.class.getClassLoader(), 
                                       new Class<?>[] { Foo.class },
                                       handler);
  
  public static Object newProxyInstance(ClassLoader loader,
                                            Class<?>[] interfaces,
                                            InvocationHandler h)
  ```

* explain

  ```
  A proxy class is a class created at runtime that implements a specified list of interfaces, known as proxy interfaces. A proxy instance is an instance of a proxy class. Each proxy instance has an associated invocation handler object, which implements the interface InvocationHandler. A method invocation on a proxy instance through one of its proxy interfaces will be dispatched to the invoke method of the instance's invocation handler, passing the proxy instance, a java.lang.reflect.Method object identifying the method that was invoked, and an array of type Object containing the arguments. The invocation handler processes the encoded method invocation as appropriate and the result that it returns will be returned as the result of the method invocation on the proxy instance.
  ```

* key feature

  1. created at runtime
  2. use asm to generate a class
  3. all methods are dispatched to `InvocationHandler#invoke(Object proxy, Method method, Object[] args)`

### process

* input parameters

  ```java
  /**
  	* loader -> jdk.internal.loader.ClassLoaders$AppClassLoader
  	* interfaces -> [main.IConsumer]
  	* h -> ConsumerProxy
  **/
  public static Object newProxyInstance(ClassLoader loader,
                                        Class<?>[] interfaces,
                                        InvocationHandler h)
  ```

* get proxy class name

  ```java
  private static Class<?> defineProxyClass() {
    String proxyName = context.packageName().isEmpty()
      ? proxyClassNamePrefix + num
      : context.packageName() + "." + proxyClassNamePrefix + num;
    // proxyName -> jdk.proxy1.$Proxy0
  }
  ```

* use asm to generate a class

  ```java
  final class ProxyGenerator extends ClassWriter {
    
    private final ClassLoader loader; //jdk.internal.loader.ClassLoaders$AppClassLoader
    private final String className; // jdk.proxy1.$Proxy0
    private final List<Class<?>> interfaces; //[main.IConsumer]
    private int proxyMethodCount; //3
    
    
    private byte[] generateClassFile() {
      addProxyMethod(hashCodeMethod);
      addProxyMethod(equalsMethod);
      addProxyMethod(toStringMethod);
      
      generateConstructor();
      generateStaticInitializer();
      generateLookupAccessor();
      return toByteArray(); // 生成字节码
    }
  }
  ```

  ```java
  public class ClassWriter extends ClassVisitor {
    private final SymbolTable symbolTable; // class字节码的符号表
    private int accessFlags; // class字节码的accessFlag
    
    public byte[] toByteArray() {
      symbolTable.addConstantUtf8(Constants.NEST_MEMBERS);
      ByteVector result = new ByteVector(size);
      AnnotationWriter.putAnnotations();
      return result.data;
    }
  }
  ```

* get constructor of generated class

  ```java
  public static Object newProxyInstance() {
    // cons -> public jdk.proxy1.$Proxy0(java.lang.reflect.InvocationHandler)
    Constructor<?> cons = getProxyConstructor(caller, loader, interfaces);
  }
  ```

* use constructor to newinstance and return

  ```java
  private static Object newProxyInstance(Class<?> caller,
                                         Constructor<?> cons,
                                         InvocationHandler h) {
    return cons.newInstance(new Object[]{h});
  }
  ```

* run method

  ```java
  public class ConsumerProxy implements InvocationHandler {
    /**
    	* proxy -> jdk.proxy1.$Proxy0
    	* method -> public abstract void main.IConsumer.buy()
    	* args -> null
    **/
    public Object invoke(Object proxy, Method method, Object[] args) {
      before();
      Object returnValue = method.invoke(consumer, args);
      after();
      return returnValue;
    }
  }
  ```

  

## invokedynamic

### reference

* [Java 8 Lambdas - A Peek Under the Hood](https://www.infoq.com/articles/Java-8-Lambdas-A-Peek-Under-the-Hood/)
* [Method handles and lambda metafactory](https://wttech.blog/blog/2020/method-handles-and-lambda-metafactory/)

### structure and content

* structure

  ```
  - main
  	- test
  		- AICTest.java
  		- LambdaTest.java
    - view
    	- OnClickListener.java
    	- View.java
  ```

* content

  ```java
  // AICTest.java
  public class AICTest {
    public AICTest() {
      View view = new View();
      view.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          System.out.println("onClick call, view hashcode = " + view.hashCode());
        }
      });
    }
  }
  ```

  ```javascript
  public class LambdaTest {
    public LambdaTest() {
      View view = new View();
      view.setOnClickListener(v -> System.out.println("onClick call, view hashcode = " + v.hashCode()));
    }
  }
  ```

### anonymous inner classes

* 执行`javac main/test/AICTest.java`

  ```
  - main
  	- test
  		- AICTest.class
  		- AICTest$1.class
  ```

* `AICTest$1.class`反编译

  ```java
  class AICTest$1 implements OnClickListener {
    AICTest$1(AICTest var1) {}
    
    public void onClick(View var1) {
      System.out.println("onClick call, view hashcode = " + var1.hashCode());
    }
  }
  ```

* 执行`javap -p -v main.test.AICTest`

  ```java
  public class AICTest {
    public AICTest();
      Code:
        13: new           #10 // class main/test/AICTest$1
        18: invokespecial #12 // Method main/test/AICTest$1."<init>":(Lmain/test/AICTest;)V
  }
  ```

  * 实际实现就是new AICTest$1，然后执行它的构造方法

* disadvantages

  * the compiler generates a new class file for each anonymous inner class. The filename usually looks like ClassName$1.
  * each class file needs to be loaded and verified before being used, which impacts the startup performance of the application. The loading may be an expensive operation, including disk I/O and decompressing the JAR file itself.

### lambda

* 执行`javac main/test/LambdaTest.java `

  ```
  - main
  	- test
  		- LambdaTest.class
  ```

* 执行`javap -c LambdaTest`

  ```java
  public class LambdaTest {
    public LambdaTest();
      Code:
        13: invokedynamic #10,  0 // InvokeDynamic #0:onClick:()Lmain/view/OnClickListener;
          
    private static void lambda$new$0(main.view.View);
    	Code:
    		0: getstatic     #18      // Field java/lang/System.out:Ljava/io/PrintStream;
        3: aload_0
        4: invokevirtual #24      // Method java/lang/Object.hashCode:()I
        7: invokedynamic #28,  0  // InvokeDynamic #1:makeConcatWithConstants:(I)Ljava/lang/String;
        12: invokevirtual #32     // Method java/io/PrintStream.println:(Ljava/lang/String;)V
  }
  
  BootstrapMethods:
  	0: #47 REF_invokeStatic java/lang/invoke/LambdaMetafactory.metafactory
      Method arguments:
  			#54 (Lmain/view/View;)V
        #55 REF_invokeStatic main/test/LambdaTest.lambda$new$0:(Lmain/view/View;)V
        #54 (Lmain/view/View;)V
  	1: #58 REF_invokeStatic java/lang/invoke/StringConcatFactory.makeConcatWithConstants
      Method arguments:
  			#64 onClick call, view hashcode = \u0001
  ```

  * 执行`invokedynamic`指令，跳到执行`LambdaMetafactory.metafactory`获取`CallSite`对象

* 运行时

  ```java
  public static CallSite metafactory(MethodHandles.Lookup caller,
                                     String interfaceMethodName,
                                     MethodType factoryType,
                                     MethodType interfaceMethodType,
                                     MethodHandle implementation,
                                     MethodType dynamicMethodType)
  ```

  * `caller` -> `main.test.LamdaTest`
  * `interfaceMethodName` -> `onClick`
  * `factoryType` -> `()OnClickListener`
  * `interfaceMethodType` -> `View(void)`
  * `implementation` -> `MethodHandle(View)void`
  * `dynamicMethodType` -> `View(void)`

### example

* lambda

  ```java
  String toBeTrimmed = " text with spaces ";
  Supplier<String> lambda = toBeTrimmed::trim;
  ```

* real

  ````java
  String toBeTrimmed = " text with spaces ";
  Method reflectionMethod = String.class.getMethod("trim");
  Lookup lookup = MethodHandles.lookup();
  MethodHandle handle = lookup.unreflect(reflectionMethod);
  CallSite callSite = LambdaMetafactory.metafactory(
    // method handle lookup
    lookup,
    // name of the method defined in the target functional interface
    "get",
    // type to be implemented and captured objects
    // in this case the String instance to be trimmed is captured
    MethodType.methodType(Supplier.class, String.class),
    // type erasure, Supplier will return an Object
    MethodType.methodType(Object.class),
    // method handle to transform
    handle,
    // Supplier method real signature (reified)
    // trim accepts no parameters and returns String
    MethodType.methodType(String.class));
  Supplier<String> lambda = (Supplier<String>) callSite.getTarget().bindTo(toBeTrimmed).invoke();
  ````

***

## jni

### process

* file structure

  ```shell
  jni
  ├── HelloWorldJNI.class
  ├── HelloWorldJNI.java
  ├── jni_HelloWorldJNI.cpp
  ├── jni_HelloWorldJNI.h
  ├── jni_HelloWorldJNI.o
  └── libnative.dylib
  ```

1. create a file named `HelloWorldJNI.java`, add a native method to it.

   ```java
   package jni;
   
   public class HelloWorldJNI {
   
       static {
           //load native library
           System.loadLibrary("hello_world_jni");
       }
   
       public static void main(String[] args) {
           new HelloWorldJNI().sayHello();
       }
   
       // Declare a native method sayHello() that receives no arguments and returns void
       private native void sayHello();
   }
   ```

2. use `javac -h` to generate `jni_HelloWorldJNI.h` file

   `javac -h jni jni/HelloWorldJNI.java`

   ```c++
   JNIEXPORT void JNICALL Java_jni_HelloWorldJNI_sayHello
     (JNIEnv *, jobject);
   ```

3. create `jni_HelloWorldJNI.cpp`并实现方法

   ```cpp
   #include "jni_HelloWorldJNI.h"
   #include<iostream>
   
   JNIEXPORT void JNICALL Java_jni_HelloWorldJNI_sayHello
     (JNIEnv *, jobject) {
   std::cout << "Hello World JNI" << std::endl;
   }; 
   ```

4. 编译生成可重定位目标文件

   ```shell
   clang++ 
   	-c # 生成可重定位目标文件
   	-I ${JAVA_HOME}/include # `jni.h`
   	-I ${JAVA_HOME}/include/darwin #`jni_md.h`
   	jni/jni_HelloWorldJNI.cpp # 源文件
   	-o jni/jni_HelloWorldJNI.o # 生成目标文件
   ```

5. 链接生成动态链接库

   ```shell
   clang++ 
   	-dynamiclib # 指定生成动态链接库
   	jni/jni_HelloWorldJNI.o # 源文件
   	-o jni/libhello_world_jni.dylib # 生成目标文件
   ```

6. 运行

   ```shell
   java 
   	-Djava.library.path=jni # 将jni目录添加进native库搜索目录
   	jni.HelloWorldJNI # 全限定类名
   ```

   `Hello World JNI`

***

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

* use example

  ```java
  class SharedPreferencesImpl {
    private boolean mLoaded = false; // 标记位，是否读取文件已完成
    
    public String getString(String key, @Nullable String defValue) {
      synchronized (mLock) { // 获取对象锁是执行wait()和notify的必要条件
        while (!mLoaded) {
          try {
            mLock.wait(); // 释放对象锁，进入wait; 获取对象锁，执行完wait返回
          } catch (InterruptedException unused) {}
        }; 
        String v = (String)mMap.get(key); // 读取文件已完成，正常读取
        return v != null ? v : defValue;
      }
    }
    
   	private void loadFromDisk() { // 真正从文件读内容
      synchronized (mLock) { // 获取对象锁是执行wait()和notify的必要条件
        try {
          // 省略噼里啪啦一系列读文件的操作
        } catch (Throwable t) {
          mThrowable = t;
        } finally {
          mLock.notifyAll(); // 进行notify
        }
      }
    }
  }
  ```

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

* difference between WeakRefrence

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
    
    final class ObserveOnObserver<T> extends BasicIntQueueDisposable<T> {
      final Observer<? super T> downstream; // 下游的Observer
      Disposable upstream; // 上游的Observable
      final Scheduler.Worker worker; // 调度器
      
      @Override
      public void onNext(T t) {
        worker.schedule(this); // 执行自己实现的Runnable
      }
      
      @Override
      public void run() {
        T v = q.poll(); // 里面有一堆复杂的 for(;;) 从队列取的逻辑，这里省略了
        downstream.onNext(); // 下游的Observer开始执行
      }
    }
  }
  ```

  * 结论：`observeOn()`下面写的所有操作符的`onNext`都在它指定的线程执行

* Observable#subscibeOn()

  ```java
  class ObservableSubscribeOn<T> {
    public ObservableSubscribeOn(ObservableSource<T> source, Scheduler scheduler) {
      // 父类持有源source
      super(source);
      this.scheduler = scheduler;
    }
    
    @Override
    public void subscribeActual(final Observer<? super T> observer) {
      final SubscribeOnObserver<T> parent = new SubscribeOnObserver<>(observer); //observer包一层
      observer.onSubscribe(parent); // 调用observer#onSubscibe()
      // scheduler开始调度SubscribeTask这个任务
      parent.setDisposable(scheduler.scheduleDirect(new SubscribeTask(parent)));
    }
    
    class SubscribeTask implements Runnable {
      private final SubscribeOnObserver<T> parent;
      @Override
      public void run() {
        source.subscribe(parent); // 核心的一行，等于upstream.subscibe(wrapObserver)在指定线程执行
      }
    }
    
    final class SubscribeOnObserver<T> extends AtomicReference<Disposable> {
      final Observer<? super T> downstream;
      public void onNext(T t) { downstream.onNext(t); } // onNext原封不动往下传递
    }
  }
  ```

  * 结论
    1. subscibeOn() 指定了`upstream#subscribe() `执行，由于最顶的Observable的onNext就是在`subscribe()`内执行，因此如果不做其他线程切换的话，所有的操作符回调都会在指定的线程执行；因此一般在`Observable#subscibe({}, {})`的上一行加`.observeOn(AndroidSchedulers.mainThread())`来切线程
    2. subscibeOn()内的Observer就没做啥事，一个字 -> `透传`

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

* Observable#debounce

  ```java
  public final class ObservableDebounceTimed<T> extends AbstractObservableWithUpstream<T, T> {
  	final class DebounceTimedObserver<T> {
      Disposable timer;
      volatile long index; // 用于只取最后一个
      boolean done; // 用于onComplete直接返回
      
      public void onNext(T t) {
        long idx = index + 1;
        index = idx; // index自增
        DebounceEmitter<T> de = new DebounceEmitter<T>(t, idx, this); // 包成一个Runnable
        timer = de;
        d = worker.schedule(de, timeout, unit); // 调度这个Runnable
      }
      
      public void onComplete() {
        if (de != null) {
          de.run(); // 直接拿runnable来run
        }
      }
      
      void emit(long idx, T t, DebounceEmitter<T> emitter) {
        if (idx == index) { // 保证只取最后一个
          downstream.onNext(t); // 传给downstream
          emitter.dispose();
        }
      }
    }
    
    static final class DebounceEmitter<T> {
      public void run() { // runnable的实现
        if (once.compareAndSet(false, true)) {
          parent.emit(idx, value, this); 
        }
      }
    }
  }
  ```

* 总结

  1. 设计模式很像Fresco的consumer与producer
  2. subscibe顺序 -> **自底向上，持有upstream**
  3. onNext顺序 -> **自顶向下，持有downstream**

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

## string

### class

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence,
               Constable, ConstantDesc
```

### Immutable

* advantages

  1. Security: Since Strings can be used in Cryptography and Network communication, it is important for their values to remain unchanged during transmission or storage.
  2. Thread Safety
  3. string pool cache
  4. use strings in hash-based collections.

* how to ensure immutable

  1. Final class.

  2. Private final fields: The state of a String object is stored in private final fields.

  3. Operations return new objects: All methods that perform operations on a String, such as substring(), replace(), etc., return a new String object that has the modified value.

     ```java
     String str = "Hello";
     str.concat(" World!"); // Creates a new String object "Hello World!", but str still refers to the old "Hello" object
     ```

### interned strings

* what

  * They are a subset of String pool. 

  * Interned strings in Java are the Strings stored in constant pool (String pool) of Heap area.

* explanation 

  * When a String literal is created, the JVM checks if the String literal already exists in the constant pool. If the String literal already exists in the constant pool, then instead of creating a new String literal, JVM returns the reference to the existing String literal. This reuse of existing String literals is called interning of Strings.

* advantages

  * It reduces memory usage
  * It helps with String comparison as the JVM can use the `==` operator instead of the `equals()` method in certain cases.

* intern method

  * signature - `public native String intern()`
  * When the intern method is invoked, if the pool already contains a string equal to this String object as determined by the equals(Object) method, then the string from the pool is returned. Otherwise, this String object is added to the pool and a reference to this String object is returned.

* exmaples

  * "java", "jvm", "class"

### StringBuilder/Buffer

* why mutable

  ```java
  abstract class AbstractStringBuilder {
    // 这个数组是可变的
    byte[] value;
    // The count is the number of characters used.
    int count;
    
    public AbstractStringBuilder append(String str) {
        int len = str.length();
      	// 执行扩容操作
        ensureCapacityInternal(count + len);
      	// 插入
        putStringAt(count, str);
        count += len;
        return this;
    }
  }
  ```

* thread safe

  * StringBuilder is not thread-safe

    ```java
    public StringBuilder append(String str) {
        super.append(str);
        return this;
    }
    ```

  * StringBuffer is thread-safe

    ```java
    // it use synchronized keyword
    public synchronized StringBuffer append(String str) {
        toStringCache = null;
        super.append(str);
        return this;
    }

### encodings and character sets

* example

  ```java
  String str = "Hello, World!";
  byte[] bytes = str.getBytes(StandardCharsets.UTF_8); // encode
  
  // To convert a byte array back to string using the same encoding
  String newStr = new String(bytes, StandardCharsets.UTF_8); // decode
  ```

***

## uncaught-exception-handler

### source code

```java
class Thread implements Runnable {
  // member virable
  private volatile UncaughtExceptionHandler uncaughtExceptionHandler;
  // inner interface
  public interface UncaughtExceptionHandler {
    void uncaughtException(Thread t, Throwable e);
  }
  // set method
  public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
    checkAccess();
    uncaughtExceptionHandler = eh;
  }
  
  // jvm calls this method when an exception occurs
  // Dispatch an uncaught exception to the handler. This method is intended to be called only by the JVM.
  private void dispatchUncaughtException(Throwable e) {
    getUncaughtExceptionHandler().uncaughtException(this, e);
  }
}
```