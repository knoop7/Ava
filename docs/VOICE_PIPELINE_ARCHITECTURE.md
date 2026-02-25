# Ava Voice Assistant Pipeline Architecture Documentation

## Overview

This document describes the complete voice processing pipeline of the Ava voice assistant, including the full flow from microphone input to Home Assistant response.

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Ava Android App                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌─────────────────────┐    ┌──────────────────────┐   │
│  │ MicrophoneInput│───▶│VoiceSatelliteAudioInput│───▶│   VoiceSatellite    │   │
│  │  (16kHz PCM)  │    │  (Wake word/Audio stream)│    │   (State machine)   │   │
│  └──────────────┘    └─────────────────────┘    └──────────┬───────────┘   │
│                                                              │               │
│                                                              ▼               │
│  ┌──────────────┐    ┌─────────────────────┐    ┌──────────────────────┐   │
│  │  TtsPlayer   │◀───│VoiceSatellitePlayer │◀───│VoiceSatelliteStateMachine│
│  │  (TTS playback)│    │   (Audio playback)   │    │    (Event handling)   │   │
│  └──────────────┘    └─────────────────────┘    └──────────────────────┘   │
│                                                                              │
│                              ┌─────────────┐                                │
│                              │   Server    │                                │
│                              │ (TCP:6053)  │                                │
│                              └──────┬──────┘                                │
└─────────────────────────────────────┼───────────────────────────────────────┘
                                      │
                                      │ ESPHome Native API Protocol
                                      │ (Protobuf over TCP)
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Home Assistant                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────────┐ │
│  │ ESPHome Add-on  │───▶│ Assist Pipeline │───▶│ STT/Intent/TTS Engines  │ │
│  │ (Protocol parse) │    │ (Voice pipeline) │    │ (Whisper/OpenAI/EdgeTTS)│ │
│  └─────────────────┘    └─────────────────┘    └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Core Component Details

### 2.1 MicrophoneInput (`audio/MicrophoneInput.kt`)

**Responsibility**: Raw audio capture

```kotlin
// Audio parameters
- Sample rate: 16000 Hz
- Channels: Mono (CHANNEL_IN_MONO)
- Format: 16-bit PCM (ENCODING_PCM_16BIT)
- Source: VOICE_RECOGNITION

// Audio effects
- AcousticEchoCanceler (AEC) - Echo cancellation
- AutomaticGainControl (AGC) - Automatic gain
- NoiseSuppressor (NS) - Noise suppression
```

**Key Methods**:
- `start()`: Start recording
- `read()`: Read audio buffer (blocking)
- `close()`: Release resources

### 2.2 VoiceSatelliteAudioInput (`esphome/voicesatellite/VoiceSatelliteAudioInput.kt`)

**Responsibility**: Wake word detection + Audio stream management

```kotlin
// State
- isStreaming: Boolean  // Whether sending audio stream to HA
- activeWakeWords: List<String>  // Active wake words
- activeStopWords: List<String>  // Active stop words

// Output types
sealed class AudioResult {
    data class Audio(val audio: ByteString)  // Audio data
    data class WakeDetected(val wakeWord: String)  // Wake word detected
    data class StopDetected(val stopWord: String)  // Stop word detected
}
```

**Audio Stream Control Logic**:
```
1. Continuously read audio from microphone
2. Always perform wake word/stop word detection
3. Only emit(AudioResult.Audio) when isStreaming=true
4. Emit(AudioResult.WakeDetected) when wake word detected
5. Emit(AudioResult.StopDetected) when stop word detected
```

### 2.3 VoiceSatellite (`esphome/voicesatellite/VoiceSatellite.kt`)

**Responsibility**: Core coordinator, implements ESPHome Voice Assistant protocol

**State Machine**:
```
Connected ──(wake word)──▶ Listening ──(VAD end)──▶ Processing ──(TTS start)──▶ Responding
    ▲                                                                              │
    └──────────────────────────(TTS end/RUN_END)───────────────────────────────────┘
```

**Key Message Handling**:

| Message Type | Direction | Description |
|---------|------|------|
| `VoiceAssistantConfigurationRequest` | HA→App | Request wake word configuration |
| `voiceAssistantConfigurationResponse` | App→HA | Return available wake words |
| `voiceAssistantRequest{start=true}` | App→HA | Start voice session |
| `voiceAssistantRequest{start=false}` | App→HA | **End voice session** |
| `voiceAssistantAudio{data=...}` | App→HA | Audio data stream |
| `VoiceAssistantAudio{end=true}` | HA→App | HA notifies audio reception ended |
| `VoiceAssistantEventResponse` | HA→App | Voice events (see table below) |

**VoiceAssistantEvent Event Types**:

