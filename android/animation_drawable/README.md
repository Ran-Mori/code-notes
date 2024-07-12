## AnimationDrawable使用

1. 定义一个`xml`

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <animation-list>
       <item android:drawable="@drawable/red" android:duration="2000" />
       <item android:drawable="@drawable/blue" android:duration="2000" />
       <item android:drawable="@drawable/green" android:duration="2000" />
   </animation-list>
   ```

2. 将`xml`赋值给`View.background`

3. 将`background`转成`AnimationDrawable`，然后调用`start()`方法开始动画

   ```kotlin
   (imageView?.drawable as? AnimationDrawable)?.start()
   ```
