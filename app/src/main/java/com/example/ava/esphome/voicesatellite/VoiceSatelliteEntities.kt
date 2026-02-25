package com.example.ava.esphome.voicesatellite

import android.content.Context
import com.example.ava.R
import com.example.ava.esphome.entities.ButtonEntity
import com.example.ava.esphome.entities.MediaPlayerEntity
import com.example.ava.esphome.entities.NumberEntity
import com.example.ava.esphome.entities.SelectEntity
import com.example.ava.esphome.entities.ServiceArg
import com.example.ava.esphome.entities.ServiceEntity
import com.example.ava.esphome.entities.SwitchEntity
import com.example.ava.esphome.entities.TextEntity
import com.example.ava.notifications.NotificationScenes
import com.example.ava.settings.BrowserSettings
import com.example.ava.settings.NotificationSettingsStore
import com.example.ava.settings.PlayerSettings
import com.example.ava.settings.PlayerSettingsStore
import com.example.ava.settings.ScreensaverSettings
import com.example.ava.settings.ScreensaverSettingsStore
import com.example.ava.settings.screensaverSettingsStore
import com.example.ava.settings.quickEntitySettingsStore
import com.example.ava.settings.QuickEntitySettingsStore
import com.example.esphomeproto.api.EntityCategory
import kotlinx.coroutines.flow.map

object VoiceSatelliteEntities {
    
