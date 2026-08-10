# Camera

Ava streams live camera video and snapshots directly to Home Assistant via the native ESPHome protocol. No intermediate server, no HTTP bridge, no browser rendering — just a direct binary stream from the Android camera to HA.

Compatible with Android 5-16.

---

## Why Ava's Camera Is Smoother Than WallPanel

[WallPanel](https://github.com/thanksmister/wallpanel-android) is a popular Android kiosk app that also streams camera to Home Assistant. But users consistently report lag, stuttering, and frame accumulation on WallPanel. Here is why Ava does not have these problems:

### The Lag Problem in HTTP-Based Camera Apps

WallPanel and similar apps stream camera frames over HTTP. This approach has a fundamental flaw:

1. **HTTP buffering** — Each frame is sent as an HTTP response. Both the server and HA's client buffer the data. When the network is slow, frames queue up in multiple buffers
2. **No backpressure** — The camera produces frames at a fixed rate. If the network cannot keep up, frames accumulate in memory. The stream falls further and further behind real-time
3. **Multiple layers of latency** — The frame passes through HTTP, TCP, HA's HTTP client, HA's camera component, and the browser canvas. Each step adds delay
4. **Result** — After a few minutes, the "live" stream is showing frames from 10-30 seconds ago. The only fix is to restart the stream

### Ava's Approach — No Buffers, No Lag

Ava uses a fundamentally different streaming architecture:

| Aspect | WallPanel (HTTP) | Ava (Native ESPHome) |
|--------|-----------------|---------------------|
| Transport | HTTP multipart stream | Native ESPHome protocol |
| Buffering | Server + client buffers | No intermediate buffer |
| Backpressure | None — frames queue in memory | Smart frame dropping — old frames are discarded |
| Frame dropping | No — every frame is queued | Yes — only the latest frame is kept |
| Lag accumulation | Yes — buffers grow over time | No — the stream always shows current frames |

**The key difference:** Ava never lets frames accumulate. If the network is slow, the frame rate drops gracefully, but the frames you see are always current — never seconds behind. The stream self-corrects automatically. There is no growing delay, no need to restart.

---

## Features

### Static Photos

Capture single photos on demand from Home Assistant.

- Configurable image size (Original, 500px, 720px, 1080px square crop)
- Auto rotation correction based on device orientation
- Manual orientation override (Portrait, Portrait Flipped, Landscape, Landscape Flipped)
- Front or back camera selection
- Works on old devices with legacy camera hardware

### Video Stream

Live video streaming via native ESPHome protocol.

- Frame rate: 1-15 FPS (configurable)
- Resolution: 240p-720p (configurable)
- Smart frame dropping — never accumulates lag
- Automatic orientation handling with manual override
- Video recording state persists across app restarts

### Face Detection

On-device face detection. No cloud, no external API — all processing happens locally on the Android device.

- Draws bounding boxes around detected faces on the video stream
- Reports person detected as a binary sensor to HA
- Smoothing algorithm for stable detection (no flickering)

### Gender Detection

On-device gender classification for each detected face.

- Counts male and female faces separately
- Reports as separate sensors to HA
- Smoothing to prevent flickering
- Only runs when face detection is enabled

---

## Settings

Go to **Settings** -> **Advanced**

| Setting | Description | Default |
|---------|-------------|---------|
| Remote Camera | Allow HA to control camera remotely | Off |
| Camera Mode | Snapshot or Video mode | Snapshot |
| Camera Selection | Front or back camera | Front |
| Video FPS | Video stream frame rate (1-15) | 2 fps |
| Video Resolution | Video stream resolution (240-720) | 240p |
| Image Orientation | Auto, Portrait, Portrait Flipped, Landscape, Landscape Flipped | Auto |
| Image Size | Original, 500px, 720px, 1080px | 500px |
| Face Detection | Detect faces in video stream | Off |
| Show Face Box | Draw bounding box around detected faces | Off |

### Camera Mode

| Mode | Description |
|------|-------------|
| Snapshot Mode | HA triggers a snapshot on demand via button press |
| Video Mode | HA toggles recording on/off via switch; live stream while recording |

### Camera Selection

| Camera | Description |
|--------|-------------|
| Back Camera | Rear-facing camera |
| Front Camera | Front-facing camera (default — better for kiosk/dashboard use) |

### Frame Rate Guide

| Frame Rate | Use Case | Bandwidth |
|------------|----------|-----------|
| 1 fps | Static monitoring, lowest bandwidth | ~30 KB/s |
| 2 fps | Default, general monitoring | ~60 KB/s |
| 5 fps | General surveillance | ~150 KB/s |
| 10 fps | Motion monitoring | ~300 KB/s |
| 15 fps | Maximum smoothness | ~450 KB/s |

> Actual bandwidth depends on resolution, scene complexity, and JPEG compression. These are estimates for 480p.

### Resolution Guide

| Resolution | Pixels | Bandwidth | Use Case |
|------------|--------|-----------|----------|
| Smooth | 320x240 | Lowest | Low-bandwidth, battery saving |
| SD | 480x360 | Low | General monitoring |
| HD | 640x480 | Medium | Clear image, good balance |
| FHD | 960x720 | High | Maximum detail |

### Image Orientation

| Orientation | Description |
|-------------|-------------|
| Auto | Use device sensor to determine rotation |
| Portrait | Force 0° rotation |
| Portrait Flipped | Force 180° rotation |
| Landscape | Force 90° rotation |
| Landscape Flipped | Force 270° rotation |

### Image Size (Snapshot)

| Size | Description |
|------|-------------|
| Original | No crop, full sensor image |
| 500px | Center square crop, 500x500 |
| 720px | Center square crop, 720x720 |
| 1080px | Center square crop, 1080x1080 |

---

## Device Compatibility

Ava's camera works across a wide range of Android devices, from modern flagships to decade-old tablets.

### Standard Devices (Android 7+)

Full CameraX support with hardware-accelerated JPEG encoding. Works on:
- Google Pixel (all generations)
- Samsung Galaxy (all generations)
- Xiaomi / Redmi / Poco
- OnePlus / Oppo / Vivo
- Most modern Android devices

### Legacy Devices (Android 5-6)

Devices with older camera hardware are automatically detected and handled with special care:
- Retry logic for camera initialization failures
- Automatic fallback to closest available resolution

Tested on:
- Samsung Galaxy Tab S2
- Samsung Galaxy Tab A (2016)
- Amazon Fire Tablet (5th gen+)
- Various Android 5-6 era tablets

### Kiosk Devices (Meta Portal, etc.)

Some kiosk devices block background camera access. Ava automatically handles these restrictions:
- Meta Portal (all models)
- Other kiosk devices with similar camera restrictions
- Automatically detected — no manual configuration needed

### Devices Without Camera

Ava auto-detects camera availability. If no camera is present, camera entities are not created in HA.

---

## Home Assistant Integration

### Camera Entities

| Entity | Description | Mode |
|--------|-------------|------|
| `camera.your_device_name_camera_snapshot` | Snapshot camera | Snapshot |
| `camera.your_device_name_video_camera` | Video stream camera | Video |
| `button.your_device_name_take_snapshot` | Trigger snapshot | Snapshot |
| `switch.your_device_name_video_recording` | Toggle video recording | Video |
| `switch.your_device_name_torch` | Flashlight | Both |

### Flashlight

The flashlight is controlled through `CameraManager.setTorchMode()`, so it does not need a camera
session: it works with the camera closed, in either camera mode, and does not compete with
snapshots or the video stream for the camera.

Its state is reported from the system's torch callback rather than from the last command sent, so
it stays correct when something else switches the torch off — which happens when another app opens
that camera, or when the device gets too hot. Devices with no flash unit get no entity at all.

Use it as a light for night snapshots:

```yaml
actions:
  - action: switch.turn_on
    target:
      entity_id: switch.your_device_name_torch
  - delay: "00:00:01"
  - action: button.press
    target:
      entity_id: button.your_device_name_take_snapshot
  - action: switch.turn_off
    target:
      entity_id: switch.your_device_name_torch
```

### Face Detection Entities

| Entity | Description |
|--------|-------------|
| `binary_sensor.your_device_name_face_detected` | Person detected (occupancy class) |
| `sensor.your_device_name_male_count` | Number of male faces detected |
| `sensor.your_device_name_female_count` | Number of female faces detected |

### Dashboard Card

```yaml
type: picture-entity
entity: camera.your_device_name_video_camera
camera_view: live
show_name: false
show_state: false
```

### Picture Glance (with quick actions)

```yaml
type: picture-glance
entity: camera.your_device_name_video_camera
camera_view: live
entities:
  - entity: switch.your_device_name_video_recording
    name: Recording
  - entity: binary_sensor.your_device_name_face_detected
    name: Person
```

### Snapshot to File

```yaml
service: camera.snapshot
target:
  entity_id: camera.your_device_name_camera_snapshot
data:
  filename: /config/www/snapshots/front_door_{{ now().strftime('%Y%m%d_%H%M%S') }}.jpg
```

---

## Scenarios

### Security Camera with Person Detection

Combine video stream + face detection + notifications for a complete security camera:

```yaml
automation:
  - alias: "Person Detected - Send Snapshot"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_face_detected
        to: "on"
    action:
      - service: camera.snapshot
        target:
          entity_id: camera.ava_camera_snapshot
        data:
          filename: /config/www/snapshots/detection_{{ now().strftime('%Y%m%d_%H%M%S') }}.jpg
      - service: notify.mobile_app
        data:
          message: "Person detected"
          data:
            image: /api/snapshot_proxy/camera.ava_camera_snapshot
```

### Occupancy-Based Lighting

Turn on lights when a person is detected on camera, turn off when no one is there:

```yaml
automation:
  - alias: "Camera Occupancy - Lights On"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_face_detected
        to: "on"
    action:
      - service: light.turn_on
        target:
          entity_id: light.living_room
      - service: input_boolean.turn_on
        target:
          entity_id: input_boolean.room_occupied

  - alias: "Camera Occupancy - Lights Off"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_face_detected
        to: "off"
        for:
          minutes: 5
    action:
      - service: light.turn_off
        target:
          entity_id: light.living_room
      - service: input_boolean.turn_off
        target:
          entity_id: input_boolean.room_occupied
```

### Doorbell Simulation with Snapshot

Use a physical button or HA dashboard button to simulate a doorbell — capture a snapshot and send it to your phone:

```yaml
automation:
  - alias: "Doorbell Press"
    trigger:
      - platform: state
        entity_id: button.ava_take_snapshot
        to: "unavailable"
    action:
      - delay: 2
      - service: camera.snapshot
        target:
          entity_id: camera.ava_camera_snapshot
        data:
          filename: /config/www/doorbell.jpg
      - service: notify.mobile_app
        data:
          title: "Doorbell"
          message: "Someone is at the door"
          data:
            image: /local/doorbell.jpg
```

### Motion-Activated Recording

Start recording when motion is detected by a separate PIR sensor, stop after 5 minutes of no motion:

```yaml
automation:
  - alias: "Motion - Start Recording"
    trigger:
      - platform: state
        entity_id: binary_sensor.hallway_pir
        to: "on"
    action:
      - service: switch.turn_on
        target:
          entity_id: switch.ava_video_recording

  - alias: "No Motion - Stop Recording"
    trigger:
      - platform: state
        entity_id: binary_sensor.hallway_pir
        to: "off"
        for:
          minutes: 5
    action:
      - service: switch.turn_off
        target:
          entity_id: switch.ava_video_recording
```

### Time-Lapse Capture

Take a snapshot every hour and store it for time-lapse:

```yaml
automation:
  - alias: "Hourly Snapshot"
    trigger:
      - platform: time_pattern
        hours: "/1"
    action:
      - service: camera.snapshot
        target:
          entity_id: camera.ava_camera_snapshot
        data:
          filename: /config/www/timelapse/{{ now().strftime('%Y%m%d_%H') }}.jpg
```

### Combine with Proximity Sensor — Smart Display

When someone approaches (proximity), turn on the display and start the camera. When they leave, turn everything off:

```yaml
automation:
  - alias: "Approach - Smart Display On"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_proximity
        to: "on"
    action:
      - service: switch.turn_on
        target:
          entity_id: switch.ava_display
      - service: switch.turn_on
        target:
          entity_id: switch.ava_video_recording

  - alias: "Leave - Smart Display Off"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_proximity
        to: "off"
        for:
          minutes: 2
    action:
      - service: switch.turn_off
        target:
          entity_id: switch.ava_video_recording
      - service: switch.turn_off
        target:
          entity_id: switch.ava_display
```

### Combine with Voice — Snapshot on Wake Word

When Ava detects a wake word, capture a snapshot to see who is talking:

```yaml
automation:
  - alias: "Wake Word Snapshot"
    trigger:
      - platform: state
        entity_id: assist_satellite.ava
        to: "listening"
    action:
      - service: camera.snapshot
        target:
          entity_id: camera.ava_camera_snapshot
        data:
          filename: /config/www/wake_{{ now().strftime('%Y%m%d_%H%M%S') }}.jpg
```

### Combine with Bluetooth Presence — Known vs Unknown Person

If Bluetooth presence shows a family member is home, suppress the person detection alert. If no one is home, treat it as an intruder:

```yaml
automation:
  - alias: "Intruder Alert"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_face_detected
        to: "on"
    condition:
      - condition: state
        entity_id: binary_sensor.ava_bluetooth_presence
        state: "off"
    action:
      - service: camera.snapshot
        target:
          entity_id: camera.ava_camera_snapshot
        data:
          filename: /config/www/intruder_{{ now().strftime('%Y%m%d_%H%M%S') }}.jpg
      - service: notify.mobile_app
        data:
          title: "Intruder Alert"
          message: "Person detected but no family member is home"
          data:
            image: /local/intruder_latest.jpg
            priority: high
```

### Combine with Audio Events — Sound + Camera

When an audio event (glass breaking, alarm, baby crying) is detected, capture a snapshot to see what happened:

```yaml
automation:
  - alias: "Audio Event - Capture Camera"
    trigger:
      - platform: state
        entity_id: sensor.ava_audio_event
        to: "glass_breaking"
    action:
      - service: camera.snapshot
        target:
          entity_id: camera.ava_camera_snapshot
        data:
          filename: /config/www/audio_event_{{ now().strftime('%Y%m%d_%H%M%S') }}.jpg
      - service: notify.mobile_app
        data:
          title: "Glass Breaking Detected"
          message: "Audio event triggered camera capture"
          data:
            image: /local/audio_event_latest.jpg
```

### Gender-Based Automation

Count people by gender for analytics or adjust behavior:

```yaml
automation:
  - alias: "Log Visitor Count"
    trigger:
      - platform: state
        entity_id: sensor.ava_male_count
    action:
      - service: input_number.set_value
        target:
          entity_id: input_number.male_visitors_today
        data:
          value: "{{ states('input_number.male_visitors_today') | int + 1 }}"
```

### Multi-Camera Dashboard

Place multiple Ava devices in different rooms and view all cameras on one dashboard:

```yaml
type: grid
columns: 2
cards:
  - type: picture-entity
    entity: camera.living_room_ava_video_camera
    camera_view: live
  - type: picture-entity
    entity: camera.kitchen_ava_video_camera
    camera_view: live
  - type: picture-entity
    entity: camera.bedroom_ava_video_camera
    camera_view: live
  - type: picture-entity
    entity: camera.garage_ava_video_camera
    camera_view: live
```

---

## Performance Tips

### For Best Video Quality

1. Use 5G WiFi if available
2. Keep device close to router
3. Start with 480p, increase if stable
4. Use 5 FPS for smooth motion, 2 FPS for static scenes
5. Keep the device plugged in — video streaming is CPU-intensive

### For Low Bandwidth

1. Use 1-2 FPS frame rate
2. Use 240p resolution
3. Use snapshot mode instead of video stream
4. Only enable video recording when needed (use switch entity)

### For Battery Life

1. Use snapshot mode — camera only activates on demand
2. Lower FPS to 1-2
3. Lower resolution to 240p
4. Turn off face detection when not needed
5. Use proximity or Bluetooth presence to trigger recording only when someone is present

---

## FAQ

### Camera not showing in HA?

1. Check if camera permission is granted
2. Check if Remote Camera is enabled in Settings -> Advanced
3. Confirm Home Assistant is connected
4. Check if the device has a camera (Ava auto-detects)
5. For kiosk devices, ensure overlay permission is granted

### Video stream laggy?

Ava's stream should not accumulate lag. If you experience lag:
1. Lower frame rate (try 1-2 fps)
2. Lower resolution (try 240p)
3. Check WiFi signal strength
4. Check if another app is using the camera
5. Restart the Ava service — the stream will resume fresh

### Video stream shows "camera off" placeholder?

The video recording switch is off. Turn it on via:
- HA: `switch.turn_on` on the `video_recording` entity
- Ava app: Settings -> Advanced -> Camera Mode -> Video

### Face detection not working?

1. Enable Face Detection in Settings -> Advanced
2. Ensure camera mode is Video (face detection only works on video stream)
3. Check lighting conditions — the TFLite model needs reasonable light
4. Ensure the face is clearly visible and not too small in the frame
5. Face detection runs on-device — no internet needed

### Gender count keeps flickering?

Gender detection uses a 5-frame median smoothing window. If it still flickers:
1. Increase FPS to 5+ for more samples
2. Ensure good lighting
3. Faces at extreme angles may confuse the model

### Camera fails to start on old device?

1. Ava has legacy HAL support with retry logic — it should work on Android 5-6 devices
2. If the camera still fails, try switching to the front camera
3. Some very old devices may not support CameraX — try Snapshot mode instead of Video
4. Check if another app has exclusive camera access

### Camera works but video is dark?

1. This is normal on devices without auto-exposure in ImageAnalysis mode
2. Try a different camera (front vs back)
3. Ensure the scene is well-lit
4. Some devices limit exposure in background camera mode

### Can I use camera and voice at the same time?

Yes. Camera streaming and voice satellite run independently. The camera uses `ImageAnalysis` which does not conflict with audio recording.

### Can I use camera and Bluetooth media at the same time?

On most devices, yes. Camera and Bluetooth audio use different hardware paths. On some very low-end devices, heavy camera usage may cause Bluetooth audio stuttering.

---

*Back to [Home](Home)*
