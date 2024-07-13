## reference

* [理解 Context.getSystemService 原理](https://juejin.cn/post/6844903812159815687)

## process

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

## ServiceManager

