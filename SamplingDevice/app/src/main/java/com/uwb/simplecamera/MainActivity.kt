package com.uwb.simplecamera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Size
import android.view.Surface.ROTATION_0
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.uwb.simplecamera.databinding.ActivityMainBinding
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // start openCV
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        // init Python 3.8 (default)
        //if (!Python.isStarted()) {
            //Python.start(AndroidPlatform(this));
        //}

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        //viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        //viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }
        viewBinding.lightsourceButton.setOnClickListener { updateRealWorldLightSourceSizes() }
        viewBinding.rwlsNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {}

            override fun onTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {}

            override fun afterTextChanged(p0: Editable?) {
                viewBinding.lightsourceButton.isEnabled = !p0.isNullOrEmpty();
            }

        })


        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun updateRealWorldLightSourceSizes()
    {
        val inputLightsources = viewBinding.rwlsNumber.text.split(",").toTypedArray()
        var result = ArrayList<Int>()
        for (token in inputLightsources)
        {
            val num = token.toIntOrNull()
            if (num != null)
            {
                result.add(num)
            }
        }
        result.sort()
        AppData.realWorldLightsources = result
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }
/*
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        //viewBinding.videoCaptureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@MainActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }
    }
*/
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Size(1920, 1080)
        // Size(480, 640)
        cameraProviderFuture.addListener({
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy(Size(3840, 2160), FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Image Capture
            imageCapture = ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()

            // Video Capture
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Image Analyzer
            // .setResolutionSelector(resolutionSelector)
            val imageAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetRotation(ROTATION_0)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer(this) { luma ->
                        //Log.d(TAG, "Average luminosity: $luma")
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer, videoCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
        /*
        val manager: CameraManager? = getSystemService(CAMERA_SERVICE) as CameraManager?
        calculateFOV(manager)
        Log.d(TAG, "horizontal : " + horizonalAngle + "      vertical : " + verticalAngle)

        @Suppress("DEPRECATION")
        val camera: Camera = Camera.open()
        val p: Camera.Parameters = camera.getParameters()
        val thetaV = p.getVerticalViewAngle().toDouble()
        val thetaH = p.getHorizontalViewAngle().toDouble()
        Log.d(TAG, "horizontalOLD : " + thetaH + "      verticalOLD : " + thetaV )*/
    }
    /*var horizonalAngle: Float = 0f
    var verticalAngle: Float = 0f

    private fun calculateFOV(cManager: CameraManager?) {
        try {
            for (cameraId in cManager?.cameraIdList!!) {
                val characteristics: CameraCharacteristics =
                    cManager.getCameraCharacteristics(cameraId)
                val cOrientation: Int? = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    val maxFocus: FloatArray? =
                        characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val size: SizeF? =
                        characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    val w: Float = size?.getWidth() ?: 0f
                    val h: Float = size?.getHeight() ?: 0f
                    if (maxFocus == null)
                    {
                        return
                    }
                    horizonalAngle = (2 * atan((w / (maxFocus[0] * 2)).toDouble())).toFloat()
                    verticalAngle = (2 * atan((h / (maxFocus[0] * 2)).toDouble())).toFloat()
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }*/

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    private class LuminosityAnalyzer(private val context: Context, private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private var lastTimeStamp = 0L
        private val analysisInterval = 300 // milliseconds

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        // Required because ImageProxy and Samsung devices is rotated 90 degrees.
        // Need portrait orientation at rotation = 0
        private fun rotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
            val matrix = Matrix().apply {
                postRotate(degree)
            }

            return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }

        override fun analyze(image: ImageProxy) {
            Log.d("TimingLog", "-------------analyze function called-------------")
            val currentTimeStamp = System.currentTimeMillis()
            if (currentTimeStamp - lastTimeStamp < analysisInterval) {
                lastTimeStamp = currentTimeStamp
                image.close()
                return
            }
            //val buffer = image.planes[0].buffer
            //val data = buffer.toByteArray()
            //val pixels = data.map { it.toInt() and 0xFF }
            //val luma = pixels.average()

            //listener(luma)
            val startTime = System.currentTimeMillis()
            //val bitmap = image.toBitmap()
            val bitmap = rotateBitmap(image.toBitmap(), image.imageInfo.rotationDegrees.toFloat())
            val rotateTime = System.currentTimeMillis()
            Log.d("TimingLog", "bitmap rotation took ${blockTimingInSeconds(rotateTime, startTime)} s")

            /*
            val width = bitmap.width
            val height = bitmap.height
            Log.d(TAG, image.imageInfo.rotationDegrees.toString())
            Log.d(TAG, "Width: $width, Height: $height")
             */
            //Log.d(TAG, "pixel 0, 0: " + bitmap[0, 0])
            //var mat = Mat()
            //Utils.bitmapToMat(bitmap, mat)
            //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)

/*
            val inputStream = context.assets.open("lights/wctpp2.jpg")
            val imageMap = rotateBitmap(BitmapFactory.decodeStream(inputStream), 90f) //90f

            val width = imageMap.width
            val height = imageMap.height
            Log.d(TAG, image.imageInfo.rotationDegrees.toString())
            Log.d(TAG, "Width: $width, Height: $height")
*/
            // val byteArray = bitmapToByteArray(bitmap)
            //val blockTime = System.currentTimeMillis()
            //Log.d("TimingLog", "bitmap -> byteArray took ${blockTimingInSeconds(blockTime, rotateTime)} s")

            //pythonLightEstimation(byteArray)
            val pythonTime = System.currentTimeMillis()
            //Log.d("TimingLog", "Python Algorithm took ${blockTimingInSeconds(pythonTime, blockTime)} s")

            val datamap = estimateMultipleLightSources(bitmap, AppData.realWorldLightsources) //bitmap
            //val datamap = estimateMultipleDarkSources(imageMap)
            Log.d("ContentLog", datamap.toString())
            val kotlinTime = System.currentTimeMillis()
            Log.d("TimingLog", "Kotlin Algorithm took ${blockTimingInSeconds(kotlinTime, pythonTime)} s")
            sendJSON(datamap)

            //saveImage(bitmap)
            image.close()
        }

        private fun saveImage(finalBitmap: Bitmap) {
            val root: String? = Environment.getExternalStorageDirectory().toString()
            val myDir = File("/storage/emulated/0/Download" + "/saved_images")
            myDir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fname = "Shutta_" + timeStamp + ".jpg"

            val file = File(myDir, fname)
            if (file.exists()) file.delete()
            try {
                val out = FileOutputStream(file)
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        // taken from chatgpt
        private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
            val plane = image.planes[0]
            val buffer = plane.buffer
            buffer.rewind()

            val width = image.width
            val height = image.height
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride

            val rowPadding = rowStride - pixelStride * width

            // Create a Bitmap with exact size (including potential row padding)
            val bitmap = createBitmap(rowStride / pixelStride, height)

            bitmap.copyPixelsFromBuffer(buffer)

            return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        }

        // taken from chatgpt
        fun bitmapToByteArray(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): ByteArray {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(format, 100, outputStream)
            return outputStream.toByteArray()
        }

        private fun pythonLightEstimation(byteArray: ByteArray) {
            val py = Python.getInstance()
            val module = py.getModule("estimate_lightsource")
            try {
                val result = module.callAttr("estimate", byteArray)
                val resultMap = pythonDictionaryToKotlinMap(result)
                Log.d("ContentLog", resultMap.toString())
                sendJSON(resultMap)
            } catch (e: PyException) {
                Log.e(TAG, "Python call ERROR: " + e.message.toString())
            }
        }

        // chatgpt
        private fun pythonDictionaryToKotlinMap(pyDict: PyObject): Map<String, Float> {
            val map = mutableMapOf<String, Float>()
            for (key in pyDict.asMap().keys) {
                val value = pyDict.asMap()[key]
                if (key != null && value != null) {
                    map[key.toString()] = value.toFloat()
                }
            }
            return map
        }

        // chatgpt
        private fun sendJSON(map: Map<String, Any>, unityIp: String = "192.168.1.71", port: Int = 8052) {
            val jsonString = JSONObject(map)
            sendJSON(jsonString)
        }

        // 192.168.1.71 samsung tablet
        // 192.168.1.10 pc
        // 192.168.1.96 phone
        private fun sendJSON(obj: JSONObject, unityIp: String = "192.168.1.71", port: Int = 8052) {
            val jsonString = obj.toString()

            Thread {
                var socket: Socket? = null
                try {
                    val blockTime = System.currentTimeMillis()
                    socket = Socket(unityIp, port)
                    socket.getOutputStream().write(jsonString.toByteArray())
                    socket.getOutputStream().flush()

                    // Read response from server
                    val inputStream = socket.getInputStream()
                    val buffer = ByteArray(1024)
                    val bytesRead = inputStream.read(buffer)
                    val response = String(buffer, 0, bytesRead)

                    Log.d("TCP", "Server response: $response")
                    Log.d("TimingLog", "Python Algorithm took ${blockTimingInSeconds(System.currentTimeMillis(), blockTime)} s")
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    socket?.close()
                }
            }.start()
        }

        private fun blockTimingInSeconds(end: Long, start: Long): Double {
            return (end - start) / 1000.0
        }

    }
}

object AppData {
    var realWorldLightsources: ArrayList<Int> = ArrayList<Int>()
}

