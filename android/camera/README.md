# camera

## reference

* [Android Camera 编程从入门到精通](https://www.jianshu.com/p/f63f296a920b)
* [Android相机开发——CameraView源码解析](https://www.jianshu.com/p/0ac7234dcefc)

## FutureChain

* run order

  ```java
  ListenableFuture<Boolean> adminIsLoggedIn = 
    FutureChain.from(usersDatabase.getAdminUser()) // first run 
    .transform(User::getId, directExecutor()) // second run
    .transform(ActivityService::isLoggedIn, threadPool); // third run
  ```

## initialization

1. `CameraX` init

   ```java
   // androidx.camera.core.CameraX
   public final class CameraX {
     // a repository for storing cameras
     CameraRepository mCameraRepository = new CameraRepository();
     private CameraFactory mCameraFactory;
     private CameraDeviceSurfaceManager mSurfaceManager;
     
     // do init acition
     // it runs on a new Thread
     private void initAndRetryRecursively() {
    		CameraFactory.Provider cameraFactoryProvider = mCameraXConfig.getCameraFactoryProvider(null);
       mCameraFactory = cameraFactoryProvider.newInstance(mAppContext, cameraThreadConfig, availableCamerasLimiter);
       mSurfaceManager = surfaceManagerProvider.newInstance(mAppContext, mCameraFactory.getCameraManager(), mCameraFactory.getAvailableCameraIds());
       mCameraRepository.init(mCameraFactory);
     }
   }
   ```

2. create `Camera2CameraImpl`

   ```java
   // androidx.camera.core.impl.CameraRepository
   class CameraRepository {
     private final Map<String, CameraInternal> mCameras = new LinkedHashMap<>();
     
     public void init(@NonNull CameraFactory cameraFactory) {
       Set<String> camerasList = cameraFactory.getAvailableCameraIds();
       for (String id : camerasList) {
         // call cameraFactory.getCamera(id)
         mCameras.put(id, cameraFactory.getCamera(id));
       }
     }
   }
   
   // androidx.camera.camera2.internal.Camera2CameraImpl
   class Camera2CameraImpl {
     Camera2CameraImpl() throws CameraUnavailableException {
       mCameraManager = cameraManager;
       mCameraStateRegistry = cameraStateRegistry;
       mDisplayInfoManager = displayInfoManager;
       mCaptureSession = newCaptureSession();
     }
   }
   ```

3. create `androidx.camera.core.CameraX`

   ```java
   // androidx.camera.lifecycle.ProcessCameraProvider#getOrCreateCameraXInstance
   private ListenableFuture<CameraX> getOrCreateCameraXInstance(@NonNull Context context) {
     // create an instance of CameraX
     CameraX cameraX = new CameraX(context, mCameraXConfigProvider);
     mCameraXInitializeFuture = CallbackToFutureAdapter.getFuture(completer -> {
       // mCameraXShutdownFuture then cameraX.getInitializeFuture(), and it forms a chain.
       ListenableFuture<Void> future =
         FutureChain.from(mCameraXShutdownFuture).transformAsync(
                 input -> cameraX.getInitializeFuture(),
                 CameraXExecutors.directExecutor());
   
       Futures.addCallback(future, new FutureCallback<Void>() {
         @Override
         public void onSuccess(@Nullable Void result) { completer.set(cameraX); }
       }, CameraXExecutors.directExecutor());
   
       return "ProcessCameraProvider-initializeCameraX";
     });
     return mCameraXInitializeFuture;
   }
   ```

4. finish create `ProcessCameraProvider`

   ```java
   public static ListenableFuture<ProcessCameraProvider> getInstance(@NonNull Context context) {
     return Futures.transform(sAppInstance.getOrCreateCameraXInstance(context),
       cameraX -> {
           sAppInstance.setCameraX(cameraX);
           sAppInstance.setContext(ContextUtil.getApplicationContext(context));
           return sAppInstance;
       }, CameraXExecutors.directExecutor());
   }
   ```

## camera2 api

* what is?

  * the Camera2 API in Android is a more advanced and flexible framework introduced in Android 5.0 (Lollipop) to replace the original Camera API (Camera1).

* features

  1. Supports advanced features like RAW capture, burst mode, slow-motion video, and more.
  2. Allows setting and querying detailed parameters for each capture, such as ISO, exposure time, focus distance, and more.
  3. Supports capturing multiple streams concurrently, such as preview, image capture, and video recording.
  4. Allows using multiple outputs simultaneously, such as a surface for preview and another surface for recording.
  5. Allows access to multiple cameras simultaneously (e.g., dual-camera setups).

* camera1 api example

  ```java
  Camera camera = Camera.open();
  Camera.Parameters params = camera.getParameters();
  params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
  camera.setParameters(params);
  camera.startPreview();
  
  camera.takePicture(null, null, new Camera.PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, Camera camera) {
          // Save the picture data
      }
  });
  ```

* camera2 api example

  ```java
  CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
  String cameraId = manager.getCameraIdList()[0];
  CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
  StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
  
  manager.openCamera(cameraId, new CameraDevice.StateCallback() {
      @Override
      public void onOpened(@NonNull CameraDevice camera) {
          SurfaceTexture texture = textureView.getSurfaceTexture();
          texture.setDefaultBufferSize(map.getOutputSizes(SurfaceTexture.class)[0].getWidth(),
                                       map.getOutputSizes(SurfaceTexture.class)[0].getHeight());
          Surface surface = new Surface(texture);
  
          try {
              CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
              builder.addTarget(surface);
              camera.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                  @Override
                  public void onConfigured(@NonNull CameraCaptureSession session) {
                      session.setRepeatingRequest(builder.build(), null, null);
                  }
  
                  @Override
                  public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                  }
              }, null);
          } catch (CameraAccessException e) {
              e.printStackTrace();
          }
      }
  
      @Override
      public void onDisconnected(@NonNull CameraDevice camera) {
          camera.close();
      }
  
      @Override
      public void onError(@NonNull CameraDevice camera, int error) {
          camera.close();
      }
  }, null);
  ```


## how preview

1. query all available camera hardware devices

   ```java
   // android.hardware.camera2.CameraManager.CameraManagerGlobal#connectCameraServiceLocked
   private void connectCameraServiceLocked() {
     // use binder ipc to get cameraServiceBinder
     IBinder cameraServiceBinder = ServiceManager.getService(CAMERA_SERVICE_BINDER_NAME);
     // call addListener to query camera status of current device
     CameraStatus[] cameraStatuses = cameraService.addListener(this);
   }
   
   // frameworks/av/services/camera/libcameraservice/CameraService.cpp
   Status CameraService::addListener(const sp<ICameraServiceListener>& listener, std::vector<hardware::CameraStatus> *cameraStatuses) {
       return addListenerHelper(listener, cameraStatuses);
   }
   
   // frameworks/base/core/java/android/hardware/CameraStatus.java
   public class CameraStatus implements Parcelable {
       public String cameraId; // specify different camera devices
       public int status;
       public String[] unavailablePhysicalCameras;
       public String clientPackage;
   }
   ```

2. open camera

   ```java
   // android.hardware.camera2.CameraManager#openCameraDeviceUserAsync
   private CameraDevice openCameraDeviceUserAsync(String cameraId, // open which camera
   	CameraDevice.StateCallback callback // open success or fail callback
   ) { 
     // wrap to a impl
   	android.hardware.camera2.impl.CameraDeviceImpl deviceImpl = new android.hardware.camera2.impl.CameraDeviceImpl(cameraId, callback);
     // wrap callback
     ICameraDeviceCallbacks callbacks = deviceImpl.getCallbacks();
     
     // use binder to connect camera
     ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
     cameraService.connectDevice(callbacks, cameraId,mContext.getOpPackageName());
   }
   
   // frameworks/av/services/camera/libcameraservice/CameraService.cpp
   Status CameraService::connectDevice(
           const sp<hardware::camera2::ICameraDeviceCallbacks>& cameraCb,
           const std::string& unresolvedCameraId,
           const std::string& clientPackageName) {
     // delegate open action to connectHelper
     ret = connectHelper<hardware::camera2::ICameraDeviceCallbacks,CameraDeviceClient>(cameraCb,
               cameraId, /*api1CameraId*/-1, clientPackageNameAdj);
     return ret;
   }
   ```

3. receive OpenCameraSuccess callback

   ```java
   // androidx.camera.camera2.internal.Camera2CameraImpl.StateCallback#onOpened
   final class StateCallback extends CameraDevice.StateCallback {
     public void onOpened(@NonNull CameraDevice cameraDevice) {
       switch (mState) {
         case OPENING:
         case REOPENING:
             setState(InternalState.OPENED);
             openCaptureSession(); // ready to call startPreview
             break;
       }
     }
   }
   ```

4. start preview

   ```java
   // androidx.camera.camera2.internal.CaptureSession#open
   public ListenableFuture<Void> open(@NonNull SessionConfig sessionConfig,
               @NonNull CameraDevice cameraDevice,
               @NonNull SynchronizedCaptureSessionOpener opener) {
     switch (mState) {
       case INITIALIZED:
         // set state to GET_SURFACE
         mState = State.GET_SURFACE; 
         // get surface future
         List<DeferrableSurface> surfaces = sessionConfig.getSurfaces();
         mConfiguredDeferrableSurfaces = new ArrayList<>(surfaces);
         mSynchronizedCaptureSessionOpener = opener;
         ListenableFuture<Void> openFuture = FutureChain.from(
     										// real get surface synchronously
                         mSynchronizedCaptureSessionOpener.startWithDeferrableSurface(
                                 mConfiguredDeferrableSurfaces,
                                 TIMEOUT_GET_SURFACE_IN_MS))
                 .transformAsync(
           							// after get surface, then openCaptureSession
                         surfaceList -> openCaptureSession(surfaceList, sessionConfig,
                                 cameraDevice),
                         mSynchronizedCaptureSessionOpener.getExecutor());
     }
   }
   
   // android.hardware.camera2.impl.CameraDeviceImpl#createCaptureSession
   public void createCaptureSession(SessionConfiguration config)
               throws CameraAccessException {
     // config holds surfaces and callback
     createCaptureSessionInternal(config.getInputConfiguration(), outputConfigs,
               config.getStateCallback(), config.getExecutor(), config.getSessionType(),
               config.getSessionParameters());
   }
   
   // android.hardware.camera2.impl.ICameraDeviceUserWrapper#submitRequestList
   public SubmitInfo submitRequestList(CaptureRequest[] requestList, boolean streaming)
     // mRemoteDevice is ICameraDeviceUser$Stub$Proxy
     // it's ipc
     return mRemoteDevice.submitRequestList(requestList, streaming);
   }
   ```

5. receive preview success callback

   ```c++
   // frameworks/av/camera/aidl/android/hardware/camera2/ICameraDeviceCallbacks.aidl
   // ipc is received by android.hardware.camera2.impl.CameraDeviceImpl 
   interface ICameraDeviceCallbacks
   {
       oneway void onDeviceError(int errorCode, in CaptureResultExtras resultExtras);
       oneway void onDeviceIdle();
       oneway void onCaptureStarted(in CaptureResultExtras resultExtras, long timestamp);
       oneway void onResultReceived(in CameraMetadataNative result,
                                    in CaptureResultExtras resultExtras,
                                    in PhysicalCaptureResultInfo[] physicalCaptureResultInfos);
       oneway void onPrepared(int streamId);
       oneway void onRepeatingRequestError(in long lastFrameNumber,
                                           in int repeatingRequestId);
       oneway void onRequestQueueEmpty();
   }
   ```

   