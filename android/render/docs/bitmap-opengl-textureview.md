# BitmapFactory → OpenGL → TextureView

这个页面演示一条完全显式、可以逐段打断点的渲染链路：先用 `BitmapFactory` 把项目里的 JPG 解码成 CPU `Bitmap`，再由 App 自己创建 EGL/OpenGL 环境，把像素上传成 `GL_TEXTURE_2D`，用 shader 绘制一个矩形，最后把 OpenGL 的输出 buffer 交给 `TextureView` 显示。

它和“大纹理上传”页面关注的不是同一件事：

- “大纹理上传”把 `Bitmap` 交给普通 `View.onDraw()` / `Canvas.drawBitmap()`，后续纹理上传由 HWUI/RenderThread 管理。
- 本页面不经过 `Canvas.drawBitmap()`；App 直接调用 `GLUtils.texImage2D()`，因此 EGLContext、GL texture、shader、draw 和 swap 都由自己的 GL 线程管理。

## 完整主线

```text
app/src/main/res/drawable-nodpi/bitmap_gl_sample.jpg
  |
  | BitmapFactory.decodeResource(..., inScaled=false)
  v
CPU Bitmap (960x640, ARGB_8888)
  |
  | GLUtils.texImage2D -> glTexImage2D
  v
App OpenGL texture (GL_TEXTURE_2D, textureId)
  |
  | vertex shader + fragment shader + glDrawArrays(GL_TRIANGLE_STRIP)
  v
EGL window surface
  |
  | eglSwapBuffers
  v
Surface(TextureView.surfaceTexture) queues a rendered buffer
  |
  v
TextureView / HWUI consumes newest buffer as part of the normal View tree
  |
  v
App window Surface -> SurfaceFlinger -> display
```

## 核心知识点总结

### 1. TextureView 这段链路只有一个内容 BufferQueue

`TextureView` 管理 consumer 侧的 `SurfaceTexture`；`Surface(surfaceTexture)` 是同一个 BufferQueue 的 producer 入口。EGL 用这个 `Surface` 创建 window `EGLSurface` 后，OpenGL 就成为该 BufferQueue 的 producer，而不是再创建一个独立的 OpenGL BufferQueue。

```text
EGL / OpenGL producer
  -> Surface
  -> [TextureView 内容 BufferQueue]
  -> SurfaceTexture / TextureView consumer
```

完整显示链路中还会有另一个 BufferQueue：HWUI/RenderThread 把包含 TextureView 在内的整个 View 树合成到 App window 后，通过 App window BufferQueue 交给 SurfaceFlinger。它和 TextureView 的内容 BufferQueue 是两个不同层级。

### 2. Fragment Shader 不会先生成一整块独立像素内存

Fragment Shader 每次只产生一个 fragment 的临时颜色值；经过 depth、stencil、blend、color mask 等逐片元操作后，结果写入当前 framebuffer 的 color attachment。

当前 Demo 没有创建自定义 FBO，因此绑定的是 window system 提供的默认 framebuffer `0`。它的 color buffer 背后，是 EGL 预先从 `Surface` 对应 BufferQueue 中 dequeue 出来的 `GraphicBuffer`：

```text
input GL texture
  -> fragment shader
  -> per-fragment operations
  -> framebuffer 0
  -> EGLSurface 当前 GraphicBuffer
```

物理存储位置由 GPU/驱动决定：结果可能先停留在寄存器、cache 或 tile memory，随后再写回 gralloc 分配的 GraphicBuffer。GraphicBuffer 也不保证是 CPU 可直接读取的线性 RGBA 数组。

### 3. `eglSwapBuffers()` 提交的是同一个 GraphicBuffer

`eglSwapBuffers()` 会提交/flush 当前 GL 工作，并将绘制完成的 GraphicBuffer 连同同步 fence、时间戳、damage 等元数据 queue 回同一个 BufferQueue。BufferQueue 通常只传递 buffer slot、handle 和 fence，不复制整帧像素：

```text
FREE
  -> DEQUEUED
  -> GPU rendering
  -> QUEUED + fence
  -> ACQUIRED by SurfaceTexture
  -> RELEASED
  -> FREE
```

TextureView/HWUI 等待 fence 后，把最新 GraphicBuffer 作为内容纹理采样，再合成进 App window。因此 `Surface` 是 buffer producer 接口，不是一块用来接收像素拷贝的内存。

### 4. FBO 是 OpenGL 内部的离屏渲染目标

FBO（Framebuffer Object）不是像素内存，也不是 BufferQueue。它记录 color、depth、stencil attachment 分别指向哪些 Texture 或 Renderbuffer；真正保存结果的是 attachment。

当前 Demo 没有调用 `glGenFramebuffers()` / `glBindFramebuffer()`，所以 Fragment Shader 直接输出到默认 framebuffer `0`。如果加入自定义 FBO，它位于 OpenGL 内部、最终 EGLSurface 之前：

