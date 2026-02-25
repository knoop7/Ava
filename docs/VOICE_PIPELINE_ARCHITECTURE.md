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
// 音频参数
- 采样率: 16000 Hz
- 声道: 单声道 (CHANNEL_IN_MONO)
- 格式: 16位 PCM (ENCODING_PCM_16BIT)
- 音源: VOICE_RECOGNITION

// 音频效果
- AcousticEchoCanceler (AEC) - 回声消除
- AutomaticGainControl (AGC) - 自动增益
- NoiseSuppressor (NS) - 噪声抑制
```

**关键方法**:
- `start()`: 启动录音
- `read()`: 读取音频缓冲区 (阻塞)
- `close()`: 释放资源

### 2.2 VoiceSatelliteAudioInput (`esphome/voicesatellite/VoiceSatelliteAudioInput.kt`)

**职责**: 唤醒词检测 + 音频流管理

```kotlin
// 状态
- isStreaming: Boolean  // 是否向HA发送音频流
- activeWakeWords: List<String>  // 激活的唤醒词
- activeStopWords: List<String>  // 激活的停止词

// 输出类型
sealed class AudioResult {
    data class Audio(val audio: ByteString)  // 音频数据
    data class WakeDetected(val wakeWord: String)  // 唤醒词检测
    data class StopDetected(val stopWord: String)  // 停止词检测
}
```

**音频流控制逻辑**:
```
1. 持续从麦克风读取音频
2. 始终进行唤醒词/停止词检测
3. 仅当 isStreaming=true 时，才 emit(AudioResult.Audio)
4. 检测到唤醒词时 emit(AudioResult.WakeDetected)
5. 检测到停止词时 emit(AudioResult.StopDetected)
```

### 2.3 VoiceSatellite (`esphome/voicesatellite/VoiceSatellite.kt`)

**职责**: 核心协调器，实现 ESPHome Voice Assistant 协议

**状态机**:
```
Connected ──(唤醒词)──▶ Listening ──(VAD结束)──▶ Processing ──(TTS开始)──▶ Responding
    ▲                                                                          │
    └──────────────────────────(TTS结束/RUN_END)───────────────────────────────┘
```

**关键消息处理**:

| 消息类型 | 方向 | 说明 |
|---------|------|------|
| `VoiceAssistantConfigurationRequest` | HA→App | 请求唤醒词配置 |
| `voiceAssistantConfigurationResponse` | App→HA | 返回可用唤醒词 |
| `voiceAssistantRequest{start=true}` | App→HA | 开始语音会话 |
| `voiceAssistantRequest{start=false}` | App→HA | **结束语音会话** |
| `voiceAssistantAudio{data=...}` | App→HA | 音频数据流 |
| `VoiceAssistantAudio{end=true}` | HA→App | HA通知音频接收结束 |
| `VoiceAssistantEventResponse` | HA→App | 语音事件 (见下表) |

**VoiceAssistantEvent 事件类型**:

| 事件 | 说明 | App 响应 |
|------|------|----------|
| `RUN_START` | 会话开始 | 初始化 TtsPlayer，设置 isStreaming=true |
| `STT_VAD_END` | VAD 检测到语音结束 | isStreaming=false, state=Processing |
| `STT_END` | STT 识别完成 | isStreaming=false, state=Processing |
| `INTENT_PROGRESS` | 意图处理中 | 可能开始流式 TTS |
| `TTS_START` | TTS 开始 | state=Responding |
| `TTS_END` | TTS 结束 | 播放 TTS 音频 |
| `RUN_END` | 会话结束 | 重置状态，可能触发连续对话 |

### 2.4 VoiceSatelliteStateMachine (`esphome/voicesatellite/VoiceSatelliteStateMachine.kt`)

**职责**: 语音事件状态管理

**超时机制**:
- `WAKE_TIMEOUT_MS = 5000ms`: 唤醒后无响应超时
- 静音检测: 100ms 静音后切换到 Processing

**错误处理**:
```kotlin
// STT 错误检测
private fun isSTTError(text: String?): Boolean {
    return text?.contains("list index out of range") == true ||
           text?.contains("索引超出范围") == true
}

