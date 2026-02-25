# Notification Scenes

Notification scenes are a signature feature of Ava, displaying beautiful full-screen animated notifications when smart home events occur.

---

## Overview

Notification scenes display full-screen animated notifications including:
- Aurora animation background
- Icon animation
- Title and description text
- Custom theme colors

---

## How It Works

### Scene Loading

Scenes are loaded from the network based on language:
- Chinese: `scenes_zh.json`
- English: `scenes_en.json`

Scene files are hosted on GitHub and downloaded automatically when Ava starts.

### Triggering Scenes

Scenes are triggered through a dropdown select entity in Home Assistant:

```yaml
service: select.select_option
target:
  entity_id: select.your_device_name_notification_scene
data:
  option: "Doorbell Ring"
```

The scene options are a dropdown list containing all available scene titles.

---

## Scene Settings

Go to **Settings** -> **Notification**

| Setting | Description | Default |
|---------|-------------|---------|
| Display Duration | How long notification shows | 10s |
| Sound | Play sound when showing | On |
| Custom Scene URL | URL to load custom scenes | Empty |

---

## Custom Scenes

You can create your own scenes by hosting a JSON file.

### JSON Format

Custom scenes must be a JSON array:

```json
[
  {
    "id": "my_scene",
    "icon": "fa-bell",
    "iconColor": "#fbbf24",
    "title": "My Scene",
    "desc": "Description text",
    "subDesc": "Sub description",
    "themeColors": ["#f59e0b", "#d97706", "#92400e"],
    "beamColor": "rgba(251, 191, 36, 0.8)",
    "dividerColor": "rgba(251, 191, 36, 0.8)",
    "dotColor": "#fcd34d"
  }
]
```

### Required Fields

| Field | Description |
|-------|-------------|
| id | Unique scene identifier |
| icon | FontAwesome icon name (e.g., fa-bell) |
| title | Scene title text |

### Optional Fields

| Field | Description | Default |
|-------|-------------|---------|
| iconColor | Icon color (hex or Tailwind) | text-amber-200 |
| desc | Description text | Empty |
| subDesc | Sub description text | Empty |
| themeColors | Array of theme colors | Amber gradient |
| beamColor | Light beam color | Amber |
| dividerColor | Divider line color | Amber |
| dotColor | Dot indicator color | Amber |

### Loading Custom Scenes

**Method 1: Using External URL**
1. Host your JSON file on a web server
2. Go to **Settings** -> **Notification**
3. Enter the URL in **Custom Scene URL**
4. Custom scenes will have `custom_` prefix added to their IDs

**Method 2: Using Home Assistant Local Files**
1. Place your JSON file in Home Assistant's `www` folder
2. Use URL: `http://your-ha-ip:8123/local/my_scenes.json`
3. This allows fast loading within local network without external server

---

## Automation Examples

### Doorbell Notification

```yaml
automation:
  - alias: "Doorbell Notification"
    trigger:
      - platform: state
        entity_id: binary_sensor.doorbell
        to: "on"
    action:
      - service: select.select_option
        target:
          entity_id: select.ava_notification_scene
        data:
          option: "Doorbell Ring"
```

### Water Leak Alert

```yaml
automation:
  - alias: "Water Leak Alert"
    trigger:
      - platform: state
        entity_id: binary_sensor.water_leak
        to: "on"
    action:
      - service: select.select_option
        target:
          entity_id: select.ava_notification_scene
        data:
          option: "Water Leak"
```

---

## Supported Colors

Ava supports multiple color formats:

| Format | Example |
|--------|---------|
| Hex | #f59e0b |
| RGBA | rgba(251, 191, 36, 0.8) |
| Tailwind | amber-500, blue-400, etc. |

### Tailwind Color Names

Supported color names include:
- amber, yellow, orange, red, rose, pink
- fuchsia, purple, violet, indigo, blue
- sky, cyan, teal, emerald, green, lime
- stone, neutral, gray, slate

Each with levels: 200, 300, 400, 500, 600, 700

---

## FAQ

### Scenes not loading?

1. Check network connection
2. Scenes are downloaded from GitHub on startup
3. Try restarting Ava

### Custom scenes not showing?

1. Check if URL is accessible
2. Verify JSON format is correct
3. Required fields: id, icon, title
4. Custom scene IDs are prefixed with `custom_`

### How to hide notification?

Notifications auto-hide after the configured duration. You can also:
- Touch the notification to dismiss
- Use the hide service

---

*Back to [Home](Home.md)*
