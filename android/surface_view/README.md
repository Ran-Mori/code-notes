## reference

* [Graphics architecture](https://source.android.com/docs/core/graphics/architecture)

## surface

### source code

* java层

  ```java
  public class Surface implements Parcelable {
    long mNativeObject; // 真正的nativeSurface
    // 持有Canvas, 但Canvas也只是native的一层包装，持有了一个native指针
    private final Canvas mCanvas = new CompatibleCanvas(); 
  }
  ```

* native层

  ```java
  // 构造函数需要传入一个生产者的引用，和BufferQueue的交互均有这个生产者的引用来完成
  Surface::Surface( const sp<GraphicBufferProducer>& bufferProducer, bool controlledByApp) : mGraphicBufferProducer(bufferProducer), mGenerationNumber(0)
  ```

### features

1. `Handle onto a raw buffer that is being managed by the screen compositor.`

2. 继承了`Parcelable`，因此可以跨进程通信，在`WMS`中传递

3. 它持有了`NativeBuffer`的指针，这个`NativieBuffer`指的是用来保存当前窗口屏幕数据的一个`buffer`

4. `ViewRootImpl`持有了这个对象，即一颗`view tree`，一个`window`，共享一个`Surface`

   ```java
   public final class ViewRootImpl implements ViewParent {
     BaseSurfaceHolder mSurfaceHolder;
   	public final Surface mSurface = new Surface(); 
   }
   ```



## 

## SurfaceView

### source code

* java code

  ```java
  public class SurfaceView extends View {
    final Surface mSurface = new Surface(); // Current surface in use
  }
  ```

### what's for?

1. When you render with an external buffer source, such as GL context or a media decoder, you need to copy buffers from the buffer source to display the buffers on the screen. Using a SurfaceView enables you to do that.
2. When the SurfaceView's view component is about to become visible, the framework asks SurfaceControl to request a new surface from SurfaceFlinger. To receive callbacks when the surface is created or destroyed, use the SurfaceHolder interface.

### featues

1. The new surface is the producer side of a BufferQueue, whose consumer is a SurfaceFlinger layer. You can update the surface with any mechanism that can feed a BufferQueue, such as surface-supplied Canvas functions, attaching an EGLSurface and drawing on the surface with GLES, or configuring a media decoder to write the surface.
2. Provides a dedicated drawing surface embedded inside of a view hierarchy.
3. 是`View`的子类；不与`window`共享`surface`，而是自己持有一个`surface`；未重写`onDraw()`方法，即不参与绘制
4. 为了解决与`window#surface`的重叠问题，`SurfaceView`是在`Z轴`的底部，通过让`window#surface`设置为透明而显示出来
5. `surface`绘制的线程可以自己定，可以不是主线程

### how to show

1. `lockCanvas()` and `unlockCanvasAndPost()`

   ```java
   // android.view.Surface#lockCanvas
   public Canvas lockCanvas(Rect inOutDirty) {
     // native lock
     mLockedObject = nativeLockCanvas(mNativeObject, mCanvas, inOutDirty);
     return mCanvas;
   }
   
   // android.view.Surface#unlockCanvasAndPost
   public void unlockCanvasAndPost(Canvas canvas) {
     // native unlockAndPost
     if (mHwuiContext != null) {
       mHwuiContext.unlockAndPost(canvas);
     } else {
       unlockSwCanvasAndPost(canvas);
     }
   }
   ```

2. provide a surface to native

   * see `android.widget.VideoView`

## SurfaceHolder

### source code

* java code

  ```java
  public class SurfaceView extends View {
    final Surface mSurface = new Surface(); // 直接new一个Surface
    
    public SurfaceHolder getHolder() { return mSurfaceHolder; }
    
    private final SurfaceHolder mSurfaceHolder = new SurfaceHolder() {
      public boolean isCreating() {
       // 返回成员变量mIsCreating
      }
      
      public void addCallback(Callback callback) {
        // 操作成员变量mCallbacks
      }
      
      public Canvas lockCanvas() {
        // 将操作委托给mSurface
      }
      
      public void unlockCanvasAndPost(Canvas canvas) {
        // 将操作委托给mSurface
      }
      
      public Surface getSurface() {
        // 返回成员变量mSurface
      }
    }
  }
  ```

### features

1. 充当`MVC`模式中的`C`，`Surface`是`M`，`SurfaceView`是`V`
2. `SurfaceView`持有`Surface`，但设置为`private`，外部通过`SurfaceHolder`这个`C`去控制`Surface`

## VideoView

### how it implemented

* VideoView.java

  ```java
  public class VideoView extends SurfaceView {
    private void openVideo() {
      // pass the data source, see below
      mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
      // pass the mSurfaceHolder(get surface), see below
      mMediaPlayer.setDisplay(mSurfaceHolder);
    }
  }
  ```

* MediaPlayer.java

  ```java
  // android.media.MediaPlayer#_setDataSource(java.io.FileDescriptor, long, long)
  // pass the file to native
  private native void _setDataSource(FileDescriptor fd, long offset, long length)
    
  // android.media.MediaPlayer#setDisplay
  public void setDisplay(SurfaceHolder sh) {
    _setVideoSurface(sh.getSurface());
    updateSurfaceScreenOn();
  }
  
  // pass the surface to native
  private native void _setVideoSurface(Surface surface);

## GLSurfaceView

* source code 

  ```java
  // android.opengl.GLSurfaceView
  public class GLSurfaceView extends SurfaceView implements SurfaceHolder.Callback2 {
    private static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {
      public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display) {
        // this method will get surfure from SurfaceHolder and pass it to native code.
        return egl.eglCreateWindowSurface(display, config, nativeWindow, null);
      }
    }
  }

