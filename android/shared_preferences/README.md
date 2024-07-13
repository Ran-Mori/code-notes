## 实现原理

1. `ContextImpl.java` 创建文件，将文件转换成`SharedPreferencesImpl.java`

   ```java
   public SharedPreferences getSharedPreferences(String name, int mode) {
     File file;
     synchronized (ContextImpl.class) { // 防多线程，上锁
       if (mSharedPrefsPaths == null) {
         mSharedPrefsPaths = new ArrayMap<>(); // 内存友好二分查找的ArrayMap
       }
       file = mSharedPrefsPaths.get(name);
       if (file == null) { // 典型的加缓存思路
         file = makeFilename(getPreferencesDir(), name + ".xml"); // 核心的新建逻辑
         mSharedPrefsPaths.put(name, file);
       }
     }
     return getSharedPreferences(file, mode); // 真正的获取
   }
   
   private File getPreferencesDir() { // 获取此应用SP的路径
       synchronized (mSync) {
         if (mPreferencesDir == null) {
           mPreferencesDir = new File(getDataDir(), "shared_prefs");
         }
         return ensurePrivateDirExists(f(mPreferencesDir, 0771, -1, null); // 读取文件
       }
   }
   
   public File getDataDir() { // dataPath是与包名相关联的路径
     if (isCredentialProtectedStorage()) {
       res = mPackageInfo.getCredentialProtectedDataDirFile();
     } else if (isDeviceProtectedStorage()) {
       res = mPackageInfo.getDeviceProtectedDataDirFile();
     } else {
       res = mPackageInfo.getDataDirFile();
     }
   }
                                       
   static File ensurePrivateDirExists(File file, int mode, int gid, String xattr) {
     try {
       Os.mkdir(path, mode); // 系统调用创文件
       Os.chmod(path, mode); // 系统调用改mode为0771
       if (gid != -1) {
           Os.chown(path, -1, gid);
       }
     }
   }
   
   public SharedPreferences getSharedPreferences(File file, int mode) {
     SharedPreferencesImpl sp;
     synchronized (ContextImpl.class) {
       sp = new SharedPreferencesImpl(file, mode); // 真正的实现类是SharedPreferencesImpl
     }
   }
   ```

2. `SharedPreferencesImpl.java`

   ```java
   SharedPreferencesImpl(File file, int mode) {
     synchronized (mLock) { mLoaded = false; }
     new Thread("SharedPreferencesImpl-load") {
       public void run() { loadFromDisk(); } // 直接新开一个线程去读xml文件到内存
     }.start();
   }
   ```

## features

* 理论上sp实现原理是读写文件，可以用于IPC。但实际上最好别用，因为系统对它的读写有一定的缓存的策略，在内存中存在一个副本，所以多进程它是读写不可靠的。
