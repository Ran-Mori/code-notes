## reference

* [Android Activity共享元素动画分析](https://juejin.cn/post/7144621475503276045#heading-4)

## view transfer

* ActivityA generate an ActivityOptions

  ```java
  public class ActivityOptions {
    static ExitTransitionCoordinator makeSceneTransitionAnimation() {
      ExitTransitionCoordinator exit = new ExitTransitionCoordinator();
      opts.mTransitionReceiver = exit; // pass ExitTransitionCoordinator to ActivityB
      opts.mSharedElementNames = names; // pass shareElements names to ActivityB
      opts.mIsReturning = false; // pass not return to ActivityB
      return exit;
    }
  }
  ```

* ActivityA start ActivityB with a bundle

  ```java
  val bundle = ActivityOptions.makeSceneTransitionAnimation(this, iv,"shareElement").toBundle()
  val intent = Intent(this, ActivityB::class.java)
  startActivity(intent, bundle) // start with a bundle
  ```

* ActivityB receive the bundle

  ````java
  public class LaunchActivityItem {
    private LaunchActivityItem(Parcel in) {
      // from bundle to new an ActivityOptions
      ActivityOptions.fromBundle(in.readBundle())
    }
  }
  ````

* use the bundle to generate an ActivityOptions

  ```java
  public class ActivityOptions {
    public static ActivityOptions fromBundle(Bundle bOptions) {
      return bOptions != null ? new ActivityOptions(bOptions) : null;
    }
  }
  ```


***

## 