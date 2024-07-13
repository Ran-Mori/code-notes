## 基础知识

* 管理和通信
  * Notice由NotificationManager管理，Widget由AppWidgetManager管理
  * NotificationManager和AppWidgetManager通过Binder和SysterServer进程中的NotificationService和AppWidgetManagerService通信
  * 视图是在SystemServer进程的xxxService中被加载的
* 采用Action
  * 理论上RemoteView可以支持View的所有操作，但那样跨进程开销太大，而且设计复杂
  * 因此目前采用Action来跨进程更新UI

## PendingIntent

* 源码

  ```java
  public final class PendingIntent implements Parcelable {
    private final IIntentSender mTarget;
    
    public static PendingIntent getActivityAsUser() {
      IIntentSender target = ActivityManager.getService().getIntentSenderWithFeature();
      eturn target != null ? new PendingIntent(target) : null;
    }
    
    public PendingIntent(IIntentSender target) {
      mTarget = Objects.requireNonNull(target);
    }
    
    public PendingIntent(IBinder target, Object cookie) {
      mTarget = IIntentSender.Stub.asInterface(target);
      mWhitelistToken = (IBinder)cookie;
    }
  }
  ```

  ```java
  // IIntentSender.aidl
  interface IIntentSender {
      void send(int code, in Intent intent, String resolvedType, in IBinder whitelistToken,
              IIntentReceiver finishedReceiver, String requiredPermission, in Bundle options);
  }
  ```

  * PendingIntent 仅仅是持有`IIntentSender`引用而已
  * 实际的`IItentSender`实例是由操作系统控制的
  * `IIntentSender`是通过`AMS`获取的

* 注释

  * A PendingIntent itself is simply a reference to a token maintained by the system describing the original data used to retrieve it.
  * If the creating application later re-retrieves the same kind of PendingIntent， it will receive a PendingIntent representing the same token if that is still valid, and can thus call cancel to remove it.

