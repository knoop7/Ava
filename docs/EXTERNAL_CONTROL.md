# Ava External Control API

Ava provides a generic broadcast interface that allows external apps (such as Tasker, virtual button software, automation tools, etc.) to control voice assistant functions.

## Supported Actions

| Action | Function |
|--------|----------|
| `com.example.ava.ACTION_TOGGLE_MIC` | Toggle microphone mute state |
| `com.example.ava.ACTION_MUTE_MIC` | Mute microphone |
| `com.example.ava.ACTION_UNMUTE_MIC` | Unmute microphone |
| `com.example.ava.ACTION_WAKE` | Manually wake voice assistant |
| `com.example.ava.ACTION_STOP` | Stop current voice session |
| `com.example.ava.ACTION_START_SERVICE` | Start Ava service |
| `com.example.ava.ACTION_STOP_SERVICE` | Stop Ava service |

## Usage

### 1. ADB Command Line

```bash
# Toggle microphone mute
adb shell am broadcast -a com.example.ava.ACTION_TOGGLE_MIC

# Mute microphone
adb shell am broadcast -a com.example.ava.ACTION_MUTE_MIC

# Unmute microphone
adb shell am broadcast -a com.example.ava.ACTION_UNMUTE_MIC

# Manual wake
adb shell am broadcast -a com.example.ava.ACTION_WAKE

# Stop voice session
adb shell am broadcast -a com.example.ava.ACTION_STOP

# Start Ava service
adb shell am broadcast -a com.example.ava.ACTION_START_SERVICE

# Stop Ava service
adb shell am broadcast -a com.example.ava.ACTION_STOP_SERVICE
```

### 2. Tasker

1. Create new Task
2. Add Action: **System → Send Intent**
3. Configure:
   - **Action**: `com.example.ava.ACTION_TOGGLE_MIC` (or other Action)
   - **Package**: `com.example.ava`
   - **Target**: `Broadcast Receiver`

### 3. Virtual Button Software (Button Mapper, Key Mapper, etc.)

1. Select button to remap
2. Set action to "Send Broadcast" or "Intent"
3. Enter Action: `com.example.ava.ACTION_TOGGLE_MIC`

### 4. Other Automation Apps (MacroDroid, Automate, etc.)

Use "Send Broadcast" or "Send Intent" function, enter corresponding Action.

### 5. Android Code Call

```kotlin
// Kotlin
val intent = Intent("com.example.ava.ACTION_TOGGLE_MIC")
sendBroadcast(intent)
```

```java
// Java
Intent intent = new Intent("com.example.ava.ACTION_TOGGLE_MIC");
sendBroadcast(intent);
```

## Physical Button Support

Ava has built-in support for the following buttons:

| Button | Function | Device Limitation |
|--------|----------|-------------------|
| F12 | Toggle microphone mute | All devices |
| Volume Mute | Short press wake / Long press adjust brightness | A64 devices |
| Menu Key | Single click toggle overlay / Long press adjust brightness | A64 devices |

For LineageOS and similar systems, physical buttons can be remapped to F12 to control microphone mute.

## Notes

- Broadcast receiver is set to `exported="true"`, allowing external apps to call
- No special permissions required to send these broadcasts
- If Ava service is not running, broadcasts will be ignored
