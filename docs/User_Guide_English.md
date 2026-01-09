# Ava User Guide

> 🌟 Turn your Android device into a smart home control panel!

---

## 📱 What is Ava?

Ava is a voice assistant app based on the ESPHome protocol. It turns your Android device into a voice satellite for Home Assistant.

**What can you do with it?**
- 🎤 Control smart home devices with voice (lights, AC, music, etc.)
- 📺 Display beautiful screensavers and clocks
- 🌤️ Show real-time weather information
- 🔔 Display full-screen notifications (doorbell, alerts, etc.)
- 🎵 Play music with album cover display
- 📷 Take photos and stream video

Put your old phone or tablet at home, and it becomes a smart control panel!

**System Requirements:**
- Android 7.0 or higher
- Home Assistant connection required

---

## 🚀 Quick Start

### Step 1: Install

1. Download the Ava APK file
2. Tap to install
3. Allow all permission requests (microphone, overlay, etc.)

### Step 2: Connect to Home Assistant

**In Home Assistant:**
1. Go to **Settings** → **Devices & Services** → **Integrations**
2. Search and add **ESPHome** integration
3. Ava will be discovered automatically, tap **Configure**

**In Ava app:**
1. Open Ava app
2. Tap the **Settings** icon in the top right
3. Select **Voice Satellite**
4. Tap **Start Service**

**After successful connection:**
- Status bar will show "Connected"
- You can see this device in Home Assistant

### Step 3: Talk!

Say the wake word (default is "Hey Jarvis"), then say your command:

- "Turn on the living room light"
- "What time is it"
- "Play music"
- "What's the weather tomorrow"

---

## ⚙️ Features

### 🎤 Voice Control

**How it works:**
1. Ava constantly listens for the wake word
2. When you say the wake word, Ava starts recording
3. Recording is sent to Home Assistant for recognition
4. Home Assistant returns a response, Ava plays the voice

**How to use:**
1. Say the wake word (like "Hey Jarvis")
2. After hearing the prompt sound, say your command
3. Ava will answer you with voice

**Where to set:** Settings → Voice Satellite

**What you can set:**

| Setting | Description | Default |
|---------|-------------|--------|
| Device Name | Name shown in Home Assistant | device_model_voice_assistant |
| Port | ESPHome communication port | 6053 |
| Wake Word | Word that triggers voice recognition | Hey Jarvis |
| Stop Word | Word that interrupts current conversation | Stop |
| Wake Sound | Prompt sound when recording starts | Optional |
| Mute | Turn off microphone | Off |

**Supported Wake Words:**
- Hey Jarvis
- Alexa
- Hey Google
- OK Google
- And more...

---

### 🖼️ Screensaver

**What is it:** After the device is idle for a while, a beautiful screensaver shows automatically.

**Types of screensavers:**
- **Image screensaver**: Shows images you choose
- **Web screensaver**: Shows web content
- **Xiaomi Wallpaper**: Shows time, date (very beautiful!)

**Where to set:** Settings → Screensaver

**What you can set:**
| Option | Description |
|--------|-------------|
| Enable | Turn screensaver on or off |
| Timeout | How long before screensaver shows (like 30 seconds) |
| Type | Choose image or web |
| URL | If you choose web, enter the web address |

---

### 🌤️ Weather Display

**What is it:** Shows current weather information on screen.

**What it shows:**
- Temperature
- Weather condition (sunny, cloudy, rain, etc.)
- Humidity
- Wind direction and speed
- Air quality

**⚠️ Note:** Weather feature only supports cities in China!

**Where to set:** Settings → Interaction → Weather

---

### 🔔 Notification Scenes

**What is it:** When smart home events happen, shows beautiful full-screen notifications.

**Built-in Scenes:**

| Scene ID | Scene Name | Purpose |
|----------|------------|--------|
| morning | Good Morning | Daily greeting |
| doorbell | Doorbell Ring | Doorbell alert |
| water_leak | Water Leak | Water sensor triggered |
| smoke | Smoke Alarm | Smoke sensor triggered |
| someone_home | Someone Home | Presence detection |
| package | Package Arrived | Delivery notification |
| timer | Timer Finished | Timer reminder |
| weather_alert | Weather Alert | Severe weather warning |

**Where to set:** Settings → Interaction → Notification Scenes

**Settings:**
| Setting | Description |
|---------|-------------|
| Display Duration | How long notification shows (5-30 seconds) |
| Sound | Sound played when notification appears |
| Custom Scene URL | Load custom scenes from network |

**Trigger in Home Assistant:**
```yaml
service: esphome.your_device_name_notification_scene
data:
  scene: "doorbell"  # Scene ID
```

---

### 🎵 Music Playback

**What is it:** Ava can play music and show beautiful album covers.