    fun buildEntities(
        audioInput: VoiceSatelliteAudioInput,
        player: VoiceSatellitePlayer,
        notificationSettingsStore: NotificationSettingsStore,
        onRestartService: (() -> Unit)?,
        context: Context,
        browserSettingsData: BrowserSettings,
        screensaverSettingsData: ScreensaverSettings,
        playerSettingsStore: PlayerSettingsStore? = null,
        playerSettingsData: PlayerSettings? = null,
        onMicrophoneVolumeChanged: ((Float) -> Unit)? = null
    ) = buildList {
        if (onRestartService != null) {
            add(ButtonEntity(
                key = 4,
                name = context.getString(R.string.entity_restart_service),
                objectId = "restart_service",
                icon = "mdi:restart",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC,
                onPress = { onRestartService() }
            ))
        }

        add(SwitchEntity(
            1,
            context.getString(R.string.entity_mute_microphone),
            "mute_microphone",
            "mdi:microphone-off",
            audioInput.muted,
            EntityCategory.ENTITY_CATEGORY_NONE
        ) { audioInput.setMuted(it) })

        add(NumberEntity(
            key = "microphone_volume".hashCode(),
            name = context.getString(R.string.entity_microphone_volume),
            objectId = "microphone_volume",
            icon = "mdi:microphone",
            minValue = 0.0f,
            maxValue = 2.0f,
            step = 0.1f,
            getState = audioInput.microphoneVolume,
            setState = { volume ->
                audioInput.setMicrophoneVolume(volume)
                onMicrophoneVolumeChanged?.invoke(volume)
            },
            entityCategory = EntityCategory.ENTITY_CATEGORY_CONFIG
        ))

        add(SwitchEntity(
            3,
            context.getString(R.string.entity_screen_toggle),
            "screen_toggle",
            "mdi:monitor",
            player.enableScreenOff,
            EntityCategory.ENTITY_CATEGORY_NONE
        ) { 
            player.enableScreenOff.set(it)
            com.example.ava.utils.RootUtils.executeScreenToggle(context, it)
        })

        add(MediaPlayerEntity(0, context.getString(R.string.entity_media_player), "media_player", player))
        
        add(SwitchEntity(
            2,
            context.getString(R.string.entity_wake_sound),
            "play_wake_sound",
            "mdi:bell-ring",
            player.enableWakeSound,
            EntityCategory.ENTITY_CATEGORY_NONE
        ) { player.enableWakeSound.set(it) })

        val browserSettings = com.example.ava.settings.BrowserSettingsStore(context)
        if (browserSettingsData.haRemoteUrlEnabled) {
            add(TextEntity(
                6,
                context.getString(R.string.entity_ha_remote_url),
                "ha_remote_url",
                "mdi:web",
                player.haRemoteUrl,
                { url -> player.setHaRemoteUrl(url) }
            ))
        }

        val screensaverSettings = ScreensaverSettingsStore(context.screensaverSettingsStore)
        if (screensaverSettingsData.enabled && screensaverSettingsData.enableHaDisplay) {
            add(SwitchEntity(
                24,
                context.getString(R.string.entity_screensaver_display),
                "screensaver_display",
                "mdi:monitor-eye",
                screensaverSettings.visible,
                EntityCategory.ENTITY_CATEGORY_NONE
            ) { enabled -> screensaverSettings.visible.set(enabled) })
        }
        
        if (screensaverSettingsData.enabled && screensaverSettingsData.screensaverUrlVisible) {
            add(TextEntity(
                25,
                context.getString(R.string.entity_screensaver_url),
                "screensaver_url",
                "mdi:web",
                screensaverSettings.screensaverUrl,
                { url -> screensaverSettings.screensaverUrl.set(url) },
                entityCategory = EntityCategory.ENTITY_CATEGORY_CONFIG
            ))
        }
        
        val advancedControlEnabled = browserSettingsData.advancedControlEnabled
        if (advancedControlEnabled) {
            add(ServiceEntity(
                key = 23,
                name = "webview_command",
                args = listOf(ServiceArg("command")),
                onExecute = { args ->
                    val command = args["command"] as? String ?: ""
                    com.example.ava.services.WebViewService.executeCommand(context, command)
                }
            ))
        }

        add(SelectEntity(
            7,
            context.getString(R.string.entity_notification_scene),
            "notification_scene",
            "mdi:bell-badge",
            NotificationScenes.ALL_SCENE_TITLES,
            player.notificationScene,
            { sceneTitle -> player.setNotificationScene(sceneTitle) },
            { NotificationScenes.ALL_SCENE_TITLES }
        ))
        
        add(NumberEntity(
            8,
            context.getString(R.string.entity_scene_display_duration),
            "scene_display_duration",
            "mdi:timer",
            minValue = 5f,
            maxValue = 999999f,
            step = 1f,
            unitOfMeasurement = "s",
            getState = notificationSettingsStore.sceneDisplayDuration.map { (it / 1000).toFloat() },
            setState = { seconds -> 
                notificationSettingsStore.sceneDisplayDuration.set(seconds.toInt().coerceAtLeast(5) * 1000)
            },
            entityCategory = EntityCategory.ENTITY_CATEGORY_CONFIG
        ))
        
        playerSettingsStore?.let { store ->
            val playerSettingsDataResolved = playerSettingsData ?: return@let
            if (playerSettingsDataResolved.enableWeatherOverlay && playerSettingsDataResolved.enableWeatherOverlayDisplay) {
                add(SwitchEntity(
                    26,
                    context.getString(R.string.entity_weather_display),
                    "weather_display",
                    "mdi:weather-partly-cloudy",
                    store.enableWeatherOverlayVisible,
                    EntityCategory.ENTITY_CATEGORY_NONE
                ) { enabled ->
                    store.enableWeatherOverlayVisible.set(enabled)
                })
            }
            
            if (playerSettingsDataResolved.enableDreamClock && playerSettingsDataResolved.enableDreamClockDisplay) {
                add(SwitchEntity(
                    27,
                    context.getString(R.string.entity_dream_clock_display),
                    "dream_clock_display",
                    "mdi:clock-outline",
                    store.enableDreamClockVisible,
                    EntityCategory.ENTITY_CATEGORY_NONE
                ) { enabled ->
                    store.enableDreamClockVisible.set(enabled)
                })
            }
        }
        
        if (browserSettingsData.haRemoteUrlEnabled && browserSettingsData.enableBrowserDisplay) {
            add(SwitchEntity(
                28,
                context.getString(R.string.entity_browser_display),
                "browser_display",
                "mdi:web-check",
                browserSettings.enableBrowserVisible,
                EntityCategory.ENTITY_CATEGORY_NONE
            ) { enabled ->
                browserSettings.enableBrowserVisible.set(enabled)
            })
        }
        
        val quickEntitySettings = QuickEntitySettingsStore(context.quickEntitySettingsStore)
        kotlinx.coroutines.runBlocking {
            val quickEntityData = quickEntitySettings.get()
            if (quickEntityData.enableQuickEntity) {
                quickEntitySettings.enableQuickEntityDisplay.set(false)
                add(SwitchEntity(
                    29,
                    context.getString(R.string.entity_quick_entity_display),
                    "quick_entity_display",
                    "mdi:view-grid",
                    quickEntitySettings.enableQuickEntityDisplay,
                    EntityCategory.ENTITY_CATEGORY_NONE
                ) { enabled ->
                    quickEntitySettings.enableQuickEntityDisplay.set(enabled)
                    if (enabled) {
                        com.example.ava.services.QuickEntityOverlayService.show(context)
                    } else {
                        com.example.ava.services.QuickEntityOverlayService.hide(context)
                    }
                })
                
                if (quickEntityData.enableHaSlots) {
                    for (i in 0 until 6) {
                        val slotState = quickEntitySettings.slotEntityId(i)
                        add(TextEntity(
                            key = 30 + i,
                            name = context.getString(R.string.entity_quick_entity_slot, i + 1),
                            objectId = "quick_entity_slot_${i + 1}",
                            icon = "mdi:card-text",
                            getState = slotState,
                            setState = { entityId ->
                                slotState.set(entityId)
                                com.example.ava.services.QuickEntityOverlayService.getInstance()?.reloadSlots()
                            },
                            entityCategory = EntityCategory.ENTITY_CATEGORY_CONFIG
                        ))
                    }
                }
            }
        }
    }
}
