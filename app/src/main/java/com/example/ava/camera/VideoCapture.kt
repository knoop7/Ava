package com.example.ava.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors


class VideoCapture(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoCapture"
        private const val JPEG_QUALITY = 75
        
        fun createPlaceholderFromAsset(context: Context, assetName: String = "camera_off.png", width: Int = 320, height: Int = 240): ByteArray {
            return try {
                val inputStream = context.assets.open(assetName)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
                if (scaledBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }
                
                ByteArrayOutputStream().apply {
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, this)
                    scaledBitmap.recycle()
                }.toByteArray()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load asset $assetName", e)
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.BLACK)
                ByteArrayOutputStream().apply {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, this)
                    bitmap.recycle()
                }.toByteArray()
            }
        }
    }
    
    private val executor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var lifecycleOwner: TempLifecycleOwner? = null
    @Volatile private var isRecording = false
    @Volatile private var lastFrameTime = 0L
    private var onFrameCallback: ((ByteArray) -> Unit)? = null
    private var frameIntervalMs = 200L
    
    fun startRecording(
        useFrontCamera: Boolean = false,
        fps: Int = 5,
        resolution: Int = 480,
        onFrame: (ByteArray) -> Unit
    ) {
        if (isRecording) return
        
        isRecording = true
        onFrameCallback = onFrame
        frameIntervalMs = 1000L / fps.coerceIn(1, 15)
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            runCatching {
                cameraProvider = cameraProviderFuture.get()
                lifecycleOwner = TempLifecycleOwner().apply { start() }
                
                val targetSize = Size(resolution * 4 / 3, resolution)
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy(targetSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                            .build()
                    )
                    .build()
                    .apply { setAnalyzer(executor, ::processFrame) }
                
                
                val selector = selectCamera(cameraProvider!!, useFrontCamera)
                
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner!!, selector, imageAnalysis)
            }.onFailure { Log.e(TAG, "Failed to start", it) }
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun selectCamera(provider: ProcessCameraProvider, useFront: Boolean): CameraSelector {
        val preferred = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        return runCatching {
            preferred.filter(provider.availableCameraInfos)
            preferred
        }.getOrElse { CameraSelector.Builder().build() }
    }
    
    private fun processFrame(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (isRecording && now - lastFrameTime >= frameIntervalMs) {
            lastFrameTime = now
            runCatching {
                convertToJpeg(imageProxy)?.let { onFrameCallback?.invoke(it) }
            }
        }
        imageProxy.close()
    }
    
    private fun convertToJpeg(imageProxy: ImageProxy): ByteArray? = runCatching {
        val width = imageProxy.width
        val height = imageProxy.height
        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]
        
        val yBuffer = yPlane.buffer.duplicate()
        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()
        
        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        
        val nv21 = ByteArray(width * height * 3 / 2)
        var pos = 0
        
        
        for (row in 0 until height) {
            val yOffset = row * yRowStride
            if (yOffset + width <= yBuffer.capacity()) {
                yBuffer.position(yOffset)
                yBuffer.get(nv21, pos, width)
            }
            pos += width
        }
        
        
        val uvHeight = height / 2
        val uvWidth = width / 2
        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val uvOffset = row * uvRowStride + col * uvPixelStride
                if (uvOffset < vBuffer.capacity() && uvOffset < uBuffer.capacity()) {
                    nv21[pos++] = vBuffer.get(uvOffset)  
                    nv21[pos++] = uBuffer.get(uvOffset)  
                } else {
                    nv21[pos++] = 128.toByte()  
                    nv21[pos++] = 128.toByte()
                }
            }
        }
        
        
        val out = ByteArrayOutputStream()
        YuvImage(nv21, ImageFormat.NV21, width, height, null)
            .compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, out)
        
        var jpeg = out.toByteArray()
        
        
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation != 0) {
            val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            bmp.recycle()
            val rotOut = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, rotOut)
            rotated.recycle()
            jpeg = rotOut.toByteArray()
        }
        
        jpeg
    }.getOrNull()
    
    fun stopRecording() {
        isRecording = false
        onFrameCallback = null
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            runCatching {
                cameraProvider?.unbindAll()
                lifecycleOwner?.stop()
                lifecycleOwner = null
                cameraProvider = null
            }
        }
    }
    
    fun close() {
        stopRecording()
        executor.shutdown()
    }
    
    private class TempLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        fun start() { registry.currentState = Lifecycle.State.STARTED }
        fun stop() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}
