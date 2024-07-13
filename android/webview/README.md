# web_view

## WebView

* 如何使用 -> `MainActivity#setContentView(WebView(this))`
* 接口
  * `public void addJavascriptInterface(Object obj, String interfaceName) {}`
  * `public void loadUrl(String url)`

## jsb

* 写一个jsb

  ```kotlin
  class WebInterface(private val mContext: Context) {
  
      @JavascriptInterface
      fun showToast(toast: String) {
          Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
      }
  }
  ```

* 注入WebView

  * `WebView#addJavascriptInterface(interface)`

* html中使用

  ```html
  <script type="text/javascript">
    <!--JNI方法处-->
    function showAndroidToast(toast) {
      WebInterface.showToast(toast);
    }
  </script>
  ```

## Assets

* what's for? -> to store raw asset files that will be used by your app, such as fonts, HTML files, JavaScript files, styling files, database files, config files, sound files, and graphic files.

* how to use? -> use apis of `AssetManager.java`

  ```java
  AssetManager assetManager = getAssets();
  InputStream inputStream = assetManager.open("file.txt");
  BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
  StringBuilder builder = new StringBuilder();
  String line;
  while ((line = reader.readLine()) != null) {
      builder.append(line);
  }
  String content = builder.toString();
  inputStream.close();
  ```

* feature

  * The `assets` directory is a directory for arbitrary files that are not compiled. Asset files that are saved in the assets folder are included as-is in the APK file, without any modification, while the files saved in the `res` directory are processed and compiled into optimized binary formats at build time.

* 位置 -> 在`main`下，和`java`同级

* 注意事项 -> `file:///android_asset/filename.txt`的方式只能访问`assets`下面的资源

***

## 