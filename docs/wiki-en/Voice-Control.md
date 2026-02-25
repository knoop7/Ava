# Voice Control

Voice control is the core feature of Ava, allowing you to control smart home devices by speaking.

---

## How It Works

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  You speak  │ -> │ Ava records │ -> │Home Assistant│ -> │ Ava plays   │
│ wake word + │    │ sends audio │    │   speech    │    │   voice     │
│   command   │    │             │    │ recognition │    │   reply     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

**Detailed Flow:**

1. **Standby**: Ava continuously listens for wake word (local processing, no internet)
2. **Wake Detection**: When wake word detected, plays prompt sound, starts recording
3. **Audio Transmission**: Recording sent to Home Assistant via ESPHome protocol
4. **Speech Recognition**: Home Assistant's voice assistant performs speech-to-text
5. **Intent Processing**: Home Assistant understands intent and executes action
6. **Speech Synthesis**: Home Assistant generates voice response
7. **Playback**: Ava receives and plays voice response

---

## Wake Words

Wake words trigger voice recognition. Ava uses local wake word detection, no internet required.

### Supported Wake Words

| Wake Word | Description |
|-----------|-------------|
| Hey Jarvis | Default, Iron Man style |
| Alexa | Amazon style |
| Hey Google | Google style |
| OK Google | Google style |
| Hey Mycroft | Mycroft style |
| Hey Siri | Apple style (testing only) |

### How to Change Wake Word

1. Open Ava app
2. Go to **Settings** → **Voice Satellite**
3. Find **Wake Word** option
4. Select your preferred wake word
5. New wake word takes effect immediately

### Wake Word Detection Technology

Ava uses **microWakeWord** for local wake word detection:

- Based on TensorFlow Lite models
- 16kHz sample rate, 16-bit audio
- Sliding window probability calculation
- Fully local processing, privacy protected

---

## Stop Words

Stop words interrupt the current conversation or stop Ava's response.

### Supported Stop Words

| Stop Word | Description |
|-----------|-------------|
| Stop | Default |
| Never mind | Cancel |
| Cancel | Cancel |

### Use Cases

- Ava is playing a long response, you want to interrupt
- You made a mistake, want to start over
- You changed your mind, don't want to execute command

---

## Wake Sound

Wake sound is played when Ava starts recording, letting you know you can start speaking.

### Settings

| Option | Description |
|--------|-------------|
| On | Play prompt sound |
| Off | Silent recording start |
| Custom | Choose your own sound file |

### How to Set

1. Go to **Settings** → **Voice Satellite**
2. Find **Wake Sound** option
3. Choose on/off, or select custom audio file

---

## Mute Mode

Mute mode turns off the microphone, Ava won't respond to any wake words.

### Use Cases

- In meetings, don't want interruptions
- Watching movies, avoid false triggers
- Temporarily disable voice features

### How to Enable

**Method 1: In Settings**
1. Go to **Settings** → **Voice Satellite**
2. Turn on **Mute** switch

**Method 2: Home Assistant Control**
```yaml
service: switch.turn_on
target:
  entity_id: switch.your_device_name_mute
```

---

## Continuous Conversation

Continuous conversation lets you issue multiple commands without saying the wake word each time.

### How It Works

1. Say wake word + first command
2. After Ava responds, automatically enters listening mode
3. Say next command directly (no wake word needed)
4. After a few seconds of silence, exits continuous conversation mode

### How to Enable

1. Go to **Settings** → **Interaction**
2. Turn on **Continuous Conversation** switch

---

## Conversation Subtitles

Conversation subtitles display what you said and Ava's response on screen.

### Display Content

- **Your speech**: Displayed at top of screen
- **Ava's response**: Displayed at bottom of screen

### How to Enable

1. Go to **Settings** → **Interaction**
2. Turn on **Conversation Subtitles** switch

---

## Settings Summary

| Setting | Location | Description | Default |
|---------|----------|-------------|---------|
| Device Name | Voice Satellite | Name shown in HA | device_model_voice_assistant |
| Port | Voice Satellite | ESPHome communication port | 6053 |
| Wake Word | Voice Satellite | Word that triggers recognition | Hey Jarvis |
| Stop Word | Voice Satellite | Word that interrupts conversation | Stop |
| Wake Sound | Voice Satellite | Prompt sound when recording starts | On |
| Mute | Voice Satellite | Turn off microphone | Off |
| Continuous Conversation | Interaction | Issue commands continuously | Off |
| Conversation Subtitles | Interaction | Display conversation text | Off |
| Volume | Interaction | Response volume | 80% |

---

## Home Assistant Services

### Manually Trigger Wake

```yaml
service: esphome.your_device_name_trigger_wake
data: {}
```

### Control Mute

```yaml
# Enable mute
service: switch.turn_on
target:
  entity_id: switch.your_device_name_mute

# Disable mute
service: switch.turn_off
target:
  entity_id: switch.your_device_name_mute
```

### Set Volume

```yaml
service: media_player.volume_set
target:
  entity_id: media_player.your_device_name
data:
  volume_level: 0.8  # 0.0 - 1.0
```

---

## FAQ

### Ava can't hear me?

1. Check if microphone permission is granted
2. Check if mute mode is enabled
3. Make sure device volume isn't muted
4. Try speaking closer to device

### Wake word recognition inaccurate?

1. Try a different wake word
2. Make sure environment isn't too noisy
3. Speak at moderate speed, pronounce clearly

### Speech recognition results wrong?

This is usually a Home Assistant issue:
1. Check HA voice assistant configuration
2. Make sure Whisper etc. components are working
3. Check network latency
4. Try speaking more clearly

---

*Back to [Home](Home.md)*
