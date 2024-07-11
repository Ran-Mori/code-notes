## xml写动画和插值器

* 动画

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <translate
      xmlns:android="http://schemas.android.com/apk/res/android"
      android:fromYDelta="0"
      android:toYDelta="100"
      android:duration="1000"
      android:interpolator="@anim/cycler" />
  ```

* 插值器

  ```xml
  <?xml version="1.0"?>
  <cycleInterpolator
      xmlns:android="http://schemas.android.com/apk/res/android"
      android:cycles="5"/>
  ```

* 以上动画的效果是 -> 在y方向上执行5此平移，达到上下shake的视觉效果

## View执行动画流程

1. 设置动画

   ```java
   // android.view.View#startAnimation
   public void startAnimation(Animation animation) {
     // 存成View的临时变量
     setAnimation(animation);
     // 确保调一次draw
     invalidate(true);
   }
   ```

2. 触发draw

   ```java
   // android.view.View#draw(android.graphics.Canvas, android.view.ViewGroup, long)
   boolean draw() {
     final Animation a = getAnimation();
     if (a != null) {
       // 有动画就执行applyLegacyAnimation
       more = applyLegacyAnimation(parent, drawingTime, a, scalingRequired);
     }
   }
   ```

3. 执行applyLegacyAnimation

   ```java
   // android.view.View#applyLegacyAnimation
   

## 插值器

1. `LinearInterpolator`
2. `AccelerateInterpolator`
3. `DecelerateInterpolator`
4. `CycleInterpolator`
5. `AnticipateInterpolator()` - starts backward then flings forward
6. `OvershootInterpolator()` - flings forward and overshoots the last value then comes back

