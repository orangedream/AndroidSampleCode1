package tw.iotec.androidsamplecode1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ExifInterface
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.OrientationEventListener
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayInputStream


const val CAMERA_PERMISSION_REQUEST = 12345

class CustomCameraActivity : AppCompatActivity() {
    private lateinit var cameraCharacteristics: CameraCharacteristics
    private var cameraInitialized: Boolean = false
    val tag = "CustomCameraActivity"
    private lateinit var textureView: TextureView
    private lateinit var cameraId: String
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var previewSize: Size
    private var rotationToDegree : SparseIntArray = SparseIntArray(4).apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_camera)

        textureView = findViewById(R.id.texture_view)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        findViewById<Button>(R.id.btn_capture).setOnClickListener {
            if(cameraPermissionGranted()) {
                if(cameraInitialized)
                    takePhoto()
                else
                    Log.e(tag, "Camera not initialized.")
            }else {
                Log.e(tag, "Camera permission not granted.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        enableCameraPreview()
    }
    override fun onPause() {
        super.onPause()
        stopBackgroundThread()
    }
    private fun enableCameraPreview(){
        if (!cameraPermissionGranted()) {
            Log.d(tag,"requesting camera permission")
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }else {
            checkTextureAvailable()
        }
    }

    private fun checkTextureAvailable() {
        if (textureView.isAvailable) {
            setupCamera()
        } else if (!textureView.isAvailable){
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                    setupCamera()
                }
                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
                }
                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    return true
                }
                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupCamera() {
        val cameraIds: Array<String> = cameraManager.cameraIdList

        for (id in cameraIds) {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(id)

            //If we want to choose the rear facing camera instead of the front facing one
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            }

            val streamConfigurationMap : StreamConfigurationMap? = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            if (streamConfigurationMap != null) {
                previewSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
                imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }
            cameraId = id
        }

        cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
    }
    /**
     * ImageAvailable Listener
     */
    private val onImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            Log.d(tag,"Photo taken.")
            var image:Image? = null
            val imageView = findViewById<ImageView>(R.id.image_view)
            try {
                image = reader.acquireLatestImage()
                val planes = image.planes
                val buffer = planes[0].buffer
                buffer.rewind()
                val data = ByteArray(buffer.capacity())
                buffer.get(data)

                // some orientation info could be in EXIF info
                val exifInterface = ExifInterface(ByteArrayInputStream(data))
                val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                var rotationDegrees = 0
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = -90  // reverse rotation
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees = -180
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees = -270
                }

                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap == null)
                    Log.e(tag, "bitmap is null")
                else {
                    // rotate bitmap
                    val matrix = Matrix()

                    // rotate bitmap according to EXIF orientation info
                    matrix.postRotate(rotationDegrees.toFloat())

                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.width, true)
                    val newBitmap = Bitmap.createBitmap(scaledBitmap,0,0,scaledBitmap.width,scaledBitmap.height,matrix,true)

                    runOnUiThread {
                        imageView.setImageBitmap(newBitmap)
                    }
                }
            } catch (e:Exception) {
                Log.e(tag,"photo capture error: $e")
            }
            image?.close()
        }

    @SuppressLint("MissingPermission")
    private fun takePhoto() {
        Log.d(tag,"Taking photo...")
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)

        // acquire display rotation
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            baseContext.display?.rotation
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }

        // fix rotation by sensorOrientation
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, fixSensorOrientation(cameraCharacteristics, rotation!!))

        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        cameraCaptureSession.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {

            override fun onCaptureStarted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                timestamp: Long,
                frameNumber: Long
            ) {
                Log.v(tag,"onCaptureStarted")
            }

            override fun onCaptureProgressed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                partialResult: CaptureResult
            ) {
                Log.v(tag,"onCaptureProgressed")
            }

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                Log.v(tag,"onCaptureCompleted")
            }
        }, null)

    }

    // Orientation calculation
    private fun fixSensorOrientation(c: CameraCharacteristics, orientation: Int): Int {
        var deviceOrientation = rotationToDegree[orientation]
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90

        // Reverse device orientation for front-facing cameras
        val facingFront =
            c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.v(tag,"Camera opened.")

            cameraDevice = camera
            cameraInitialized = true

            val surfaceTexture : SurfaceTexture? = textureView.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface = Surface(surfaceTexture)

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)  // preview camera
            captureRequestBuilder.addTarget(previewSurface)

            cameraDevice.createCaptureSession(listOf(previewSurface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(tag,"Capture configure failed.")

                }
                override fun onConfigured(session: CameraCaptureSession) {
                    Log.v(tag,"Capture configured.")

                    cameraCaptureSession = session

                    cameraCaptureSession.setRepeatingRequest(
                        captureRequestBuilder.build(),
                        null,
                        backgroundHandler
                    )

                }
            }, null)
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.v(tag,"Camera disconnected.")
            cameraInitialized = false
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            val errorMsg = when(error) {
                ERROR_CAMERA_DEVICE -> "Camera not found?"
                ERROR_CAMERA_DISABLED -> "Camera disabled"
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_CAMERA_SERVICE -> "Camera out of service?"
                ERROR_MAX_CAMERAS_IN_USE -> "Max cameras in use"
                else -> "Unknown"
            }
            Log.e(tag, "Error connecting camera : $errorMsg")
        }
    }

    /**
     * Camera permission
     */
    private fun cameraPermissionGranted() : Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        return false
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(tag,"Camera permission acquired.")
                    checkTextureAvailable()
                }else{
                    Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG).show()

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        intent.data = Uri.fromParts("package", this.packageName, null)
                        startActivity(intent)
                    }
                }
            }
        }
    }




    /**
     * Background thread
     */
    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("CameraThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundHandlerThread.quitSafely()
        backgroundHandlerThread.join()
    }


}