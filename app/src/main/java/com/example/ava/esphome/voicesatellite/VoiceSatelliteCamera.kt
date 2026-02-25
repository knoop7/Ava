package com.example.ava.esphome.voicesatellite

import android.content.Context
import android.util.Log
import com.example.ava.R
import com.example.ava.esphome.EspHomeDevice
import com.example.ava.esphome.entities.ButtonEntity
import com.example.ava.esphome.entities.CameraEntity
import com.example.ava.esphome.entities.BinarySensorEntity
import com.example.ava.esphome.entities.SensorEntity
import com.example.ava.esphome.entities.SwitchEntity
import com.example.ava.settings.ExperimentalSettingsStore
import com.example.esphomeproto.api.EntityCategory
import com.example.ava.detection.initFaceDetector
import com.example.ava.detection.closeFaceDetector
import com.example.ava.detection.detectFacesAndDraw
import com.example.ava.detection.initGenderDetector
import com.example.ava.detection.closeGenderDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class VoiceSatelliteCamera(
    private val context: Context,
    private val scope: CoroutineScope,
    private val device: EspHomeDevice,
    private val experimentalSettingsStore: ExperimentalSettingsStore
) {
    private var cameraCapture: com.example.ava.camera.CameraCapture? = null
    private var cameraEntity: CameraEntity? = null
    private var videoCapture: com.example.ava.camera.VideoCapture? = null
    private var videoCameraEntity: CameraEntity? = null
    private var isRecording = false
    private var detectionEnabled = false
    private var personDetectedState = MutableStateFlow(false)
    private var personDetectedEntity: BinarySensorEntity? = null
    
    private var lastHasHuman = false
    private var faceBoxEnabled = true
    private var genderDetectionEnabled = false
    private var maleCountEntity: SensorEntity? = null
    private var femaleCountEntity: SensorEntity? = null
    private var lastMaleCount = -1
    private var lastFemaleCount = -1
    
    private val prefs = context.getSharedPreferences("video_recording_prefs", Context.MODE_PRIVATE)
    private val KEY_RECORDING_ENABLED = "recording_enabled"

    companion object {
        private const val TAG = "VoiceSatelliteCamera"
        
        fun hasSavedRecordingState(context: Context): Boolean {
            return context.getSharedPreferences("video_recording_prefs", Context.MODE_PRIVATE)
                .getBoolean("recording_enabled", false)
        }
        
        fun clearSavedRecordingState(context: Context) {
            context.getSharedPreferences("video_recording_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("recording_enabled", false).apply()
        }
    }

    fun initSnapshot() {
        cameraCapture = com.example.ava.camera.CameraCapture(context)
        cameraEntity = CameraEntity(
            key = 10,
            name = context.getString(R.string.entity_camera_snapshot),
            objectId = "camera_snapshot",
            icon = "mdi:camera",
            entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
        )
        
        device.addEntity(ButtonEntity(
            key = 11,
            name = context.getString(R.string.entity_take_snapshot),
            objectId = "take_snapshot",
            icon = "mdi:camera",
            entityCategory = EntityCategory.ENTITY_CATEGORY_NONE
        ) {
            scope.launch(Dispatchers.IO) {
                takeSnapshot()
            }
        })
        
        cameraEntity?.let { entity ->
            device.addEntity(entity)
            scope.launch(Dispatchers.IO) {
                entity.sendImage(com.example.ava.camera.VideoCapture.createPlaceholderFromAsset(context, "camera_off.png", 320, 240))
            }
        }
    }

    private suspend fun takeSnapshot() {
        val capture = cameraCapture ?: return
        val entity = cameraEntity ?: return
        
        Log.d(TAG, "Taking snapshot...")
        
        val settings = experimentalSettingsStore.get()
        val useFrontCamera = settings.cameraPosition == com.example.ava.settings.CameraPosition.FRONT.name
        val targetSize = settings.imageSize
        
        val imageData = capture.capturePhoto(useFrontCamera, targetSize)
        if (imageData != null) {
            entity.sendImage(imageData)
            Log.d(TAG, "Snapshot sent: ${imageData.size} bytes, targetSize: $targetSize")
        } else {
            Log.e(TAG, "Failed to capture snapshot")
        }
    }

    fun initVideo() {
        videoCapture = com.example.ava.camera.VideoCapture(context)
        videoCameraEntity = CameraEntity(
            key = 12,
            name = context.getString(R.string.entity_camera_video),
            objectId = "video_camera",
            icon = "mdi:video",
            entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
        )
        
        val savedRecordingState = prefs.getBoolean(KEY_RECORDING_ENABLED, false)
        val recordingStateFlow = MutableStateFlow(savedRecordingState)
        device.addEntity(SwitchEntity(
            key = 13,
            name = context.getString(R.string.entity_video_recording),
            objectId = "video_recording",
            icon = "mdi:record-rec",
            getState = recordingStateFlow,
            entityCategory = EntityCategory.ENTITY_CATEGORY_NONE,
            setState = { enabled ->
                recordingStateFlow.value = enabled
                prefs.edit().putBoolean(KEY_RECORDING_ENABLED, enabled).apply()
                scope.launch(Dispatchers.IO) {
                    when (enabled) {
                        true -> startVideoRecording()
                        false -> {
                            stopVideoRecording()
                            videoCameraEntity?.sendImage(
                                com.example.ava.camera.VideoCapture.createPlaceholderFromAsset(context, "camera_off.png", 320, 240)
                            )
                        }
                    }
                }
            }
        ))
        videoCameraEntity?.let { entity ->
            device.addEntity(entity)
            scope.launch(Dispatchers.IO) {
                if (savedRecordingState) {
                    Log.d(TAG, "Restoring video recording state: enabled")
                    startVideoRecording()
                } else {
                    entity.sendImage(com.example.ava.camera.VideoCapture.createPlaceholderFromAsset(context, "camera_off.png", 320, 240))
                }
            }
        }
    }
    
    private suspend fun startVideoRecording() {
        if (isRecording) return
        val capture = videoCapture ?: return
        val entity = videoCameraEntity ?: return
        
        Log.d(TAG, "Starting video recording...")
        isRecording = true
        
        val settings = experimentalSettingsStore.get()
        val useFrontCamera = settings.cameraPosition == com.example.ava.settings.CameraPosition.FRONT.name
        val fps = settings.videoFps.coerceIn(1, 15)
        val resolution = settings.videoResolution.coerceIn(240, 720)
        
        capture.startRecording(useFrontCamera, fps, resolution) { frameData ->
            val outputFrame = processFrameForDetection(frameData)
            entity.sendImage(outputFrame)
        }
    }
    
    private fun stopVideoRecording() {
        if (!isRecording) return
        val capture = videoCapture ?: return
        
        Log.d(TAG, "Stopping video recording...")
        isRecording = false
        capture.stopRecording()
    }

    fun initDetection() {
        Log.d(TAG, "initDetection called")
        val initialized = initFaceDetector(context)
        if (!initialized) {
            Log.e(TAG, "Failed to init face detector")
            return
        }
        
        initGenderDetector(context)
        genderDetectionEnabled = true
        
        personDetectedEntity = BinarySensorEntity(
            key = 21,
            name = context.getString(R.string.entity_person_detected),
            objectId = "face_detected",
            icon = "mdi:face-recognition",
            deviceClass = "occupancy",
            getState = personDetectedState
        )
        device.addEntity(personDetectedEntity!!)
        
        maleCountEntity = SensorEntity(
            key = 23,
            name = context.getString(R.string.entity_male_count),
            objectId = "male_count",
            icon = "mdi:face-man",
            entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
        )
        device.addEntity(maleCountEntity!!)
        
        femaleCountEntity = SensorEntity(
            key = 24,
            name = context.getString(R.string.entity_female_count),
            objectId = "female_count",
            icon = "mdi:face-woman",
            entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
        )
        device.addEntity(femaleCountEntity!!)
        
        detectionEnabled = true
    }
    
    fun setFaceBoxEnabled(enabled: Boolean) {
        faceBoxEnabled = enabled
    }
    
    fun processFrameForDetection(frameData: ByteArray): ByteArray {
        if (!detectionEnabled) {
            return frameData
        }
        val pair = detectFacesAndDraw(frameData, faceBoxEnabled, genderDetectionEnabled) ?: return frameData
        val (annotatedFrame, result) = pair
        
        if (result.hasFace != lastHasHuman) {
            personDetectedState.value = result.hasFace
            lastHasHuman = result.hasFace
        }
        
        if (result.maleCount != lastMaleCount) {
            maleCountEntity?.updateState(result.maleCount.toFloat())
            lastMaleCount = result.maleCount
        }
        if (result.femaleCount != lastFemaleCount) {
            femaleCountEntity?.updateState(result.femaleCount.toFloat())
            lastFemaleCount = result.femaleCount
        }
        
        return annotatedFrame
    }

    fun closeDetection() {
        personDetectedEntity?.let { device.removeEntity(it) }
        maleCountEntity?.let { device.removeEntity(it) }
        femaleCountEntity?.let { device.removeEntity(it) }
        personDetectedEntity = null
        maleCountEntity = null
        femaleCountEntity = null
        detectionEnabled = false
        genderDetectionEnabled = false
        closeFaceDetector()
        closeGenderDetector()
        lastHasHuman = false
        lastMaleCount = -1
        lastFemaleCount = -1
        personDetectedState.value = false
    }
    
    fun close() {
        stopVideoRecording()
        closeDetection()
        cameraCapture = null
        videoCapture = null
    }
}
