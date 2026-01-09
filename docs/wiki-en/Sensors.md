# Sensors

Ava can read device environment sensor data and expose it to Home Assistant.

---

## Supported Sensors

| Sensor Type | Description | Unit |
|-------------|-------------|------|
| Light Sensor | Ambient light intensity | lux |
| Magnetic Sensor | Magnetic field strength | μT |
| Proximity Sensor | Object proximity detection | cm or binary |

---

## Light Sensor

### Features

- Measures ambient light intensity
- Used for light sensor screen off feature
- Real-time data updates

### Home Assistant Entity

```
sensor.your_device_name_light_level
```

### Use Cases

**Auto-adjust lighting:**
```yaml
automation:
  - alias: "Auto Light Adjustment"
    trigger:
      - platform: numeric_state
        entity_id: sensor.ava_light_level
        below: 50
    action:
      - service: light.turn_on
        target:
          entity_id: light.living_room
```

---

## Magnetic Sensor

### Features

- Measures ambient magnetic field strength
- Calculates combined value from three axes
- Can be used to detect metal objects

### Home Assistant Entity

```
sensor.your_device_name_magnetic_field
```

### Technical Implementation

Magnetic field magnitude calculation:
```
magnitude = sqrt(x² + y² + z²)
```

---

## Proximity Sensor

### Features

- Detects if objects are close to device
- Used for proximity sensor wake feature
- Typically used to detect face approaching

### Home Assistant Entity

```
binary_sensor.your_device_name_proximity
```

### Use Cases

**Wake screen when someone approaches:**
```yaml
automation:
  - alias: "Proximity Wake"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_proximity
        to: "on"
    action:
      - service: switch.turn_off
        target:
          entity_id: switch.ava_screensaver
```

---

## Device Capability Detection

Ava automatically detects if device has these sensors:

| Detection Method | Description |
|------------------|-------------|
| hasLightSensor | Has light sensor |
| hasMagneticSensor | Has magnetic sensor |
| hasProximitySensor | Has proximity sensor |
| hasAnySensor | Has any sensor |

If device lacks a sensor, the corresponding entity will not be created.

---

## Technical Implementation

### Reactive Data Flow

Ava uses Kotlin StateFlow for reactive sensor data updates:

```kotlin
private val _lightLevel = MutableStateFlow(0f)
val lightLevel: StateFlow<Float> = _lightLevel.asStateFlow()
```

### Sensor Sampling Rate

| Sensor | Sampling Rate |
|--------|---------------|
| Light | SENSOR_DELAY_FASTEST |
| Magnetic | SENSOR_DELAY_NORMAL |
| Proximity | SENSOR_DELAY_NORMAL |

Light sensor uses fastest sampling rate to ensure responsive light sensor screen off feature.

---

## FAQ

### Sensor data not updating?

1. Check if device has that sensor
2. Check if Ava service is running
3. Some device sensors may be restricted by system

### Light sensor screen off not working?

1. Confirm device has light sensor
2. Check if light sensor screen off is enabled in settings
3. Default threshold is 2 lux

### Proximity sensor wake not working?

1. Confirm device has proximity sensor
2. Check if proximity sensor wake is enabled in settings
3. Check proximity sensor's maximum range

---

*Back to [Home](Home.md)*
