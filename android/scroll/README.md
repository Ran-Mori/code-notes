## View

* mScrollY

  * 计算公式: View位置top边缘的纵坐标(一般是0) - View内容的top边缘的纵坐标(一般为负数)
  * 所以手向上滑动，下面的内容展示出来，这时的`mScrollY`是正数且越来越大
  * 设置`mScrollY`后，会在`draw()`的时候将canvas移动来达到`scroll`的效果

* scrollTo(int, int)

  * 代码实现

    ```java
    public void scrollTo(int x, int y) {
      if (mScrollX != x || mScrollY != y) {
        int oldX = mScrollX;
        int oldY = mScrollY;
        mScrollX = x;
        mScrollY = y; // 赋值
        invalidateParentCaches(); // 清缓存
        onScrollChanged(mScrollX, mScrollY, oldX, oldY); // 执行回调
        if (!awakenScrollBars()) {
          postInvalidateOnAnimation(); // 请求执行draw()
        }
      }
    }
    ```

  * 当一个View设置`mScrollX/mScrollY`后，会进行`draw`但不会重新进行`measure、layout`

  * 这个View本身的宽高、布局、位置全部都不变


## ScrollView

```java
public void scrollTo(int x, int y) {
  // we rely on the fact the View.scrollBy calls scrollTo.
  if (getChildCount() > 0) {
    View child = getChildAt(0);
    x = clamp(x, getWidth() - mPaddingRight - mPaddingLeft, child.getWidth());
    y = clamp(y, getHeight() - mPaddingBottom - mPaddingTop, child.getHeight());
    if (x != mScrollX || y != mScrollY) {
      super.scrollTo(x, y); // 它就包了一层，限定只有一个child，实际还是调用View#scrollTo(x, y)
    }
  }
}

// 重写了onTouchEvent()，手势的时候实现滑动scroll
public boolean onTouchEvent(MotionEvent ev) {
  // 噼里啪啦一堆实现
}
```

## Scroller

* 使用介绍

  ```kotlin
  class MyView: View() {
    val scroller = Scorller(context)
    
    fun smoothScrollTo(int destX, int destY) {
      scroller.startScroll(startX, startY, dx, dy, duration)
      invalidate()
    }
    
    override fun computeScroll() {
      if (scroller.computeScrollOffset()) {
        val currX = scroller.getCurrX()
        val currY = scroller.getCurrY()
        scrollTo(currX, currY)
        // Update the position of the view here
        invalidate()
      }
    }
  }
  ```

  ```java
  public class View {
    boolean draw() {
      computeScroll();
    }
  }
  ```

* 作用

  * 仅仅作为一个计算器，计算了一下应该滑动的值，它本身并不负责滑动

* 过程

  1. 先new了一个`Scroller`对象
  2. 接着调用`scroller.startScroll(startX, startY, dx, dy, duration)`，规定了在指定的duration内滑动多远
  3. 然后马上调用`invalidate()`触发调用`draw()`
  4. 在`draw()`中又调用到了`computeScroll()`，View中本身没有实现这个方法，因此调用到了子类的`computeScroll()`方法
  5. `computeScroll()`中用`scroller`计算出来的应滑动的值来滑动。接着最后又调用了`invalidate()`，回到了步骤3

***

## 