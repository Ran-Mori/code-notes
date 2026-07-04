# TextureView 学习

这个页面用来学习 `TextureView` 的底层角色分工。你平时会用它，大概率已经熟悉 `SurfaceTextureListener`、相机预览、视频预览、`Surface(surfaceTexture)` 这些 API；这个实验要补的是：这些对象到底谁生产 buffer、谁消费 buffer、为什么它能像普通 View 一样旋转/透明/裁剪，又为什么它不是一个“更快的 View”。

## 主线

```text
TextureView attached to window
  -> framework creates / exposes a SurfaceTexture
  -> app receives onSurfaceTextureAvailable
  -> app creates Surface(surfaceTexture) for one producer
  -> producer queues buffers: Canvas / Camera / MediaCodec / OpenGL
  -> TextureView consumes newest buffer as a GLES texture
  -> HWUI composites that texture with the normal View hierarchy
  -> SurfaceFlinger composes the app window
```

这条链路里最重要的直觉是：

- `TextureView` 本身仍然是一个普通 `View`，所以它可以做 alpha、rotation、scale、translation，也可以被其他普通 View 盖在上面。
- `TextureView` 的内容不是靠你的 `onDraw()` 画出来的；内容来自它背后的 `SurfaceTexture`。
- `SurfaceTexture` 背后有 producer/consumer 模型。producer 往 buffer 里写，consumer 把最新 buffer 当成纹理采样。
- 用 `TextureView` 时，framework/TextureView 帮你管理 consumer 侧；你通常拿到 `SurfaceTexture` 后创建 `Surface`，交给某个 producer 写入。

## 页面里有什么

- `TextureViewLabActivity`：TextureView 学习页、`FrameMetrics`、主线程阻塞、View 属性动画、事件日志。
- `TextureViewLabView`：真正的 `TextureView`，实现 `SurfaceTextureListener`，并创建一个 `Surface(surfaceTexture)` 给后台 producer 线程使用。
- “producer 画一帧”：后台线程调用 `Surface.lockCanvas()`，画完后 `unlockCanvasAndPost()`。
- “后台 producer 连续往 Surface 投递 buffer”：模拟相机/视频/GL 一类持续产帧的 producer。
- “对 TextureView 做旋转/透明度 View 动画”：观察 buffer 内容更新和 View 属性变换是两层事。
- “normal View overlay”：普通 TextView 盖在 TextureView 上方，用来提醒你 TextureView 是 View tree 的一员。

## 角色表

| 代码/对象 | 在实验里的角色 | 你应该建立的直觉 |
| --- | --- | --- |
| `TextureView` | View tree 里的一个 View | 能做普通 View 变换，但内容来自外部 buffer |
| `SurfaceTexture` | TextureView 暴露出来的 buffer/texture 连接点 | 它连接 producer 的 buffer 和 consumer 的 GLES texture |
| `Surface(surfaceTexture)` | producer 侧入口 | Canvas、Camera、MediaCodec、OpenGL 都可以把它当输出目标 |
| `TextureViewProducer` | 实验里的 app-owned producer 线程 | 它不是 RenderThread，只是我们自己创建的产帧线程 |
| `onSurfaceTextureUpdated()` | 新 buffer 被 TextureView 更新后的回调 | 适合观察 producer 投递后 TextureView 是否收到更新 |
| `FrameMetrics` | 整个 Activity window 的帧统计 | 观察 TextureView 更新进入 View 合成后对帧耗时的影响 |

## 建议实验顺序

1. 进入“TextureView 学习”页面，打开 Logcat：

   ```bash
   adb logcat -s TextureViewLab
   ```

2. 观察启动日志：你应该先看到 `onSurfaceTextureAvailable`，随后实验自动让 producer 画第一帧。
3. 点击“producer 画一帧”。看事件日志里 `Producer posted frame` 和 `onSurfaceTextureUpdated` 的先后关系。
4. 打开“后台 producer 连续往 Surface 投递 buffer”。观察 `producerFrames` 和 `onSurfaceTextureUpdated` 是否持续增长。
5. 打开“对 TextureView 做旋转/透明度 View 动画”。这时 producer 画出来的 buffer 内容没有改变，但整个 TextureView 会被 View 属性动画变换。
6. 点击“阻塞主线程 700ms”。producer 线程可能还能尝试产帧，但 TextureView 的可见更新、回调派发、HWUI 合成仍然需要 UI/HWUI 链路继续推进。
7. 对照 `FrameMetrics`。连续 producer 加 View 变换时，重点看 `total`、`sync`、`command`、`swap`、`gpu` 是否升高。

