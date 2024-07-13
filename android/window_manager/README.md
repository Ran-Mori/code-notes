# window_manager

## links

* [Android绘制流程 —— View、Window、SurfaceFlinger](https://juejin.cn/post/6899010578145411085)
* [Android全面解析之Window机制](https://juejin.cn/post/6888688477714841608)

## 几个关键类

1. ViewManager

   * source code

     ```java
     public interface ViewManager{
         public void addView(View view, ViewGroup.LayoutParams params);
         public void updateViewLayout(View view, ViewGroup.LayoutParams params);
         public void removeView(View view);
     }
     ```

   * `ViewManager`接口定义得很纯净，就是cud三个操作

2. WindowManagerImpl

   * definition

     ```java
     // public interface WindowManager extends ViewManager
     public final class WindowManagerImpl implements WindowManager {
       private final WindowManagerGlobal mGlobal = WindowManagerGlobal.getInstance();
       public void addView(View view, ViewGroup.LayoutParams params) { mGlobal.addView(); }
     }
     ```

   * 最终`WindowManagerImpl`是实现了`ViewManager`接口

   * 将所有的操作全都委托给了`WindowManagerGlobal`

   * 创建

     ```java
     // window.java
     if (wm == null) {
       // 获取到系统WindowManagerService
       wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
     }
     // 创建一个本地使用的WindowManagerImpl对象
     mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);
     ```

3. WindowManagerGlobal

   * definition

     ```java
     public final class WindowManagerGlobal {
       private ArrayList<View> mViews = new ArrayList<View>(); // 存所有DecorView
       private ArrayList<ViewRootImpl> mRoots = new ArrayList<ViewRootImpl>(); // 存所有ViewRootImpl
       private static IWindowManager sWindowManagerService; // static全局唯一
       
       public static IWindowManager getWindowManagerService() {
         // 通过Binder获取WindowManagerService
         sWindowManagerService = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
       }
     }
     ```

## setContentView流程

1. 收到`EXECUTE_TRANSACTION`159类型的message，调用到创建window

   ```java
   public final class ActivityThread {
     class H extends Handler {
       public void handleMessage(Message msg) {
         switch (msg.what) {
           case EXECUTE_TRANSACTION:
             ClientTransaction transaction = (ClientTransaction) msg.obj;
             // 执行这个transaction，虽然不知道这个transaction到底是啥，只知道和启动Activity有关
             mTransactionExecutor.execute(transaction);
         }
       }
     }
   }
   
   class Activity {
     private Window mWindow;
     final void attach(...) {
       // 创建window
       mWindow = new PhoneWindow(this, window, activityConfigCallback);
     }
   }
   ```

2. `PhoneWindow`创建`DecorView`，并设置`DecorView`的布局

   ```java
   public class PhoneWindow extends Window {
     private DecorView mDecor; // 持有DecorView
     ViewGroup mContentParent;
     
     protected DecorView generateDecor(int featureId) {
       // new一个DecorView
       return new DecorView(context, featureId, this, getAttributes());
     }
     
     private void installDecor() {
       mDecor = generateDecor(-1);
       mContentParent = generateLayout(mDecor);
     }
     
     protected ViewGroup generateLayout(DecorView decor) {
       int layoutResource = R.layout.screen_simple;
       // 将R.layout.screen_simple传给DecorView
       mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);
       // 从DecorView中找到了R.id.content，如下xml所示
       ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT); // 从DecorView
       return contentParent;
     }
   }
   ```

   ```xml
   <!--screen_simple.xml-->
   <LinearLayout>
   	<ViewStub 
       android:id="@+id/action_mode_bar_stub"
       android:inflatedId="@+id/action_mode_bar"
       android:layout="@layout/action_mode_bar" />
   	<FrameLayout
        android:id="@android:id/content" /> <!--id为content-->
   </LinearLayout>
   ```

3. `DecorView`设置View

   ```java
   public class DecorView extends FrameLayout {
     // 第二步会调到
     void onResourcesLoaded(LayoutInflater inflater, int layoutResource) {
       // 构造这个View，布局如上xml所示
       final View root = inflater.inflate(layoutResource, null);
       // 直接作为DecorView的根子View
       addView(root, 0, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
       mContentRoot = (ViewGroup) root;
     }
   }
   ```

4. `AppCompatDelegateImpl`强行魔改`R.id.content`

   ```java
   class AppCompatDelegateImpl extends AppCompatDelegate {
     ViewGroup subDecor = null;
     
     private ViewGroup createSubDecor() {
       // 直接构造一个View，布局如下所示
       subDecor = (ViewGroup) LayoutInflater.from(themedContext).inflate(R.layout.abc_screen_toolbar, null);
     	// 从新构建的subDecor里找到一个View作为contentView
       FrameLayout contentView = subDecor.findViewById(R.id.action_bar_activity_content);
       // 获取DecorView的R.id.content
       ViewGroup windowContentView = (ViewGroup) mWindow.findViewById(android.R.id.content);
       while (windowContentView.getChildCount() > 0) {
         // 将R.id.content内的View全部移动到新的contentView中
         final View child = windowContentView.getChildAt(0);
         windowContentView.removeViewAt(0);
         contentView.addView(child);
       }
       windowContentView.setId(View.NO_ID); // 将原来的R.id.content设置为NO_ID
       contentView.setId(android.R.id.content); // 将新的contentView强设成R.id.content
     }
   }
   ```

   ```xml
   <android.support.v7.widget.ActionBarOverlayLayout
           android:id="@+id/decor_content_parent">
       <include layout="@layout/abc_screen_content_include"/>
       <android.support.v7.widget.ActionBarContainer
               android:id="@+id/action_bar_container">
           <android.support.v7.widget.Toolbar
                   android:id="@+id/action_bar"/>
           <android.support.v7.widget.ActionBarContextView
                   android:id="@+id/action_context_bar"/>
       </android.support.v7.widget.ActionBarContainer>
   </android.support.v7.widget.ActionBarOverlayLayout>
   ```

5. 执行`setContentView()`

   ```java
   class AppCompatDelegateImpl {
     ViewGroup subDecor = null;
     public void setContentView(int resId) {
       // 找到强行改后的R.id.content
       ViewGroup contentParent = mSubDecor.findViewById(android.R.id.content);
       // 清空然后强加
       contentParent.removeAllViews();
       LayoutInflater.from(mContext).inflate(resId, contentParent);
     }
   }
   ```


## 添加View进Window

1. Activity#onResume()

   ```java
   public final class ActivityThread {
     public void handleResumeActivity() {
       View decor = r.window.getDecorView(); // 获取DecorView
       decor.setVisibility(View.INVISIBLE); // 设置为不可见
       ViewManager wm = a.getWindowManager(); // 从Activity获取wm
       WindowManager.LayoutParams l = r.window.getAttributes(); // 获取LayoutParams
       wm.addView(decor, l); // 尝试把整个DecorView添加进Window
       r.activity.makeVisible(); //添加结束后最后设置为可见
     }
   }
   ```

2. WindowManagerGloabal创建ViewRootImpl

   ```java
   public final class WindowManagerGlobal {
     // 上面的addView最终会调用到这个方法，view是DecorView
     public void addView(View view, ViewGroup.LayoutParams params) {
       ViewRootImpl root = new ViewRootImpl(view.getContext(), display);
       // 把DecorView和ViewRootImpl都存一下
       mViews.add(view); 
       mRoots.add(root);
       // 调用ViewRootImpl的setView() 方法
       root.setView(view, wparams, panelParentView, userId);
     }
   }
   ```

3. ViewRootImpl#setView

   ```java
   public final class ViewRootImpl {
     View mView;
     
     public ViewRootImpl(Context context, Display display) {
       // getWindowSession()是单例，即一个应用对应一个Session
       this(context, display, WindowManagerGlobal.getWindowSession(), false);
     }
     // 上面会调到这个方法
     public void setView(View view, WindowManager.LayoutParams attrs) {
       mView = view; // 把DecorView赋值给mView
       requestLayout(); // 调一下requestLayout()
       // 跨进程Binder代理调用，通过系统进程的WindowManagerService建立连接
       res = mWindowSession.addToDisplayAsUser(mWindow, mWindowAttributes)
     }
   }
   ```

## window添加进WMS

1. com.android.server.wm.Session#addToDisplayAsUser()

   ```java
   // 继承Stub，是Binder IPC的server侧
   // There is generally one Session object per process 
   class Session extends IWindowSession.Stub {
     final WindowManagerService mService; // 持有WMS
     
     @Override // Override，说明是IWindowSession接口定义的方法
     public int addToDisplayAsUser(IWindow window, WindowManager.LayoutParams attrs) {
       return mService.addWindow(this, window);
     }
   }
   ```

2. WindowManagerService#addWindow()

   ```java
   // 继承Stub，是Binder IPC的server侧
   public class WindowManagerService extends IWindowManager.Stub {
     public int addWindow(Session session, IWindow client) {
       // 代码太复杂，就不贴了
     }
   }
   ```

## 删除View过程

1. WindowManagerGlobal#removeView()

   ```java
   public final class WindowManagerGlobal {
     public void removeView(View view, boolean immediate) {
       removeViewLocked(index, immediate);
     }
     private void removeViewLocked(int index, boolean immediate) {
       ViewRootImpl root = mRoots.get(index);
       View view = root.getView();
       boolean deferred = root.die(immediate); // 将操作委托给ViewRootImpl, 返回是否异步
       if (deferred) {
         mDyingViews.add(view); // 如果是同步，立即就删除，如果是异步，添进mDyingViews
       }
     }
   }
   ```

2. ViewRootImpl#die()

   ```java
   public final class ViewRootImpl implements ViewParent {
     boolean die(boolean immediate) {
       if (immediate && !mIsInTraversal) {
         doDie(); // 同步，直接doDie
         return false;
       }
       mHandler.sendEmptyMessage(MSG_DIE); // 异步，抛个事件，收到事件后也是执行doDie()
       return true;
     }
     void doDie() {
       dispatchDetachedFromWindow();
     }
     
     void dispatchDetachedFromWindow() {
       mView.dispatchDetachedFromWindow(); // 分发detachedFromWindow事件
       mView = null;
       mAttachInfo.mRootView = null; // 持有元素置空
       mWindowSession.remove(mWindow); //最后调用到WMS来remove
     }
   }
   ```

