## vsync

* reference

  * [“终于懂了” 系列：Android屏幕刷新机制—VSync、Choreographer 全面理解！](https://juejin.cn/post/6863756420380196877)

* how it is generated: 

  * The VSync signal is a physical signal generated by the display hardware itself. It's not a software construct within the Android framework or kernel. 
  * The Android kernel receives the VSync signal from the display hardware through specific display controller registers or interrupt mechanisms. 

* Functionality

  1. Display Hardware: Generates the VSync signal at its refresh rate.

  2. Kernel: Receives the VSync signal and makes it available to the system.

  3. Hardware Composer (HWC): Utilizes the VSync signal for scheduling display updates.

  4. SurfaceFlinger: Relies on VSync for display composition synchronization.

  5. Applications: Can leverage the Choreographer class to schedule rendering in sync with VSync.

* how generate

  ```java
  // frameworks/native/libs/gui/DisplayEventDispatcher.cpp
  int DisplayEventDispatcher::handleEvent(int, int events, void*) {
    if (processPendingEvents(&vsyncTimestamp, &vsyncDisplayId, &vsyncCount, &vsyncEventData)) {
      // call dispatchVsync
      dispatchVsync(vsyncTimestamp, vsyncDisplayId, vsyncCount, vsyncEventData);
    }
  }
  
  // frameworks/base/core/jni/android_view_DisplayEventReceiver.cpp
  void NativeDisplayEventReceiver::dispatchVsync(nsecs_t timestamp, PhysicalDisplayId displayId, uint32_t count, VsyncEventData vsyncEventData) {
    // call jni method
    env->CallVoidMethod(receiverObj.get(), gDisplayEventReceiverClassInfo.dispatchVsync, timestamp, displayId.value, count);
  }
  
  // android.view.DisplayEventReceiver#dispatchVsync
  private void dispatchVsync(long timestampNanos, long physicalDisplayId, int frame, VsyncEventData vsyncEventData) {
    onVsync(timestampNanos, physicalDisplayId, frame, vsyncEventData);
  }
  
  // android.view.Choreographer.FrameDisplayEventReceiver
  @Override
  public void onVsync(long timestampNanos, long physicalDisplayId, int frame, VsyncEventData vsyncEventData) {
    Message msg = Message.obtain(mHandler, this);
    msg.setAsynchronous(true);
    // sene a message
    mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
  }
  
  @Override
  public void run() {
    // call doFrame
    doFrame(mTimestampNanos, mFrame, mLastVsyncEventData);
  }
  
  // android.view.Choreographer
  void doFrame(long frameTimeNanos, int frame, DisplayEventReceiver.VsyncEventData vsyncEventData) {
    doCallbacks(Choreographer.CALLBACK_INPUT, frameData, frameIntervalNanos);
    doCallbacks(Choreographer.CALLBACK_ANIMATION, frameData, frameIntervalNanos);
    doCallbacks(Choreographer.CALLBACK_INSETS_ANIMATION, frameData, frameIntervalNanos);
    doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameData, frameIntervalNanos);
    doCallbacks(Choreographer.CALLBACK_COMMIT, frameData, frameIntervalNanos);
  }
  
  void doCallbacks(int callbackType, FrameData frameData, long frameIntervalNanos) {
    CallbackRecord callbacks = mCallbackQueues[callbackType].extractDueCallbacksLocked(now / TimeUtils.NANOS_PER_MS);
    for (CallbackRecord c = callbacks; c != null; c = c.next) {
      // do run
      c.run(frameData);
    }
  }
  
  // android.view.Choreographer.CallbackRecord
  public void run(long frameTimeNanos) {
    if (token == FRAME_CALLBACK_TOKEN) {
      ((FrameCallback)action).doFrame(frameTimeNanos);
    } else {
      ((Runnable)action).run();
    }
  }
  
  // process of add callback
  // android.view.ViewRootImpl#scheduleTraversals
  void scheduleTraversals() {
    if (!mTraversalScheduled) {
      mTraversalScheduled = true;
      // set SyncBarrier
      mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
      // post a callback to mChoreographer
      mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
    }
  }
  
  // android.view.Choreographer
  public static final int CALLBACK_INPUT = 0;
  public static final int CALLBACK_ANIMATION = 1;
  public static final int CALLBACK_INSETS_ANIMATION = 2;
  public static final int CALLBACK_TRAVERSAL = 3; // Handles layout and draw. Runs after all other asynchronous messages have been handled.
  public static final int CALLBACK_COMMIT = 4;
  
  private void postCallbackDelayedInternal(int callbackType, Object action, Object token, long delayMillis) {
    // add a callBack to mCallbackQueues
    mCallbackQueues[callbackType].addCallbackLocked(dueTime, action, token);
    if (dueTime <= now) {
      scheduleFrameLocked(now);
    } else {
      Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_CALLBACK, action);
      msg.arg1 = callbackType;
      msg.setAsynchronous(true);
      mHandler.sendMessageAtTime(msg, dueTime);
    }
  }
  ```

## create RenderThread

```java
// android.view.ViewRootImpl
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
  enableHardwareAcceleration(attrs);
}
private void enableHardwareAcceleration(WindowManager.LayoutParams attrs) {
  mAttachInfo.mThreadedRenderer = ThreadedRenderer.create(mContext, translucent, attrs.getTitle().toString());
}

// android.graphics.HardwareRenderer
private final long mNativeProxy;

public HardwareRenderer() {
  // create a RenderNode
  mRootNode = RenderNode.adopt(nCreateRootRenderNode());
	// create a proxy, it is frameworks/base/libs/hwui/renderthread/RenderProxy.cpp
  mNativeProxy = nCreateProxy(!mOpaque, mRootNode.mNativeRenderNode);
}

// frameworks/base/libs/hwui/jni/android_graphics_HardwareRenderer.cpp
static jlong android_view_ThreadedRenderer_createRootRenderNode(JNIEnv* env, jobject clazz) {
  // RenderNode of ThreadedRenderer is RootRenderNode
  // RootRenderNode extends to RenderNode
  RootRenderNode* node = new RootRenderNode(std::make_unique<JvmErrorReporter>(env));
  return reinterpret_cast<jlong>(node);
}

// frameworks/base/libs/hwui/renderthread/RenderProxy.cpp
RenderProxy::RenderProxy(RenderNode* rootRenderNode,)
        : mRenderThread(RenderThread::getInstance()) { // call RenderThread::getInstance()
  mDrawFrameTask.setContext(&mRenderThread, mContext, rootRenderNode);
}

// frameworks/base/libs/hwui/thread/ThreadBase.h
// it is a looper queue runnable pattern
class ThreadBase : public Thread {
public:
  ThreadBase()
    : Thread(false)
    , mLooper(new Looper(false))
    , mQueue([this]() { mLooper->wake(); }, mLock) {}

// frameworks/base/libs/hwui/renderthread/RenderThread.cpp
RenderThread& RenderThread::getInstance() {
  // real create RenderThread
  [[clang::no_destroy]] static sp<RenderThread> sInstance = []() {
      sp<RenderThread> thread = sp<RenderThread>::make();
      thread->start("RenderThread");
      return thread;
  }();
  return *sInstance;
}
// while true
bool RenderThread::threadLoop() {
  initThreadLocals();
	while (true) {
    waitForWork();
    processQueue();
  }
}
// init Choreographer, EglManager
void RenderThread::initThreadLocals() {
  setupFrameInterval();
  initializeChoreographer();
  mEglManager = new EglManager();
  mRenderState = new RenderState(*this);
  mVkManager = VulkanManager::getInstance();
  mCacheManager = new CacheManager(*this);
}
  
void RenderThread::initializeChoreographer() {
  // when sync comes, it will call choreographerCallback
  mLooper->addFd(AChoreographer_getFd(mChoreographer), 0, Looper::EVENT_INPUT,
                     RenderThread::choreographerCallback, this);
  mVsyncSource = new ChoreographerSource(this);
}
```

## createCanvas

```c++
// android.graphics.RecordingCanvas
public final class RecordingCanvas extends BaseRecordingCanvas {
	// constructor
  private RecordingCanvas(RenderNode node, int width, int height) {
    super(nCreateDisplayListCanvas(node.mNativeRenderNode, width, height));
  }
  // jni create a canvas, and it is associated with RenderNode
  private static native long nCreateDisplayListCanvas(long node, int width, int height);
}

// frameworks/base/libs/hwui/jni/android_graphics_DisplayListCanvas.cpp
static jlong android_view_DisplayListCanvas_createDisplayListCanvas() {
  // create the RenderNode in cpp
  RenderNode* renderNode = reinterpret_cast<RenderNode*>(renderNodePtr);
  // it calls Canvas::create_recording_canvas
  return reinterpret_cast<jlong>(Canvas::create_recording_canvas(width, height, renderNode));
}

// frameworks/base/libs/hwui/hwui/Canvas.cpp
Canvas* Canvas::create_recording_canvas(int width, int height, uirenderer::RenderNode* renderNode) {
  // so the realy Canvas in a instance of SkiaRecordingCanvas
  return new uirenderer::skiapipeline::SkiaRecordingCanvas(renderNode, width, height);
}

// frameworks/base/libs/hwui/pipeline/skia/SkiaRecordingCanvas.cpp
void SkiaRecordingCanvas::initDisplayList(uirenderer::RenderNode* renderNode, int width, int height) {
  // get mDisplayList from RenderNode
  mDisplayList = renderNode->detachAvailableList();
  mDisplayList->attachRecorder(&mRecorder, SkIRect::MakeWH(width, height));
}
```

## drawCircle

```c++
// android.graphics.BaseCanvas#nDrawCircle
public abstract class BaseCanvas {
  private static native void nDrawCircle(long nativeCanvas, float cx, float cy, float radius, long nativePaint);
}

//frameworks/base/libs/hwui/jni/android_graphics_Canvas.cpp
static void drawCircle(JNIEnv* env, jobject, jlong canvasHandle, jfloat cx, jfloat cy) {
  // from above, we know that get_canvas returns SkiaRecordingCanvas
  get_canvas(canvasHandle)->drawCircle(cx, cy, radius, *paint);
}

// frameworks/base/libs/hwui/pipeline/skia/SkiaRecordingCanvas.cpp
void SkiaRecordingCanvas::drawCircle(uirenderer::CanvasPropertyPrimitive* x) {
  // it will only save infomation in the mDisplayList，and it will not directly calculate the frame buffer
  drawDrawable(mDisplayList->allocateDrawable<AnimatedCircle>(x, y, radius, paint));
}
```

## SurfaceFlinger

* structure

  ![结构图片](https://img2018.cnblogs.com/blog/821933/201907/821933-20190730111306166-2128331293.png)

* features

  1. it is a daemon process in android.
  2. It is not a part of Android SDK, but a part of AOSP.

## draw process

```java
// android.view.ViewRootImpl#draw
private boolean draw(boolean fullRedrawNeeded, boolean forceDraw) {
  // use gpu instead of cpu
  if (isHardwareEnabled()) {
    mAttachInfo.mThreadedRenderer.draw(mView, mAttachInfo, this);
  }
}

// android.view.ThreadedRenderer#draw
void draw(View view, AttachInfo attachInfo, DrawCallbacks callbacks) {
  updateRootDisplayList(view, callbacks);
}
private void updateRootDisplayList(View view, DrawCallbacks callbacks) {
  updateViewTreeDisplayList(view);
}
private void updateViewTreeDisplayList(View view) {
  view.updateDisplayListIfDirty();
}

// android.view.View
// hold a mRenderNode
final RenderNode mRenderNode;
public View(Context context) {
  mRenderNode = RenderNode.create(getClass().getName(), new ViewAnimationHostBridge(this));
}

// frameworks/base/libs/hwui/jni/android_graphics_RenderNode.cpp
static jlong android_view_RenderNode_create(JNIEnv* env, jobject, jstring name) {
  // RenderNode of View is RenderNode
  RenderNode* renderNode = new RenderNode();
  return reinterpret_cast<jlong>(renderNode);
}

// android.view.View
public RenderNode updateDisplayListIfDirty() {
  final RecordingCanvas canvas = renderNode.beginRecording(width, height);
  try {
    // save draw infomation in the displayList of canvas
    draw(canvas);
  } finally {
    renderNode.endRecording();
  }
  return renderNode;
}

// android.view.ThreadedRenderer#draw
void draw(View view, AttachInfo attachInfo, DrawCallbacks callbacks) {
  updateRootDisplayList(view, callbacks);
  // finish saveing all information in displayList
  // this method is a sync method, and it will wait for a return
  int syncResult = syncAndDrawFrame(frameInfo);
}

static int android_view_ThreadedRenderer_syncAndDrawFrame(JNIEnv* env, jobject clazz, jlong proxyPtr, jlongArray frameInfo, jint frameInfoSize) {
  RenderProxy* proxy = reinterpret_cast<RenderProxy*>(proxyPtr);
  // let proxy run syncAndDrawFrame
  return proxy->syncAndDrawFrame();
}

// frameworks/base/libs/hwui/renderthread/RenderProxy.cpp
int RenderProxy::syncAndDrawFrame() {
  return mDrawFrameTask.drawFrame();
}

// frameworks/base/libs/hwui/renderthread/DrawFrameTask.cpp
int DrawFrameTask::drawFrame() {
  mSyncResult = SyncResult::OK;
  mSyncQueued = systemTime(SYSTEM_TIME_MONOTONIC);
  postAndWait(); // post and wait
  return mSyncResult;
}
void DrawFrameTask::postAndWait() {
  AutoMutex _lock(mLock); // lock 
  // queue a runnable to mRenderThread, it will do run() method when being scheduled
  mRenderThread->queue().post([this]() { run(); }); 
  mSignal.wait(mLock); // wait
}

void DrawFrameTask::run() {
  CanvasContext* context = mContext;
  // let context run draw
  context->draw(solelyTextureViewUpdates);
}

// frameworks/base/libs/hwui/renderthread/CanvasContext.cpp
void CanvasContext::draw(bool solelyTextureViewUpdates) {
  // it will call EglManager::beginFrame to get a buffer from SurfaceFlinger
  Frame frame = getFrame();
  // real draw
  drawResult = mRenderPipeline->draw(
                frame, windowDirty, dirty, mLightGeometry, &mLayerUpdateQueue, mContentDrawBounds,
                mOpaque, mLightInfo, mRenderNodes, &(profiler()), mBufferParams, profilerLock());
  // swapBuffer and ready to display. it will call EglManager::swapBuffers, it will give buffer back to SurfaceFlinger
	bool didSwap = mRenderPipeline->swapBuffers(frame, drawResult, windowDirty);
}

// frameworks/base/libs/hwui/renderthread/EglManager.cpp
Frame EglManager::beginFrame(EGLSurface surface) {
  makeCurrent(surface);
  Frame frame;
  frame.mSurface = surface;
  // get a buffer to draw
  frame.mBufferAge = queryBufferAge(surface);
  eglBeginFrame(mEglDisplay, surface);
  return frame;
}

// frameworks/base/libs/hwui/pipeline/skia/SkiaOpenGLPipeline.cpp
IRenderPipeline::DrawResult SkiaOpenGLPipeline::draw(const Frame& frame, const SkRect& screenDirty, const SkRect& dirty) {
  renderFrame(*layerUpdateQueue, dirty, renderNodes, opaque, contentDrawBounds, surface);
}

// frameworks/base/libs/hwui/pipeline/skia/SkiaPipeline.cpp
void SkiaPipeline::renderFrame(const LayerUpdateQueue& layers, const std::vector<sp<RenderNode>>& nodes, sk_sp<SkSurface> surface) {
  SkCanvas* canvas = tryCapture(surface.get(), nodes[0].get(), layers);
  renderLayersImpl(layers, opaque);
  renderFrameImpl(clip, nodes, opaque, contentDrawBounds, canvas, preTransform);
  endCapture(surface.get());
}

void SkiaPipeline::renderFrameImpl(const std::vector<sp<RenderNode>>& nodes, SkCanvas* canvas) {
  SkCanvas* layerCanvas = layerNode->getLayerSurface()->getCanvas();
  // send instructions to gpu
  cachedContext->flushAndSubmit();
}

// external/skia/include/gpu/GrDirectContext.h
void flushAndSubmit(GrSyncCpu sync = GrSyncCpu::kNo) {
  // send instructions to gpu
  this->flush(GrFlushInfo());
  this->submit(sync);
}

// android.view.ThreadedRenderer#draw
void draw(View view, AttachInfo attachInfo, DrawCallbacks callbacks) {
  int syncResult = syncAndDrawFrame(frameInfo);
  // gpu draw return
}
```

## BufferQueue

* 好文链接 - [深入浅出Android BufferQueue](https://zhuanlan.zhihu.com/p/62813895)

* 模型 - 生产者消费者模式

  * 生产者 - 产生图像源数据，如`Surface`，截图时的`SurfaceFlinger`
  * 消费者 - 消费图像源数据，如`SurfaceFlinger`，截图时另外的一个`BufferQueue`

* BufferState

  * FREE - 所有权归`BufferQueue`
  * DEQUEUED - 所有权归生产者
  * QUEUED - 已填充数据，但未被消费者获取，所有权归`BufferQueue`
  * ACQUIRED - 所有权归消费者

* `Surface`生产者

  ```c++
  // 获取一个Buffer来draw
  static jlong nativeLockCanvas() {
  	//1. 通过Surface::lock方法，获取一个合适的Buffer
    status_t err = surface->lock(&outBuffer, dirtyRectPtr);
    //2. 构造一个Bitmap，地址指向步骤1获取的Buffer的地址，这样在这个Bitmap上绘制的内容，直接绘制到了GraphicBuffer
    SkBitmap bitmap;
    //将GraphicBuffer构造成一个Bitmap，设置给Canvas
    Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
    // canvas始终要调用一次setBitmap()，无论是java调还是native调
    nativeCanvas->setBitmap(bitmap);
  }
  
  static void nativeUnlockCanvasAndPost() {
  	// detach the canvas from the surface
  	Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
  	nativeCanvas->setBitmap(SkBitmap()); // 设置bitmap为另一个对象即与buffer解绑
    err = queueBuffer(mLockedBuffer.get(), fd); // 直接将数据放入BufferQueue
  }
  ```

## 