# System Requirements

This page lists the system requirements for running Ava.

## Why Choose Ava?

Compared to solutions like Fully Kiosk Browser, Ava has the following advantages:

| Comparison | Ava | Fully Kiosk |
|------------|-----|-------------|
| Memory Usage | 100-150 MB | 200-300 MB |
| Display Method | Overlay (better effect) | Embedded browser |
| Voice Assistant | Native support | Requires extra config |
| Price | Free & Open Source | Paid |

More info: https://www.fully-kiosk.com/

---

## Android Device Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| Android Version | 7.0 (API 24) | 10.0 or higher |
| CPU Architecture | armeabi-v7a | arm64-v8a |
| RAM | 1GB | 2GB or higher |
| Storage | 100MB | 200MB |

### A64 Device Special Optimization

Ava has special optimizations for A64 chip devices (such as some Allwinner tablets, smart screens, etc.):

- Automatic A64 device type detection
- Performance optimization for low-end hardware
- Support for older devices running Android 7.0-8.0

---

## Network Requirements

| Requirement | Description |
|-------------|-------------|
| WiFi | Device must be on same LAN as Home Assistant |
| Port | 6053 (ESPHome default port) must be accessible |
| Stability | Stable WiFi connection recommended |

---

## Home Assistant Requirements

| Component | Description |
|-----------|-------------|
| Home Assistant | Must be installed and running |
| ESPHome Integration | Must be installed in HA |
| Voice Assistant | Voice assistant pipeline required (optional) |

### Recommended Voice Assistant Components

| Component | Purpose | Description |
|-----------|---------|-------------|
| Whisper | Speech-to-Text | Runs locally, privacy protected |
| Piper | Text-to-Speech | Runs locally, fast |
| Home Assistant Conversation Agent | Intent Processing | Built-in, no extra config |

---

## Permission Requirements

### Required Permissions

| Permission | Purpose |
|------------|---------|
| RECORD_AUDIO | Microphone recording for voice input |
| SYSTEM_ALERT_WINDOW | Overlay for screensaver, notifications, etc. |
| FOREGROUND_SERVICE | Keep service running |
| INTERNET | Network access for HA communication |
| WAKE_LOCK | Keep device awake |

### Optional Permissions

| Permission | Purpose |
|------------|---------|
| CAMERA | Camera features |
| BLUETOOTH | Bluetooth presence detection |
| ACCESS_FINE_LOCATION | Required for Bluetooth scanning |
| WRITE_SETTINGS | Adjust screen brightness |

---

## Recommended Devices

### Devices Suitable as Control Panels

| Device Type | Pros | Cons |
|-------------|------|------|
| Old phones | Cheap, easy to obtain | Small screen |
| Tablets | Large screen, good display | Needs fixed placement |
| Touch Screen Speakers | Good audio, high quality microphone | Dedicated device |
| Android devices with screens | Designed for this purpose | May be expensive |

### Device Selection Tips

1. **Screen Size**: 7-10 inches is ideal for control panels
2. **Microphone**: Ensure good microphone quality for voice recognition
3. **Speaker**: Ensure acceptable speaker quality
4. **Touch Screen Speakers**: Supports touch screen speaker devices like Xiaomi Touch Screen Speaker

### Noise Cancellation Technology

Ava has built-in noise cancellation technology to improve voice recognition accuracy in noisy environments.

---

## Device Capability Detection

Ava automatically detects device capabilities and adjusts features based on hardware:

| Detection | Purpose |
|-----------|--------|
| Camera | Auto-detect front/back cameras |
| Light Sensor | Light sensor screen off feature |
| Proximity Sensor | Proximity sensor wake feature |
| Temperature/Humidity/Pressure Sensors | Environment sensor data |
| A64 Device Type | Low-end device optimization |

---

## Known Compatibility Issues

| Issue | Affected Devices | Solution |
|-------|-----------------|----------|
| WebView rendering issues | Some old devices | Switch to software rendering mode |
| Inaccurate wake word detection | Devices with poor microphones | Speak closer to device |
| Overlay permission | Some custom ROMs | Manually grant in settings |

---

*Back to [Home](Home.md)*