## Perfetto 观察点

```bash
adb shell perfetto -o /data/misc/perfetto-traces/textureview-lab.pftrace -t 15s --app com.render sched freq idle gfx view wm am binder_driver
adb pull /data/misc/perfetto-traces/textureview-lab.pftrace .
```

先搜 App 自己打的 slice：

```text
RTLab.TextureViewActivity#onCreate
RTLab.TextureView.onSurfaceTextureAvailable
RTLab.TextureView.producerFrame(single)
RTLab.TextureView.producerFrame(continuous)
RTLab.TextureView.mainThreadSleep700ms
```

然后看这些线程/区域：

- 线程：`main`
- 线程：`RenderThread`
- 线程：`TextureViewProducer`
- 线程：`TextureViewFrameMetricsCollector`
- 系统进程：`surfaceflinger`
- HWUI 相关 slice：`DrawFrame`、`syncFrameState`、`dequeueBuffer`、`queueBuffer`

理解 Perfetto 时要分清两个线程：

- `TextureViewProducer` 是这个实验自己创建的 producer。它负责往 `Surface` 里写 buffer。
- `RenderThread` 是 Android HWUI 的渲染线程。它负责把 View tree，包括 TextureView 对应的纹理，合成进 app window。

它们名字都和“渲染”有关，但职责完全不同。

## 对应源码

- `app/src/main/java/com/render/textureview/TextureViewLabActivity.kt`
- `app/src/main/java/com/render/textureview/TextureViewLabView.kt`
- `app/src/main/java/com/render/common/FrameStats.kt`

## 概念补充

### TextureView 和 SurfaceView 的差异

`SurfaceView` 更像是在 View 层级里挖出一个独立 surface/layer，让另一个 producer 直接往那个 surface 里写。它适合高性能视频、相机、游戏等场景，尤其在 Android 7.0 之后，SurfaceView 在缩放、移动、转场同步方面已经比早期稳定很多。

`TextureView` 则是把外部 buffer 当成纹理带回普通 View tree 里。代价是它通常要经过 HWUI/GPU 合成，不容易走硬件 overlay；好处是它能自然参与普通 View 的 alpha、rotation、scale、clip、overlay。

一句话：

```text
SurfaceView: 更像独立 surface/layer，性能路径更直接
TextureView: 更像 View tree 里的纹理窗口，变换和叠加更自然
```

### SurfaceTexture 不是“你画布的名字”

`SurfaceTexture` 更像一个连接器：一端连 producer 写入的 buffer，另一端把最新 buffer 暴露成 GLES 纹理。原始使用 `SurfaceTexture` 时，consumer 侧常见动作是 `updateTexImage()`；但在 `TextureView` 场景下，这部分通常由 framework/TextureView 处理。

所以你在业务里常见的写法：

```kotlin
override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
    val surface = Surface(surfaceTexture)
    // pass surface to camera / player / codec / custom renderer
}
```

可以理解成：

```text
TextureView owns consumer side
your camera/player/renderer owns producer side
Surface(surfaceTexture) is the producer endpoint
```

### 只能有一个清晰的 producer

这个实验用 `Surface.lockCanvas()` 当 producer，是为了让链路可见。真实项目里如果你已经把 `Surface(surfaceTexture)` 交给 Camera、MediaCodec、MediaPlayer 或 GL 线程，就不要同时再用 `lockCanvas()` 往同一个 Surface 画。一个 Surface 目标应该有一个明确 producer，否则 buffer 生命周期和同步会非常混乱。

### TextureView 不是解决 UI 卡顿的捷径

把内容放进 TextureView，并不代表 UI 线程就无关了。producer 可以在后台线程产帧，但 TextureView 仍然作为 View tree 的一部分被 HWUI 合成。如果主线程被阻塞，View 状态同步、回调派发、下一帧提交都会受影响。

这也是这个页面保留“阻塞主线程 700ms”按钮的原因：它不是为了证明 producer 停了，而是为了观察“producer 能产帧”和“屏幕能顺畅显示这些帧”不是同一件事。

## 官方源码/文档入口

- `TextureView` API reference: https://developer.android.com/reference/android/view/TextureView
- `SurfaceTexture` API reference: https://developer.android.com/reference/android/graphics/SurfaceTexture
- Android graphics architecture: https://source.android.com/docs/core/graphics/architecture
- SurfaceView and GLSurfaceView architecture: https://source.android.com/docs/core/graphics/arch-sv-glsv
- `TextureView.java`: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/TextureView.java
- `SurfaceTexture.java`: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/graphics/java/android/graphics/SurfaceTexture.java
