# YX M5612 硬件集成指南

本文档介绍如何将 YX M5612 硬件平台的人体感应传感器和 LED 灯控制功能集成到 Ava 项目中。

## 概述

YX M5612 是一款 Android 硬件平台，提供了丰富的 GPIO 控制接口，包括：
- **人体感应传感器** (GPIO 4)
- **LED 灯控制** (蓝灯、红灯、绿灯)

## 已集成功能

### 1. 人体感应传感器
- **类型**: BinarySensor
- **设备类型**: motion
- **图标**: mdi:motion-sensor
- **实体 ID**: `yx_motion_sensor`
- **显示名称**: `人体感应`
- **功能**: 实时检测人体移动，状态自动上报到 Home Assistant

### 2. LED 灯控制
- **蓝灯**: Switch 实体 (`yx_blue_led`)
- **红灯**: Switch 实体 (`yx_red_led`)
- **绿灯**: Switch 实体 (`yx_green_led`)
- **图标**: mdi:lightbulb
- **显示名称**: `蓝灯` / `红灯` / `绿灯`
- **功能**: 通过 Home Assistant 开关控制 LED 灯

### 3. 人体感应屏幕控制
- **功能**: 根据人体感应状态自动控制屏幕开关
- **有人时**: 自动打开屏幕（可设置延迟）
- **无人时**: 自动关闭屏幕（可设置延迟）
- **权限要求**: 需要 Root 权限或 Shizuku 权限

> **实体命名规则**: 遵循 ESPHome 协议标准，使用固定 `objectId`，Home Assistant 会自动添加设备前缀。例如设备名为 "客厅"，则完整实体 ID 为 `binary_sensor.living_room_voice_assistant_yx_motion_sensor`。

## 文件结构

```
app/src/main/java/com/example/ava/hardware/yxapi/
├── YxDeviceManagerWrapper.kt    # YX API 封装类
└── YxHardwareSensors.kt         # 硬件传感器管理类

app/libs/
└── yxapi.jar                     # YX 硬件 API 库
```

## 核心类说明

### YxDeviceManagerWrapper
封装了 YX API 的硬件控制功能：

```kotlin
// 获取实例
val yxManager = YxDeviceManagerWrapper.getInstance(context)

// 初始化 GPIO
yxManager.initGpio()

// 读取人体感应状态
val motionDetected: StateFlow<Boolean> = yxManager.motionDetected

// 控制 LED
yxManager.setBlueLed(true)   // 开蓝灯
yxManager.setRedLed(false)   // 关红灯
yxManager.setGreenLed(true)  // 开绿灯
```

### YxHardwareSensors
管理所有 YX 硬件实体，负责与 ESPHome 协议集成：

```kotlin
// 创建实例
val yxSensors = YxHardwareSensors(context, scope, espHomeDevice)

// 初始化（会自动创建并注册所有实体）
yxSensors.init()

// 停止并释放资源
yxSensors.stop()
```

## 设置选项

在 `ExperimentalSettings` 中添加了以下设置项：

| 设置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `yxHardwareEnabled` | Boolean | false | 启用 YX 硬件支持 |
| `yxMotionSensorEnabled` | Boolean | true | 启用人感传感器 |
| `yxLedControlEnabled` | Boolean | true | 启用 LED 灯控制 |
| `yxMotionScreenControlEnabled` | Boolean | false | 启用人感屏幕控制（有人开屏，无人关屏） |
| `yxMotionScreenOnDelay` | Int | 0 | 感应到有人后延迟开屏时间（秒，0-60） |
| `yxMotionScreenOffDelay` | Int | 30 | 无人后延迟关屏时间（秒，5-300） |

## 集成步骤

### 1. 添加依赖
已在 `app/build.gradle.kts` 中添加：
```kotlin
implementation(files("libs/yxapi.jar"))
```

### 2. 复制 JAR 文件
确保 `yxapi.jar` 已复制到 `app/libs/` 目录。

### 3. 在 VoiceSatellite 中初始化
在创建 VoiceSatellite 实例后，添加以下代码：

```kotlin
// 在 VoiceSatelliteService.kt 的 createVoiceSatellite() 方法中

val experimentalSettings = experimentalSettingsStore.get()
if (experimentalSettings.yxHardwareEnabled) {
    val yxSensors = YxHardwareSensors(
        context = this@VoiceSatelliteService,
        scope = lifecycleScope,
        device = voiceSatellite  // EspHomeDevice 实例
    ).apply {
        init()
    }
}
```

### 4. Home Assistant 配置
集成后，Home Assistant 会自动发现并添加以下实体（以设备名 "客厅" 为例）：

- `binary_sensor.living_room_voice_assistant_yx_motion_sensor` - 人体感应传感器
- `switch.living_room_voice_assistant_yx_blue_led` - 蓝灯控制
- `switch.living_room_voice_assistant_yx_red_led` - 红灯控制
- `switch.living_room_voice_assistant_yx_green_led` - 绿灯控制

**多设备场景示例**:

如果有多台设备，例如：
- 客厅（设备名: 客厅）
- 卧室（设备名: 卧室）

则会生成以下实体：
- `binary_sensor.living_room_voice_assistant_yx_motion_sensor` - 客厅人体感应
- `binary_sensor.bedroom_voice_assistant_yx_motion_sensor` - 卧室人体感应
- `switch.living_room_voice_assistant_yx_blue_led` - 客厅蓝灯
- `switch.bedroom_voice_assistant_yx_blue_led` - 卧室蓝灯

## 使用示例

### 自动化示例 - 人体感应开灯
```yaml
automation:
  - alias: "人体感应开灯"
    trigger:
      - platform: state
        entity_id: binary_sensor.living_room_voice_assistant_yx_motion_sensor
        to: "on"
    action:
      - service: switch.turn_on
        target:
          entity_id: switch.living_room_voice_assistant_yx_blue_led
```

### 自动化示例 - 语音控制灯光
```yaml
automation:
  - alias: "语音控制蓝灯"
    trigger:
      - platform: conversation
        command: "打开蓝灯"
    action:
      - service: switch.turn_on
        target:
          entity_id: switch.living_room_voice_assistant_yx_blue_led
```

## 注意事项

1. **权限要求**: YX API 需要系统权限，确保应用具有相应的权限
2. **硬件兼容性**: 仅适用于 YX M5612 平台
3. **GPIO 冲突**: 避免与其他使用 GPIO 4 的功能冲突
4. **资源释放**: 确保在 Service 销毁时调用 `yxSensors.stop()`

## 故障排除

### 人体感应无响应
- 检查 GPIO 4 是否正确连接
- 确认 `yxHardwareEnabled` 和 `yxMotionSensorEnabled` 设置为 true
- 查看日志：`adb logcat -s YxHardwareSensors`

### LED 灯无法控制
- 确认 LED 硬件连接正确
- 检查 `yxLedControlEnabled` 设置
- 验证 `yxapi.jar` 是否正确加载

## 扩展开发

如需添加更多 YX 硬件功能：

1. 在 `YxDeviceManagerWrapper` 中添加新的控制方法
2. 在 `YxHardwareSensors` 中创建对应的实体
3. 在 `ExperimentalSettings` 中添加相应的设置项
4. 更新本文档

## 参考

- [YX API 开发文档](../yxapi-m5612/README.md)
- [ESPHome 实体类型](https://esphome.io/components/)
- [Home Assistant 自动化](https://www.home-assistant.io/docs/automation/)
