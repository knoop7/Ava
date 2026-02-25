# Bluetooth

Ava supports Bluetooth presence detection and Bluetooth proxy features.

---

## Feature Overview

### Bluetooth Presence Detection

- Detect if phones, wearables, and other Bluetooth devices are nearby
- Determine if user is home
- Trigger automation scenes

### Bluetooth Proxy (Key Feature)

Ava can act as a Bluetooth proxy, forwarding Bluetooth device data to Home Assistant.

**Features:**
- Supports 5 device slots
- Native integration with Home Assistant Bluetooth integration
- Extends Bluetooth coverage range

> **Note:** The Bluetooth proxy feature source code is not open source and is only available in release builds. All other feature code is available on GitHub.

---

## How It Works

### BLE Scanning

Ava uses Bluetooth Low Energy (BLE) scanning to detect devices:

1. Periodically scans for nearby Bluetooth devices
2. Matches target devices by MAC address
3. Updates presence status to Home Assistant

### Presence Determination

| Status | Condition |
|--------|-----------|
| Home | Target device detected |
| Away | Device not detected for multiple consecutive scans |

---

## Settings

Go to **Settings** -> **Bluetooth**

### Presence Detection Settings

| Setting | Description | Default |
|---------|-------------|--------|
| Enable Bluetooth Detection | Turn on Bluetooth presence detection | Off |
| Target Device | MAC address of Bluetooth device to detect | Empty |
| Scan Interval | Scanning frequency | 30s |

### Bluetooth Proxy Settings

| Setting | Description |
|---------|-------------|
| Enable Proxy | Turn on Bluetooth proxy feature |
| Device Slot 1-5 | Configure Bluetooth devices to proxy |

---

## Permission Requirements

Bluetooth features require these permissions:

| Permission | Description |
|------------|-------------|
| BLUETOOTH | Basic Bluetooth permission |
| BLUETOOTH_SCAN | Bluetooth scanning permission (Android 12+) |
| ACCESS_FINE_LOCATION | Location permission (required for Bluetooth scanning) |

---

## Home Assistant Integration

### Presence Entity

```
binary_sensor.your_device_name_bluetooth_presence
```

### Bluetooth Proxy Integration

Bluetooth proxy natively integrates with Home Assistant's Bluetooth integration:

1. Add Bluetooth integration in HA
2. Ava will be automatically discovered as a Bluetooth proxy
3. Bluetooth devices proxied through Ava will appear in HA

**Supported Device Types:**
- Bluetooth thermometers/hygrometers
- Bluetooth scales
- Bluetooth plant sensors
- Other BLE devices

### Automation Examples

**Turn on lights when arriving home:**
```yaml
automation:
  - alias: "Arrival Lights"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_bluetooth_presence
        to: "on"
    action:
      - service: light.turn_on
        target:
          entity_id: light.living_room
```

**Turn off all devices when leaving:**
```yaml
automation:
  - alias: "Departure Shutdown"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_bluetooth_presence
        to: "off"
        for:
          minutes: 5
    action:
      - service: homeassistant.turn_off
        target:
          entity_id: group.all_lights
```

---

## FAQ

### Bluetooth detection inaccurate?

1. Ensure target device Bluetooth is on
2. Check if MAC address is correct
3. Adjust scan interval
4. Ensure location permission is granted

### High battery usage?

1. Increase scan interval (e.g., 60 seconds)
2. Only enable Bluetooth detection when needed

### Cannot scan device?

1. Check Bluetooth permissions
2. Check location permission (required by Android)
3. Ensure target device is discoverable

---

*Back to [Home](Home.md)*