| Event | Description | App Response |
|------|------|----------|
| `RUN_START` | Session started | Initialize TtsPlayer, set isStreaming=true |
| `STT_VAD_END` | VAD detected speech end | isStreaming=false, state=Processing |
| `STT_END` | STT recognition complete | isStreaming=false, state=Processing |
| `INTENT_PROGRESS` | Intent processing | May start streaming TTS |
| `TTS_START` | TTS started | state=Responding |
| `TTS_END` | TTS ended | Play TTS audio |
| `RUN_END` | Session ended | Reset state, may trigger continuous conversation |

### 2.4 VoiceSatelliteStateMachine (`esphome/voicesatellite/VoiceSatelliteStateMachine.kt`)

**Responsibility**: Voice event state management

**Timeout Mechanism**:
- `WAKE_TIMEOUT_MS = 5000ms`: Timeout after wake with no response
- Silence detection: Switch to Processing after 100ms silence

**Error Handling**:
```kotlin
// STT error detection
private fun isSTTError(text: String?): Boolean {
    return text?.contains("list index out of range") == true ||
           text?.contains("index out of range") == true
}

// TTS error detection  
private fun isTTSAboutError(text: String?): Boolean {
    // Same as above
}
```

### 2.5 VoiceSatellitePlayer (`esphome/voicesatellite/VoiceSatellitePlayer.kt`)

**Responsibility**: Audio playback management

**Players**:
- `ttsPlayer`: TTS voice playback
- `mediaPlayer`: Media playback (music, etc.)
- `wakeSoundPlayer`: Wake sound playback

**Volume Control**:
- `duck()`: Lower media volume (duckMultiplier=0.5)
- `unDuck()`: Restore media volume

**Wake Sound**:
```kotlin
suspend fun playWakeSound(wakeWordIndex: Int = 0, onCompletion: () -> Unit = {}) {
    val sound = if (wakeWordIndex == 1) wakeSound2.get() else wakeSound.get()
    if (enabled) wakeSoundPlayer.play(sound, onCompletion)
}
```

### 2.6 Server (`server/Server.kt`) & ClientConnection (`server/ClientConnection.kt`)

**Responsibility**: TCP server, implements ESPHome Native API protocol

**Protocol Format**:
```
┌─────────┬─────────────┬──────────────┬─────────────┐
│ 0x00    │ Length      │ MessageType  │ Payload     │
│ (1byte) │ (VarUInt)   │ (VarUInt)    │ (Protobuf)  │
└─────────┴─────────────┴──────────────┴─────────────┘
```

**Connection Management**:
- Single client mode (new connection disconnects old one)
- Default port: 6053

---

## 3. Complete Voice Flow

### 3.1 Wake Flow

```
1. MicrophoneInput continuously captures audio
2. VoiceSatelliteAudioInput detects wake word
3. emit(AudioResult.WakeDetected("hey jarvis"))
4. VoiceSatellite.onWakeDetected() is called
5. Check current state to prevent duplicate wake
6. Call wakeSatellite()
   - state = Listening
   - isStreaming = true
   - sendVoiceAssistantStartRequest()
   - playWakeSound()
```

### 3.2 Audio Stream Transmission

```
1. VoiceSatelliteAudioInput detects isStreaming=true
2. emit(AudioResult.Audio(audioBytes))
3. VoiceSatellite.handleAudioResult() processes
4. sendMessage(voiceAssistantAudio { data = audioBytes })
5. Send to HA via Server/ClientConnection
```

### 3.3 Response Flow

```
1. HA sends VoiceAssistantEventResponse(STT_END)
2. VoiceSatelliteStateMachine.handleVoiceEvent() processes
3. isStreaming = false, state = Processing
4. HA sends VoiceAssistantEventResponse(TTS_START)
5. state = Responding
6. HA sends VoiceAssistantEventResponse(TTS_END, url=...)
7. TtsPlayer.playTts(url)
8. Call onTtsFinished() after playback completes
9. Check if continuing conversation, otherwise stopSatellite()
```

### 3.4 Session End

```
1. stopSatellite() is called
2. stateMachine.reset()
3. audioInput.isStreaming = false
4. player.ttsPlayer.stop()
5. player.unDuck()
6. sendVoiceAssistantStopRequest()  // Important! Notify HA session ended
7. state = Connected
```

---

## 4. Known Issues and Fixes

### 4.1 HA Freeze Issue

**Issue**: When re-waking during Responding/Processing state, HA was not notified to end current session

