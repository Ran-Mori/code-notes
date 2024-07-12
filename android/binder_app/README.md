## reference

* [Android框架分析系列](https://mr-cao.gitbooks.io/android/content/)

## aidl

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

## 几者关系

* `binder` - It is the core mechanism of the Android IPC system. It allows different processes to communicate with each other by passing messages.
* `IBinder` - It is the interface for the `Binder` object. It defines the methods(`transact()`, `isBinderAlive()`) that can be used to communicate with a remote process.
* `Service` - It is a component in the Android system that runs in the background and provides a set of APIs that can be accessed by other processes.
* `client` - It is the process that uses a service provided by another process.
* `server` - It is the process that provides a service that can be used by other processes.
* `Stub` - It is an object that resides in the process that provides a service, and which can be called by other processes. When a remote process wants to access a service implemented by another process, it sends a request to the stub object. The stub then forwards the request to the actual implementation(`CalculatorServer`) of the service in the local process.
* `Proxy` - It is an object that resides in the process that needs to access a remote service, and which acts as a surrogate for the stub object in the remote process. The proxy object provides a local API that looks as if it is calling the remote service directly. When a call is made on the proxy object, it is actually sent to the stub object in the remote process.

## 注意事项

1. 一定要记得server侧在中进行注册


## process

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

## features

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

2. `onTransact()`方法会运行在服务端的一个线程池中，因此即使它很耗时，也应该用同步的方式去实现；但RPC返回耗时久，客户端不能放在UI线程执行
