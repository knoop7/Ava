# Echo Show 8 (crown) Hardware Analysis

## Device Information
- **Codename**: crown
- **SoC**: MediaTek MT8163 (ARM64 Cortex-A53 Quad-core)
- **System**: LineageOS 18.1 (Android 11)
- **Kernel**: Linux 4.x

## Source Repositories
- **Device Tree**: https://github.com/amazon-oss/android_device_amazon_crown
- **Kernel**: https://github.com/amazon-oss/android_kernel_amazon_mt8163
- **Common Config**: https://github.com/amazon-oss/android_device_amazon_mt8163-common
- **Hardware HAL**: https://github.com/amazon-oss/android_hardware_amazon
- **Releases**: https://github.com/amazon-oss/releases

## Display/Backlight

### Panel
- **LCM Driver**: `lcm,lcm_dts_jd936x_crown` (JD936x)
- **Touchscreen**: Focaltech

### Backlight Control
```
lcd-backlight {
    compatible = "mediatek,lcd-backlight";
    led_mode = <0x05>;  // PWM mode
    gpios = <GPIO 43>;
}
```

- **Control Method**: PWM (DISP_PWM0/PWM1)
- **Brightness Range**: 0-255 (system settings)
- **Minimum Brightness Limit**: LineageOS limits minimum to 10

### Known Issues
- Touchscreen may become unresponsive when brightness is set to 0
- Use minimum brightness 1-10 to maintain touch response

## Bluetooth

### Chip
- **Type**: MT8163 built-in CONSYS (WiFi/BT combo chip)
- **Driver**: `mediatek,mt8163-consys`

### Audio
```
audio_bt_cvsd {
    compatible = "mediatek,mt8163-audio_bt_cvsd";
    // Bluetooth voice CVSD codec
}
```

### GPIO Configuration
- `wifi_pwr_on/off`: WiFi power control
- `bgf_int`: Bluetooth/GPS/FM interrupt
- `pmu_pw_en`: Power management

### Bluetooth Proxy Issue (Issue #18)
**Symptom**: Bluetooth proxy shows as dead in Bermuda, scanner turns to skull icon after 2-3 seconds

**Possible Causes**:
1. MT8163 CONSYS chip has BLE scanning limitations
2. LineageOS Bluetooth driver is incomplete
3. Special scan parameter configuration needed

**Kernel Info**:
```
consys@18070000 {
    compatible = "mediatek,mt8163-consys";
    pinctrl-names = "wifi_pwr_on", "wifi_pwr_off", "bgf_int", "pmu_pw_en";
}
```

**Kernel Analysis Results**:
- crown_defconfig has **no CONFIG_BT** configuration
- Bluetooth implemented via **userspace driver**, not kernel Bluetooth subsystem
- Only `CONFIG_MTK_BTCVSD=y` (audio codec) and `CONFIG_SND_BT_SCO_I2S=y`
- This may cause Android BLE scanning API to be incompatible with MT8163 userspace Bluetooth driver

**Bluetooth HAL Analysis** (source: `mt8163-dev/android_hardware_mediatek`):
- Bluetooth communicates with CONSYS chip via UART (`/dev/stpbt`)
- chipId `0x8163` uses `stBtDefault_consys` configuration
- HAL interacts with kernel driver via `COMBO_IOCTL_*`
- BLE scanning handled by Android Bluetooth stack, HAL only handles low-level communication

**Possible BLE Scanning Issue Causes**:
1. CONSYS chip firmware may have BLE scan frequency limitations
2. Userspace driver may not fully support continuous BLE scanning
3. Need to adjust scan parameters (interval/window) to accommodate hardware limitations

**Recommended Debug Steps**:
```bash
# Check Bluetooth device node
adb shell ls -la /dev/stpbt

# View Bluetooth HAL logs
adb logcat -s bt_mtk:V

# Check Bluetooth firmware loading
adb shell dmesg | grep -i "wmt\|consys\|bt"

# Test BLE scanning (requires root)
adb shell "echo 1 > /sys/kernel/debug/bluetooth/hci0/features"
```

## Known Issues and Solutions

### 1. App Crash (NPE)
**Issue**: handler is null in `FloatingWindowService$EsperSphereView.stopAnimations`
**Status**: Fixed in latest version

### 2. Cannot Obtain Overlay Permission
**Issue**: LineageOS on Echo Show may not have standard overlay settings interface
**Solution**: Grant via ADB

### 3. Screen Cannot Recover After Turning Off
**Issue**: Touchscreen becomes unresponsive when brightness is set to 0
**Solution**: Use minimum brightness 10

## ADB Commands

### Grant Overlay Permission (Required)
```bash
adb shell appops set com.example.ava SYSTEM_ALERT_WINDOW allow
```

If the above command doesn't work, try:
```bash
adb shell pm grant com.example.ava android.permission.SYSTEM_ALERT_WINDOW
```

### Screen Control
```bash
# Set brightness (0-255)
adb shell settings put system screen_brightness 128

# Set screen timeout
adb shell settings put system screen_off_timeout 1800000

# Dark mode
adb shell cmd uimode night yes
```

### Disable Unnecessary Apps
```bash
adb shell pm disable-user com.android.contacts
adb shell pm disable-user org.lineageos.recorder
adb shell pm disable-user com.android.calculator2
```

## Root Method
1. Flash Magisk ZIP via TWRP
2. Complete installation in Magisk app after reboot

## Reference Links
- [XDA Jailbreak Thread](https://xdaforums.com/t/unlock-root-twrp-unbrick-amazon-echo-show-8-1st-gen-2019-crown.4766687/)
- [XDA LineageOS Thread](https://xdaforums.com/t/rom-unofficial-11-crown-lineageos-18-1-for-the-amazon-echo-show-8-2019.4766709/)
- [Complete Installation Guide](https://www.derekseaman.com/2025/11/home-assistant-hacking-your-echo-show-5-and-8.html)
