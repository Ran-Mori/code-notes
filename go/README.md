# golang

## chanel

### basic

* 作用：可以让一个`goroutine`通过`channel`给另一个`goroutine`发送消息

* 操作

  1. 创建：`ch := make(char 发送的类型, size)`
  2. 发送：`ch <- x` 
  3. 接收：`x = <- ch` 
  4. 关闭`close(channel)`

* 缓冲

  1. 不带缓冲 -> `ch := make(char) or ch := make(char, 0)`
  2. 带缓冲 -> `ch := make(char, 1)`

* 不带缓冲

  * 不带缓冲时，发送操作会使`g`进入阻塞挂起状态，直到有接收方接收时`g`才会继续执行

* 底层结构 -> `runtime#hchan`

  ```go
  type hchan struct {
  	qcount   uint           // total data in the queue
  	dataqsiz uint           // size of the circular queue
  	buf      unsafe.Pointer // points to an array of dataqsiz elements
  	elemsize uint16
  	closed   uint32
  	elemtype *_type // element type
  	sendx    uint   // send index
  	recvx    uint   // receive index
  	recvq    waitq  // list of recv waiters
  	sendq    waitq  // list of send waiters
  	lock mutex
  }
  ```

  1. 缓存区
     * 用的是`环形队列`
     * 优势：环形队列不用频繁分配和释放内存，可以降低`GC`的压力达到更高的性能
  2. 发送队列
     * 用的链表实现
  3. 接收队列
     * 同发送队列
  4. `mutex`
     * 访问`chan`之前必须获得锁
     * 即进入队列和操作数据那很小一段时间是锁的，其他时间都不锁
  5. 状态值
     * 标志是否关闭

* 发送数据

  * 语法糖：编译器会将`<-`转换成对应的函数`chansend1()、chanrecv1()`
  * 三种情况
    1. 直接发送 -> 接收队列里面已有等待的`g`，直接取`g`出来，赋值然后唤醒
    2. 有缓存 -> 直接放入环形队列
    3. 阻塞 -> 把自己放入发送队列

* 非阻塞chanel

  * 使用

    ```go
    select {
      case 1 <- c1:
      	//...
      case c2 >- 2:
      	//...
      default:
      	//...
    }
    ```

### select

* what?

  * It is a keyword in Go that is used for handling communication between goroutines.
  * It allows you to wait for multiple channel operations to happen at the same time and process the ones that are ready.
  * The main purpose of `select` statement is to choose which channel is ready and proceed further with the operation.

* example

  ```go
  func main() {
  	done := make(chan bool, 2)
  
  	go func() {
  		// do something
  		done <- true
  	}()
  
  	go func() {
  		// do something
  		done <- true
  	}()
  
  	var timeRemain = 4 * time.Second
  	for i := 0; i < 2; i++ {
  		var startTime = time.Now().UnixNano()
  		select {
  		case <-done:
  		case <-time.After(timeRemain):
  		}
  		timeRemain -= time.Duration(time.Now().UnixNano() - startTime)
  	}
  	time.Sleep(8 * time.Second)
  }
  ```

***
