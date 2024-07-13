## path与query

* 示例

  ```kotlin
  @GET("/posts/{id}")
  fun getRespById(@Path("id") id: Int): Observable<DataResponse>
  ```

  ```kotlin
  @GET("/comments")
  fun getComment(@Query("postId") postId: Int): Observable<List<DataComment>>
  ```

## basic

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

## process

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

## extra knowledge

1. factory pattern
2. adapter pattern
3. dynamic proxy
4. static proxy
5. httpmethod cache
