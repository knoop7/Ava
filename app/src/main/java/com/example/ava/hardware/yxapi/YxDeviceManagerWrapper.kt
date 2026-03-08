package com.example.ava.hardware.yxapi

import android.content.Context
import android.os.yx.IYxGpioListener
import android.os.yx.YxDeviceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * YX M5612 设备管理器封装类
 * 封装 yxapi.jar 的硬件控制功能
 * 支持人体感应传感器和 LED 灯控制
 */
class YxDeviceManagerWrapper private constructor(context: Context) {

    private val manager: YxDeviceManager = YxDeviceManager.getInstance(context)

    // 人体感应状态流
    private val _motionDetected = MutableStateFlow(false)
    val motionDetected: StateFlow<Boolean> = _motionDetected.asStateFlow()

    // LED 灯状态流
    private val _blueLedState = MutableStateFlow(false)
    val blueLedState: StateFlow<Boolean> = _blueLedState.asStateFlow()

    private val _redLedState = MutableStateFlow(false)
    val redLedState: StateFlow<Boolean> = _redLedState.asStateFlow()

    private val _greenLedState = MutableStateFlow(false)
    val greenLedState: StateFlow<Boolean> = _greenLedState.asStateFlow()

    // GPIO 监听器
    private val motionListener = object : IYxGpioListener.Stub() {
        override fun onNewValue(value: Int) {
            _motionDetected.value = (value == 1)
        }
    }

    companion object {
        @Volatile
        private var instance: YxDeviceManagerWrapper? = null

        fun getInstance(context: Context): YxDeviceManagerWrapper {
            return instance ?: synchronized(this) {
                instance ?: YxDeviceManagerWrapper(context.applicationContext).also {
                    instance = it
                }
            }
        }

        // GPIO 引脚定义
        const val GPIO_MOTION_SENSOR = 4      // 人体感应传感器

        // LED 控制命令
        const val LED_BLUE_ON = "io21"
        const val LED_BLUE_OFF = "io20"
        const val LED_RED_ON = "io31"
        const val LED_RED_OFF = "io30"
        const val LED_GREEN_ON = "io41"
        const val LED_GREEN_OFF = "io40"
    }

    /**
     * 初始化 GPIO
     */
    fun initGpio() {
        // 设置 GPIO 方向
        manager.setGpioDirection(GPIO_MOTION_SENSOR, 1)  // 输入 - 人体感应

        // 注册人体感应监听器
        manager.register(motionListener, GPIO_MOTION_SENSOR)

        // 读取初始状态
        _motionDetected.value = manager.getGpioValue(GPIO_MOTION_SENSOR) == 1
    }

    /**
     * 释放资源
     */
    fun release() {
        // 关闭所有 LED
        setBlueLed(false)
        setRedLed(false)
        setGreenLed(false)
    }

    // ==================== LED 控制 ====================

    /**
     * 设置蓝灯状态
     */
    fun setBlueLed(on: Boolean) {
        manager.setOemFunc(if (on) LED_BLUE_ON else LED_BLUE_OFF)
        _blueLedState.value = on
    }

    /**
     * 设置红灯状态
     */
    fun setRedLed(on: Boolean) {
        manager.setOemFunc(if (on) LED_RED_ON else LED_RED_OFF)
        _redLedState.value = on
    }

    /**
     * 设置绿灯状态
     */
    fun setGreenLed(on: Boolean) {
        manager.setOemFunc(if (on) LED_GREEN_ON else LED_GREEN_OFF)
        _greenLedState.value = on
    }

    // ==================== 传感器读取 ====================

    /**
     * 获取当前人体感应状态
     */
    fun getMotionState(): Boolean {
        return manager.getGpioValue(GPIO_MOTION_SENSOR) == 1
    }

    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): YxDeviceInfo {
        return YxDeviceInfo(
            imei = manager.telephonyImei ?: "",
            serialNumber = manager.serialno ?: "",
            model = manager.androidModle ?: "",
            macAddress = manager.deviceMacaddress ?: ""
        )
    }
}

/**
 * YX 设备信息数据类
 */
data class YxDeviceInfo(
    val imei: String,
    val serialNumber: String,
    val model: String,
    val macAddress: String
)