## create a surface

```java
// android.view.SurfaceControl
// Handle to an on-screen Surface managed by the system compositor.
public final class SurfaceControl implements Parcelable {
  
}

// android.view.ViewRootImpl
class ViewRootImpl {
  // hold a SurfaceControl
  private final SurfaceControl mSurfaceControl = new SurfaceControl();
  
  private void performTraversals() {
    relayoutResult = relayoutWindow(params, viewVisibility, insetsPending);
  }
  private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility, boolean insetsPending) {
    // pass the surfaceControl as a parameter
    relayoutResult = mWindowSession.relayout(mWindow, params, requestedWidth, requestedHeight, viewVisibility, mSurfaceControl)
  }
}

// com.android.server.wm.Session
public int relayout(IWindow window, WindowManager.LayoutParams attrs, SurfaceControl outSurfaceControl) {
  // mService is WindowManagerService
  // pass the surfaceControl as a parameter
  int res = mService.relayoutWindow(this, window, attrs, outSurfaceControl)
}

// com.android.server.wm.WindowManagerService
public int relayoutWindow(Session session, IWindow client, LayoutParams attrs, SurfaceControl outSurfaceControl) {
  result = createSurfaceControl(outSurfaceControl, result, win, winAnimator);
}
private int createSurfaceControl(SurfaceControl outSurfaceControl, int result) {
  WindowSurfaceController surfaceController;
  // try to create a SurfaceController
  surfaceController = winAnimator.createSurfaceLocked();
  if (surfaceController != null) {
    surfaceController.getSurfaceControl(outSurfaceControl);
  } else {outSurfaceControl.release();
  }

  return result;
}

// com.android.server.wm.WindowStateAnimator
WindowSurfaceController createSurfaceLocked() {
  // create an instance of WindowSurfaceController
  mSurfaceController = new WindowSurfaceController(attrs.getTitle().toString(), format, flags, this, attrs.type);
}

// com.android.server.wm.WindowSurfaceController
WindowSurfaceController(String name, int format, int flags, WindowStateAnimator animator, int windowType) {
  final SurfaceControl.Builder b = win.makeSurface()
      .setParent(win.getSurfaceControl())
      .setName(name)
      .setFormat(format)
      .setFlags(flags)
      .setMetadata(METADATA_WINDOW_TYPE, windowType)
      .setMetadata(METADATA_OWNER_UID, mWindowSession.mUid)
      .setMetadata(METADATA_OWNER_PID, mWindowSession.mPid)
      .setCallsite("WindowSurfaceController");
  // use build pattern to create a WindowSurfaceController
  mSurfaceControl = b.build();
}

// android.view.SurfaceControl
private SurfaceControl(SurfaceSession session, String name) {
  Parcel metaParcel = Parcel.obtain();
  // jni create, pass metaParcel as a parameter
  mNativeObject = nativeCreate(session, name, w, h, format, flags, metaParcel);
  mNativeHandle = nativeGetHandle(mNativeObject);
}

// frameworks/base/core/jni/android_view_SurfaceControl.cpp
static jlong nativeCreate(JNIEnv* env, jclass clazz, jobject sessionObj, jobject metadataParcel) {
  // get data from Parcel
  Parcel* parcel = parcelForJavaObject(env, metadataParcel);
  // call createSurfaceChecked
  status_t err = client->createSurfaceChecked(String8(name.c_str()), w, h, format, &surface, flags, parentHandle, std::move(metadata));
}

// frameworks/native/libs/gui/SurfaceComposerClient.cpp
status_t SurfaceComposerClient::createSurfaceChecked(const String8& name, uint32_t w, uint32_t h) {
  // call createSurface
  binder::Status status = mClient->createSurface(std::string(name.c_str()), std::move(metadata));
}

// gen/aidl_library/android/gui/ISurfaceComposerClient.cpp
BpSurfaceComposerClient::createSurface(const ::android::gui::LayerMetadata& metadata, ::android::gui::CreateSurfaceResult* _aidl_return) {
  // use binder rpc to get a surface from SurfaceFlinger
  _aidl_ret_status = remote()->transact(BnSurfaceComposerClient::TRANSACTION_createSurface, _aidl_data, &_aidl_reply, 0);
  if (_aidl_ret_status == ::android::UNKNOWN_TRANSACTION && ISurfaceComposerClient::getDefaultImpl()) [[unlikely]] {
     return ISurfaceComposerClient::getDefaultImpl()->createSurface(name, flags, parent, metadata, _aidl_return);
  }
}

// frameworks/native/services/surfaceflinger/Client.cpp
binder::Status Client::createSurface(const std::string& name,  const gui::LayerMetadata& metadata, gui::CreateSurfaceResult* outResult) {
  // creat a layer from SurfaceFlinger
  const status_t status = mFlinger->createLayer(args, *outResult);
  return binderStatusFromStatusT(status);
}

// com.android.server.wm.WindowManagerService
private int createSurfaceControl(SurfaceControl outSurfaceControl, int result) {
  WindowSurfaceController surfaceController;
  // surfaceController is returned by SurfaceFlinger
  surfaceController = winAnimator.createSurfaceLocked();
  if (surfaceController != null) {
    // get a surface
    surfaceController.getSurfaceControl(outSurfaceControl);
  } else {
    outSurfaceControl.release();
  }
  return result;
}

// com.android.server.wm.WindowSurfaceController
void getSurfaceControl(SurfaceControl outSurfaceControl) {
  // use copyFrom to get the Surface
  outSurfaceControl.copyFrom(mSurfaceControl, "WindowSurfaceController.getSurfaceControl");
}

// frameworks/base/core/jni/android_view_SurfaceControl.cpp
static jlong nativeCopyFromSurfaceControl(JNIEnv* env, jclass clazz, jlong surfaceControlNativeObj) {
 	// create a surface from surfaceControlNativeObj
  sp<SurfaceControl> surface(reinterpret_cast<SurfaceControl *>(surfaceControlNativeObj));
  sp<SurfaceControl> newSurface = new SurfaceControl(surface);
  newSurface->incStrong((void *)nativeCreate);
  return reinterpret_cast<jlong>(newSurface.get());
}

// android.view.ViewRootImpl
class ViewRootImpl {
  // hold a SurfaceControl
  private final SurfaceControl mSurfaceControl = new SurfaceControl();
  // hold a Surface
  public final Surface mSurface = new Surface();
  
  private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility, boolean insetsPending) {
    // pass the surfaceControl as a parameter
    relayoutResult = mWindowSession.relayout(mWindow, params, requestedWidth, requestedHeight, viewVisibility, mSurfaceControl);
    // now mSurfaceControl is modified by SurfaceFlinger
    // use mSurfaceControl to create a Surface.
    mSurface.copyFrom(mSurfaceControl);
  }
  private void performTraversals() {
    // mSurface is passed to mThreadedRenderer
    wInitialized = mAttachInfo.mThreadedRenderer.initialize(mSurface);
  }
}
```

