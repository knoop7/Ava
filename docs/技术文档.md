# Ava Technical Documentation

## Project Overview

Ava is an Android voice assistant app based on ESPHome protocol that transforms Android devices into smart home control panels.

- **Minimum Support**: Android 7.0 (API 24)
- **Target Version**: Android 16 (API 36)
- **Architecture Support**: arm64-v8a, armeabi-v7a (32/64-bit)
- **Development Language**: Kotlin + C++ (JNI)

---

## Project Structure

```
Ava/
├── app/                          # Main application module
│   └── src/main/java/com/example/ava/
│       ├── audio/                # Audio input
│       ├── bluetooth/            # Bluetooth presence detection
│       ├── camera/               # Camera photo/video
│       ├── esphome/              # ESPHome protocol implementation
│       │   ├── entities/         # Entity type definitions
│       │   └── voicesatellite/   # Voice satellite core
│       ├── microwakeword/        # Wake word detection
│       ├── notifications/        # Notification scenes
│       ├── nsd/                  # Network service discovery
│       ├── players/              # Audio players
│       ├── sensor/               # Environmental sensors
│       ├── sensors/              # Diagnostic sensors
│       ├── server/               # TCP server
│       ├── services/             # Android services
│       ├── settings/             # Settings storage
│       ├── ui/                   # Jetpack Compose UI
│       ├── update/               # Auto update
│       ├── utils/                # Utility classes
│       ├── wakelocks/            # Wake locks
│       └── weather/              # Weather service
├── esphomeproto/                 # ESPHome Protobuf protocol
├── microfeatures/                # C++ audio frontend processing
│   └── src/main/cpp/
│       ├── MicroFrontend.cpp     # Audio feature extraction
│       ├── kissfft/              # FFT library
│       └── tensorflow/           # TensorFlow Lite
└── gradle/                       # Gradle configuration
```

---

## Core Modules

### 1. ESPHome Protocol Layer

#### EspHomeDevice.kt
ESPHome device base class, implements protocol communication:

```kotlin
abstract class EspHomeDevice(
    coroutineContext: CoroutineContext,
    name: String,
    port: Int = 6053,
    entities: Iterable<Entity>
)
```

- Manages TCP server connection
- Handles Protobuf message send/receive
- Maintains entity state subscription

#### Supported Entity Types

| Entity Type | File | Purpose |
|-------------|------|---------|
| MediaPlayerEntity | Media player control |
| CameraEntity | Camera image/video |
| SwitchEntity | Switch control |
| SensorEntity | Numeric sensor |
| BinarySensorEntity | Binary sensor |
| ButtonEntity | Button trigger |
| NumberEntity | Number adjustment |
| SelectEntity | Option selection |
| TextEntity | Text input |
| TextSensorEntity | Text sensor |
| ServiceEntity | Service call |

### 2. Voice Satellite Core

#### VoiceSatellite.kt
Voice satellite main class, coordinates all features:

- Wake word detection and response
- Audio stream transmission
- TTS playback
- Entity registration and management
- Sensor data collection

#### VoiceSatelliteAudioInput.kt
Audio input processing:

- 16kHz sample rate
- Wake word/stop word detection
- Audio stream control

#### VoiceSatellitePlayer.kt
Audio playback management:

- TTS player
- Media player
- Wake prompt sound
- Volume control

### 3. Wake Word Detection

#### MicroFrontend.cpp (C++)
Audio feature extraction, based on TensorFlow Lite Micro Frontend:

```cpp
class MicroFrontend {
    FrontendConfig frontend_config;
    FrontendState frontend_state;
public:
    FrontendOutput ProcessSamples(int16_t *samples, size_t *num_samples_read);
};
```

Configuration parameters:
- Window size: 30ms
- Step: 10ms
- Filter channels: 40
- Frequency range: 125Hz - 7500Hz
- PCAN gain control
- Log scale

#### WakeWordDetector.kt
Wake word detector:

```kotlin
class WakeWordDetector(wakeWordProvider: WakeWordProvider) {
    fun detect(audio: ByteBuffer): List<DetectionResult>
    fun setActiveWakeWords(wakeWordIds: List<String>)
}
```

#### MicroWakeWord.kt
TensorFlow Lite model inference:

- Load .tflite model
- Sliding window probability calculation
- Threshold judgment

### 4. Bluetooth Presence Detection

#### BluetoothPresenceManager.kt
Bluetooth device tracking and presence detection:

```kotlin
class BluetoothPresenceManager {
    val trackedDevices: StateFlow<Map<String, TrackedDevice>>
    val devicePresence: StateFlow<Map<String, Boolean>>
    
    fun onDeviceScanned(address: String, rssi: Int)
    fun checkTimeouts()
}
```

