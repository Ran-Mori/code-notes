# view_tree_observer

## 类关系

* View.java

  ```java
  // 每个View都有一个获取ViewTreeObserver的接口，且都是从mAttachInfo获取
  public ViewTreeObserver getViewTreeObserver() {
      if (mAttachInfo != null) {
          return mAttachInfo.mTreeObserver;
      }
      return mFloatingTreeObserver;
  }
  
  void dispatchAttachedToWindow(AttachInfo info, int visibility) {
    	// View attach的时候赋值
      mAttachInfo = info;
  }
  
  // 是一个静态类
  final static class AttachInfo {
   	IWindow mWindow;
    IBinder mWindowToken;
    WindowId mWindowId;
    View mRootView;
    ViewTreeObserver mTreeObserver; //持有ViewTreeObserver
    AttachInfo() {
      // 构造函数时new一个
      mTreeObserver = new ViewTreeObserver(context);
    }
  }
  ```

* ViewRootImpl.java

  ```java
  // 持有AttachInfo
  final View.AttachInfo mAttachInfo;
  
  public ViewRootImpl() {
    // 构造函数时赋值
    mAttachInfo = new View.AttachInfo()
  }
  
  private void performTraversals() {
    // WindowAttachedChange回调
    mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true);
    
    if (didLayout) {
      // 先做layout
      performLayout(lp, mWidth, mHeight);
      // layout结束后调GlobalLayout回调
      mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
    }
    
    // 先调OnPreDraw，且记录是否cancel
    boolean cancelAndRedraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw();
    if (cancelAndRedraw) {
      // Try again. 直接重试整个traversal，即重measure、layout、draw
      scheduleTraversals();
    } else {
      // 未取消才真正的draw
      performDraw()
    }
  }
  ```

## ViewOnGlobalLayout

* 触发时机: layout结束后，preDraw之前
* 用处，可以在这时候获取View的宽高

## ViewOnPreDraw

* 触发时机: layout结束后，draw之前
* 返回值: bool表示是否取消这次draw
* 用处
  1. 获取View的宽高
  2. 取消draw重走`traversal`，这样UI就不会跳变

## ViewOnDraw

* 触发时机: draw之后
* 用: 暂时没有想到
