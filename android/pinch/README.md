## matrix

* 是什么

  *  一个`3*3`的矩阵
  *  `scaletype`的一种类型

* 模型

  ```bash
  scaleX, skewX, translateX
  skewY, scaleY, translateY
  0, 0, 1
  ```

* 能实现的变换

  1. scale
  2. translate
  3. skew
  4. rotate

* api

  * `public void set(Matrix src)`
  * `public boolean postTranslate(float dx, float dy)`
  * `public boolean postScale(float sx, float sy, float px, float py)`

* 延伸

  * 其他的7种`scaletype`，源码都是通过设置`matrix`来实现的

## 多点触控

* 多指相关Action
  * `ACTION_POINTER_DOWN` -> A non-primary pointer has gone down
  * `ACTION_POINTER_UP` -> A non-primary pointer has gone up
* 一个`MotionEvent`包含多个手指，可以通过传入`pointerIndex`获取
  * `public final float getX(int pointerIndex)`
