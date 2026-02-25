package com.example.ava.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object KeepAliveHelper {
    
    enum class Manufacturer {
        HUAWEI, HONOR, XIAOMI, OPPO, VIVO, IQOO, MEIZU, SAMSUNG, ONEPLUS, REALME, OTHER
    }
    
    fun getManufacturer(): Manufacturer {
        val brand = Build.BRAND?.lowercase() ?: ""
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        
        return when {
            brand == "huawei" || manufacturer == "huawei" -> Manufacturer.HUAWEI
            brand == "honor" || manufacturer == "honor" -> Manufacturer.HONOR
            brand == "xiaomi" || manufacturer == "xiaomi" || brand == "redmi" -> Manufacturer.XIAOMI
            brand == "oppo" || manufacturer == "oppo" -> Manufacturer.OPPO
            brand == "vivo" || manufacturer == "vivo" -> Manufacturer.VIVO
            brand == "iqoo" || manufacturer == "iqoo" -> Manufacturer.IQOO
            brand == "meizu" || manufacturer == "meizu" -> Manufacturer.MEIZU
            brand == "samsung" || manufacturer == "samsung" -> Manufacturer.SAMSUNG
            brand == "oneplus" || manufacturer == "oneplus" -> Manufacturer.ONEPLUS
            brand == "realme" || manufacturer == "realme" -> Manufacturer.REALME
            else -> Manufacturer.OTHER
        }
    }
    
    fun isHuaweiOrHonor(): Boolean {
        val m = getManufacturer()
        return m == Manufacturer.HUAWEI || m == Manufacturer.HONOR
    }
    
    fun isVivoOrIqoo(): Boolean {
        val m = getManufacturer()
        return m == Manufacturer.VIVO || m == Manufacturer.IQOO
    }
    
    fun openAutoStartSettings(context: Context): Boolean {
        return when (getManufacturer()) {
            Manufacturer.HUAWEI, Manufacturer.HONOR -> openHuaweiAutoStart(context)
            Manufacturer.XIAOMI -> openXiaomiAutoStart(context)
            Manufacturer.OPPO, Manufacturer.REALME -> openOppoAutoStart(context)
            Manufacturer.VIVO, Manufacturer.IQOO -> openVivoAutoStart(context)
            Manufacturer.MEIZU -> openMeizuAutoStart(context)
            Manufacturer.SAMSUNG -> openSamsungAutoStart(context)
            Manufacturer.ONEPLUS -> openOnePlusAutoStart(context)
            else -> openDefaultSettings(context)
        }
    }
    
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return when (getManufacturer()) {
            Manufacturer.HUAWEI, Manufacturer.HONOR -> openHuaweiBatterySettings(context)
            Manufacturer.XIAOMI -> openXiaomiBatterySettings(context)
            Manufacturer.OPPO, Manufacturer.REALME -> openOppoBatterySettings(context)
            Manufacturer.VIVO, Manufacturer.IQOO -> openVivoBatterySettings(context)
            else -> openDefaultBatterySettings(context)
        }
    }
    
    private fun openHuaweiAutoStart(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
            ))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openHuaweiBatterySettings(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            ))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openXiaomiAutoStart(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
            Intent().setComponent(ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            ))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openXiaomiBatterySettings(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            )).putExtra("package_name", context.packageName).putExtra("package_label", getAppName(context))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openOppoAutoStart(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.coloros.phonemanager",
                "com.coloros.phonemanager.MainActivity"
            ))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openOppoBatterySettings(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.coloros.oppoguardelf",
                "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.coloros.oppoguardelf",
                "com.coloros.powermanager.fuelgaue.PowerSaverModeActivity"
            ))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openVivoAutoStart(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
            )),
            Intent().setComponent(ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.MainGuideActivity"
            ))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openVivoBatterySettings(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.iqoo.powersaving",
                "com.iqoo.powersaving.PowerSavingManagerActivity"
            ))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openMeizuAutoStart(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.meizu.safe",
                "com.meizu.safe.permission.SmartBGActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.meizu.safe",
                "com.meizu.safe.security.SHOW_APPSEC"
            )).addCategory(Intent.CATEGORY_DEFAULT).putExtra("packageName", context.packageName)
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openSamsungAutoStart(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.battery.ui.BatteryActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.samsung.android.sm",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            ))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openOnePlusAutoStart(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            ))
        )
        return tryStartActivities(context, intents)
    }
    
    private fun openDefaultSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun openDefaultBatterySettings(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun tryStartActivities(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                continue
            }
        }
        return openDefaultSettings(context)
    }
    
    private fun getAppName(context: Context): String {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(context.packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            "Ava"
        }
    }
    
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }
    
    fun getKeepAliveInstructions(): String {
        return when (getManufacturer()) {
            Manufacturer.HUAWEI, Manufacturer.HONOR -> """
                华为/荣耀手机设置：
                1. 应用启动管理 → 找到本应用 → 关闭自动管理 → 开启"允许自启动"、"允许关联启动"、"允许后台活动"
                2. 电池 → 更多电池设置 → 休眠时始终保持网络连接
                3. 设置 → 应用 → 应用启动管理 → 手动管理
            """.trimIndent()
            
            Manufacturer.XIAOMI -> """
                小米手机设置：
                1. 设置 → 应用设置 → 应用管理 → 找到本应用 → 自启动 → 开启
                2. 设置 → 省电与电池 → 无限制
                3. 安全中心 → 授权管理 → 自启动管理 → 允许
            """.trimIndent()
            
            Manufacturer.OPPO, Manufacturer.REALME -> """
                OPPO/realme手机设置：
                1. 设置 → 应用管理 → 找到本应用 → 耗电管理 → 允许后台运行
                2. 手机管家 → 权限隐私 → 自启动管理 → 允许
                3. 设置 → 电池 → 更多设置 → 优化策略 → 关闭
            """.trimIndent()
            
            Manufacturer.VIVO, Manufacturer.IQOO -> """
                vivo/iQOO手机设置：
                1. 设置 → 应用与权限 → 应用管理 → 找到本应用 → 权限 → 后台弹出界面 → 允许
                2. i管家 → 应用管理 → 权限管理 → 自启动 → 允许
                3. 设置 → 电池 → 后台耗电管理 → 允许后台高耗电
            """.trimIndent()
            
            Manufacturer.MEIZU -> """
                魅族手机设置：
                1. 手机管家 → 权限管理 → 后台管理 → 允许后台运行
                2. 设置 → 应用管理 → 找到本应用 → 权限管理 → 后台运行 → 允许
            """.trimIndent()
            
            Manufacturer.SAMSUNG -> """
                三星手机设置：
                1. 设置 → 应用程序 → 找到本应用 → 电池 → 允许后台活动
                2. 设置 → 设备维护 → 电池 → 未监视的应用程序 → 添加本应用
            """.trimIndent()
            
            Manufacturer.ONEPLUS -> """
                一加手机设置：
                1. 设置 → 应用 → 应用管理 → 找到本应用 → 电池 → 不优化
                2. 设置 → 电池 → 电池优化 → 所有应用 → 找到本应用 → 不优化
            """.trimIndent()
            
            else -> """
                通用设置：
                1. 设置 → 应用 → 找到本应用 → 电池 → 不限制
                2. 设置 → 电池 → 电池优化 → 找到本应用 → 不优化
                3. 允许应用自启动和后台运行
            """.trimIndent()
        }
    }
}
