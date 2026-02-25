# Echo Show 8 (crown) 硬件分析

## 设备信息
- **代号**: crown
- **SoC**: MediaTek MT8163 (ARM64 Cortex-A53 四核)
- **系统**: LineageOS 18.1 (Android 11)
- **内核**: Linux 4.x

## 源码仓库
- **设备树**: https://github.com/amazon-oss/android_device_amazon_crown
- **内核**: https://github.com/amazon-oss/android_kernel_amazon_mt8163
- **通用配置**: https://github.com/amazon-oss/android_device_amazon_mt8163-common
- **硬件HAL**: https://github.com/amazon-oss/android_hardware_amazon
- **Releases**: https://github.com/amazon-oss/releases

## 显示/背光

### 面板
- **LCM驱动**: `lcm,lcm_dts_jd936x_crown` (JD936x)
- **触摸屏**: Focaltech

### 背光控制
```
lcd-backlight {
    compatible = "mediatek,lcd-backlight";
    led_mode = <0x05>;  // PWM模式
    gpios = <GPIO 43>;
}
```

- **控制方式**: PWM (DISP_PWM0/PWM1)
- **亮度范围**: 0-255 (系统设置)
- **最小亮度限制**: LineageOS限制最小为10

### 已知问题
- 亮度设为0时触摸屏可能失效
- 需要使用最小亮度1-10保持触摸响应

## 蓝牙

### 芯片
- **类型**: MT8163内置CONSYS (WiFi/BT组合芯片)
- **驱动**: `mediatek,mt8163-consys`

### 音频
```
audio_bt_cvsd {
    compatible = "mediatek,mt8163-audio_bt_cvsd";
    // 蓝牙语音CVSD编解码
}
```

### GPIO配置
- `wifi_pwr_on/off`: WiFi电源控制
- `bgf_int`: 蓝牙/GPS/FM中断
- `pmu_pw_en`: 电源管理

### 蓝牙代理问题 (Issue #18)
**现象**: 蓝牙代理在Bermuda中显示为死亡状态，扫描器2-3秒后变成骷髅图标

**可能原因**:
1. MT8163 CONSYS芯片的BLE扫描有限制
2. LineageOS蓝牙驱动不完善
3. 需要特殊的扫描参数配置

**内核信息**:
```
consys@18070000 {
    compatible = "mediatek,mt8163-consys";
    pinctrl-names = "wifi_pwr_on", "wifi_pwr_off", "bgf_int", "pmu_pw_en";
}
```

**内核分析结果**:
- crown_defconfig中**没有CONFIG_BT**配置
- 蓝牙通过**用户空间驱动**实现，不是内核蓝牙子系统
- 仅有`CONFIG_MTK_BTCVSD=y`（音频编解码）和`CONFIG_SND_BT_SCO_I2S=y`
- 这可能导致Android BLE扫描API与MT8163用户空间蓝牙驱动不完全兼容

**蓝牙HAL分析** (来源: `mt8163-dev/android_hardware_mediatek`):
- 蓝牙通过UART (`/dev/stpbt`) 与CONSYS芯片通信
- chipId `0x8163` 使用 `stBtDefault_consys` 配置
- HAL通过 `COMBO_IOCTL_*` 与内核驱动交互
- BLE扫描由Android蓝牙栈处理，HAL只负责底层通信

**可能的BLE扫描问题原因**:
1. CONSYS芯片固件可能有BLE扫描频率限制
2. 用户空间驱动可能不完全支持连续BLE扫描
3. 需要调整扫描参数（interval/window）以适配硬件限制

**建议调试步骤**:
```bash
# 检查蓝牙设备节点
adb shell ls -la /dev/stpbt

# 查看蓝牙HAL日志
adb logcat -s bt_mtk:V

# 检查蓝牙固件加载
adb shell dmesg | grep -i "wmt\|consys\|bt"

# 测试BLE扫描（需要root）
adb shell "echo 1 > /sys/kernel/debug/bluetooth/hci0/features"
```

## 已知问题和解决方案

### 1. 应用崩溃 (NPE)
**问题**: `FloatingWindowService$EsperSphereView.stopAnimations`中的handler为null
**状态**: 已在最新版本修复

### 2. 悬浮窗权限无法获取
**问题**: LineageOS在Echo Show上可能没有标准的悬浮窗设置界面
**解决方案**: 通过ADB授权

### 3. 屏幕关闭后无法恢复
**问题**: 亮度设为0时触摸屏失效
**解决方案**: 使用最小亮度10

## ADB命令

### 悬浮窗权限授权（必须）
```bash
adb shell appops set com.example.ava SYSTEM_ALERT_WINDOW allow
```

如果上面命令不生效，试试：
```bash
adb shell pm grant com.example.ava android.permission.SYSTEM_ALERT_WINDOW
```

### 屏幕控制
```bash
# 设置亮度 (0-255)
adb shell settings put system screen_brightness 128

# 设置屏幕超时
adb shell settings put system screen_off_timeout 1800000

# 深色模式
adb shell cmd uimode night yes
```

### 禁用不需要的应用
```bash
adb shell pm disable-user com.android.contacts
adb shell pm disable-user org.lineageos.recorder
adb shell pm disable-user com.android.calculator2
```

## Root方式
1. 通过TWRP刷入Magisk ZIP
2. 重启后在Magisk应用中完成安装

## 参考链接
- [XDA Jailbreak帖子](https://xdaforums.com/t/unlock-root-twrp-unbrick-amazon-echo-show-8-1st-gen-2019-crown.4766687/)
- [XDA LineageOS帖子](https://xdaforums.com/t/rom-unofficial-11-crown-lineageos-18-1-for-the-amazon-echo-show-8-2019.4766709/)
- [完整安装指南](https://www.derekseaman.com/2025/11/home-assistant-hacking-your-echo-show-5-and-8.html)
