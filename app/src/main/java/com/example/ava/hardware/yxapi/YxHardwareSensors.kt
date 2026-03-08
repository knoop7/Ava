package com.example.ava.hardware.yxapi

import android.content.Context
import android.util.Log
import com.example.ava.esphome.EspHomeDevice
import com.example.ava.esphome.entities.BinarySensorEntity
import com.example.ava.esphome.entities.SwitchEntity
import com.example.ava.settings.ExperimentalSettingsStore
import com.example.ava.utils.RootUtils
import com.example.esphomeproto.api.EntityCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * YX M5612 硬件传感器管理类
 * 管理人体感应传感器和 LED 灯控制
 * 支持人体感应屏幕控制（有人开屏，无人关屏）
 * 实体命名遵循 ESPHome 协议，由 Home Assistant 自动添加设备前缀
 */
class YxHardwareSensors(
    private val context: Context,
    private val scope: CoroutineScope,
    private val device: EspHomeDevice,
    private val experimentalSettingsStore: ExperimentalSettingsStore
) {
    private val yxManager: YxDeviceManagerWrapper = YxDeviceManagerWrapper.getInstance(context)

    // 实体引用
    private var motionSensorEntity: BinarySensorEntity? = null
    private var blueLedEntity: SwitchEntity? = null
    private var redLedEntity: SwitchEntity? = null
    private var greenLedEntity: SwitchEntity? = null

    // 更新任务
    private var sensorUpdateJob: Job? = null
    private var screenControlJob: Job? = null

    // 屏幕控制状态
    private var lastMotionState: Boolean = false
    private var screenOffJob: Job? = null

    companion object {
        private const val TAG = "YxHardwareSensors"

        // 实体 Key 定义
        const val KEY_MOTION_SENSOR = 100
        const val KEY_BLUE_LED = 101
        const val KEY_RED_LED = 102
        const val KEY_GREEN_LED = 103
    }

    /**
     * 初始化硬件传感器
     */
    fun init() {
        Log.d(TAG, "Initializing YX hardware sensors")

        // 初始化 YX 设备管理器
        yxManager.initGpio()

        // 创建并注册实体
        createEntities()

        // 启动状态更新循环
        startSensorUpdateLoop()

        // 启动屏幕控制监听
        startScreenControl()

        Log.d(TAG, "YX hardware sensors initialized successfully")
    }

    /**
     * 创建所有硬件实体
     * 使用固定 objectId，ESPHome 会自动添加设备前缀
     */
    private fun createEntities() {
        // 1. 人体感应传感器 (BinarySensor)
        motionSensorEntity = BinarySensorEntity(
            key = KEY_MOTION_SENSOR,
            name = "人体感应",
            objectId = "yx_motion_sensor",  // 固定 ID，ESPHome 会自动添加设备前缀
            deviceClass = "motion",  // Home Assistant 设备类型: 运动检测
            icon = "mdi:motion-sensor",
            getState = yxManager.motionDetected,
            isStatusBinarySensor = false,
            disabledByDefault = false
        )
        motionSensorEntity?.let { device.addEntity(it) }

        // 2. 蓝灯控制 (Switch)
        blueLedEntity = SwitchEntity(
            key = KEY_BLUE_LED,
            name = "蓝灯",
            objectId = "yx_blue_led",  // 固定 ID，ESPHome 会自动添加设备前缀
            icon = "mdi:lightbulb",
            getState = yxManager.blueLedState,
            entityCategory = EntityCategory.ENTITY_CATEGORY_NONE,
            setState = { on ->
                yxManager.setBlueLed(on)
            }
        )
        blueLedEntity?.let { device.addEntity(it) }

        // 3. 红灯控制 (Switch)
        redLedEntity = SwitchEntity(
            key = KEY_RED_LED,
            name = "红灯",
            objectId = "yx_red_led",  // 固定 ID，ESPHome 会自动添加设备前缀
            icon = "mdi:lightbulb",
            getState = yxManager.redLedState,
            entityCategory = EntityCategory.ENTITY_CATEGORY_NONE,
            setState = { on ->
                yxManager.setRedLed(on)
            }
        )
        redLedEntity?.let { device.addEntity(it) }

        // 4. 绿灯控制 (Switch)
        greenLedEntity = SwitchEntity(
            key = KEY_GREEN_LED,
            name = "绿灯",
            objectId = "yx_green_led",  // 固定 ID，ESPHome 会自动添加设备前缀
            icon = "mdi:lightbulb",
            getState = yxManager.greenLedState,
            entityCategory = EntityCategory.ENTITY_CATEGORY_NONE,
            setState = { on ->
                yxManager.setGreenLed(on)
            }
        )
        greenLedEntity?.let { device.addEntity(it) }
    }

    /**
     * 启动传感器状态更新循环
     * 用于定期同步硬件状态
     */
    private fun startSensorUpdateLoop() {
        sensorUpdateJob?.cancel()
        sensorUpdateJob = scope.launch {
            // 初始延迟，等待连接建立
            delay(1000)

            while (true) {
                try {
                    // 每 5 秒同步一次状态
                    delay(5000)

                    // 可以在这里添加额外的状态同步逻辑
                    // 例如：读取实际硬件状态并与 Flow 同步

                } catch (e: Exception) {
                    Log.e(TAG, "Error in sensor update loop", e)
                }
            }
        }
    }

    /**
     * 启动屏幕控制功能
     * 根据人体感应状态自动控制屏幕开关
     */
    private fun startScreenControl() {
        screenControlJob?.cancel()
        screenControlJob = scope.launch {
            // 监听人体感应状态变化
            yxManager.motionDetected.drop(1).collect { motionDetected ->
                handleMotionStateChange(motionDetected)
            }
        }
    }

    /**
     * 处理人体感应状态变化
     */
    private suspend fun handleMotionStateChange(motionDetected: Boolean) {
        val settings = experimentalSettingsStore.get()

        // 检查是否启用了屏幕控制
        if (!settings.yxMotionScreenControlEnabled) {
            return
        }

        if (motionDetected) {
            // 检测到有人
            Log.d(TAG, "Motion detected - scheduling screen on")

            // 取消之前的关屏任务
            screenOffJob?.cancel()
            screenOffJob = null

            // 延迟开屏
            val onDelay = settings.yxMotionScreenOnDelay * 1000L
            if (onDelay > 0) {
                delay(onDelay)
            }

            // 检查设置是否仍然启用（可能用户在延迟期间关闭了设置）
            if (experimentalSettingsStore.get().yxMotionScreenControlEnabled) {
                turnScreenOn()
            }
        } else {
            // 检测到人离开
            Log.d(TAG, "Motion cleared - scheduling screen off")

            // 延迟关屏
            val offDelay = settings.yxMotionScreenOffDelay * 1000L
            screenOffJob = scope.launch {
                delay(offDelay)

                // 检查设置是否仍然启用，且期间没有再次检测到人体
                if (experimentalSettingsStore.get().yxMotionScreenControlEnabled &&
                    !yxManager.motionDetected.value) {
                    turnScreenOff()
                }
            }
        }

        lastMotionState = motionDetected
    }

    /**
     * 打开屏幕
     */
    private fun turnScreenOn() {
        Log.d(TAG, "Turning screen on")
        try {
            RootUtils.executeScreenToggle(context, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn screen on", e)
        }
    }

    /**
     * 关闭屏幕
     */
    private fun turnScreenOff() {
        Log.d(TAG, "Turning screen off")
        try {
            RootUtils.executeScreenToggle(context, false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn screen off", e)
        }
    }

    /**
     * 停止硬件传感器
     */
    fun stop() {
        Log.d(TAG, "Stopping YX hardware sensors")

        // 取消所有任务
        sensorUpdateJob?.cancel()
        sensorUpdateJob = null
        screenControlJob?.cancel()
        screenControlJob = null
        screenOffJob?.cancel()
        screenOffJob = null

        // 释放资源
        yxManager.release()

        Log.d(TAG, "YX hardware sensors stopped")
    }

    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): YxDeviceInfo {
        return yxManager.getDeviceInfo()
    }
}