```text
input texture
  -> shader pass 1
  -> custom FBO -> intermediate texture
  -> shader pass 2
  -> framebuffer 0 -> EGLSurface GraphicBuffer
  -> eglSwapBuffers -> TextureView
```

`eglSwapBuffers()` 不会自动提交自定义 FBO。要显示 FBO 内容，必须先重新绑定 framebuffer `0`，再把 FBO 的输出纹理绘制到默认 framebuffer。FBO 常用于相机滤镜、模糊、美颜、Bloom、阴影、离屏截图和其他多 pass 效果。

一句话记忆：

```text
Texture 是 shader 可采样的图像存储；
FBO 决定 shader 输出写到哪里；
framebuffer 0 连接 EGLSurface；
Surface 连接 BufferQueue producer；
SurfaceTexture / TextureView 消费最终 buffer。
```

首次进入页面时，以上链路会自动执行一次。测试 JPG 的四角颜色、`TOP` 箭头和 3:2 构图用于直接检查：

- 图片有没有上下颠倒；
- 图片有没有被非等比拉伸；
- OpenGL 输出是否确实进入 TextureView；
- 普通 `TextView` overlay 是否能盖在 TextureView 上面。

## 页面里的代码分工

### `BitmapOpenGlLabActivity`

Activity 只负责学习页 UI、对照按钮、事件日志和 window `FrameMetrics`。它不持有 EGL 或 textureId。

两个实验按钮分别是：

- “重解码 JPG + 重传”：重新执行 `BitmapFactory.decodeResource()`、`GLUtils.texImage2D()`、draw、swap。
- “复用 GL 纹理重绘”：跳过 JPG 解码和纹理上传，直接复用已有 `textureId` 执行 draw、swap。

这两个按钮的差别可以帮助你把“准备纹理”和“用纹理绘制”分开看。

### `BitmapOpenGlLabView`

它是一个真正的 `TextureView`，实现 `SurfaceTextureListener`。核心职责是管理两个生命周期：

```text
TextureView SurfaceTexture lifecycle
  -> onSurfaceTextureAvailable / SizeChanged / Destroyed

Activity lifecycle
  -> onHostResume / onHostPause / release
```

当 `SurfaceTexture` 可用而且 Activity 处于 resumed 状态时，它执行：

```kotlin
val surface = Surface(surfaceTexture)
val thread = HandlerThread("BitmapOpenGlProducer")
// 把 surface 交给同一个线程上的 EGL/OpenGL renderer
```

这里的 `BitmapOpenGlProducer` 是 App 自己创建的 GL producer 线程，不是系统的 `RenderThread`。

销毁时的顺序也很重要：先在 GL 线程删除 GL 对象并销毁 `EGLSurface` / `EGLContext`，再释放 producer `Surface`，最后让 TextureView 释放 `SurfaceTexture`。否则仍在工作的 EGL 可能继续访问已经失效的 native window。

### `BitmapTextureRenderer`

这个类的所有方法都只在 `BitmapOpenGlProducer` 线程运行。它负责：

1. `eglGetDisplay()`、`eglInitialize()`、`eglChooseConfig()`；
2. 创建 OpenGL ES 2.0 `EGLContext`；
3. 用 `Surface(surfaceTexture)` 创建 EGL window surface；
4. `eglMakeCurrent()`；
5. 编译、链接 vertex shader 和 fragment shader；
6. 用 `BitmapFactory` 解码 JPG；
7. 用 `GLUtils.texImage2D()` 上传纹理；
8. `glDrawArrays()` 绘制；
9. `eglSwapBuffers()` 投递输出 buffer；
10. 在暂停或 surface 销毁时释放所有 GL/EGL 资源。

## EGL、OpenGL texture、TextureView 的关系

这段 Demo 最容易混淆的是“texture”出现了不止一次。

### 第一层：App 创建的输入纹理

```text
JPG -> Bitmap -> GL_TEXTURE_2D
```

这是 `BitmapTextureRenderer.textureId`。fragment shader 采样的就是它。它属于这个 App 自己的 EGLContext；Activity 暂停、context 被销毁后，这个 textureId 也就失效了。

### 第二层：TextureView 消费的输出 buffer

OpenGL 并不是直接把上面的 `textureId` 交给 TextureView。实际过程是：

```text
sample input texture
  -> render pixels into EGL window surface's current buffer
  -> eglSwapBuffers queues that completed buffer
  -> TextureView consumes the newest buffer through its SurfaceTexture
  -> HWUI treats TextureView content as a texture while composing the View tree
```

所以这里存在两次不同语义的“纹理”：

