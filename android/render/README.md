# Android Render 学习实验台

这个项目用多个独立页面学习 Android 渲染链路。`MainActivity` 只做路由，不承载具体实验逻辑；每个学习主题都有自己的 Activity、View、trace slice、FrameMetrics 和文档。

## 学习页面

- [RenderThread 学习](docs/renderthread.md)：观察 `Choreographer#doFrame`、`View.onDraw()`、`FrameMetrics`、属性动画、SurfaceView 对照组，以及 Perfetto 中真正的 `RenderThread`。
- [大纹理上传](docs/texture-upload.md)：模拟大 Bitmap 内容变脏后，下一帧 `drawBitmap` 触发 HWUI 重新上传纹理到 GPU 的卡顿场景。
- [TextureView 学习](docs/textureview.md)：把 `TextureView` 拆成 `View`、`SurfaceTexture`、`Surface`、`BufferQueue` 和 producer/consumer，观察它如何作为普通 View 参与合成。

## 代码结构

```text
app/src/main/java/com/render/
  MainActivity.kt                    # 学习页路由入口
  common/
    FrameStats.kt                    # FrameMetrics 统计模型
    LabUi.kt                         # 代码方式搭 UI 的公共 helper
  renderthread/
    RenderThreadLabActivity.kt       # RenderThread 学习页
    RenderThreadLabView.kt           # 普通 View / onDraw 观察点
    SurfaceCanvasLabView.kt          # SurfaceView app-owned producer 对照组
  textureupload/
    TextureUploadLabActivity.kt      # 大纹理上传学习页
    TextureUploadLabView.kt          # 大 Bitmap / dirty / drawBitmap 观察点
  textureview/
    TextureViewLabActivity.kt        # TextureView 学习页
    TextureViewLabView.kt            # SurfaceTexture listener / producer Surface / buffer 更新观察点
```

后面新增学习主题时，优先按这个形状拆：

```text
app/src/main/java/com/render/<topic>/
  <Topic>LabActivity.kt
  <Topic>LabView.kt

docs/<topic>.md
```

然后只在 `MainActivity` 里新增一条路由。

## 运行

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.render/.MainActivity
```

也可以直接启动某个学习页：

```bash
adb shell am start -n com.render/.renderthread.RenderThreadLabActivity
adb shell am start -n com.render/.textureupload.TextureUploadLabActivity
adb shell am start -n com.render/.textureview.TextureViewLabActivity
```

看日志：

```bash
adb logcat -s RenderThreadLab TextureUploadLab TextureViewLab
```

## Perfetto

两个学习页都可以用同一个 App trace 命令开始：

```bash
adb shell perfetto -o /data/misc/perfetto-traces/render-lab.pftrace -t 15s --app com.render sched freq idle gfx view wm am binder_driver
adb pull /data/misc/perfetto-traces/render-lab.pftrace .
```

不同主题要搜的 app slice 写在各自文档里。
