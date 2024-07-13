## purpose

* `measure`：determine the size requirements of a view before it is drawn on the screen. 

* `layout`：position the view on the screen.

* `draw`：draw the view on the screen.

## core method

* `void setMeasuredDimension(int measuredWidth, int measuredHeight)`
  *  This method must be called by onMeasure(int, int) to store the measured width and measured height.
* `boolean setFrame(int left, int top, int right, int bottom)` 
  * Assign a size and position to this view.
  * This is called from layout.
* `convas?.drawxxx()`

## recursive?

* measure - yes

  ```java
  //RelativeLayout#onMeasure()
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int count = views.length;
    for (int i = 0; i < count; i++) {
      final View child = views[i];
      if (child.getVisibility() != GONE) {
        // 先遍历所有children，依次调用measure完每一个child
        measureChild(child, params, myWidth, myHeight);
      }
    }
    //最后设置自己measure的宽高结果
    setMeasuredDimension(width, height);
  }
  ```

* layout - no

  ```java
  //View#layout()
  public void layout(int l, int t, int r, int b) {
    //已经调用setFrame()把自己放好了
    boolean changed = isLayoutModeOptical(mParent) ? setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);
    
    if (changed || condition.isOk()) {
      // 然后再尝试layout children
      onLayout(changed, l, t, r, b);
    }
  }
  ```

* draw - no

  ```java
  //View#draw()
  public void draw(Canvas canvas) {
    // Step 3, draw the content. 把自己draw完了
    onDraw(canvas);
    // Step 4, draw the children. 然后再draw children
    dispatchDraw(canvas);
  }
  ```

## MeasureSpec

1. 结构

   1. 前2位表示mode，后30位表示实际大小

2. 三种mode

   * `UNSPECIFIED`：父对子无任何限制，要多大给多大。一般不用

   * `EXACTLY`：父已经检测出了子的精确大小，就给那么大。对应`dp、match_parent`

   * `AT_MOST`：父指定了一个最大值，最大不能超过这个值。对应`wrap_content`

3. 如何获得

   * 公式 -> `子MeasureSpec = 父MeasureSpec + 子LayoutParams`

   * 代码实现 ->`ViewGroup#measureChild(View, int, int)`、 `ViewGoup#getChildMeasureSpec(int, int ,int)`

     ```java
     protected void measureChild(View child, int parentWidthMeasureSpec,
                 int parentHeightMeasureSpec) {
       // 获取子View的LayoutPrarams
       final LayoutParams lp = child.getLayoutParams();
     
       // 通过父View的MeasureSpec + 子View的LayoutPrarams获取子View的MeasureSpec
       final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,mPaddingLeft + mPaddingRight, lp.width);
       final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,mPaddingTop + mPaddingBottom, lp.height);
     
       // 调用子View的measure()进行测量
       child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
     }
     ```

   * 图示: 横轴为parent，众轴为child

     |              | EXACTLY | AT_MOST |
     | ------------ | ------- | ------- |
     | dp           | EXACTLY | EXACTLY |
     | match_parent | EXACTLY | AT_MOST |
     | wrap_content | AT_MOST | AT_MOST |

## measure相关方法

* `View#measure()`

  * 签名 -> `public final void measure(int widthMeasureSpec, int heightMeasureSpec)`

  * 特点 -> `final`不可被重写，此方法内会调用`onMeasure`

  * 源码

    ```java
    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
      // 调用onMeasure()
      onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    ```

* `View#onMeasure()`

  * 签名 -> `protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)`

  * 作用 -> 确定这个View的测量宽和测量高

  * 特点 -> `View`中有一个基础的默认实现，但子`View`和子`ViewGroup`都会进行复写

  * 源码

    ```java
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      //默认的实现，直接获取默认宽高调用setMeasuredDimension()
      setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                           getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
    ```

