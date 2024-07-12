## 官方文档

* [coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/topics/coroutines-guide.md)

## 是什么

* Coroutines are computations that run on top of threads and can be suspended. When a coroutine is "suspended", the corresponding computation is paused, removed from the thread, and stored in memory. Meanwhile, the thread is free to be occupied with other activities. 

## 执行顺序

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

## 核心概念

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

## Structured concurrency

* what?
  * It is a programming paradigm for writing concurrent code that emphasizes safety, clarity, and predictability. 
* features
  * Coroutines follow a principle of **structured concurrency** which means that new coroutines can be only launched in a specific **CoroutineScope** which delimits the lifetime of the coroutine.
  * An outer scope cannot complete until all its children coroutines complete.

## CPS

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

## 协程原理

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

## 线程切换

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

## Flow

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

## 