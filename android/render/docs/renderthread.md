# RenderThread 学习

这个页面专门用来学习普通 View 硬件加速路径中的 RenderThread 边界。它不试图“调用 RenderThread”，因为普通 App 代码没有这个入口；它做的是把应用层能观察到的边界暴露出来：`Choreographer#doFrame`、`View.onDraw()`、`FrameMetrics`、Logcat、Perfetto、`dumpsys gfxinfo`。

## 主线

```text
VSync
  -> Choreographer#doFrame(main)
  -> ViewRoot traversal / View.onDraw(main, record DisplayList)
  -> ThreadedRenderer / HardwareRenderer
  -> RenderThread syncFrameState + DrawFrame
  -> Skia + OpenGL/Vulkan + GPU
  -> SurfaceFlinger composition
```

对一个有 App 开发经验的人来说，最容易误会的是：`RenderThread` 不是第二个 UI Thread。你的 `View.onDraw()` 通常仍然在 `main` 线程执行；硬件加速下，它更像是在记录一份绘制命令/DisplayList。RenderThread 在后面消费这些记录好的工作，和 Skia、OpenGL/Vulkan、GPU、Surface 交接。

## 页面里有什么

- `RenderThreadLabActivity`：RenderThread 学习页的 Activity、`Choreographer`、`FrameMetrics`、主线程阻塞、属性动画。
- `RenderThreadLabView`：普通自定义 View。它会在 `onDraw()` 里打印当前线程、计数、轻/重绘制耗时。
- 属性动画方块：只改变 `translationX/rotation/alpha`，用来观察“视觉在动”和“自定义 View 是否每帧 onDraw”不是一回事。
- `FrameMetrics` 面板：显示每帧总耗时、draw、sync、command、swap、GPU 等指标。
- `SurfaceCanvasLabView`：使用 App 自己创建的 `AppSurfaceThread` 往 `SurfaceView` 画 Canvas。它用于对照：这不是系统 RenderThread，而是另一个 buffer producer。

## 建议实验顺序

1. 进入“RenderThread 学习”页面，先看事件日志。你会看到 `RenderThread page started on main`、`Choreographer#doFrame on main`、`View.onDraw ... on main`。
2. 打开“每个 VSync 后 postInvalidateOnAnimation”。观察 `onDraw` 计数持续增加。这说明自定义 View 每帧都在 UI 线程记录绘制命令。
3. 打开“让 onDraw 做更多 CPU 工作”。观察 `FrameMetrics` 里的 `draw` 和 `total` 增加，`>16.6ms` 计数变多。这里卡的是主线程记录阶段，不是 RenderThread 神秘变慢。
4. 点击“阻塞主线程 700ms”。你会看到明显掉帧。RenderThread 可能还能处理已经提交的旧工作，但它不能凭空生成新的 View 状态。
5. 打开“只做属性动画，不主动重绘 View”。观察方块能动，但自定义 View 的 `onDraw` 不一定同步增长。这个实验用来理解 RenderNode/硬件层的变换和普通自定义绘制不是同一个成本模型。
6. 看 SurfaceView 对照组日志。`AppSurfaceThread` 是你自己起的线程，和系统 HWUI RenderThread 不是一个东西。

## Perfetto 观察点

```bash
adb shell perfetto -o /data/misc/perfetto-traces/renderthread-lab.pftrace -t 15s --app com.render sched freq idle gfx view wm am binder_driver
adb pull /data/misc/perfetto-traces/renderthread-lab.pftrace .
```

重点找：

- 进程：`com.render`
- 线程：`main`
- 线程：`RenderThread`
- 线程：`FrameMetricsCollector`
- 线程：`AppSurfaceThread`
- 系统进程：`surfaceflinger`
- App slice：`RTLab.RenderThreadActivity#onCreate`、`RTLab.Choreographer#doFrame(main)`、`RTLab.View.onDraw(record DisplayList)`、`RTLab.mainThreadSleep700ms`
- HWUI 相关 slice：`DrawFrame`、`syncFrameState`、`dequeueBuffer`、`queueBuffer`

当你打开重 `onDraw` 或阻塞主线程时，`main` 线程上的实验片段会变长；RenderThread 只能消费已经同步过去的渲染状态。这个对比比单看源码更容易建立直觉。

## dumpsys gfxinfo

```bash
adb shell dumpsys gfxinfo com.render
adb shell dumpsys gfxinfo com.render framestats
```

这里能看到应用帧耗时统计。它没有 Perfetto 直观，但适合快速确认有没有大量慢帧。

## 对应源码

- `app/src/main/java/com/render/renderthread/RenderThreadLabActivity.kt`
- `app/src/main/java/com/render/renderthread/RenderThreadLabView.kt`
- `app/src/main/java/com/render/renderthread/SurfaceCanvasLabView.kt`
- `app/src/main/java/com/render/common/FrameStats.kt`

## 概念补充

### vsync 注册流程

App 通过 `android.view.Choreographer#postFrameCallback()` 表示自己对下一次 vsync 信号感兴趣。

最终会通过 `android.view.DisplayEventReceiver#nativeScheduleVsync`，由 App 层告诉 framework 层自己对下一次 vsync 信号感兴趣。

native 的 DisplayEventReceiver 和 SurfaceFlinger 有一个 EventLoop 机制。

SurfaceFlinger 通过 Hardware Abstract Layer 和 Hardware Composer 建立联系。

一次注册只收到一次回调，下一次回调需要自己再注册。

### RenderThread

一个 App 进程只有一个 native 层的 RenderThread 线程，但是可以拥有不同的 `android.view.ThreadedRenderer`、`android.graphics.HardwareRenderer` 对象。

native 进程的 RenderThread 维护一个渲染队列，队列中的每个任务有自己的 Surface、CanvasContext。

App 进程一旦提交了指令后，就可以 `queueBuffer`，GPU 是否绘制完毕交由后面 SurfaceFlinger 来控制。

### BLASTBufferQueue

BLASTBufferQueue 用于减少 buffer 和窗口属性不同步导致的显示问题，并提高 window resize、SurfaceView、转场动画等场景的稳定性。

三缓冲可以减少 App 空闲等待；但如果 App 绘制得过快，就是 producer 过快，由于 consumer 还是只有 16.6ms 的速率，因此某些 touch 操作反而可能延迟一帧。

## 官方源码/文档入口

- Android graphics architecture: https://source.android.com/docs/core/graphics/architecture
- SurfaceView and GLSurfaceView: https://source.android.com/docs/core/graphics/arch-sv-glsv
- Frame pacing: https://source.android.com/docs/core/graphics/frame-pacing
- `RenderThread.cpp`: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/libs/hwui/renderthread/RenderThread.cpp
- `RenderProxy.cpp`: https://android.googlesource.com/platform/frameworks/base/+/master/libs/hwui/renderthread/RenderProxy.cpp
- `DrawFrameTask.cpp`: https://android.googlesource.com/platform/frameworks/base/+/master/libs/hwui/renderthread/DrawFrameTask.cpp
- `ThreadedRenderer.java`: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/view/ThreadedRenderer.java
- Perfetto CLI simple mode: https://perfetto.dev/docs/reference/perfetto-cli
- ATrace app instrumentation: https://perfetto.dev/docs/getting-started/atrace