* `ViewGroup#measureChild()`

  * 作用 -> 构造子View的MeasureSpec，调用`child.measure()`

  * 源码

    ```java
    protected void measureChild(View child, int parentWidthMeasureSpec,
                int parentHeightMeasureSpec) {
      // 获取子View的LayoutPrarams
      final LayoutParams lp = child.getLayoutParams();
    
      // 通过父View的MeasureSpec + 子View的LayoutPrarams获取子View的MeasureSpec
      final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,mPaddingLeft + mPaddingRight, lp.width);
      final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,mPaddingTop + mPaddingBottom, lp.height);
    
      // 调用子View的measure()进行测量
      child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
    ```

* `ViewGroup#measureChildren()`

  * 作用 -> 遍历children并调用`measureChild(child, xx, xx)`

  * 源码

    ```java
    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
      final int size = mChildrenCount;
      final View[] children = mChildren;
      for (int i = 0; i < size; ++i) {
        final View child = children[i];
        if ((child.mViewFlags & VISIBILITY_MASK) != GONE) {
          // 循环遍历measure每一个child
          measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
      }
    }
    ```

## measure流程

1. 叶子节点`View`

   * `View#measure()` -> `View#onMeasure()`来确定自己的宽高
   * `View#onMeasure()`的默认实现 -> `View#getDefaultSize()`
   * 现状 -> 除了`View`类外根本不会有其他子类使用，子类都是根据自身的内容来决定自己的宽高，而不是仅仅根据一个`MeasureSpec`
2. 非叶子节点`ViewGroup`

   * `View#measure()` -> `XXXLayout#onMeasure()`调用来确定自己的高
   * 每个`XXXLayout`会重写`View#onMeasure()`，一般的业务逻辑是创建两个临时变量来记录自己宽高，遍历测量所有子View的宽高，每测量一个子View就更新临时变量值(比如LinearLayout的高由各个View累加)
   * `ViewGroup#measureChildren、ViewGroup#measureChild`一般没人用。这两个方法的逻辑都包含在`XXXLayout#onMeasure()`内

## self defined view wrap_content ?

* `View#onMeasure()`获取宽高默认的实现

  ```java
  //View#getDefaultSize
  public static int getDefaultSize(int size, int measureSpec) {
    //size是getSuggestedMinimumWidth()，返回mMinWidth或mBackground.getMinimumWidth()
    int result = size;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);
  
    switch (specMode) {
      case MeasureSpec.UNSPECIFIED:
        result = size;
        break;
      case MeasureSpec.AT_MOST:
      case MeasureSpec.EXACTLY:
        result = specSize;
        break;
    }
    return result;
  }
  ```

* 当`specMode`是`EXACTILY`时，`result = specSize`是无问题的；

* 当`specMode`是`AT_MOST`，此`View`设置为`match_parent`时无问题；当`specMode`是`AT_MOST`，此`View`设置为`wrap_content`就有大问题。因为它的实际效果和`match_parent`一样了，因此自定义View必须重写`onMeasure`，否则不支持`wrap_content`

## layout相关方法

1. `View#layout()`

   * 签名 -> `public void layout(int l, int t, int r, int b)`

   * 作用 -> 确定`View`本身的位置

   * 工作 -> 调用`setFrame()`方法来确定4个点的位置(相对于父容器)；如果布局有改变，则调用`View#onLayout()`方法来递归布局children

   * 源码

     ```java
     public void layout(int l, int t, int r, int b) {
       if (changed || conditon.isOk) {
         onLayout(changed, l, t, r, b);
       }
     }
     ```

2. `View#onLayout()`

   * 签名 -> `protected void onLayout(boolean changed, int left, int top, int right, int bottom)`

   * 特点 -> 在`View`中是个空方法，需要子`View`和`ViewGroup`去自己实现

   * 源码

     ```java
     protected void onLayout(boolean changed, int left, int top, int right, int bottom) { 
       //空实现 
     }
     ```

3. `ViewGroup#layout()`

   * 什么都没做，就是调用父类`View#layout()`

   * 源码

     ```java
     public final void layout(int l, int t, int r, int b) {
       super.layout(l, t, r, b)
     }
     ```

4. `ViewGroup#onLayout()`

   * 必须让子类重写

     ```java
     protected abstract void onLayout(boolean changed, int l, int t, int r, int b);
     ```

## layout流程

