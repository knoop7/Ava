package com.example.ava.camera

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Controls the device flashlight, independently of any camera session.
 *
 * Deliberately uses [CameraManager.setTorchMode] rather than CameraX's `CameraControl.enableTorch`:
 * the CameraX route needs a camera bound to a lifecycle, so the torch could only be lit while a
 * snapshot or video stream happened to be running, and it would go dark again the moment that
 * session ended. `setTorchMode` works with the camera closed, which is what makes a flashlight
 * useful on its own — and it means the torch does not compete with [CameraCapture] or
 * [VideoCapture] for the camera. It also needs no CAMERA permission.
 *
 * Reported state comes from the system's own [CameraManager.TorchCallback] rather than from the
 * last value that was requested, so it stays truthful when something else turns the torch off —
 * which happens when another app opens that camera, or when the device gets too hot.
 */
class TorchController(private val context: Context) {

    companion object {
        private const val TAG = "TorchController"
    }

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val _state = MutableStateFlow(false)
    val state: StateFlow<Boolean> = _state

    private var cameraId: String? = null
    private var torchCallback: CameraManager.TorchCallback? = null

    /** True when any camera on this device reports a flash unit. */
    fun isAvailable(): Boolean = findTorchCameraId() != null

    /** Begins tracking torch state. Safe to call more than once. */
    fun start() {
        if (torchCallback != null) return
        val id = findTorchCameraId() ?: return
        cameraId = id

        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(changedId: String, enabled: Boolean) {
                if (changedId == id) _state.value = enabled
            }

            override fun onTorchModeUnavailable(changedId: String) {
                // Raised while another app holds the camera. The torch is off in that state, and
                // setTorchMode will fail until it is released.
                if (changedId == id) _state.value = false
            }
        }
        torchCallback = callback
        cameraManager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun setEnabled(enabled: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, enabled)
        } catch (e: CameraAccessException) {
            // Typically another app holding the camera, or a thermal block. Nothing to correct
            // here: the callback above reports what the torch is actually doing.
            Log.w(TAG, "Could not switch torch ${if (enabled) "on" else "off"}: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Camera $id no longer supports a torch: ${e.message}")
        }
    }

    /** Turns the torch off and stops tracking it, so it is never left lit after a shutdown. */
    fun close() {
        setEnabled(false)
        torchCallback?.let {
            runCatching { cameraManager.unregisterTorchCallback(it) }
        }
        torchCallback = null
        cameraId = null
        _state.value = false
    }

    /**
     * Picks the camera whose flash to drive.
     *
     * Chosen by flash availability rather than by the configured capture lens: the torch is a light
     * source, not the lens being captured from, and on the overwhelming majority of devices only
     * the back camera reports a flash unit at all. Back is preferred when several qualify, and any
     * camera with a flash is accepted rather than insisting on a particular facing — the same
     * reasoning as the front/back fallback in the capture path.
     */
    private fun findTorchCameraId(): String? {
        return try {
            val withFlash = cameraManager.cameraIdList.filter { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (withFlash.isEmpty()) return null
            withFlash.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: withFlash.first()
        } catch (e: CameraAccessException) {
            Log.w(TAG, "Could not enumerate cameras for a torch: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Could not read camera characteristics for a torch: ${e.message}")
            null
        }
    }
}
