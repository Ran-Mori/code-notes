## reference

* [google - Shrink, obfuscate, and optimize your app](https://developer.android.com/build/shrink-code)
* [Understanding ProGuard](https://medium.com/@dugguRK/understanding-proguard-a23bbac14863)

## features

* four functions

  1. Code shrinking - detects and safely removes unused classes, fields, methods, and attributes from your app and its library dependencies.
  2. Resource shrinking - removes unused resources from your packaged app, including unused resources in your app’s library dependencies. 
  3. Obfuscation - shortens the name of classes and members, which results in reduced DEX file sizes.
  4. Optimization - inspects and rewrites your code to further reduce the size of your app’s DEX files. For example, if R8 detects that the `else {}` branch for a given if/else statement is never taken, R8 removes the code for the `else {}` branch.

* R8 - it is the default compiler that converts your project’s Java bytecode into the DEX format that runs on the Android platform.

* how to use

  ```groovy
  android {
      buildTypes {
          release {
              minifyEnabled true // Enables code shrinking, obfuscation, and optimization for only your project's release build type.
              shrinkResources true // Enables resource shrinking
              proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),'proguard-rules.pro'
          }
      }
      ...
  }
  ```

## shrink your code

* how to do it? - R8 inspects your app’s code to build a graph of all methods, member variables, and other classes that your app might access at runtime. Code that is not connected to that graph is considered *unreachable* and may be removed from the app.

* config

  * add a `-keep` line in the ProGuard rules file.

    ```bash
    -keep public class MyClass
    ```

  * use `@Keep` annotation

## mapping.txt

* locatioin - `app/build/outputs/mapping/realease/mapping.txt`

* content

  ```bash
  com.proguard.MainActivity -> com.proguard.MainActivity:
      int $r8$clinit -> o
      kotlin.Lazy textView$delegate -> n
      1:1:kotlin.Lazy kotlin.LazyKt__LazyJVMKt.lazy(kotlin.jvm.functions.Function0):0:0 -> <init>
      1:1:void <init>():0 -> <init>
      2:2:void <init>():0:0 -> <init>
      1:1:android.widget.TextView getTextView():0:0 -> onCreate
      1:1:void onCreate(android.os.Bundle):0 -> onCreate
      2:2:void onCreate(android.os.Bundle):0:0 -> onCreate
  ```