// TTS 错误检测  
private fun isTTSAboutError(text: String?): Boolean {
    // 同上
}
```

### 2.5 VoiceSatellitePlayer (`esphome/voicesatellite/VoiceSatellitePlayer.kt`)

**职责**: 音频播放管理

**播放器**:
- `ttsPlayer`: TTS 语音播放
- `mediaPlayer`: 媒体播放 (音乐等)
- `wakeSoundPlayer`: 唤醒提示音播放

**音量控制**:
- `duck()`: 降低媒体音量 (duckMultiplier=0.5)
- `unDuck()`: 恢复媒体音量

**唤醒提示音**:
```kotlin
suspend fun playWakeSound(wakeWordIndex: Int = 0, onCompletion: () -> Unit = {}) {
    val sound = if (wakeWordIndex == 1) wakeSound2.get() else wakeSound.get()
    if (enabled) wakeSoundPlayer.play(sound, onCompletion)
}
```

### 2.6 Server (`server/Server.kt`) & ClientConnection (`server/ClientConnection.kt`)

**职责**: TCP 服务器，实现 ESPHome Native API 协议

**协议格式**:
```
┌─────────┬─────────────┬──────────────┬─────────────┐
│ 0x00    │ Length      │ MessageType  │ Payload     │
│ (1byte) │ (VarUInt)   │ (VarUInt)    │ (Protobuf)  │
└─────────┴─────────────┴──────────────┴─────────────┘
```

**连接管理**:
- 单客户端模式 (新连接会断开旧连接)
- 默认端口: 6053

---

## 3. 完整语音流程

### 3.1 唤醒流程

```
1. MicrophoneInput 持续采集音频
2. VoiceSatelliteAudioInput 检测到唤醒词
3. emit(AudioResult.WakeDetected("hey jarvis"))
4. VoiceSatellite.onWakeDetected() 被调用
5. 检查当前状态，防止重复唤醒
6. 调用 wakeSatellite()
   - state = Listening
   - isStreaming = true
   - sendVoiceAssistantStartRequest()
   - playWakeSound()
```

### 3.2 音频流传输

```
1. VoiceSatelliteAudioInput 检测到 isStreaming=true
2. emit(AudioResult.Audio(audioBytes))
3. VoiceSatellite.handleAudioResult() 处理
4. sendMessage(voiceAssistantAudio { data = audioBytes })
5. 通过 Server/ClientConnection 发送到 HA
```

### 3.3 响应流程

```
1. HA 发送 VoiceAssistantEventResponse(STT_END)
2. VoiceSatelliteStateMachine.handleVoiceEvent() 处理
3. isStreaming = false, state = Processing
4. HA 发送 VoiceAssistantEventResponse(TTS_START)
5. state = Responding
6. HA 发送 VoiceAssistantEventResponse(TTS_END, url=...)
7. TtsPlayer.playTts(url)
8. 播放完成后调用 onTtsFinished()
9. 检查是否继续对话，否则 stopSatellite()
```

### 3.4 会话结束

```
1. stopSatellite() 被调用
2. stateMachine.reset()
3. audioInput.isStreaming = false
4. player.ttsPlayer.stop()
5. player.unDuck()
6. sendVoiceAssistantStopRequest()  // 重要！通知 HA 会话结束
7. state = Connected
```

---

## 4. 已知问题与修复

### 4.1 HA 卡死问题

**问题**: 在 Responding/Processing 状态下重新唤醒时，未通知 HA 结束当前会话

**修复** (2026-01-13):
```kotlin
// VoiceSatellite.kt - onWakeDetected()
if (currentState == Responding || currentState == Processing) {
    player.ttsPlayer.stop()
    audioInput.isStreaming = false
    player.unDuck()
    stateMachine.reset()
    sendVoiceAssistantStopRequest()  // 新增：先发送停止请求
    wakeSatellite(wakeWordPhrase, wakeWordIndex = wakeWordIndex)
    return
}