- App GL shader 的输入纹理：来自 JPG；
- TextureView/HWUI 合成时采样的内容纹理：来自 App GL producer 已经渲染完成的 buffer。

不要把这两者理解成同一个 textureId 在两个 EGLContext 之间直接传递。

## 为什么要先创建 `Surface(surfaceTexture)`

`TextureView` 回调给 App 的 `SurfaceTexture` 是连接 producer/consumer 的对象。App 为 producer 创建 SDK 层入口：

```kotlin
val producerSurface = Surface(surfaceTexture)
```

然后 EGL 把这个 `Surface` 当作 native window 来创建输出目标：

```kotlin
val eglSurface = EGL14.eglCreateWindowSurface(
    eglDisplay,
    eglConfig,
    producerSurface,
    intArrayOf(EGL14.EGL_NONE),
    0
)
```

从这之后：

- `glDrawArrays()` 修改当前 EGLSurface 对应的 back buffer；
- `eglSwapBuffers()` 完成当前帧，并把同一个 GraphicBuffer queue 回 BufferQueue 供 consumer 获取；
- `TextureView.onSurfaceTextureUpdated()` 可以作为“TextureView 收到新内容”的观察点。

App 不需要、也不能通过公开 SDK 直接操作中间的 BufferQueue。

## JPG 解码与纹理上传

测试图片放在 `drawable-nodpi`，同时显式设置 `inScaled=false`：

```kotlin
val bitmap = BitmapFactory.decodeResource(
    resources,
    R.drawable.bitmap_gl_sample,
    BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inScaled = false
    }
)
```

这样不同 density 的设备不会在解码时自动缩放测试图，页面状态应该稳定显示 `jpeg=960x640`。

上传代码的核心是：

```kotlin
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
```

`GLUtils.texImage2D()` 是 Android 对 `glTexImage2D()` 的便利封装，它根据 `Bitmap` 格式选择合适的 GL format/type，并把 CPU 像素复制/上传到当前绑定的纹理。调用返回后，Demo 会 `bitmap.recycle()`；GL 纹理已经拥有自己的内容，不再依赖这个 CPU Bitmap。

在真实项目中还要考虑：

- `GL_MAX_TEXTURE_SIZE`：图片宽高不能超过当前设备限制；
- 大图的 CPU 内存峰值：`width * height * 4` 只是 ARGB_8888 像素的近似下限；
- 上传发生在哪一帧、是否会阻塞 GL 线程；
- 是否应该采样、分块、复用 texture storage，或改用更合适的图片/视频管线；
- EGLContext 丢失后必须重新创建 GL 资源，旧 textureId 不能继续使用。

## 为什么顶点纹理坐标要翻转 Y

Android `Bitmap` 的第 0 行是图片顶部，而 OpenGL 通常把纹理坐标 `t=0` 看作底部。这个 Demo 在 quad 顶点数据中把纹理 Y 坐标翻转：

```text
screen bottom -> texture t=1
screen top    -> texture t=0
```

如果去掉翻转，测试图里的 `TOP` 和向上箭头会倒过来。这里没有修改原 Bitmap，而是在采样坐标阶段解决方向差异。

## 为什么画面保持 3:2 而不是铺满 TextureView

测试 JPG 是 960x640，也就是 3:2。TextureView 在不同设备上的实际宽高比可能不同。Demo 根据：

```text
surfaceAspect = surfaceWidth / surfaceHeight
bitmapAspect  = bitmapWidth / bitmapHeight
```

计算 vertex shader 的 `uScale`，选择缩放 X 或 Y，做类似 `centerInside` 的等比显示。未被图片覆盖的区域由 `glClearColor()` 填充，所以不会拉伸图片。

## 线程模型

```text
main
  BitmapOpenGlLabActivity
  TextureView.SurfaceTextureListener callbacks
  onSurfaceTextureUpdated callbacks
  View overlay / window FrameMetrics UI

BitmapOpenGlProducer
  BitmapFactory.decodeResource
  EGL object creation and destruction
  shader compile/link
  GL texture upload
  glDrawArrays
  eglSwapBuffers

RenderThread (system/HWUI)
  consumes TextureView content as part of the View tree
  renders/composes the Activity window
```

“OpenGL 在后台线程输出”和“Activity 主线程完全不影响显示”不是一回事。App GL producer 可以不在 main 上解码与绘制，但 TextureView 仍是普通 View，View 状态同步和整个 window 的 HWUI 合成仍依赖 Android 的 UI 渲染链路。

## 状态和耗时应该怎样读

页面状态会显示：

- TextureView surface 是否可用、尺寸是多少；
- `onSurfaceTextureUpdated()` 次数；
- JPG 解码后的宽高和 Bitmap config；
- 当前设备 `GL_MAX_TEXTURE_SIZE`；
- App GL textureId；
- decode、upload、draw、swap 的 CPU 调用耗时；
- GL thread 和 `GL_RENDERER`。

