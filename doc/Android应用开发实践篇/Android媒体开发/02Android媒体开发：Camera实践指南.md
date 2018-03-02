# Android媒体开发：Camera实践指南

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

- 一 Camera实践指南
    - 1.1 打开相机
    - 1.2 关闭相机
    - 1.3 开启预览
    - 1.4 关闭预览
    - 1.5 拍照
    - 1.6 开始视频录制
    - 1.7 结束视频录制
- 二 Camera2实践指南
    - 2.1 打开相机
    - 2.2 关闭相机
    - 2.3 开启预览
    - 2.4 关闭预览
    - 2.5 拍照
    - 2.6 开始视频录制
    - 2.7 结束视频录制

Android Camera 相关API也是Android生态碎片化最为严重的一块，首先Android本身就有两套API，Android 5.0以下的Camera和Android 5.0以上的Camera2，而且
更为严重的时，各家手机厂商都Camera2的支持程度也各不相同，这就导致我们在相机开发中要花费很大精力来处理兼容性问题。

相机开发的一般流程是什么样的？🤔

1. 检测并访问相机资源 检查手机是否存在相机资源，如果存在则请求访问相机资源。
2. 创建预览界面，创建继承自SurfaceView并实现SurfaceHolder接口的拍摄预览类。有了拍摄预览类，即可创建一个布局文件，将预览画面与设计好的用户界面控件融合在一起，实时显示相机的预览图像。
3. 设置拍照监听器，给用户界面控件绑定监听器，使其能响应用户操作, 开始拍照过程。
4. 拍照并保存文件，将拍摄获得的图像转换成位图文件，最终输出保存成各种常用格式的图片。
5. 释放相机资源，相机是一个共享资源，当相机使用完毕后，必须正确地将其释放，以免其它程序访问使用时发生冲突。

相机开发一般需要注意哪些问题？🤔

1. 版本兼容性问题，Android 5.0以下的Camera和Android 5.0以上使用Camera2，Android 4.0以下的SurfaceView和Android 4.0以上的TextureView，Android 6.0以上要做相机等运行时权限兼容。
2. 设备兼容性问题，Camera/Camera2里的各种特性在有些手机厂商的设备实现方式和支持程度是不一样的，这个需要做兼容性测试，一点点踩坑。
3. 各种场景下的生命周期变化问题，最常见的是后台场景和锁屏场景，这两种场景下的相机资源的申请与释放，Surface的创建与销毁会带来一些问题，这个我们
后面会仔细分析。

关于Camera/Camear2

既然要解决这种兼容性问题，就要两套并用，那是不是根据版本来选择：Android 5.0 以下用Camera，Android 5.0以上用Camera2呢？🤔

事实上，这样是不可取的。前面说过不同手机厂商对Camera2的支持程度各不相同，即便是Android 5.0 以上的手机，也存在对Camera2支持非常差的情况，这个时候就要降级使用Camera，如何判断对Camera的支持
程度我们下面会说。

关于SurfaceView/TextureView

- SurfaceView是一个有自己Surface的View。界面渲染可以放在单独线程而不是主线程中。它更像是一个Window，自身不能做变形和动画。
- TextureView同样也有自己的Surface。但是它只能在拥有硬件加速层层的Window中绘制，它更像是一个普通View，可以做变形和动画。

