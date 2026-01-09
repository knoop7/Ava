package com.example.ava.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.math.min


class CameraCapture(private val context: Context) {
    
    companion object {
        private const val TAG = "CameraCapture"
        private const val JPEG_QUALITY = 80
    }
    
    private val executor = Executors.newSingleThreadExecutor()
    
    
    suspend fun capturePhoto(
        useFrontCamera: Boolean = false,
        targetSize: Int = 500
    ): ByteArray? = suspendCancellableCoroutine { continuation ->
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    
                    
                    val lifecycleOwner = TempLifecycleOwner()
                    lifecycleOwner.start()
                    
                    
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setJpegQuality(JPEG_QUALITY)
                        .build()
                    
                    
                    val cameraSelector = selectCamera(cameraProvider, useFrontCamera)
                    
                    
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageCapture
                    )
                    
                    
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                try {
                                    val jpegData = imageProxyToJpeg(image, targetSize)
                                    image.close()
                                    
                                    
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        cameraProvider.unbindAll()
                                        lifecycleOwner.stop()
                                    }
                                    
                                    if (continuation.isActive) {
                                        continuation.resume(jpegData)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing captured image", e)
                                    image.close()
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        cameraProvider.unbindAll()
                                        lifecycleOwner.stop()
                                    }
                                    if (continuation.isActive) {
                                        continuation.resume(null)
                                    }
                                }
                            }
                            
                            override fun onError(exception: ImageCaptureException) {
                                Log.e(TAG, "Image capture failed", exception)
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    cameraProvider.unbindAll()
                                    lifecycleOwner.stop()
                                }
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to bind camera", e)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }, ContextCompat.getMainExecutor(context))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get camera provider", e)
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
        
        continuation.invokeOnCancellation {
            Log.d(TAG, "Photo capture cancelled")
        }
    }
    
    
    private fun imageProxyToJpeg(image: ImageProxy, targetSize: Int): ByteArray {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        
        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bitmap = rotatedBitmap
        }
        
        val finalBitmap: Bitmap
        if (targetSize == 0) {
            
            finalBitmap = bitmap
        } else {
            
            val size = min(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            val squareBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)
            if (squareBitmap != bitmap) {
                bitmap.recycle()
            }
            
            
            finalBitmap = Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)
            if (finalBitmap != squareBitmap) {
                squareBitmap.recycle()
            }
        }
        
        
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        finalBitmap.recycle()
        
        return outputStream.toByteArray()
    }
    
    fun close() {
        executor.shutdown()
    }
    
    
    private fun selectCamera(provider: ProcessCameraProvider, useFrontCamera: Boolean): CameraSelector {
        val preferredSelector = when (useFrontCamera) {
            true -> CameraSelector.DEFAULT_FRONT_CAMERA
            false -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        return runCatching {
            preferredSelector.filter(provider.availableCameraInfos)
            preferredSelector
        }.getOrElse {
            
            CameraSelector.Builder().build()
        }
    }
    
    
    private class TempLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        
        override val lifecycle: Lifecycle
            get() = registry
        
        fun start() {
            registry.currentState = Lifecycle.State.STARTED
        }
        
        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }
}
