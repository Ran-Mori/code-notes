## 参考文档

* [Fresco架构设计赏析](https://juejin.cn/post/6844903784460582926)

## 重要构成

* `DraweeView`

  1. 官方解释 -> `View that displays a DraweeHierarchy.`

  2. 继承`ImageView`，但它的接口别用，未来会考虑直接继承`View`。唯一交集: 利用`ImageView`来显示`Drawable`

  3. 持有`DraweeHolder`对象

     ```java
     public class DraweeView extends ImageView {
       private DraweeHolder<DH> mDraweeHolder;
     }
     ```

* `DraweeHolder`

  * 官方解释 -> `A holder class for Drawee controller and hierarchy.`

  * 持有`DraweeController`和`DraweeHierarchy`

    ```java
    public class DraweeHolder {
      @Nullable private DH mHierarchy;
      private DraweeController mController = null;
    }
    ```

* `DraweeHierachy`

  * `Draweable`的容器，从`BACKGROUND -> OVERLAY`一共包含7层`Drawable`

    ```java
    public class GenericDraweeHierarchy {
      private static final int BACKGROUND_IMAGE_INDEX = 0;
      private static final int PLACEHOLDER_IMAGE_INDEX = 1;
      private static final int ACTUAL_IMAGE_INDEX = 2;
      private static final int PROGRESS_BAR_IMAGE_INDEX = 3;
      private static final int RETRY_IMAGE_INDEX = 4;
      private static final int FAILURE_IMAGE_INDEX = 5;
      private static final int OVERLAY_IMAGES_INDEX = 6;
    }
    ```

* `DraweeController` 

  * 控制图片的加载，请求，并根据不同事件控制`Hierarchy`

  * 持有`DraweeHierarchy`

    ```java
    public abstract class AbstractDraweeController {
      private SettableDraweeHierarchy mSettableDraweeHierarchy;
    }
    ```

* `ImagePipline` - 顾名思义

## 图片加载简要流程

1. `Controller`将请求任务委托给`DataSource`，在`DataSource`内注册一个请求结果的回调 -> `DataSubscriber`

2. `DataSource`通过经过一系列`Producer`委托责任链处理最终获得`result`，调用到`DataSubscriber`的方法

3. 将`result`传递给`Hierachy`

4. `DraweeView`将`Hierachy`的`topLevelDrawable`取出来展示

## 与加载相关的一些关键接口

1. `DataSource`

   ```java
   public interface DataSource<T> {
     //获取结果
     T getResult();
     //查询状态
     boolean isFinished();
     boolean hasFailed();
     //注册回调，类似于RxJava
     void subscribe(DataSubscriber<T> dataSubscriber, Executor executor);
   }
   ```

2. `DataSubscirber`

   ```java
   public interface DataSubscriber<T> {
     //成功回调
     void onNewResult(@Nonnull DataSource<T> dataSource);
     //失败回调
     void onFailure(@Nonnull DataSource<T> dataSource);
     //取消回调
     void onFailure(@Nonnull DataSource<T> dataSource);
   }
   ```

3. `Producer`

   ```java
   public interface Producer<T> {
     // 产生数据，并通知consumer消费
     void produceResults(Consumer<T> consumer, ProducerContext context);
   }
   ```

4. `Consumer`

   ```java
   public interface Consumer<T> {
     //实际上就是一个回调
     void onNewResult(@Nullable T newResult, @Status int status);
     void onFailure(Throwable t);
     void onCancellation();
   }
   ```

## 图片加载详细流程

1. 设置`controller`，订阅`dataSourceSubsciber`

   ```java
   //DraweeView#setController
   public void setController(draweeController) {
     //将设置controller委托给mDraweeHolder
     mDraweeHolder.setController(draweeController);
   }
   
   //DraweeHolder#setController
   public void setController(draweeController) {
     mController = draweeController;
     if (wasAttached) {
       //尝试进行attach
       attachController();
     }
   }
   private void attachController() {
     //调用controller的attach
     mController.onAttach();
   }
   
   //AbstractDraweeController
   public void onAttach() {
     if (!mIsRequestSubmitted) {
       submitRequest();
     }
   }
   protected void submitRequest() {
     final T closeableImage = getCachedImage();
     
     if (closeableImage != null) {
       //有缓存(代码只找了内存缓存)，根本不用请求，直接return
       return
     }
     
     //内存缓存没有就通过mDataSource进行请求
     DataSubscriber<T> dataSubscriber = new BaseDataSubscriber<T>() {
       public void onNewResultImpl(DataSource<T> dataSource) {
         //...
       }
       public void onFailureImpl(DataSource<T> dataSource) {
         //...
       }
       public void onProgressUpdate(DataSource<T> dataSource) {
         //...
       }
     }
     mDataSource.subscribe(dataSubscriber, mUiThreadImmediateExecutor);
   }
   ```

   * `controller`进行`attach`有两条路径
     1. 当进行赋值设置`controller`时会把`controller`给`attach`
     2. 当`DraweeView#onAttachedToWindow()`时也会尝试将当前已赋值的`controller`进行`attach`
     3. `detach`同理

2. `DataSource`获取、首个`Producer`获取并注入`DataSource`

   ```java
   //AbstractDraweeController
   protected void submitRequest() {
     //非常核心的一行，去获取dataSource
   	mDataSource = getDataSource();
   }
   
   //从AbstractDraweeController#getDataSource()开始调用，会调到ImagePipeline#fetchDecodedImage()
   //ImagePipeline
   public DataSource<CloseableReference<CloseableImage>> fetchDecodedImage() {
     //首个Producer获取，一般是BitmapMemoryCacheProducer
     Producer<CloseableReference<CloseableImage>> producerSequence =
             mProducerSequenceFactory.getDecodedImageProducerSequence(imageRequest);
     return submitFetchRequest(producerSequence, ...)
   }
   
   private <T> DataSource<CloseableReference<T>> submitFetchRequest() {
     //将首个Producer给传进去，new一个CloseableProducerToDataSourceAdapter
     return CloseableProducerToDataSourceAdapter.create(producerSequence, settableProducerContext);
   }
   ```

3. 首个`Producer`开始执行`produceResults`，并注册`Consumer`

   ```java
   //CloseableProducerToDataSourceAdapter
   private CloseableProducerToDataSourceAdapter() {
     //构造函数直接走到super，super是AbstractProducerToDataSourceAdapter
     super(producer, settableProducerContext, listener);
   }
   
   //AbstractProducerToDataSourceAdapter
   protected AbstractProducerToDataSourceAdapter() {
     //开始调用producer.produceResults了，并且注册了回调createConsumer()
     //这里的producer是BitmapMemoryCacheProducer
     producer.produceResults(createConsumer(), settableProducerContext);
   }
   
   private Consumer<T> createConsumer() {
     return new BaseConsumer<T>() {
       protected void onNewResultImpl(T newResult) {
         //注册的回调就是给dataSource的实现类result赋值，这样接口方法`T getResult();`才能返回
         AbstractProducerToDataSourceAdapter.this.onNewResultImpl(newResult);
       }
     }
   }
   ```

## 各种各种的`Producer`

1. `BitmapMemoryCacheProducer`

   * 尝试从内存缓存中找`Bitmap`

   * 包装`consumer`，将`result`存入`mMemoryCache`

     ```java
     //内存缓存容器
     private final MemoryCache<CacheKey, CloseableImage> mMemoryCache;
     //cacheKey容器
     private final CacheKeyFactory mCacheKeyFactory;
     //下一个Producer -> ThreadHandoffProducer
     private final Producer<CloseableReference<CloseableImage>> mInputProducer;
     
     public void produceResults(Consumer<CloseableReference<CloseableImage>> consumer, ProducerContext context) {
       //...代码太多了，讲一下代码的逻辑
       
       //看下isBitmapCacheEnabled，开启就从根据key从内存缓存map里找，cacheKey默认是图片url，找到的话就执行回调然后返回，如下图
       if(cachedReference != null) {
         consumer.onNewResult(cachedReference);
         return;
       }
       
       //没找到就将请求委托给mInputProducer
       mInputProducer.produceResults(wrappedConsumer, producerContext);
     }
     
     //包装consumer
     protected Consumer<CloseableReference<CloseableImage>> wrapConsumer() {
       return new DelegatingConsumer {
         public void onNewResultImpl() {
           //是否支持写入缓存
           if (isBitmapCacheEnabledForWrite) {
             //写入缓存
             newCachedResult = mMemoryCache.cache(cacheKey, newResult);
           }
           //写入缓存后才执行原来的consumer的onNewResult()
           getConsumer().onNewResult()
         }
       }
     }
     ```

2. `ThreadHandoffProducer` -> 不找，将任务委托到非UI线程

   ```java
   //下一个Producer -> BitmapMemoryCacheKeyMultiplexProducer(父类是MultiplexProducer)
   private final Producer<T> mInputProducer;
   //一个queue，里面有ThreadPoolExecutor
   private final ThreadHandoffProducerQueue mThreadHandoffProducerQueue;
   
   public void produceResults(Consumer<T> consumer, ProducerContext context) {
     // new了一个runnable出来，在这个runnable内会将图片请求委托给mInputProducer
   	Runnable<T> runnable = new StatefulProducerRunnable {
       protected void onSuccess(@Nullable T ignored) {
         mInputProducer.produceResults(consumer, context);
       }
     }
     //将runnable添加到异步线程池里面等待执行
     mThreadHandoffProducerQueue.addToQueueOrExecute(runnable);
   }
   ```

3. `MultiplexProducer` -> 不找，Producer for combining multiple identical requests into a single request.

   ```java
   //下一个Producer -> BitmapMemoryCacheProducer
   private final Producer<T> mInputProducer;
   
   public void produceResults(Consumer<T> consumer, ProducerContext context) {
     //组合的过程比较复杂，与请求过程关系不大，先跳过不看了
     mInputProducer.produceResults(forwardingConsumer, multiplexProducerContext);
   }
   ```

4. `BitmapMemoryCacheProducer` -> 又找了一次`Bitmap`内存缓存，简直离谱

   ```java
   //下一个Producer -> DecodeProducer
   private final Producer<CloseableReference<CloseableImage>> mInputProducer;
   
   public void produceResults(Consumer<CloseableReference<CloseableImage>> consumer, ProducerContext context) {
     //没找到就将请求委托给mInputProducer
     mInputProducer.produceResults(wrappedConsumer, producerContext);
   }
   ```

5. `DecodeProducer` -> 将解码任务封在`consumer`里往下传递

   ```java
   public class DecodeProducer {
     public void produceResults() {
       ProgressiveJpegParser jpegParser = new ProgressiveJpegParser(mByteArrayPool);
       progressiveDecoder =
               new NetworkImagesProgressiveDecoder(consumer,jpegParser);
       mInputProducer.produceResults(progressiveDecoder)
     }
   }
   ```

6. `ResizeAndRotateProducer` 

   * Resizes and rotates images according to the EXIF orientation data or a specified rotation angle. 

   * 包一层`consumer`进行`resize、rotate`

     ```java
     public class ResizeAndRotateProducer {
       public void produceResults(final Consumer<EncodedImage> consumer, final ProducerContext context) {
         mInputProducer.produceResults(
             new TransformingConsumer(consumer, context, mIsResizingEnabled, mImageTranscoderFactory),
             context);
       }
     }
     ```

7. `AddImageTransformMetaDataProducer` ->

   * Add image transform meta data producer. 

   * 包一层`consumer`

     ```java
     public class AddImageTransformMetaDataProducer {
       public void produceResults(Consumer<EncodedImage> consumer, ProducerContext context) {
         mInputProducer.produceResults(new AddImageTransformMetaDataConsumer(consumer), context);
       }
     }
     ```

8. `EncodedMemoryCacheProducer` 

   * 从内存缓存中找`encoded image`
   * 包一层`consumer`将结果写入`mMemoryCache`

9. `DiskCacheReadProducer` -> 从磁盘中找

10. `DiskCacheWriteProducer` -> 仅仅是包了个`consumer`用于存磁盘缓存

11. `NetworkFetchProducer`

## 网络数据转成图片

1. 把网络流转换为`EncodeImage`

   ```java
   public class NetworkFetchProducer {
     protected static void notifyConsumer(PooledByteBufferOutputStream pooledOutputStream) {
       encodedImage = new EncodedImage(result);
     }
   }
   ```

2. 决定图片的类型

   ```java
   public class DefaultImageFormatChecker {
     public final ImageFormat determineFormat(byte[] headerBytes, int headerSize) {
       if (isJpegHeader(headerBytes, headerSize)) {
         return DefaultImageFormats.JPEG;
       }
   
       if (isPngHeader(headerBytes, headerSize)) {
         return DefaultImageFormats.PNG;
       }
     }
   }
   ```

3. 将`EncodeImage`变为`Bitmap`

   ```java
   public abstract class DefaultDecoder {
     private CloseableReference<Bitmap> decodeFromStream() {
       //EncodedImage只是包含所有信息，没有被解码。可以从中获取流
       InputStream in = encodedImage.getInputStream();
       //调用系统的方法，将流变成Bitmap。系统方法会调用到native
       Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream, null, options);
     }
   }
   ```

4. 将`Bitmap`变成`CloseableStaticBitmap`

   ```java
   public class DefaultImageDecoder {
     public CloseableStaticBitmap decodeJpeg() {
       //先从`EncodeImage`变为`Bitmap`
       CloseableReference<Bitmap> bitmapReference = ...;
       //在从`Bitmap`变成`CloseableStaticBitmap`
        CloseableStaticBitmap closeableStaticBitmap = new CloseableStaticBitmap(bitmapReference);
     }
   }
   ```

5. 将`CloseableStaticBitmap`变成`BitmapDrawable`

   ```java
   public class DefaultDrawableFactory {
     public Drawable createDrawable(CloseableImage closeableImage) {
       Drawable bitmapDrawable =
               new BitmapDrawable(mResources, closeableStaticBitmap.getUnderlyingBitmap());
     }
   }
   ```

6. 不同的中间物意义

  7. `PooledByteBufferOutputStream` - 这是网络流

  8. `EncodeImage` 

     * it is implemented a lightweight wrapper around an encoded byte stream.
     * Encoded image data is a compressed representation of an image that has been prepared for storage or transmission. 
     * Encoded images are generally smaller in size than decoded images because they have been compressed to reduce their file size. 
     * it cannot be directly displayed on a screen without first being decoded.
     * it takes up very little memory compared to the decoded image data. This allows Fresco to load and cache multiple images at once without using excessive memory.

  9. `DecodeImage`

     * Decoded image data  is the uncompressed representation of an image that can be directly displayed on a screen.
     * Decoded images are generally larger in size than their encoded counterparts because they contain all of the image data necessary for display, such as pixel values and color information.
     * Decoding an encoded image involves unpacking the compressed data and reconstructing the original image.
     * The process of decoding an encoded image typically involves reading the encoded image data from disk or network, decompressing the data, and constructing a DecodedImage object that represents the resulting bitmap or other format suitable for display.

  10. `Bitmap`

      * DecodedImage is a higher-level abstraction used by Fresco to manipulate and manage the image data, while Bitmap is a lower-level representation of the actual pixel data that can be displayed on a screen.

## 不同level drawable显示原理

* `DraweeView`始终都只设置了一个`Drawable`

  ```java
  public void setHierarchy(DH hierarchy) {
    mDraweeHolder.setHierarchy(hierarchy);
    //设置drawable
    super.setImageDrawable(mDraweeHolder.getTopLevelDrawable());
  }
  
  public void setController(@Nullable DraweeController draweeController) {
    mDraweeHolder.setController(draweeController);
    //设置drawable
    super.setImageDrawable(mDraweeHolder.getTopLevelDrawable());
  }
  
  @Deprecated
  public void setImageDrawable(@Nullable Drawable drawable) {
    init(getContext());
    mDraweeHolder.setController(null);
    //过期的方式，别去用
    super.setImageDrawable(drawable);
  }
  ```

* `ImageView#Drawable` = `getTopLevelDrawable()` = `RootDrawable` = `FadeDrawable`

  ```java
  public class GenericDraweeHierarchy {
    private final RootDrawable mTopLevelDrawable;
    
    GenericDraweeHierarchy() {
      mTopLevelDrawable = new RootDrawable(mFadeDrawable); //将fadeDrawable给包了一层
    }
    
    public Drawable getTopLevelDrawable() {
      return mTopLevelDrawable; //对外暴露的方法
    }
  }
  ```

* `FadeDrawble`原理

  ```java
  //一个drawable容器
  private final Drawable[] mLayers;
  //动画时间
  int mDurationMs;
  //每个level(index)的透明度
  int[] mAlphas;
  //Determines whether to fade-out a layer to zero opacity (false) or to fade-in to the full opacity (true)
  boolean[] mIsLayerOn;
  //The index of the layer that contains the actual image 
  private final int mActualImageLayer;
  
  public FadeDrawable(Drawable[] layers, int actualImageLayer) {
    //赋一个值
    mLayers = layers;
    //初始化每一层的alpha
    mAlphas = new int[layers.length];
    //把真正显示的层给设置好
    mActualImageLayer = actualImageLayer;
  }
  
  //设置某一层的drawable可见
  public void fadeInLayer(int index) {
    mTransitionState = TRANSITION_STARTING; //设置drawing的state
    mIsLayerOn[index] = true; //将isOn设置成true
    invalidateSelf(); //请求redraw
  }
  
  //原理同上
  public void fadeOutLayer(int index) {
    mTransitionState = TRANSITION_STARTING;
    mIsLayerOn[index] = false;
    invalidateSelf();
  }
  
  //专门只显示某一层
  public void fadeToLayer(int index) {
    mTransitionState = TRANSITION_STARTING;
    Arrays.fill(mIsLayerOn, false);
    mIsLayerOn[index] = true;
    invalidateSelf();
  }
  
  //核心的draw
  public void draw(Canvas canvas) {
    switch (mTransitionState) {
      case TRANSITION_RUNNING:
        done = updateAlphas(ratio); //核心语句，draw时更新alpha
        //更新状态
        mTransitionState = done ? TRANSITION_NONE : TRANSITION_RUNNING;
        break;
    }
    for (int i = 0; i < mLayers.length; i++) {
      //上面alpha数组更新好后，遍历更新每一层drawable的alpha
      drawDrawableWithAlpha(canvas, mLayers[i], (int) Math.ceil(mAlphas[i] * mAlpha / 255.0));
    }
  }
  
  private boolean updateAlphas(float ratio) {
    for (int i = 0; i < mLayers.length; i++) {
      //更新数组内的alpha
      mAlphas[i] = (int) (mStartAlphas[i] + dir * 255 * ratio);
    }
  }
  ```

* 总结

  1. 只有一个`RootDrawable`，但这个`RootDrawable`包装了`FadeDrawable`，`FadeDrawable`是一个`Drawable`数组容器
  2. 显示不同的层是通过设置`alpha`来控制`draw`实现的

## PostProcessor

1. url -> 上屏粗流程

   1. fresco进行网络请求

   2. 将网络请求的`jpg`图片封装成位图`BitMap`
   3. `BitMap`封装成`BitMapDrawable`
   4. `BitMapDrawable`在屏幕上显示出来

2. 简介

   1. 是什么 -> fresco提供的一个API

   2. 作用 -> 在上述过程的 2~3之间提供一个hook，对`BitMap`做一些自定义的处理

3. `new`一个 `PostprocessorProducer`，并将`BaseProducer`传进去当作下一个`inputProducer`

   ```java
   //ImagePipeline
   public DataSource<CloseableReference<CloseableImage>> fetchDecodedImage() {
     //这个入口会去获取首个producer
     Producer<CloseableReference<CloseableImage>> producerSequence =
             mProducerSequenceFactory.getDecodedImageProducerSequence(imageRequest);
     return submitFetchRequest(...)
   }
   
   //ProducerSequenceFactory
   public Producer getDecodedImageProducerSequence(imageRequest) {
     //基础的Producer
     Producer<> pipelineSequence = getBasicDecodedImageSequence(imageRequest);
     //如果有Postprocessor，就将producer包一层，将pipelineSequence作为下一个Producer
     if (imageRequest.getPostprocessor() != null) {
       //开始包一层
       pipelineSequence = getPostprocessorSequence(pipelineSequence);
     }
     //返回包了一层的Producer
     return pipelineSequence;
   }
   private Producer getPostprocessorSequence() {
     //将刚才的Producer当成inputProducer传进去
     PostprocessorProducer postprocessorProducer = mProducerFactory.newPostprocessorProducer(inputProducer);
   }
   ```

4. `PostprocessorProducer`的处理实际上是把`Consumer`给包一层

   ```java
   //PostprocessorProducer
   public void produceResults() {
     //从ImageRequest中获取Processor
     Postprocessor postprocessor = context.getImageRequest().getPostprocessor();
     //将原来的Consumer用PostprocessorConsumer包一层
     PostprocessorConsumer postprocessorConsumer = new PostprocessorConsumer(consumer, listener, postprocessor, context);
     //啥都不干，直接让mInputProducer去produce图片。把包好的consumer给传进去
     mInputProducer.produceResults(postprocessorConsumer, context);
   }
   ```

5. `PostprocessorConsumer`处理

   1. `DelegatingConsumer`

      * 以下代码示例如何将`Consumer`给包一层

      * 泛型表示这个`DelegateConsumer`的`输入`和`输出`

        ```java
        public abstract class DelegatingConsumer<I, O> extends BaseConsumer<I> {
          private final Consumer<O> mConsumer;
          public DelegatingConsumer(Consumer<O> consumer) {
            mConsumer = consumer;
          }
          public Consumer<O> getConsumer() {
            return mConsumer;
          }
          
          protected void onFailureImpl(Throwable t) { mConsumer.onFailure(t);}
        }
        ```

   2. `PostprocessorConsumer` -> 将内层`Consumer`给拦截住，处理结束后在通知内层`Consumer`

      ```java
      //PostprocessorConsumer
      private final Postprocessor mPostprocessor;
      //涉及异步线程操作了
      private final Executor mExecutor;
      
      //缓存、网络等producer返回结果了
      protected void onNewResultImpl(CloseableReference<CloseableImage> newResult, @Status int status) {
        //开始准备Postprocessing
        submitPostprocessing();
      }
      
      private void submitPostprocessing() {
        mExecutor.execute(
          new Runnable() {
            @Override
            public void run() {
              //在异步线程中执行Postprocessing
              doPostprocessing(closeableImageRef, status);
            }
          }
      }
      
      private void doPostprocessing(CloseableReference<CloseableImage> sourceImageRef) {
        //核心处理的三行代码
        CloseableStaticBitmap staticBitmap = (CloseableStaticBitmap) sourceImage;
        Bitmap sourceBitmap = staticBitmap.getUnderlyingBitmap();
        CloseableReference<Bitmap> bitmapRef = mPostprocessor.process(sourceBitmap, mBitmapFactory);
        //处理后的Bitmap
        destImageRef = new CloseableStaticBitmap(bitmapRef);
        //获取内容的consumer，然后将新图片通知给内层consumer
        getConsumer().onNewResult(destImageRef, status);
      }
      ```