更多关于SurfaceView与TextureView区别的内容可以参考这篇文章[Android 5.0(Lollipop)中的SurfaceTexture，TextureView, SurfaceView和GLSurfaceView](http://blog.csdn.net/jinzhuojun/article/details/44062175).

那么如何针对版本进行方案的选择呢？🤔

官方的开源库[cameraview](https://github.com/google/cameraview)给出了方案：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/media/cameraview_overview.png" width="400"/>

既然要两套并用，就要定义统一的接口，针对不同场景提供不同的实现，使用的时候也是根据不同的场景来创建不同的实例。

我们不难发现，这个接口一般需要定义以下功能：

- 打开相机
- 关闭相机
- 开启预览
- 关闭预览
- 拍照
- 开始视频录制
- 结束视频录制

定义好了接口，我们就有了思路，针对相机的具体特性实现相应的方案，那么另一个问题就出来了，相机在日常开发中一般作为一个SDK的形式存在供各个业务方调用，那么如何设计
出一个功能与UI相分离，高度可定制的相机SDK呢？🤔

答案就是利用Fragment，将各种点击事件（点击拍照、点击切换摄像头、点击切换闪光模式等）对应的功能封装在Fragment里，业务方在用的时候可以在Fragment之上蒙一层
UI（当然我们也需要提供默认的实现），这样就可以让功能和UI相分离，集成起来也非常的简便。

相机SDK框架图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/media/camera_sdk_structure.png" width="400"/>

- CameraActivity：相机界面，主要用来实现UI的定制，实际功能（点击事件）交由CameraFragment完成。
- CameraFragment：向CameraActivity提供功能接口，完成CameraActivity里的点击事件，例如：拍照、录像等。
- CameraLifecycle：处理相机随着Activity生命周期变化的情况，内部持有CameraManager，处理相机初始化和释放，预览的创建与销毁等问题。
- CameraManager：相机的实际管理者，调用相机API来操作相机，进行拍照和录像等操作。
- Camera/Camera2：相机API。

[phoenix](https://github.com/guoxiaoxing/phoenix)项目最新版本[![Download](https://api.bintray.com/packages/guoxiaoxing/maven/phoenix/allList/download.svg)](https://bintray.com/guoxiaoxing/maven/phoenix/_latestVersion)已经实现了这套方案，效果图如下所示：

<p align="center">
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/media/function_4.png" width="400"/>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/media/play_3.gif" width="400"/>
</p>

理解了整体的架构，我们接着就来分析针对这套架构，Camera/Camera2分别该如何实现。

## 一 Camera实践指南

Camera API中主要涉及以下几个关键类：

- Camera：操作和管理相机资源，支持相机资源切换，设置预览和拍摄尺寸，设置光圈、曝光等相关参数。
- SurfaceView：用于绘制相机预览图像，提供实时预览的图像。
- SurfaceHolder：用于控制Surface的一个抽象接口，它可以控制Surface的尺寸、格式与像素等，并可以监视Surface的变化。
- SurfaceHolder.Callback：用于监听Surface状态变化的接口。

SurfaceView和普通的View相比有什么区别呢？🤔

>普通View都是共享一个Surface的，所有的绘制也都在UI线程中进行，因为UI线程还要处理其他逻辑，因此对View的更新速度和绘制帧率无法保证。这显然不适合相机实时
预览这种情况，因而SurfaceView持有一个单独的Surface，它负责管理这个Surface的格式、尺寸以及显示位置，它的Surface绘制也在单独的线程中进行，因而拥有更高
的绘制效率和帧率。

SurfaceHolder.Callback接口里定义了三个函数：

- surfaceCreated(SurfaceHolder holder); 当Surface第一次创建的时候调用，可以在这个方法里调用camera.open()、camera.setPreviewDisplay()来实现打开相机以及连接Camera与Surface
等操作。
- surfaceChanged(SurfaceHolder holder, int format, int width, int height); 当Surface的size、format等发生变化的时候调用，可以在这个方法里调用camera.startPreview()开启预览。
- surfaceDestroyed(SurfaceHolder holder); 当Surface被销毁的时候调用，可以在这个方法里调用camera.stopPreview()，camera.release()等方法来实现结束预览以及释放

### 1.1 打开相机

打开相机之前我们需要先获取系统相机的相关信息。

```java
//有多少个摄像头
numberOfCameras = Camera.getNumberOfCameras();

for (int i = 0; i < numberOfCameras; ++i) {
    final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

    Camera.getCameraInfo(i, cameraInfo);
    //后置摄像头
    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
        faceBackCameraId = i;
        faceBackCameraOrientation = cameraInfo.orientation;
    } 
    //前置摄像头
    else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        faceFrontCameraId = i;
        faceFrontCameraOrientation = cameraInfo.orientation;
    }
}
```

知道了相机相关信息，就可以通过相机ID打开相机了。

```java
camera = Camera.open(cameraId);
```

另外，打开相机以后你会获得一个Camera对象，从这个对象里可以获取和设置相机的各种参数信息。

```java

//获取相机参数
camera.getParameters();
//设置相机参数
camera.getParameters();
```

常见的参数有以下几种。

闪光灯配置参数，可以通过Parameters.getFlashMode()接口获取。

- Camera.Parameters.FLASH_MODE_AUTO 自动模式，当光线较暗时自动打开闪光灯；
- Camera.Parameters.FLASH_MODE_OFF 关闭闪光灯；
- Camera.Parameters.FLASH_MODE_ON 拍照时闪光灯；
- Camera.Parameters.FLASH_MODE_RED_EYE 闪光灯参数，防红眼模式。

对焦模式配置参数，可以通过Parameters.getFocusMode()接口获取。

- Camera.Parameters.FOCUS_MODE_AUTO 自动对焦模式，摄影小白专用模式；
- Camera.Parameters.FOCUS_MODE_FIXED 固定焦距模式，拍摄老司机模式；
- Camera.Parameters.FOCUS_MODE_EDOF 景深模式，文艺女青年最喜欢的模式；
- Camera.Parameters.FOCUS_MODE_INFINITY 远景模式，拍风景大场面的模式；
- Camera.Parameters.FOCUS_MODE_MACRO 微焦模式，拍摄小花小草小蚂蚁专用模式；

场景模式配置参数，可以通过Parameters.getSceneMode()接口获取。

- Camera.Parameters.SCENE_MODE_BARCODE 扫描条码场景，NextQRCode项目会判断并设置为这个场景；
- Camera.Parameters.SCENE_MODE_ACTION 动作场景，就是抓拍跑得飞快的运动员、汽车等场景用的；
- Camera.Parameters.SCENE_MODE_AUTO 自动选择场景；
- Camera.Parameters.SCENE_MODE_HDR 高动态对比度场景，通常用于拍摄晚霞等明暗分明的照片；
- Camera.Parameters.SCENE_MODE_NIGHT 夜间场景；


### 1.2 关闭相机

关闭相机很简单，只需要把相机释放掉就可以了。

```java
camera.release();
```

### 1.3 开启预览

Camera的预览时通过SurfaceView的SurfaceHolder进行的，先通过，具体说来：

```java
private void startPreview(SurfaceHolder surfaceHolder) {
    try {
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(currentCameraId, cameraInfo);
        int cameraRotationOffset = cameraInfo.orientation;

        //获取相机参数
        final Camera.Parameters parameters = camera.getParameters();
        //设置对焦模式
        setAutoFocus(camera, parameters);
        //设置闪光模式
        setFlashMode(mCameraConfigProvider.getFlashMode());

        if (mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_PHOTO
                || mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_UNSPECIFIED)
            turnPhotoCameraFeaturesOn(camera, parameters);
        else if (mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_PHOTO)
            turnVideoCameraFeaturesOn(camera, parameters);

        final int rotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }

        //根据前置与后置摄像头的不同，设置预览方向，否则会发生预览图像倒过来的情况。
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayRotation = (cameraRotationOffset + degrees) % 360;
            displayRotation = (360 - displayRotation) % 360; // compensate
        } else {
            displayRotation = (cameraRotationOffset - degrees + 360) % 360;
        }
        this.camera.setDisplayOrientation(displayRotation);

        if (Build.VERSION.SDK_INT > 13
                && (mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_VIDEO
                || mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_UNSPECIFIED)) {
//                parameters.setRecordingHint(true);
        }

        if (Build.VERSION.SDK_INT > 14
                && parameters.isVideoStabilizationSupported()
                && (mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_VIDEO
                || mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_UNSPECIFIED)) {
            parameters.setVideoStabilization(true);
        }

        //设置预览大小
        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        parameters.setPictureSize(photoSize.getWidth(), photoSize.getHeight());

        //设置相机参数
        camera.setParameters(parameters);
        //设置surfaceHolder
        camera.setPreviewDisplay(surfaceHolder);
        //开启预览
        camera.startPreview();

    } catch (IOException error) {
        Log.d(TAG, "Error setting camera preview: " + error.getMessage());
    } catch (Exception ignore) {
        Log.d(TAG, "Error starting camera preview: " + ignore.getMessage());
    }
}
```

### 1.4 关闭预览

关闭预览很简单，直接调用camera.stopPreview()即可。

```java
camera.stopPreview();
```

### 1.5 拍照

拍照时通过调用Camera的takePicture()方法来完成的，

```java
takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback postview, PictureCallback jpeg)
```

该方法有三个参数：

- ShutterCallback shutter：在拍照的瞬间被回调，这里通常可以播放"咔嚓"这样的拍照音效。
- PictureCallback raw：返回未经压缩的图像数据。
- PictureCallback postview：返回postview类型的图像数据
- PictureCallback jpeg：返回经过JPEG压缩的图像数据。

我们一般用的就是最后一个，实现最后一个PictureCallback即可。

```java
camera.takePicture(null, null, new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            //存储返回的图像数据
            final File pictureFile = outputPath;
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions.");
                return;
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
                fileOutputStream.write(bytes);
                fileOutputStream.close();
            } catch (FileNotFoundException error) {
                Log.e(TAG, "File not found: " + error.getMessage());
            } catch (IOException error) {
                Log.e(TAG, "Error accessing file: " + error.getMessage());
            } catch (Throwable error) {
                Log.e(TAG, "Error saving file: " + error.getMessage());
            }
        }
 });
```

拍照完成后如果还要继续拍照则调用camera.startPreview()继续开启预览，否则关闭预览，释放相机资源。

### 1.6 开始视频录制

视频的录制时通过MediaRecorder来完成的。

```java
if (prepareVideoRecorder()) {
            mediaRecorder.start();
            isVideoRecording = true;
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    videoListener.onVideoRecordStarted(videoSize);
                }
            });
}
```
MediaRecorder主要用来录制音频和视频，在使用之前要进行初始化和相关参数的设置，如下所示：

```java
protected boolean preparemediaRecorder() {
    mediaRecorder = new MediaRecorder();
    try {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        
        //输出格式
        mediaRecorder.setOutputFormat(camcorderProfile.fileFormat);
        //视频帧率
        mediaRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
        //视频大小
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        //视频比特率
        mediaRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
        //视频编码器
        mediaRecorder.setVideoEncoder(camcorderProfile.videoCodec);
        
        //音频编码率
        mediaRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
        //音频声道
        mediaRecorder.setAudioChannels(camcorderProfile.audioChannels);
        //音频采样率
        mediaRecorder.setAudioSamplingRate(camcorderProfile.audioSampleRate);
        //音频编码器
        mediaRecorder.setAudioEncoder(camcorderProfile.audioCodec);
        
        File outputFile = outputPath;
        String outputFilePath = outputFile.toString();
        //输出路径
        mediaRecorder.setOutputFile(outputFilePath);
        
        //设置视频输出的最大尺寸
        if (mCameraConfigProvider.getVideoFileSize() > 0) {
            mediaRecorder.setMaxFileSize(mCameraConfigProvider.getVideoFileSize());
            mediaRecorder.setOnInfoListener(this);
        }
        
        //设置视频输出的最大时长
        if (mCameraConfigProvider.getVideoDuration() > 0) {
            mediaRecorder.setMaxDuration(mCameraConfigProvider.getVideoDuration());
            mediaRecorder.setOnInfoListener(this);
        }
        mediaRecorder.setOrientationHint(getVideoOrientation(mCameraConfigProvider.getSensorPosition()));
        
        //准备
        mediaRecorder.prepare();

        return true;
    } catch (IllegalStateException error) {
        Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
    } catch (IOException error) {
        Log.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
    } catch (Throwable error) {
        Log.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
    }
    releasemediaRecorder();
    return false;
}
```

值得一提的是，日常的业务中经常对拍摄视频的时长或者大小有要求，这个可以通过mediaRecorder.setOnInfoListener()来处理，OnInfoListener会监听正在录制的视频，然后我们
可以在它的回调方法里处理。

```java
   @Override
public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
    if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what) {
        //到达最大时长
    } else if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what) {
        //到达最大尺寸
    }
}
```
更多关于MediaRecorder的介绍可以参考[MediaRecorder官方文档](https://developer.android.com/reference/android/media/MediaRecorder.html)。

### 1.7 结束视频录制

结束视频录制也很简单，只需要调用mediaRecorder.stop()方法即可。

```java
mediaRecorder.stop();
```
此外，如果不再使用相机，也要注意释放相机资源。

以上便是Camera的全部内容，还是比较简单的，下面我们接着来讲Camera2的相关内容，注意体会两者的区别。

## 二 Camera2实践指南

- [Android Camera2 官方视频](https://www.youtube.com/watch?v=Xtp3tH27OFs)
- [Android Camera2 官方文档](https://developer.android.com/reference/android/hardware/camera2/package-summary.html)
- [Android Camera2 官方用例](https://github.com/googlesamples/android-Camera2Basic)

Camera2 API中主要涉及以下几个关键类：

- CameraManager：摄像头管理器，用于打开和关闭系统摄像头
- CameraCharacteristics：描述摄像头的各种特性，我们可以通过CameraManager的getCameraCharacteristics(@NonNull String cameraId)方法来获取。
- CameraDevice：描述系统摄像头，类似于早期的Camera。
- CameraCaptureSession：Session类，当需要拍照、预览等功能时，需要先创建该类的实例，然后通过该实例里的方法进行控制（例如：拍照 capture()）。
- CaptureRequest：描述了一次操作请求，拍照、预览等操作都需要先传入CaptureRequest参数，具体的参数控制也是通过CameraRequest的成员变量来设置。
- CaptureResult：描述拍照完成后的结果。

Camera2拍照流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/media/camera2_structure.png" width="400"/>

开发者通过创建CaptureRequest向摄像头发起Capture请求，这些请求会排成一个队列供摄像头处理，摄像头将结果包装在CaptureMetadata中返回给开发者。整个流程建立在一个CameraCaptureSession的会话中。

### 2.1 打开相机

打开相机之前，我们首先要获取CameraManager，然后获取相机列表，进而获取各个摄像头（主要是前置摄像头和后置摄像头）的参数。

```java
mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
try {
    final String[] ids = mCameraManager.getCameraIdList();
    numberOfCameras = ids.length;
    for (String id : ids) {
        final CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);

        final int orientation = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (orientation == CameraCharacteristics.LENS_FACING_FRONT) {
            faceFrontCameraId = id;
            faceFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            frontCameraCharacteristics = characteristics;
        } else {
            faceBackCameraId = id;
            faceBackCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            backCameraCharacteristics = characteristics;
        }
    }
} catch (Exception e) {
    Log.e(TAG, "Error during camera initialize");
}
```

Camera2与Camera一样也有cameraId的概念，我们通过mCameraManager.getCameraIdList()来获取cameraId列表，然后通过mCameraManager.getCameraCharacteristics(id)
获取每个id对应摄像头的参数。

关于CameraCharacteristics里面的参数，主要用到的有以下几个：

- LENS_FACING：前置摄像头（LENS_FACING_FRONT）还是后置摄像头（LENS_FACING_BACK）。
- SENSOR_ORIENTATION：摄像头拍照方向。
- FLASH_INFO_AVAILABLE：是否支持闪光灯。
- CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL：获取当前设备支持的相机特性。

注：事实上，在各个厂商的的Android设备上，Camera2的各种特性并不都是可用的，需要通过characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)方法
来根据返回值来获取支持的级别，具体说来：

- INFO_SUPPORTED_HARDWARE_LEVEL_FULL：全方位的硬件支持，允许手动控制全高清的摄像、支持连拍模式以及其他新特性。              
- INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED：有限支持，这个需要单独查询。
- INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY：所有设备都会支持，也就是和过时的Camera API支持的特性是一致的。

利用这个INFO_SUPPORTED_HARDWARE_LEVEL参数，我们可以来判断是使用Camera还是使用Camera2，具体方法如下：

```java
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public static boolean hasCamera2(Context mContext) {
    if (mContext == null) return false;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
    try {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        String[] idList = manager.getCameraIdList();
        boolean notFull = true;
        if (idList.length == 0) {
            notFull = false;
        } else {
            for (final String str : idList) {
                if (str == null || str.trim().isEmpty()) {
                    notFull = false;
                    break;
                }
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(str);

                final int supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    notFull = false;
                    break;
                }
            }
        }
        return notFull;
    } catch (Throwable ignore) {
        return false;
    }
}
```

更多ameraCharacteristics参数，可以参见[CameraCharacteristics官方文档](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics.html)。

打开相机主要调用的是mCameraManager.openCamera(currentCameraId, stateCallback, backgroundHandler)方法，如你所见，它有三个参数：

- String cameraId：摄像头的唯一ID。
- CameraDevice.StateCallback callback：摄像头打开的相关回调。
- Handler handler：StateCallback需要调用的Handler，我们一般可以用当前线程的Handler。

```java
 mCameraManager.openCamera(currentCameraId, stateCallback, backgroundHandler);
```

上面我们提到了CameraDevice.StateCallback，它是摄像头打开的一个回调，定义了打开，关闭以及出错等各种回调方法，我们可以在
这些回调方法里做对应的操作。

```java
private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        //获取CameraDevice
        mcameraDevice = cameraDevice;
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
        //关闭CameraDevice
        cameraDevice.close();

    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int error) {
        //关闭CameraDevice
        cameraDevice.close();
    }
};
```

### 2.2 关闭相机

通过上面的描述，关闭就很简单了。

```java
//关闭CameraDevice
cameraDevice.close();
```

### 2.3 开启预览

Camera2都是通过创建请求会话的方式进行调用的，具体说来：

1. 调用mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)方法创建CaptureRequest，调用
2. mCameraDevice.createCaptureSession()方法创建CaptureSession。

```java
CaptureRequest.Builder createCaptureRequest(@RequestTemplate int templateType)
```

createCaptureRequest()方法里参数templateType代表了请求类型，请求类型一共分为六种，分别为：

- TEMPLATE_PREVIEW：创建预览的请求
- TEMPLATE_STILL_CAPTURE：创建一个适合于静态图像捕获的请求，图像质量优先于帧速率。
- TEMPLATE_RECORD：创建视频录制的请求
- TEMPLATE_VIDEO_SNAPSHOT：创建视视频录制时截屏的请求
- TEMPLATE_ZERO_SHUTTER_LAG：创建一个适用于零快门延迟的请求。在不影响预览帧率的情况下最大化图像质量。
- TEMPLATE_MANUAL：创建一个基本捕获请求，这种请求中所有的自动控制都是禁用的(自动曝光，自动白平衡、自动焦点)。

```java
createCaptureSession(@NonNull List<Surface> outputs, @NonNull CameraCaptureSession.StateCallback callback, @Nullable Handler handler)
```
createCaptureSession()方法一共包含三个参数：

- List<Surface> outputs：我们需要输出到的Surface列表。
- CameraCaptureSession.StateCallback callback：会话状态相关回调。
- Handler handler：callback可以有多个（来自不同线程），这个handler用来区别那个callback应该被回调，一般写当前线程的Handler即可。

关于CameraCaptureSession.StateCallback里的回调方法：

- onConfigured(@NonNull CameraCaptureSession session); 摄像头完成配置，可以处理Capture请求了。
- onConfigureFailed(@NonNull CameraCaptureSession session); 摄像头配置失败
- onReady(@NonNull CameraCaptureSession session); 摄像头处于就绪状态，当前没有请求需要处理。
- onActive(@NonNull CameraCaptureSession session); 摄像头正在处理请求。
- onClosed(@NonNull CameraCaptureSession session); 会话被关闭
- onSurfacePrepared(@NonNull CameraCaptureSession session, @NonNull Surface surface); Surface准备就绪

理解了这些东西，创建预览请求就十分简单了。

```java
previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
previewRequestBuilder.addTarget(workingSurface);

//注意这里除了预览的Surface，我们还添加了imageReader.getSurface()它就是负责拍照完成后用来获取数据的
mCameraDevice.createCaptureSession(Arrays.asList(workingSurface, imageReader.getSurface()),
        new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                cameraCaptureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                Log.d(TAG, "Fail while starting preview: ");
            }
        }, null);
```

可以发现，在onConfigured()里调用了cameraCaptureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)，这样我们就可以
持续的进行预览了。

注：上面我们说了添加了imageReader.getSurface()它就是负责拍照完成后用来获取数据，具体操作就是为ImageReader设置一个OnImageAvailableListener，然后在它的onImageAvailable()
方法里获取。

```java
mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            //当图片可得到的时候获取图片并保存
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

 };
```

### 2.4 关闭预览

关闭预览就是关闭当前预览的会话，结合上面开启预览的内容，具体实现如下：

```java
if (captureSession != null) {
    captureSession.close();
    try {
        captureSession.abortCaptures();
    } catch (Exception ignore) {
    } finally {
        captureSession = null;
    }
}
```

### 2.5 拍照

拍照具体来说分为三步：

1. 对焦

```java
try {
    //相机对焦
    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
    //修改状态
    previewState = STATE_WAITING_LOCK;
    //发送对焦请求
    captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
} catch (Exception ignore) {
}
```

我们定义了一个CameraCaptureSession.CaptureCallback来处理对焦请求返回的结果。

```java
private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                    @NonNull CaptureRequest request,
                                    @NonNull CaptureResult partialResult) {
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
            //等待对焦
            final Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
            if (afState == null) {
                //对焦失败，直接拍照
                captureStillPicture();
            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                    || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                    || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState
                    || CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState) {
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    previewState = STATE_PICTURE_TAKEN;
                    //对焦完成，进行拍照
                    captureStillPicture();
                } else {
                    runPreCaptureSequence();
                }
            }
    }
};
```

2. 拍照

我们定义了一个captureStillPicture()来进行拍照。


```java
private void captureStillPicture() {
    try {
        if (null == mCameraDevice) {
            return;
        }
        
        //构建用来拍照的CaptureRequest
        final CaptureRequest.Builder captureBuilder =
                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(imageReader.getSurface());

        //使用相同的AR和AF模式作为预览
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        //设置方向
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getPhotoOrientation(mCameraConfigProvider.getSensorPosition()));

        //创建会话
        CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                Log.d(TAG, "onCaptureCompleted: ");
            }
        };
        //停止连续取景
        captureSession.stopRepeating();
        //捕获照片
        captureSession.capture(captureBuilder.build(), CaptureCallback, null);

    } catch (CameraAccessException e) {
        Log.e(TAG, "Error during capturing picture");
    }
}
```
3. 取消对焦

拍完照片后，我们还要解锁相机焦点，让相机恢复到预览状态。

```java
try {
    //重置自动对焦
    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
    captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
    //相机恢复正常的预览状态
    previewState = STATE_PREVIEW;
    //打开连续取景模式
    captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
} catch (Exception e) {
    Log.e(TAG, "Error during focus unlocking");
}
```

### 2.6 开始视频录制

```java

//先关闭预览，因为需要添加一个预览输出的Surface，也就是mediaRecorder.getSurface()
closePreviewSession();

//初始化MediaRecorder，设置相关参数
if (preparemediaRecorder()) {

    final SurfaceTexture texture = Camera2Manager.this.texture;
    texture.setDefaultBufferSize(videoSize.getWidth(), videoSize.getHeight());

    try {
        //构建视频录制aptureRequest
        previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        final List<Surface> surfaces = new ArrayList<>();

        //设置预览Surface
        final Surface previewSurface = workingSurface;
        surfaces.add(previewSurface);
        previewRequestBuilder.addTarget(previewSurface);

        //设置预览输出Surface
        workingSurface = mediaRecorder.getSurface();
        surfaces.add(workingSurface);
        previewRequestBuilder.addTarget(workingSurface);

        mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                captureSession = cameraCaptureSession;

                previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                try {
                    //持续发送Capture请求，实现实时预览。
                    captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                } catch (Exception e) {
                }

                try {
                    //开始录像
                    mediaRecorder.start();
                } catch (Exception ignore) {
                    Log.e(TAG, "mediaRecorder.start(): ", ignore);
                }

                isVideoRecording = true;

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cameraVideoListener.onVideoRecordStarted(videoSize);
                    }
                });
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                Log.d(TAG, "onConfigureFailed");
            }
        }, backgroundHandler);
    } catch (Exception e) {
        Log.e(TAG, "startVideoRecord: ", e);
    }
}
```
关于MediaRecorder上面讲Camera的时候我们就已经说过，这里不再赘述。

以上便是视频录制的全部内容，就是简单的API使用，还是比较简单的。

### 2.7 结束视频录制

结束视频录制主要也是关闭会话以及释放一些资源，具体说来：

1. 关闭预览会话
2. 停止mediaRecorder
3. 释放mediaRecorder

```java
//关闭预览会话
if (captureSession != null) {
    captureSession.close();
    try {
        captureSession.abortCaptures();
    } catch (Exception ignore) {
    } finally {
        captureSession = null;
    }
}

//停止mediaRecorder
if (mediaRecorder != null) {
    try {
        mediaRecorder.stop();
    } catch (Exception ignore) {
    }
}

//释放mediaRecorder
try {
    if (mediaRecorder != null) {
        mediaRecorder.reset();
        mediaRecorder.release();
    }
} catch (Exception ignore) {

} finally {
    mediaRecorder = null;
}
```

以上便是Camera/Camera2实践的相关内容，更多关于图像、视频处理的内容可以参见[phoenix](https://github.com/guoxiaoxing/phoenix)项目。