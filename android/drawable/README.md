## AnimationDrawable

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

## VectorDrawable

* 矢量图可以理解成是一个接口，而Android的`xml drawable`是对这个接口的实现

* bessel_shadow.xml

  ```xml
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:aapt="http://schemas.android.com/aapt"
      android:width="333dp"
      android:height="260dp"
      android:viewportWidth="333"
      android:viewportHeight="260">
    <path
        android:pathData="M0,130a166.5,130 0,1 0,333 0a166.5,130 0,1 0,-333 0z">
      <aapt:attr name="android:fillColor">
        <gradient 
            android:centerX="166.5"
            android:centerY="130"
            android:gradientRadius="130"
            android:type="radial">
          <item android:offset="0" android:color="#59000000"/>
          <item android:offset="0.07" android:color="#58000000"/>
          <item android:offset="0.13" android:color="#56000000"/>
          <item android:offset="0.2" android:color="#51000000"/>
          <item android:offset="0.27" android:color="#4C000000"/>
          <item android:offset="0.33" android:color="#44000000"/>
          <item android:offset="0.4" android:color="#3B000000"/>
          <item android:offset="0.47" android:color="#31000000"/>
          <item android:offset="0.53" android:color="#27000000"/>
          <item android:offset="0.6" android:color="#1D000000"/>
          <item android:offset="0.67" android:color="#14000000"/>
          <item android:offset="0.73" android:color="#0D000000"/>
          <item android:offset="0.8" android:color="#07000000"/>
          <item android:offset="0.87" android:color="#03000000"/>
          <item android:offset="0.93" android:color="#02000000"/>
          <item android:offset="1" android:color="#00000000"/>
        </gradient>
      </aapt:attr>
    </path>
  </vector>
  ```

## ShapeDrawable

* linear_gradient_shadow.xml

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <shape xmlns:android="http://schemas.android.com/apk/res/android"
      android:shape="oval">
      <gradient
          android:type="radial"
          android:centerX="0.5"
          android:centerY="0.5"
          android:endColor="#00000000"
          android:gradientRadius="150dp"
          android:startColor="#59000000" />
  </shape>
  ```

  