* [stack over flow](https://stackoverflow.com/questions/9583230/what-is-the-purpose-of-intentsender)

  * Instances of `IIntentSender` can not be made directly, but rather must be created from an existing `PendingIntent` with `PendingIntent.getIntentSender()`.
  * As for a `PendingIntent`, it's basically a token that you give to another application which allows that application to use your app's permissions to execute a specific piece of your app's code.

## RemoteView

* 跨进程通信

  ```java
  public class RemoteViews implements Parcelable {
    public ApplicationInfo mApplication;
    private int mLayoutId;
    
    public RemoteViews(String packageName, int layoutId) {
      // 构造参数核心就是给两个变量赋值
      mApplication = application;
      mLayoutId = layoutId;
    }
    
    public void writeToParcel(Parcel dest, int flags) {
      // 写入Parcel传递给远端进程
      mApplication.writeToParcel(dest, flags);
      dest.writeInt(mLayoutId);
    }
    
    public static final Parcelable.Creator<RemoteViews> CREATOR = new Parcelable.Creator<RemoteViews>() {
      public RemoteViews createFromParcel(Parcel parcel) {
        // 远端进程从Parcel读构造RemoteView
        return new RemoteViews(parcel);
      }
    }
    
    public RemoteViews(Parcel parcel) {
      // 远端进程从Parcel读构造RemoteView
      mApplication = ApplicationInfo.CREATOR.createFromParcel(parcel);
      mViewId = parcel.readInt();
    }
  }
  ```

* 远端构造View

  ```java
  // 加载布局并更新界面
  public View apply(Context context, ViewGroup parent) {
    RemoteViews rvToApply = getRemoteViewsToApply(context, size);
  	// 通过LayoutInflater.from直接常规构造一个View
    View result = inflateView(context, rvToApply, parent);
    // 开始performApply
    rvToApply.performApply(result, parent, handler, null);
    return result;
  }
  
  // 只更新界面
  private void performApply(View v, ViewGroup parent) {
    for (int i = 0; i < count; i++) {
      Action a = mActions.get(i);
      // apply就是执行所有反射执行所有更新的方法
      a.apply(v, parent, handler, colorResources);
    }
  }
  
  private void reapply(Context context, View v) {
    RemoteViews rvToApply = getRemoteViewsToReapply(context, v, size);
    rvToApply.performApply(v, (ViewGroup) v.getParent());
  }
  ```

  ```java
  // Widget的parent容器，运行在远端进程
  public class AppWidgetHostView extends FrameLayout {
    // 更新Widget界面
    public void updateAppWidget(RemoteViews remoteViews) {
      RemoteViews rvToApply = remoteViews.getRemoteViewsToApply(mContext, mCurrentSize);
      // 调用reapply
      rvToApply.reapply(mContext, mView);
    }
  }
  ```

* 更新View

  ```java
  public void setImageViewResource(int viewId, int srcId) {
    setInt(viewId, "setImageResource", srcId);
  }
  
  public void setInt(int viewId, String methodName, int value) {
    // 仅仅是加了一个ReflectionAction
    addAction(new ReflectionAction(viewId, methodName, BaseReflectionAction.INT, value));
  }
  
  private void addAction(Action a) {
    mActions.add(a);
  }
  
  // 一个Action是可以跨进程通信的
  private abstract static class Action implements Parcelable {}
  
  abstract class BaseReflectionAction extends Action {
    // 真正改视图的地方，在远端执行
    public final void apply(View root, ViewGroup rootParent) {
      // 远端进程就是一个普通的View，直接findViewById找到这个View
      final View view = root.findViewById(viewId);
      // 然后反射执行方法改变视图
      getMethod(view, this.methodName, param, false).invoke(view, value);
    }
  }
  
  private final class ReflectionAction extends BaseReflectionAction {
    public void writeToParcel(Parcel out, int flags) {
      // 一样的，写入Parcel传给远端进程
    }
    ReflectionAction(Parcel in) {
      // 远端进程通过Parcel构造Action
    }
  }
  ```

* 点击实现

  ```java
  public void setOnClickPendingIntent(int viewId, PendingIntent pendingIntent) {
    // 将pendingIntent包成了RemoteResponse
    // 将response包成SetOnClickResponse
    RemoteResponse resp = RemoteResponse.fromPendingIntent(pendingIntent);
    addAction(new SetOnClickResponse(viewId, response));
  }
  
  public static class RemoteResponse {
    private PendingIntent mPendingIntent;
    // 封装了PendingIntent跨进程通信
    private void writeToParcel(Parcel dest, int flags) {
      PendingIntent.writePendingIntentOrNullToParcel(mPendingIntent, dest);
    }
    private void readFromParcel(Parcel parcel) {
      mPendingIntent = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
    }
  }
  
  private class SetOnClickResponse extends Action {
    public void apply(View root, ViewGroup rootParent) {
      final View target = root.findViewById(viewId);
      // 调用setOnClickListener
      target.setOnClickListener(v -> mResponse.handleViewInteraction(v, handler));
    }
  }
  ```

## RemoteView IPC

* put into intent

  ```java
  public class Intent implements Parcelable, Cloneable {
    // as RemoteView has implmented Parcelable interface, so it can be passed as the second parameter
    public Intent putExtra(String name, Parcelable value) {
      mExtras.putParcelable(name, value);
      return this;
    }
  }
  ```

* get from intent

  ```java
  public class Intent implements Parcelable, Cloneable {
    public <T> T getParcelableExtra(String name, Class<T> clazz) {
      return mExtras == null ? null : mExtras.getParcelable(name, clazz);
    }
  }
  ```

* apply

  ```java
  public class RemoteViews implements Parcelable {
    // Inflates the view hierarchy represented by this object and applies all of the actions.
    public View apply(Context context, ViewGroup parent) {
      View result = inflateView(context, rvToApply, parent);
      rvToApply.performApply(result, parent, handler, null);
      return result;
    }
  }
  ```


***

## 