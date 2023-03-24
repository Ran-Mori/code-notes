## Java

## Rxjava#Subject

* `Represents an Observer and an Observable at the same time, allowing multicasting events from a single source to multiple child Observers.`
* 各种`Subject`的区别有实际代码通过log表现

### 执行顺序

* 验证`Observable、Observer、map`等操作的执行顺序
* 验证`observeOn、subscribeOn`线程的区别

***