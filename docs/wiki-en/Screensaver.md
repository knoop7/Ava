# Screensaver

The screensaver system displays beautiful content when your device is idle, while protecting the screen.

---

## Overview

When the device is idle for a period of time, Ava automatically displays a screensaver. Touch the screen to exit.

**Screensaver Types:**
- Xiaomi Wallpaper (recommended)
- Web Screensaver
- Image Screensaver

---

## Xiaomi Wallpaper

Xiaomi Wallpaper is Ava's built-in beautiful screensaver, mimicking Xiaomi phone's wallpaper style.

### Display Content

```
┌─────────────────────────────────────┐
│                                     │
│           12:34                     │
│                                     │
│     Thursday, January 9, 2026       │
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

### Features by Language

| Feature | Chinese Environment | English Environment |
|---------|--------------------|--------------------|
| Time | 24-hour format | 24-hour format |
| Date | 2026年1月9日 星期四 | Thursday, January 9, 2026 |
| Poetry | Shown (changes on refresh) | Hidden |

### How to Enable

1. Go to **Settings** → **Screensaver**
2. Turn on **Screensaver** switch
3. Turn on **Xiaomi Wallpaper** switch
4. Set **Timeout** (how long before screensaver shows)

---

## Web Screensaver

Web screensaver can display any web content, such as weather, calendar, dashboards, etc.

### Use Cases

- Display Home Assistant dashboard
- Display weather website
- Display custom web page

### How to Set Up

1. Go to **Settings** → **Screensaver**
2. Turn on **Screensaver** switch
3. Turn off **Xiaomi Wallpaper** switch
4. Enter web address in **Screensaver URL**
5. Set **Timeout**

### Recommended URLs

| URL | Description |
|-----|-------------|
| file:///android_asset/xiaomi_wallpaper.html | Built-in Xiaomi Wallpaper |
| http://your-ha:8123/lovelace/screensaver | HA screensaver dashboard |
| https://www.bing.com | Bing homepage |

---

## Image Screensaver

Image screensaver can display network images, supports MJPEG video streams.

### Supported Formats

- JPEG/JPG
- PNG
- GIF
- MJPEG video stream

### How to Set Up

Set image URL via Home Assistant service:

```yaml
service: esphome.your_device_name_screensaver_image
data:
  url: "http://example.com/image.jpg"
```

---

## Screensaver Controls

### Timeout

Set how long before screensaver shows when idle.

| Option | Description |
|--------|-------------|
| 15 seconds | Quick screensaver |
| 30 seconds | Default |
| 60 seconds | Longer wait |
| 120 seconds | Much longer wait |
| 300 seconds | 5 minutes |

### Touch to Wake

Touch anywhere on screen to exit screensaver.

### Light Sensor Screen Off

When ambient light is very low (e.g., lights off at night), automatically turn off screen to save power.

**How to Enable:**
1. Go to **Settings** → **Screensaver**
2. Turn on **Light Sensor Screen Off** switch

**How It Works:**
- Screen turns off when light below 2 lux
- Screen turns on when light restored

### Proximity Sensor Wake

When someone approaches the device, automatically wake the screen.

**How to Enable:**
1. Go to **Settings** → **Screensaver**
2. Turn on **Proximity Sensor Wake** switch

**How It Works:**
- Uses device's proximity sensor
- Wakes screen when object detected nearby

---

## Settings Summary

| Setting | Location | Description | Default |
|---------|----------|-------------|---------|
| Screensaver | Screensaver | Enable/disable screensaver | Off |
| Xiaomi Wallpaper | Screensaver | Use built-in Xiaomi Wallpaper | Off |
| Screensaver URL | Screensaver | Custom web screensaver address | Empty |
| Timeout | Screensaver | How long before screensaver shows | 30s |
| Light Sensor Screen Off | Screensaver | Auto screen off in dark | Off |
| Proximity Sensor Wake | Screensaver | Auto wake when approached | Off |

---

## Home Assistant Control

Screensaver is controlled via switch and text entities.

### Show/Hide Screensaver

```yaml
# Show screensaver
service: switch.turn_on
target:
  entity_id: switch.your_device_name_screensaver

# Hide screensaver
service: switch.turn_off
target:
  entity_id: switch.your_device_name_screensaver
```

### Set Screensaver Image URL

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_screensaver_image_url
data:
  value: "http://example.com/image.jpg"
```

---

## FAQ

### Screensaver not showing?

1. Check if screensaver switch is on
2. Check if overlay permission is granted
3. Check timeout setting
4. If using web screensaver, check if URL is correct

### Screensaver shows blank?

1. Check network connection
2. Check if URL is accessible
3. Try using built-in Xiaomi Wallpaper

### Light sensor not working?

1. Check if device has light sensor
2. Make sure sensor isn't blocked
3. Check permission settings

---

*Back to [Home](Home.md)*