Features:
- BLE scanning and device tracking
- RSSI threshold judgment
- Away delay detection
- Classic Bluetooth SDP query
- BLE advertising (for iOS device detection)

Configuration parameters:
- RSSI threshold: -120 ~ 0 dBm
- Away delay: 5 ~ 3600 seconds
- Scan interval: 6 seconds

### 5. Camera Module

#### CameraCapture.kt
Static photo capture:

```kotlin
class CameraCapture(context: Context) {
    suspend fun capturePhoto(
        useFrontCamera: Boolean = false,
        targetSize: Int = 500
    ): ByteArray?
}
```

- Based on CameraX
- Supports front and rear cameras
- Auto rotation correction
- Square cropping and scaling

#### VideoCapture.kt
Real-time video stream:

- Adjustable frame rate (1-15 fps)
- Adjustable resolution (240-720p)
- JPEG frame output

### 6. Sensor Module

#### EnvironmentSensorManager.kt
Environmental sensor management:

```kotlin
class EnvironmentSensorManager(context: Context) {
    val lightLevel: StateFlow<Float>      // Light (lux)
    val magneticField: StateFlow<Float>   // Magnetic field (μT)
    val proximity: StateFlow<Float>       // Proximity distance
}
```

#### DiagnosticSensorManager.kt
Diagnostic information collection:

- WiFi signal strength
- Device IP address
- Storage space
- Memory usage
- Battery level/voltage
- Charging status
- Uptime

### 7. Service Layer

| Service | Function |
|---------|----------|
| VoiceSatelliteService | Main service, manages voice satellite lifecycle |
| DreamClockService | Dream clock floating window |
| WeatherOverlayService | Weather info floating window |
| VinylCoverService | Vinyl record cover floating window |
| FloatingWindowService | Conversation subtitle floating window |
| NotificationOverlayService | Notification scene fullscreen overlay |
| ScreensaverService | Image screensaver |
| ScreensaverWebViewService | Web screensaver |
| WakeAnimationService | Wake animation |
| WebViewService | Built-in browser |

### 8. Network Service Discovery

#### VoiceSatelliteNsd.kt
mDNS service registration:

```kotlin
fun registerVoiceSatelliteNsd(
    context: Context,
    name: String,
    port: Int,
    macAddress: String
): NsdRegistration
```

Service type: `_esphomelib._tcp`

Attributes:
- version: 2025.9.0
- mac: Device MAC address
- board: host
- platform: HOST
- network: wifi

### 9. Auto Update

#### AppUpdater.kt
App auto update:

```kotlin
object AppUpdater {
    suspend fun checkUpdate(context: Context): UpdateInfo?
    fun downloadAndInstall(context: Context, updateInfo: UpdateInfo)
}
```

- Check version from GitHub
- Download APK
- Auto install

### 10. Audio Processing Enhancement

#### GpioAecController.kt
Hardware echo cancellation control:

```kotlin
object GpioAecController {
    fun activateAEC(): Boolean      // Activate echo cancellation
    fun activateBeamforming(): Boolean  // Activate beamforming
}
```

Supported devices:
- Ococci A64 (GPIO116)
- Holi platform (GPIO144)

---

## Settings Storage

Uses Jetpack DataStore for persistent settings:

| Settings File | Content |
|---------------|---------|
| VoiceSatelliteSettings | Satellite name, port, MAC address |
| MicrophoneSettings | Wake word, stop word, mute state |
| PlayerSettings | Volume, wake sound, floating window toggle |
| ExperimentalSettings | Camera, sensors, diagnostic features |
| NotificationSettings | Scene display duration, custom scene URL |
| ScreensaverSettings | Screensaver toggle, URL, timeout |
| BrowserSettings | Advanced control, JS/CSS injection |

---

## Dependencies

### Core Dependencies
- Kotlin Coroutines
- Jetpack Compose
- Protobuf
- TensorFlow Lite

### Media
- ExoPlayer (Media3)
- CameraX

### Others
- Shizuku (Root-free permissions)
- Gson
- DataStore

---

## Build Configuration

```kotlin
android {
    compileSdk = 36
    minSdk = 24
    targetSdk = 36
    
    ndk {
        abiFilters.add("arm64-v8a")
        abiFilters.add("armeabi-v7a")
    }
}
```

---

## Communication Protocol

### ESPHome Native API

Binary protocol based on Protobuf, default port 6053.

Message types:
- HelloRequest/Response
- ConnectRequest/Response
- DeviceInfoRequest/Response
- ListEntitiesRequest/Response
- SubscribeStatesRequest
- VoiceAssistantRequest/Response
- VoiceAssistantAudio

