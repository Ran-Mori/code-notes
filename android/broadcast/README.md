## 接收广播

1. 定义一个`BroadcastReceiver()`

2. `context`中`registerReceiver`

   ```kotlin
   // 写一个Receiver
   private val broadReceiver = object : BroadcastReceiver() {
     override fun onReceive(context: Context?, intent: Intent?) {
       val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
     }
   }
   
   override fun onCreate(savedInstanceState: Bundle?) {
     super.onCreate(savedInstanceState)
     setContentView(R.layout.activity_main)
     // 注册一下监听，声明只监听Intent.ACTION_BATTERY_CHANGED
     registerReceiver(broadReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
   }
   ```

## 实现

```java
// android.app.ContextImpl#registerReceiverInternal
private Intent registerReceiverInternal(BroadcastReceiver receiver, int userId, IntentFilter filter) {
  // wrap receiver into rd
  IIntentReceiver rd = mPackageInfo.getReceiverDispatcher(receiver);
  // AMS
  ActivityManager.getService().registerReceiverWithFeature(
      mMainThread.getApplicationThread(), mBasePackageName, getAttributionTag(),
      AppOpsManager.toReceiverId(receiver), rd, filter, broadcastPermission, userId,
      flags);
}
```

