## 概述

* 代表安卓应用中的**一个屏幕** ，不同的屏幕对应不同的`Activity`，比如电子邮件列表屏幕、电子邮件编辑屏幕
* 之所以能够理解成代表一个`屏幕`和后续的`window`机制有关，一个`window`因为持有`ViewRootImpl`是一颗`view`树
* `Activity`的存在支持了每次调用应用不是一定从一个固定的屏幕开始(也就是通常所说的主函数)。比如浏览器点击发送邮件按钮应该从编辑邮件按钮界面开始，而不是从一般的邮件列表开始
* `Activity`提供窗口让应用绘制界面。窗口可能铺满实际物理屏幕，也可能比实际物理屏幕小
* `Acitivity`之间的依赖耦合很小

## onSaveInstanceState()

* 用户自己退出时不会调用
* 只有因为系统资源紧张，系统自动把它清除掉或者其他原因才会调用此方法
* 此方法的对应回调在`fun onCreate(savedInstanceState: Bundle?)`中

## A切换到B的执行顺序

* `A.onPause()`、`B.onCreate()`、`B.onStart()`、`B.onResume()`、`A.onStop()`
* 两者生命周期是有重叠的

## 健壮的Activity

* 一个合格稳定的Activity，一定要在意外的情况下也能逻辑正确
* 其他应用(电话)阻断了此Activity
* 系统自动回收销毁又创建此Activity
* 将此Activity放在新的窗口环境中，如画中画、多窗口环境等

## multi process

* If your app is running in multiple processes, each process will have its own instance of Application. This can happen if you have specified multiple processes in your AndroidManifest.xml using the android:process attribute.

  ```xml
  <service
      android:name=".MyService"
      android:process=":remote" />
  ```

## android:launchMode