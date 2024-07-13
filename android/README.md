# Android

## base_android_project

### 规定版本

```bash
# AS version ->  Chipmunk | 2021.2.1 Canary 1
# build date -> on October 13, 2021
```

### 设置`compileSdk`

* 规定为`33`，因为google pixel 4的api版本是`33`，方便调试源码

### 依赖`codelocator`

* 方便使用`CodeLocator`

### 基础布局

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout>
    <TextView />
</FrameLayout>
```

***

## compatibility

### compatibility layer

* what it does? - it implements the newer features using the APIs available on the older version. 

* advantages - This allows developers to use the latest features in their apps without having to worry about whether or not the app will work on older devices.

* features

  * The compatibility layer for Android is implemented by the AndroidX libraries themselves, rather than by Android OS or individual applications.
  * The compatibility code in the AndroidX libraries/Android support libraries often uses if statements and other conditional logic to provide different implementations of the same functionality depending on the version of Android that's running the app.

* example

  ```java
  public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
      if (Build.VERSION.SDK_INT >= 23) {
          activity.requestPermissions(permissions, requestCode);
      } else {
          // Code to handle permissions for older versions of Android
      }
  }
  ```

* history

  * The compatibility layer is implemented as a library, it is not a part of OS. 
  * The library that is used to implement the compatibility layer is called the **Android Support Library**. But now it's been replaced by **AndroidX Library**
  * before 2018, you need to add `implementation 'com.android.support:appcompat-v7:28.0.0'`
  * but now, you just need to add `implementation 'androidx.appcompat:appcompat:1.3.0'`

### what include

* AppCompat, Design, RecyclerView, CardView, PercentLayout

  ```xml
  androidx.appcompat:appcompat:1.3.0
  androidx.design:design:1.0.0
  androidx.recyclerview:recyclerview:1.2.0
  androidx.cardview:cardview:1.0.0
  androidx.percentlayout:percentlayout:1.0.0
  ```

***

## template code

### self define view

```kotlin
class SelfTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): AppCompatTextView(context, attrs, defStyle) {
  
}
```

### declare_styleable

1. define in xml

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <resources>
       <!--    自定义属性-->
       <declare-styleable name="CircleView">
           <attr name="circle_color" format="color"/>
       </declare-styleable>
   </resources>
   ```

2. use in runtime

   ```kotlin
   constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
     //自定义属性
     val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleView)
     val mColor = a.getColor(R.styleable.CircleView_circle_color, Color.RED)
     mPaint.color = mColor
     a.recycle()
   }
   ```

## 
