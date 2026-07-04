# 大纹理上传

这个页面用来学习“上传纹理到 GPU 太耗时”这类卡顿问题。它模拟的是业务里大图内容频繁变化，导致 HWUI 在后续帧里重新上传纹理的场景。

## 主线

```text
main: Bitmap.setPixel(1px) marks content dirty
main: View.onDraw records drawBitmap
RenderThread/HWUI: consume DisplayList and upload texture
GPU: execute texture upload / draw commands
SurfaceFlinger: compose and present
```

页面会准备 3 张 `2048x2048 ARGB_8888` mutable Bitmap，CPU 侧总大小约 48MB。实验按钮每次只改每张 Bitmap 的 1 个像素，让 CPU 修改成本尽量小；这样你在 trace 里更容易把注意力放到后续 RenderThread/HWUI/GPU 的纹理上传成本上。

## 页面里有什么

- `TextureUploadLabActivity`：大纹理上传学习页的 Activity、`Choreographer`、`FrameMetrics`、事件日志。
- `TextureUploadLabView`：准备大 Bitmap、标记 Bitmap dirty、在 `onDraw()` 里记录 `drawBitmap`。
- “准备大纹理”：分配并绘制 3 张大 Bitmap。第一次显示也可能触发纹理上传。
- “单次触发上传”：每张 Bitmap 只改 1 个像素，然后请求下一帧。
- “连续每帧标记大纹理 dirty”：每个 vsync 都改 1 个像素并请求重绘，用来模拟业务持续刷新大图。
- “释放纹理”：停止连续实验并回收 Bitmap，避免一直占用内存。

## 建议实验顺序

1. 进入“大纹理上传”页面，打开 Logcat：

   ```bash
   adb logcat -s TextureUploadLab
   ```

2. 点击“准备大纹理”，先看首次 drawBitmap 和 FrameMetrics 的变化。
3. 开始抓 Perfetto，然后点击“单次触发上传”。对齐 `RTLab.Texture.singleDirtyUpload(main)` 和下一帧 `RTLab.Texture.drawBitmap(record)`。
4. 打开“连续每帧标记大纹理 dirty”。观察慢帧数量、`sync`、`command`、`swap`、`gpu` 是否明显变化。
5. 实验结束点击“释放纹理”。

## Perfetto 观察点

```bash
adb shell perfetto -o /data/misc/perfetto-traces/texture-upload-lab.pftrace -t 15s --app com.render sched freq idle gfx view wm am binder_driver
adb pull /data/misc/perfetto-traces/texture-upload-lab.pftrace .
```

先搜 App 自己打的 slice：

```text
RTLab.TextureUploadActivity#onCreate
RTLab.Texture.Choreographer#doFrame(main)
RTLab.Texture.prepareCpuBitmaps(main)
RTLab.Texture.singleDirtyUpload(main)
RTLab.Texture.markDirtyEachFrame(main)
RTLab.Texture.drawBitmap(record)
```

然后看这些 slice 后面的区域：

- 线程：`main`
- 线程：`RenderThread`
- 线程：`TextureFrameMetricsCollector`
- 系统进程：`surfaceflinger`
- HWUI 相关 slice：`DrawFrame`、`syncFrameState`、`uploadTexture`、`dequeueBuffer`、`queueBuffer`
- 指标：App 内 `FrameMetrics` 的 `sync`、`command`、`swap`、`gpu`

注意：`RTLab.Texture.drawBitmap(record)` 仍然是在主线程记录 drawBitmap 命令，它不等于 GPU 已经上传完成。真正的上传压力通常出现在这个 slice 之后的 RenderThread / HWUI / GPU 相关时间段里。

## 对应源码

- `app/src/main/java/com/render/textureupload/TextureUploadLabActivity.kt`
- `app/src/main/java/com/render/textureupload/TextureUploadLabView.kt`
- `app/src/main/java/com/render/common/FrameStats.kt`

## 这个模拟和真实业务的对应关系

真实业务里常见触发点包括：频繁生成新的大 Bitmap、不断修改同一张 Bitmap 内容、图片解码后立刻上屏、视频/相机帧被错误地走成普通 Bitmap 绘制路径。

这个实验刻意只改 1 个像素，是为了把 CPU 准备数据的成本压低。它不代表真实业务只会改 1 个像素；它代表“Bitmap 内容已经变脏，HWUI 需要重新看待这张纹理内容”这个关键条件。
