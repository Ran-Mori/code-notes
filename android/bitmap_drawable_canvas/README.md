## dp -> pix

```kotlin
fun dp2px(context: Context, dp: Int): Float =
	TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics)
```

## Bitmap

* 参考博客 -> [Android Bitmap详解](https://www.jianshu.com/p/28c249278c49)

* 本质 -> 是一个位图，很多核心的方法全是`native`的，可以简单的把它理解成像素矩阵

* 创建 -> 一般都是直接或者间接通过`InputStream`或者`Byte[]`来进行创建

  ```java
  public class BitmapFactory {
    public static Bitmap decodeFile(String pathName);
    public static Bitmap decodeResource(Resources res, int id);
    public static Bitmap decodeByteArray(byte[] data, int offset, int length);
    public static Bitmap decodeStream(InputStream is);
  }
  ```

* 重要方法

  1. `recycle()` -> free native object, clear reference to the pixel data.


## Drawable

* 参考博客链接 -> [Android Drawable 详解](https://www.jianshu.com/p/d6c791709949)

* 本质：`something that can be drawn.`表示可以被绘制在`canvas`上的东西

* 重要方法

  1. `public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme)` -> 每一个子类`Drawable`都应该去实现，规定了从`xml`如何创建一个对应的`Drawable`

     ```java
     public final class DrawableInflater {
       Drawable inflateFromXmlForDensity() {
         //通过xml tag的名字确定创建那个drawable
         Drawable drawable = inflateFromTag(name);
         //调用这个drawable的inflate方法
         drawable.inflate(mRes, parser, attrs, theme);
         return drawable;
       }
       
       private Drawable inflateFromTag(String name) {
         //根据tag名称创建对应的drawable
         switch (name) {
                 case "selector":
                     return new StateListDrawable();
                 case "level-list":
                     return new LevelListDrawable();
                 case "layer-list":
                     return new LayerDrawable();
         }
       }
     }
     ```

  2. `public void setBounds(int left, int top, int right, int bottom)` -> 当在`canvas`进行`draw`时，规定好位置和区域。即决定了此`Drawable`被绘制在`canvas`的那个位置以及绘制多大。注意它不是决定`drawable`那部分被`draw`，而是决定`canvas`那部分来`draw`整个`drawable`

  3. `public abstract void draw(@NonNull Canvas canvas)` -> 如何把这个`drawable`绘制到`convas`上，这依赖每个`Drawable`去自己实现

* 创建

  ```java
  public abstract class Drawable {
    public static Drawable createFromStream(InputStream is);
    public static Drawable createFromResourceStream(Resources res);
    public static Drawable createFromXml(Resources r);
    public static Drawable createFromPath(String pathName);
  }
  ```

## Canvas

* 是什么 -> 提供了`draw`的方法，即暴露了`draw`的能力

* `draw something`的四个必备要素

  1. `A Bitmap to hold the pixels `
  2. `a Canvas to host the draw call`
  3. `a drawing primitive (e.g. Rect, Path, text, Bitmap)`
  4. `a paint`

* 四个要素示例

  ```kotlin
  // Drawable -> Bitmap
  val drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.takagi))
  drawable.setBounds(200, 200, 1000 ,1000)
  val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap) //要素1 -> bitmap，要素2 -> canvas，要素3 -> drawable，要素4 -> 默认
  drawable.draw(canvas)
  imageView?.setImageDrawable(BitmapDrawable(resources, bitmap))
  ```

* draw Circle example 

  ```kotlin
  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    val point = dp2px(context, 100)
    val radius = dp2px(context, 50)
    val paint = Paint().apply {
      color = resources.getColor(R.color.green, null)
      isAntiAlias = true
    }
    canvas?.drawCircle(point, point, radius, paint)
  }
  ```

## mutual conversion

1. `Bitmap` -> `Drawable`

   ```kotlin
   val bitmap = BitmapFactory.decodeResource(resources, R.drawable.takagi)
   val bitmapDrawable = BitmapDrawable(resources, bitmap)
   ```

2. `Drwable` -> `Bitmap`

   ```kotlin
   val drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.takagi))
               drawable.setBounds(200, 200, 500 ,500)
   val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
   val canvas = Canvas(bitmap)
   drawable.draw(canvas)
   ```

3. View -> Bitmap

   ```kotlin
   val bitmap = Bitmap.createBitmap(textView?.width ?: 100, textView?.height ?: 100, Bitmap.Config.ARGB_8888)
   val canvas = Canvas(bitmap)
   textView?.draw(canvas)
   ```
