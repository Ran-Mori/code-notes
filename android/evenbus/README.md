## 概述

* 就是一个耦合度极低的观察者模式的框架

## ref

* [Analysis of EventBus principle](https://programmer.group/analysis-of-eventbus-principle.html)

## 四种threadMode模式

* `POSTING`：在`Observable`的线程执行
* `Main`：`Observer`方法在`Main`(UI线程)执行
  * 如果`Observable`是在`Main`线程发出post。那么`Observer`立即执行，导致`Observerable`被阻塞
  * 如果`Observable`不在`Main`线程发出post，那么所有post构成一个队列，依次执行，`Observable`不会被阻塞
* `MAIN_ORDERED`：post总是在一个队列里，`Observable`永远不会被阻塞
* `BACKGROUND`
  * 如果`Observable`是在`Main`线程发出post。那么事件被**队列化**安排到一条固定的`Backgroud`线程执行，有可能会阻塞`backgroud`线程
  * 如果被`Observable`不是在`Main`线程发出post。那么任务队列就直接在发出post的那条线程执行
* `ASYNC`：既不在`Main`线程执行，也不在`Observable`的post线程执行。EventBus有一个线程池

## 源码解析

```java
public class EventBus {
  public void register(Object subscriber) {
    //查找订阅方法
    List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
    //将方法进行订阅
    synchronized (this) {
      for (SubscriberMethod subscriberMethod : subscriberMethods) {
        subscribe(subscriber, subscriberMethod);
      }
    }
  }
  
  public void post(Object event) {
    //将事件抛出，调用postSingleEvent方法
    postSingleEvent(eventQueue.remove(0), postingState);
  }
  
  private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
    synchronized (this) {
      //通过抛出的事件找到订阅方法
      subscriptions = subscriptionsByEventType.get(eventClass);
    }
    for (Subscription subscription : subscriptions) {
      //将事件处理传递给postToSubscription方法
      postToSubscription(subscription, event, postingState.isMainThread); 
    }
  }
  
  private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
    switch (subscription.subscriberMethod.threadMode) {
      //区分注解threadMode
      case POSTING:
        invokeSubscriber(subscription, event);
        break;
      case MAIN:
        //将事件处理传递给invokeSubscriber
        invokeSubscriber(subscription, event);
        break;
      case ASYNC:
        asyncPoster.enqueue(subscription, event);
        break;
    }
  }
  
  void invokeSubscriber(Subscription subscription, Object event) {
    //直接反射开始执行
    subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
  }
}
```

## sticky event

1. 参考链接：[Sticky Events](https://greenrobot.org/eventbus/documentation/configuration/sticky-events/)
2. 概念：当使用`EventBus.getDefault().postSticky()`抛出一个事件时，这个事件就是`sticky event`。内存中始终会存储最近抛出的一个`sticky event`
3. 作用：一旦注册了EventBus，即`EventBus.getDefault().register(this)`，它`@Subscribe(sticky = true)`的方法就会看是否有这个已经抛出过被存储在内存中的`sticky event`，一旦有则立刻执行`@Subscribe`的方法体
4. 关键API
   * EventBus.getDefault().postSticky(stickyEvent)