**Special features:**
- Vinyl record style cover display
- Auto-fetch NetEase Music covers
- Voice announcements (TTS)

**Where to set:** Settings → Interaction

**Settings:**
| Setting | Description |
|---------|-------------|
| Volume | Media playback volume |
| Vinyl Cover | Show album cover when playing music |
| Conversation Subtitles | Show voice conversation text |

**Play music in Home Assistant:**
```yaml
service: media_player.play_media
target:
  entity_id: media_player.your_device_name
data:
  media_content_id: "http://example.com/music.mp3"
  media_content_type: "music"
```

---

### 📷 Camera

**What is it:** Use the device camera to take photos or record video.

**What it can do:**
- Take photos and send to Home Assistant
- Live video streaming (MJPEG format)
- Supports front and back camera

**Where to set:** Settings → Experimental → Camera

**Settings:**
| Setting | Description |
|---------|-------------|
| Enable Camera | Turn on camera feature |
| Use Front Camera | Default to front or back camera |
| Video Frame Rate | 1-15 fps |
| Video Resolution | 240p-720p |

**In Home Assistant:**
- Camera appears as a camera entity
- Add camera card to your dashboard

---

### 🌐 Built-in Browser

**What is it:** Display web pages inside Ava.

**What it can do:**
- Show Home Assistant dashboards
- Show any web page
- Inject custom CSS/JS
- Pull-to-refresh support

**Where to set:** Settings → Browser

**Settings:**
| Setting | Description |
|---------|-------------|
| Homepage URL | Default web page address |
| Render Mode | Hardware/Software rendering |
| Custom CSS | Inject custom styles |
| Custom JS | Inject custom scripts |

**Control in Home Assistant:**
```yaml
service: esphome.your_device_name_ha_remote_url
data:
  url: "http://your-ha-address:8123/lovelace/0"
```

---

## 🔧 FAQ

### Ava can't hear me?

**Check these:**
1. Is microphone permission granted?
   - Go to phone Settings → Apps → Ava → Permissions → Microphone → Allow
2. Is wake word set correctly?
   - Settings → Voice Satellite → Wake Words
3. Is the device muted?
   - Check volume buttons

---

### Can't connect to Home Assistant?

**Check these:**
1. Are device and Home Assistant on the same WiFi?
2. Is Home Assistant address correct?
3. Is ESPHome integration enabled in Home Assistant?

---

### Screensaver not showing?

**Check these:**
1. Is screensaver enabled?
   - Settings → Screensaver → Enable
2. Is overlay permission granted?
   - Go to phone Settings → Apps → Ava → Permissions → Overlay → Allow
3. Is timeout set?
   - Settings → Screensaver → Timeout

---

### Weather not showing?

**Check these:**
1. Did you select a city in China? (Only supports China cities)
2. Is the city selected correctly?
3. Is network connection working?

---

## 📋 Permissions

| Permission | Why needed |
|------------|-----------|
| Microphone | To hear you speak |
| Overlay | To show screensaver, notifications, weather, etc. |
| Camera | For photos and video |
| Internet | To connect to Home Assistant |
| Bluetooth | To detect if you're home |
| Location | Required for Bluetooth scanning |

---

## 🎨 Interface Guide

### Main Screen

```
┌─────────────────────────────┐
│                             │
│        12:34                │
│                             │
│   Thursday, January 9, 2026 │
│                             │
│    ☀️ Sunny 25°C            │
│                             │
│                             │
│                             │
└─────────────────────────────┘
```

### Settings Menu

```
Settings
├── Voice Satellite
│   ├── Name
│   ├── Port
│   └── Wake Words
├── Interaction
│   ├── Weather
│   ├── Notification Scenes
│   └── Floating Windows
├── Screensaver
│   ├── Enable
│   ├── Timeout
│   └── Type
├── Browser
│   ├── Homepage
│   └── Advanced
└── Experimental
    ├── Camera
    └── Sensors
```

---

## 💡 Tips

### 1. Choose a Good Wake Word

Choose a word that's easy to say but you don't often say in normal conversation. This way Ava won't trigger by accident.

### 2. Protect the Screen

If the device screen is on for long periods:
- Lower screen brightness
- Use screensaver (screensaver moves content to prevent burn-in)

### 3. Keep Network Stable

Good WiFi signal means better voice recognition accuracy.

### 4. Update Regularly

Check for app updates. New versions have more features and fixes.

---

## 📞 Get Help

Having problems? Get help here:

- **GitHub Issues**: https://github.com/knoop7/Ava/issues
- **Home Assistant Community**: https://community.home-assistant.io/

---

## 🙏 Credits

- Original Project: [brownard/Ava](https://github.com/brownard/Ava)
- ESPHome: https://esphome.io/
- Home Assistant: https://www.home-assistant.io/

---

*Last Updated: 2026-01-09*