// VoiceSatellite.kt - stopSatellite()
private suspend fun stopSatellite() {
    stateMachine.reset()
    audioInput.isStreaming = false
    player.ttsPlayer.stop()
    player.unDuck()
    sendVoiceAssistantStopRequest()  // 新增：通知 HA 会话结束
    _state.value = Connected
    // ...
}
```

### 4.2 TTS Provider 错误

**错误日志**:
```
HomeAssistantError: Provider edge_tts not found
TextToSpeechError: Text-to-speech engine edge_tts does not support language zh-CN
```

**原因**: HA 端 TTS 引擎配置问题，非 App 端问题

**解决方案**:
1. 确认 edge_tts 集成已安装
2. 检查语音助手管道 TTS 配置
3. 重启 Home Assistant

---

## 5. 调试指南

### 5.1 关键日志 TAG

| TAG | 组件 | 说明 |
|-----|------|------|
| `VoiceSatellite` | VoiceSatellite | 主协调器日志 |
| `VoiceSatelliteStateMachine` | StateMachine | 状态转换日志 |
| `VoiceSatellitePlayer` | Player | 播放器日志 |
| `TtsPlayer` | TtsPlayer | TTS 播放日志 |
| `Server` | Server | TCP 服务器日志 |
| `ClientConnection` | ClientConnection | 连接日志 |
| `MicrophoneInput` | MicrophoneInput | 麦克风日志 |

### 5.2 状态检查

```kotlin
// 检查当前状态
Log.d(TAG, "Current state: ${_state.value}")

// 检查音频流状态
Log.d(TAG, "isStreaming: ${audioInput.isStreaming}")

// 检查状态机状态
Log.d(TAG, "isWaking: ${stateMachine.isWaking}, isWakePhase: ${stateMachine.isWakePhase}")
```

### 5.3 常见问题排查

| 问题 | 可能原因 | 排查方法 |
|------|----------|----------|
| 唤醒无响应 | 唤醒词未激活 | 检查 activeWakeWords |
| 音频不发送 | isStreaming=false | 检查状态转换日志 |
| TTS 不播放 | URL 为空或播放器错误 | 检查 TtsPlayer 日志 |
| 卡在 Processing | HA 未响应 | 检查 HA 日志 |
| 连续对话不工作 | continueConversation=false | 检查 enableContinuousConversation |

---

## 6. 配置参数

### 6.1 PlayerSettings

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enableWakeSound` | Boolean | true | 启用唤醒提示音 |
| `wakeSound` | String | asset:///sounds/wake_word_triggered.wav | 唤醒词1提示音 |
| `wakeSound2` | String | asset:///sounds/wake_word_triggered.wav | 唤醒词2提示音 |
| `enableContinuousConversation` | Boolean | false | 启用连续对话 |

### 6.2 超时参数

| 参数 | 值 | 说明 |
|------|-----|------|
| `WAKE_TIMEOUT_MS` | 5000ms | 唤醒后无响应超时 |
| 静音检测延迟 | 100ms | 检测到静音后切换状态的延迟 |
| 连续对话超时 | 10000ms | 连续对话模式下的监听超时 |

---

### 4.3 MediaCodec Handler Dead Thread 问题

**错误日志**:
```
Handler (android.media.MediaCodec$EventHandler) sending message to a Handler on a dead thread
```

**原因**: ExoPlayer 在非主线程释放时，MediaCodec 的 Handler 线程已死亡但还在尝试发送消息

**修复** (2026-01-13):
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
        
        // 关键：在主线程释放 Player
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

## 7. 版本历史

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-01-13 | - | 添加 sendVoiceAssistantStopRequest() 修复 HA 卡死问题 |
| 2026-01-13 | - | 支持两个唤醒词独立提示音 |
| 2026-01-13 | - | 修复 MediaCodec Handler dead thread 问题 |

---

## 8. 参考资料

- [ESPHome Native API Protocol](https://esphome.io/components/api.html)
- [Home Assistant Assist Pipeline](https://www.home-assistant.io/integrations/assist_pipeline/)
- [ESPHome Voice Assistant](https://esphome.io/components/voice_assistant.html)