1. 叶子节点
   * `View#layout()` 把自己放好
2. 非叶子节点
   * `ViewGroup#layout()`将自己放好，会调用到`XXXLayout#onLayout()`
   * `XXXLayout#onLayout()` 将children放好

## draw相关

1. `View.draw()`

   * 这个方法一般不会`override`，一般只会重写`onDraw()`

   * 四个步骤

     * Step 1, draw the background, if need -  `(drawBackground(canvas))`
     * Step 3, draw the content - (`onDraw(canvas)`)
     * Step 4, draw the children - (`dispatchDraw(canvas)`)
     * Step 6, draw decorations (foreground, scrollbars) - (`onDrawForeground(canvas)`)

   * 实现

     ```java
     public void draw(Canvas canvas) {
       // Step 1, draw the background, if needed
       drawBackground(canvas);
       // Step 3, draw the content
       onDraw(canvas);
       // Step 4, draw the children
       dispatchDraw(canvas);
       // Step 6, draw decorations (foreground, scrollbars)
       onDrawForeground(canvas);
       // Step 7, draw the default focus highlight
       drawDefaultFocusHighlight(canvas);
     }
     ```

2. `View#onDraw()`

   * 特点 ->  在`View`中是一个空方法，需要不同的`View`和`ViewGroup`来自己实现

   * 作用 -> 负责如何`draw`自己

3. `View#dispatchDraw()`

   * 特点 -> 在`View`中是一个空方法，需要不同的`ViewGroup`来自己实现

4. `ViewGroup#drawChild`

   * 作用 -> 调用`child.draw()`

## draw()流程

1. 叶子节点
   * 直接调用`draw()`方法，通过`draw()`方法调`onDraw()`把自己draw出来
2. 非叶子节点
   1. 直接调用`draw()`方法，先通过`draw()`方法调`onDraw()`把自己draw出来，接着调用`dispatchDraw()`，复写的`dispatchDraw`会通过`drawChild()`最后调用到`child.draw()`把children给draw出来

## onXXX()与XXX()方法

* `onMeasure()、onLayout()、onDraw()` -> 代表这个View如何实现`measure、layout、draw`
* `measure()、layout()、draw()` -> 手动强项执行(或者系统某处需调用)`measure、layout、draw`
* 执行`draw()`前请确保`measure()和layout()`执行过

## where to start

* `ViewRootImpl#performTraversals()`

  ```java
  private void performTraversals() {
    // do measure
    measureHierarchy(host, lp, mView.getContext().getResources(),
                      desiredWindowWidth, desiredWindowHeight);
    // do layout
    performLayout(lp, mWidth, mHeight);
    
    // do draw
    if (!performDraw())
  }
  ```

## View#invalidate()

* 标记为`PFLAG_INVALIDATED`和`~PFLAG_DRAWING_CACHE_VALID`

  ```java
  void invalidateInternal() {
    // 置换标记位
    mPrivateFlags |= PFLAG_INVALIDATED;
  	mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
  }
  ```

* 下一次系统自动执行`ViewRootImpl#performTraversals()`时，会通过变量控制，达到只执行`performDraw()`而不执行`measureHierarchy()、performLayout()`的效果

## View#requestLayout()

* 标记为`PFLAG_FORCE_LAYOUT`和`PFLAG_INVALIDATED`。向上递归

  ```java
  public void requestLayout() {
    if (viewRoot != null && viewRoot.isInLayout()) {
      if (!viewRoot.requestLayoutDuringLayout(this)) {
        return; // 防止多次requestLayout
      }
    }
  	// 置标记位
    mPrivateFlags |= PFLAG_FORCE_LAYOUT;
    mPrivateFlags |= PFLAG_INVALIDATED;
    
    if (mParent != null && !mParent.isLayoutRequested()) {
      //递归向上调用做标记
      mParent.requestLayout();
    }
  }
  ```

* the view's requestLayout method almost does nothing except puts the flag to be 'invalidated', and the system will automatically handle it.

* 下一次系统自动执行`ViewRootImpl#performTraversals()`时，`measureHierarchy()、performLayout()、performDraw()`全都会执行