## make a hole

* 代码实现

  ```java
  // SurfaceView.java
  private OnPreDrawListener mDrawListener = () -> {
      updateSurface(); // onPreDraw()时调用updateSurface()
  };
  
  private OnScrollChangedListener mScrollChangedListener = this::updateSurface; // onScroll时也调updateSurface()
  
  protected void onAttachedToWindow() { // onAttachToWindow()时调用
    // 监听ViewRootImpl对应的Surface发生改变
    getViewRootImpl().addSurfaceChangedCallback(this);
    mAttachedToWindow = true; // 一个标记位
    // 核心实现，开始请求划出一片区域为透明
    mParent.requestTransparentRegion(SurfaceView.this);
    // onPredraw()与onScroll()时调用updateSurface()
    observer.addOnScrollChangedListener(mScrollChangedListener);
    observer.addOnPreDrawListener(mDrawListener);
  }
  
  // ViewGoup.java
  public void requestTransparentRegion(View child) {
    if (child != null) {
      child.mPrivateFlags |= View.PFLAG_REQUEST_TRANSPARENT_REGIONS; // 标记位
      if (mParent != null) {
        mParent.requestTransparentRegion(this); // 一直向上调用，最后调到ViewRootImpl
      }
    }
  }
  
  // ViewRootImpl.java
  private void performTraversals() {
    if (View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {
      params.format = PixelFormat.TRANSLUCENT; // format设置为半透明
    }
    performLayout(lp, mWidth, mHeight); // 执行完了layout
    if (View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {
      mTransparentRegion.set(x, x, x, x) // 规定透明区域的范围
      // 此方法View, ViewGroup, SurfaceView都有实现，会将对应的View的区域设置成透明
      host.gatherTransparentRegion(mTransparentRegion);
    }
  }
  ```

