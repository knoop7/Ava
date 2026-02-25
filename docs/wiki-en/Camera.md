# Camera

Ava's camera feature provides high-performance video streaming to Home Assistant, with a completely redesigned architecture that outperforms alternatives like WallPanel and Fully Kiosk.

---

## Why Ava Camera is Different

### Comparison with Alternatives

| Feature | Ava | WallPanel | Fully Kiosk |
|---------|-----|-----------|-------------|
| Memory Usage | 100-150 MB | 150-200 MB | 200-300 MB |
| Video Latency | Low | High | Medium |
| Frame Dropping | Minimal | Frequent | Occasional |
| Architecture | Native ESPHome Protocol | HTTP MJPEG | HTTP MJPEG |
| Integration | Native HA Entity | Manual Config | Manual Config |

### Technical Advantages

**1. Native ESPHome Protocol**
- Direct binary streaming via ESPHome protocol
- No HTTP overhead
- Chunked transfer for optimal performance

**2. Smart Frame Management**
- Automatic frame dropping when network is busy
- Prevents buffer overflow and lag accumulation
- `isSending` flag ensures no frame overlap

**3. Efficient Memory Usage**
- Uses Kotlin Flow for reactive streaming
- Single replay cache prevents memory bloat
- Chunk size optimized at 1024 bytes

**4. Original Architecture Design**
- Built from scratch for ESPHome integration
- Not a wrapper around existing solutions
- Designed specifically for Home Assistant

---

## Features

### Static Photos

Capture single photos on demand.

**Technical Implementation:**
- JPEG compression with configurable quality
- Auto rotation correction based on device orientation
- Square cropping for consistent display

### Video Stream

Live MJPEG video streaming.

**Technical Implementation:**
- Chunked binary transfer (1024 bytes per chunk)
- Flow-based reactive streaming
- Automatic frame rate adjustment based on network

---

## Settings

Go to **Settings** -> **Experimental**

| Setting | Description | Default |
|---------|-------------|---------|
| Enable Camera | Turn on camera feature | Off |
| Use Front Camera | Default to front or back | Back |
| Video Frame Rate | Video stream frame rate | 5 fps |
| Video Resolution | Video stream resolution | 480p |

### Frame Rate Guide

| Frame Rate | Use Case |
|------------|----------|
| 1 fps | Monitoring, lowest bandwidth |
| 5 fps | General use, balanced |
| 10 fps | Need to see motion |
| 15 fps | Highest quality |

### Resolution Guide

| Resolution | Pixels | Bandwidth |
|------------|--------|-----------|
| 240p | 320x240 | Lowest |
| 360p | 480x360 | Low |
| 480p | 640x480 | Medium |
| 720p | 1280x720 | High |

---

## Home Assistant Integration

### Camera Entity

After enabling, a camera entity appears in Home Assistant:
- `camera.your_device_name`

### Dashboard Card

```yaml
type: picture-entity
entity: camera.your_device_name
camera_view: live
```

### Snapshot Service

```yaml
service: camera.snapshot
target:
  entity_id: camera.your_device_name
data:
  filename: /config/www/snapshot.jpg
```

---

## Performance Tips

### For Best Video Quality

1. Use 5G WiFi if available
2. Keep device close to router
3. Start with 480p, increase if stable

### For Low Bandwidth

1. Use 1-2 fps frame rate
2. Use 240p or 360p resolution
3. Use single image mode instead of stream

### Troubleshooting Lag

Unlike WallPanel which often shows lag even with adjustments, Ava's architecture prevents lag accumulation:

1. Frame dropping is automatic when network is busy
2. No buffer overflow issues
3. If lag occurs, it self-corrects quickly

---

## FAQ

### Camera not showing in HA?

1. Check if camera permission is granted
2. Check if camera feature is enabled in settings
3. Confirm Home Assistant is connected

### Video stream laggy?

1. Lower frame rate (try 1-2 fps)
2. Lower resolution (try 240p)
3. Check WiFi signal strength
4. Ava auto-adjusts, lag should self-correct

### Why is Ava faster than WallPanel?

WallPanel uses HTTP MJPEG streaming which has:
- HTTP protocol overhead
- No smart frame management
- Buffer accumulation issues

Ava uses native ESPHome binary protocol with:
- Direct binary streaming
- Smart frame dropping
- Flow-based reactive architecture

---

*Back to [Home](Home.md)*