**Fix** (2026-01-13):
```kotlin
// VoiceSatellite.kt - onWakeDetected()
if (currentState == Responding || currentState == Processing) {
    player.ttsPlayer.stop()
    audioInput.isStreaming = false
    player.unDuck()
    stateMachine.reset()
    sendVoiceAssistantStopRequest()  // Added: send stop request first
    wakeSatellite(wakeWordPhrase, wakeWordIndex = wakeWordIndex)
    return
}

// VoiceSatellite.kt - stopSatellite()
private suspend fun stopSatellite() {
    stateMachine.reset()
    audioInput.isStreaming = false
    player.ttsPlayer.stop()
    player.unDuck()
    sendVoiceAssistantStopRequest()  // Added: notify HA session ended
    _state.value = Connected
    // ...
}
```

### 4.2 TTS Provider Error

**Error Log**:
```
HomeAssistantError: Provider edge_tts not found
TextToSpeechError: Text-to-speech engine edge_tts does not support language zh-CN
```

**Cause**: HA-side TTS engine configuration issue, not an App-side issue

**Solution**:
1. Confirm edge_tts integration is installed
2. Check voice assistant pipeline TTS configuration
3. Restart Home Assistant

---

## 5. Debug Guide

### 5.1 Key Log TAGs

| TAG | Component | Description |
|-----|------|------|
| `VoiceSatellite` | VoiceSatellite | Main coordinator logs |
| `VoiceSatelliteStateMachine` | StateMachine | State transition logs |
| `VoiceSatellitePlayer` | Player | Player logs |
| `TtsPlayer` | TtsPlayer | TTS playback logs |
| `Server` | Server | TCP server logs |
| `ClientConnection` | ClientConnection | Connection logs |
| `MicrophoneInput` | MicrophoneInput | Microphone logs |

### 5.2 State Checking

```kotlin
// Check current state
Log.d(TAG, "Current state: ${_state.value}")

// Check audio stream state
Log.d(TAG, "isStreaming: ${audioInput.isStreaming}")

// Check state machine state
Log.d(TAG, "isWaking: ${stateMachine.isWaking}, isWakePhase: ${stateMachine.isWakePhase}")
```

### 5.3 Common Issue Troubleshooting

| Issue | Possible Cause | Troubleshooting |
|------|----------|----------|
| No response on wake | Wake word not active | Check activeWakeWords |
| Audio not sending | isStreaming=false | Check state transition logs |
| TTS not playing | URL empty or player error | Check TtsPlayer logs |
| Stuck in Processing | HA not responding | Check HA logs |
| Continuous conversation not working | continueConversation=false | Check enableContinuousConversation |

---

## 6. Configuration Parameters

### 6.1 PlayerSettings

| Parameter | Type | Default | Description |
|------|------|--------|------|
| `enableWakeSound` | Boolean | true | Enable wake sound |
| `wakeSound` | String | asset:///sounds/wake_word_triggered.wav | Wake word 1 sound |
| `wakeSound2` | String | asset:///sounds/wake_word_triggered.wav | Wake word 2 sound |
| `enableContinuousConversation` | Boolean | false | Enable continuous conversation |

### 6.2 Timeout Parameters

| Parameter | Value | Description |
|------|-----|------|
| `WAKE_TIMEOUT_MS` | 5000ms | Timeout after wake with no response |
| Silence detection delay | 100ms | Delay before switching state after silence detected |
| Continuous conversation timeout | 10000ms | Listening timeout in continuous conversation mode |

---

### 4.3 MediaCodec Handler Dead Thread Issue

**Error Log**:
```
Handler (android.media.MediaCodec$EventHandler) sending message to a Handler on a dead thread
```

**Cause**: When ExoPlayer is released on a non-main thread, MediaCodec's Handler thread is already dead but still trying to send messages

**Fix** (2026-01-13):
```kotlin
// AudioPlayer.kt - close()
override fun close() {
    isPlayerInit = false
    val playerToRelease = _player
    _player = null
    currentListener = null
    _state.value = AudioPlayerState.IDLE
    
    if (playerToRelease != null) {
        try {
            playerToRelease.stop()
            playerToRelease.clearMediaItems()
        } catch (e: Exception) { }
        
        // Key: Release Player on main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                playerToRelease.release()
            } catch (e: Exception) { }
        }
        
        GpioAecController.activateBeamforming()
    }
}
```

---

## 7. Version History

| Date | Version | Changes |
|------|------|------|
| 2026-01-13 | - | Added sendVoiceAssistantStopRequest() to fix HA freeze issue |
| 2026-01-13 | - | Support independent wake sounds for two wake words |
| 2026-01-13 | - | Fixed MediaCodec Handler dead thread issue |

---

## 8. References

- [ESPHome Native API Protocol](https://esphome.io/components/api.html)
- [Home Assistant Assist Pipeline](https://www.home-assistant.io/integrations/assist_pipeline/)
- [ESPHome Voice Assistant](https://esphome.io/components/voice_assistant.html)