### Audio Format

- Sample rate: 16000 Hz
- Bit depth: 16-bit
- Channels: Mono
- Encoding: PCM

---

## Permission Requirements

### Required Permissions
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.INTERNET" />
```

### Optional Permissions
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
```

---

## Internationalization Support

### Language Adaptive Detection

App uses multiple criteria to detect user language environment:

```kotlin
private fun isChinese(): Boolean {
    val locale = java.util.Locale.getDefault()
    val language = locale.language.lowercase()
    val country = locale.country.uppercase()
    val timezone = java.util.TimeZone.getDefault().id
    return language.startsWith("zh") || 
        country in listOf("CN", "TW", "HK", "MO") ||
        timezone.startsWith("Asia/Shanghai") || 
        timezone.startsWith("Asia/Chongqing") ||
        timezone.startsWith("Asia/Hong_Kong") ||
        timezone.startsWith("Asia/Taipei")
}
```

### Adaptive Content

| Feature | Chinese Environment | English Environment |
|---------|---------------------|---------------------|
| Xiaomi Wallpaper Date | 2025年1月9日 星期四 | Thursday, January 9, 2025 |
| Xiaomi Wallpaper Poetry | Show | Hide |
| Weather Condition | 晴/多云/雨 | Sunny/Cloudy/Rain |
| Weather Card Labels | 湿度/空气/气压 | Humidity/Air/Pressure |
| Wind Direction | 北风/东北风 | N/NE |
| Scene JSON | scenes_zh.json | scenes_en.json |
| Weather Feature Toggle | Show | Hide |

### Scene JSON Network Loading

```kotlin
private const val SCENES_URL_ZH = "https://ghfast.top/.../scenes_zh.json"
private const val SCENES_URL_EN = "https://ghfast.top/.../scenes_en.json"

fun loadFromAssets(context: Context, onComplete: (() -> Unit)? = null) {
    loadFromNetwork(onComplete)
}
```

---

## Screensaver System

### Xiaomi Wallpaper (xiaomi_wallpaper.html)

WebView screensaver, mimics Xiaomi wallpaper style:

- Full-screen clock display
- Date and weekday
- Ancient poetry display (Chinese environment)
- Language adaptive

### Screensaver Controller (ScreensaverController.kt)

Unified screensaver lifecycle management:

- Image screensaver (ScreensaverService)
- Web screensaver (ScreensaverWebViewService)
- Timeout auto start
- Touch wake

---

## WebView Stability

### Renderer Crash Handling

```kotlin
override fun onRenderProcessGone(
    view: WebView?,
    detail: RenderProcessGoneDetail?
): Boolean {
    // Destroy old WebView
    webView?.let { wv ->
        (wv.parent as? ViewGroup)?.removeView(wv)
        wv.destroy()
    }
    // Delayed rebuild
    Handler(Looper.getMainLooper()).postDelayed({
        recreateWebView()
    }, 500)
    return true
}
```

### Correct Destruction Order

```kotlin
private fun hideWebView() {
    webView?.let { wv ->
        wv.stopLoading()
        wv.onPause()
        wv.pauseTimers()
        wv.loadUrl("about:blank")
        (wv.parent as? ViewGroup)?.removeView(wv)
        wv.destroy()
    }
}
```

### Renderer Priority

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    webView.setRendererPriorityPolicy(
        WebView.RENDERER_PRIORITY_IMPORTANT, 
        false
    )
}
```

---

## Notification Scene System

### SceneData.kt

Scene data management:

```kotlin
data class NotificationScene(
    val id: String,
    val icon: String,
    val iconColor: String,
    val title: String,
    val desc: String,
    val subDesc: String,
    val themeColors: List<String>,
    val beamColor: String,
    val dividerColor: String,
    val dotColor: String,
    val animation: String
)
```

### NotificationOverlayService.kt

Fullscreen notification overlay:

- Aurora animation background
- Icon sunrise animation
- Custom theme colors
- Prompt sound playback

---

## Weather Service

### WeatherService.kt

China Weather Network API integration:

- City coordinate query (Baidu Map API)
- Real-time weather data
- Air quality index
- Wind direction and speed

### WeatherOverlayService.kt

Weather floating window:

- Temperature display
- Weather condition icon
- 6-grid info cards
- Particle animation effects

---

## Volume Control

### VolumeControlService.kt

System volume management:

- Media volume
- Notification volume
- Alarm volume
- Mute control

---

## Version History

Current version: 0.1.7

---

## Related Links

- Original Project: https://github.com/brownard/Ava
- This Project: https://github.com/knoop7/Ava
- ESPHome: https://esphome.io/
- Home Assistant: https://www.home-assistant.io/