## key methods

* `updateSurface()`

  ```java
  // android.view.ViewTreeObserver#dispatchOnPreDraw
  public final boolean dispatchOnPreDraw() {
    for (int i = 0; i < count; i++) {
      cancelDraw |= !(access.get(i).onPreDraw());
    }
  }
  
  // android.view.SurfaceView#mDrawListener
  private final ViewTreeObserver.OnPreDrawListener mDrawListener = () -> {
    mHaveFrame = getWidth() > 0 && getHeight() > 0;
    updateSurface();
    return true;
  };
  
  // android.view.SurfaceView#mBlastBufferQueue
  private BLASTBufferQueue mBlastBufferQueue; // 一个队列
  
  // android.view.SurfaceView#updateSurface
  protected void updateSurface() {
    // 定义一大堆以changed结尾的变量
    final boolean visibleChanged;
    final boolean alphaChanged;
    final boolean creating;
    final boolean sizeChanged;
    
    if (hasChanged) {
      getLocationInWindow(mLocation); // 获取此SurfaceView在window中的位置
      
      mWindowSpaceLeft = mLocation[0]; // 记录位置在mScreenRect成员变量内
      mWindowSpaceTop = mLocation[1];
      mScreenRect.left = mWindowSpaceLeft;
      mScreenRect.top = mWindowSpaceTop;
      mScreenRect.right = mWindowSpaceLeft + getWidth();
      mScreenRect.bottom = mWindowSpaceTop + getHeight();    
    }
    
    if (creating) {
      createBlastSurfaceControls(viewRoot, name, surfaceUpdateTransaction); // 创建 BLASTBufferQueue
    }
    
    // 这一次ViewRootImpl#performDraw()之前是否要增加一些操作(比如draw这个SurfaceView)
    boolean shouldSyncBuffer = redrawNeeded && viewRoot.wasRelayoutRequested()
    if (shouldSyncBuffer) {
      // 队列加一个消息
      mBlastBufferQueue.syncNextTransaction(false, onTransactionReady);
    }
  
    // 里面进行了一大堆操作，最终返回size是否变化
    final boolean realSizeChanged = performSurfaceTransaction(viewRoot, ...);
  
    copySurface(creating, sizeChanged); // 尝试重建Surface
  
    if (!mSurfaceCreated && (surfaceChanged || visibleChanged)) {
      for (SurfaceHolder.Callback c : getSurfaceCallbacks()) {
        c.surfaceCreated(mSurfaceHolder); // 调用SurfaceCreated()
      }
    }
    
    if (creating || formatChanged || sizeChanged || hintChanged) {
      for (SurfaceHolder.Callback c : getSurfaceCallbacks()) {
        c.surfaceChanged(mSurfaceHolder, mFormat, myWidth, myHeight); // 调用SurfaceChanged()
      }
    }
    
    if (redrawNeeded) { // 需要重绘
      callbacks = getSurfaceCallbacks();
      if (shouldSyncBuffer) { // 有额外的操作
        handleSyncBufferCallback(callbacks, syncBufferTransactionCallback);
      } else { // 无额外的操作
        handleSyncNoBuffer(callbacks);
      }
    }
  }
  
  // 这个类感觉自己实现了一个同步机制
  private static class SyncBufferTransactionCallback {
    private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private Transaction mTransaction;
  
    Transaction waitForTransaction() {
     	mCountDownLatch.await();
      return mTransaction;
    }
  
    void onTransactionReady(Transaction t) {
      mTransaction = t;
      mCountDownLatch.countDown();
    }
  }
  
  // android.view.SurfaceView#copySurface
  private void copySurface(boolean surfaceControlCreated, boolean bufferSizeChanged) {
    // 非必要不执行
    if (!surfaceControlCreated && !needsWorkaround) { return; }
    if (surfaceControlCreated) {
      mSurface.copyFrom(mBlastBufferQueue); // surface只是一个java对象，copyFrom重建只需要改下nativeObject的指针就好了
    }
    if (needsWorkaround && mBlastBufferQueue != null) {
      mSurface.transferFrom(mBlastBufferQueue.createSurfaceWithHandle());
    }
  }
  
  // android.view.SurfaceView#handleSyncBufferCallback
  private void handleSyncBufferCallback(Callback[]， SyncBufferTransactionCallback) {
    getViewRootImpl().addToSync(syncBufferCallback -> {
      mBlastBufferQueue.stopContinuousSyncTransaction(); // 队列先暂停一下
      t = syncBufferTransactionCallback.waitForTransaction(); // 用自己实现的同步机制获取一个Transaction
      syncBufferCallback.onBufferReady(t); // 调用onBufferReady()准备绘制
      mParent.requestTransparentRegion(SurfaceView.this); // 完成绘制后的操作
      invalidate(); // 完成Surface绘制要求更新UI
    })
  }
  ```







## 待定

* TextureView

  * 继承自`View`，它的表现就像一个普通的`View`一样

  * 它没有自己的`Surface`，而是共享`ViewRootImpl`的`Surface`

  * 由于没有自己的`Surface`，它的理论性能比`SurfaceView`低

  * 显示的内容通过`SurfaceTexture`传递


* SurfaceTexture

  * `Captures frames from an image stream as an OpenGL ES texture.`

  * 可以把`Surface`生成的图像流，转换为纹理`Texture`，供业务方进一步加工使用