需要特别注意：

- `decode` 是 `BitmapFactory.decodeResource()` 在 CPU 上的墙钟耗时；
- `upload` 是 `GLUtils.texImage2D()` 调用返回前的墙钟耗时；
- `drawCpu` 是提交 GL draw commands 的 CPU 耗时；
- `swapCpu` 是 `eglSwapBuffers()` 调用的 CPU 耗时；
- 这些数字不是精确的 GPU execution time。OpenGL 命令可能异步执行，驱动也可能把等待放在 upload、draw、swap 或之后的同步点。

页面里的 `FrameMetrics` 统计的是 Activity window 帧，不是 App 自己这个 EGLSurface 的逐命令 GPU 计时。要看完整关系，应结合 Perfetto 或 GPU 工具。

## 建议实验顺序

1. 启动页面并看 Logcat：

   ```bash
   adb logcat -s BitmapOpenGlLab
   ```

2. 检查画面：红/黄在上，蓝/绿在下，箭头向上，山景比例正常。
3. 检查状态：`producerThread=BitmapOpenGlProducer`、`eglReady=true`、`jpeg=960x640`、`textureId` 非 0。
4. 连续点击几次“复用 GL 纹理重绘”。观察 frame 增加，但 decode/upload 数值不代表重新执行。
5. 点击“重解码 JPG + 重传”。观察日志重新出现 decode/upload，并继续复用或更新当前 texture storage。
6. 按 Home 或切换 Activity 再回来。页面会销毁旧 EGLContext，并重新解码、上传；这说明 textureId 只在当前 context 生命周期内有效。
7. 抓 Perfetto，对齐 decode、upload、draw、swap 与 TextureView/HWUI 的后续工作。

## Perfetto 观察点

```bash
adb shell perfetto -o /data/misc/perfetto-traces/bitmap-gl-textureview.pftrace -t 15s --app com.render sched freq idle gfx view wm am binder_driver
adb pull /data/misc/perfetto-traces/bitmap-gl-textureview.pftrace .
```

先搜 App 自己打的 slice：

```text
RTLab.BitmapOpenGlActivity#onCreate
RTLab.BitmapGL.onSurfaceTextureAvailable
RTLab.BitmapGL.eglInitialize
RTLab.BitmapGL.decodeJpeg
RTLab.BitmapGL.uploadTexture
RTLab.BitmapGL.draw
RTLab.BitmapGL.eglSwapBuffers
RTLab.BitmapGL.release
```

再看线程和系统区域：

- `main`
- `BitmapOpenGlProducer`
- `RenderThread`
- `BitmapGlFrameMetricsCollector`
- `surfaceflinger`
- buffer dequeue/queue、HWUI DrawFrame、GPU completion 等相关事件

## 对应源码

- `app/src/main/java/com/render/bitmapgl/BitmapOpenGlLabActivity.kt`
- `app/src/main/java/com/render/bitmapgl/BitmapOpenGlLabView.kt`
- `app/src/main/java/com/render/bitmapgl/BitmapTextureRenderer.kt`
- `app/src/main/res/drawable-nodpi/bitmap_gl_sample.jpg`

## 可以继续做的扩展

理解当前静态纹理链路后，可以按这个顺序继续扩展：

1. 在 vertex shader 里增加旋转矩阵，连续绘制动画；
2. 增加第二张 JPG，比较新建 textureId 和复用 texture storage；
3. 用 `BitmapFactory.Options.inSampleSize` 对照大图采样；
4. 增加 FBO，先离屏渲染再输出到 EGLSurface；
5. 把 JPG 输入换成 Camera 或 MediaCodec 输出，观察 producer 类型改变后哪些环节仍相同；
6. 对照 `GLSurfaceView`，理解它替你封装了哪些 EGL/线程生命周期工作。

## 官方文档入口

- `BitmapFactory`: https://developer.android.com/reference/android/graphics/BitmapFactory
- `GLUtils`: https://developer.android.com/reference/android/opengl/GLUtils
- `EGL14`: https://developer.android.com/reference/android/opengl/EGL14
- `GLES20`: https://developer.android.com/reference/android/opengl/GLES20
- `TextureView`: https://developer.android.com/reference/android/view/TextureView
- `SurfaceTexture`: https://developer.android.com/reference/android/graphics/SurfaceTexture
- Android graphics architecture: https://source.android.com/docs/core/graphics/architecture
- EGLSurface and OpenGL ES: https://source.android.com/docs/core/graphics/arch-egl-opengl
- BufferQueue and gralloc: https://source.android.com/docs/core/graphics/arch-bq-gralloc
- Khronos OpenGL ES 2.0 reference: https://registry.khronos.org/OpenGL-Refpages/es2.0/xhtml/
