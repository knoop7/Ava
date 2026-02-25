package com.example.ava.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.preference.PreferenceManager
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.res.painterResource
import com.example.ava.R
import com.example.ava.services.QuickEntityOverlayService
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.settings.QuickEntitySlot
import com.example.ava.settings.quickEntitySettingsStore
import com.example.ava.ui.prefs.rememberBooleanPreference
import com.example.ava.ui.screens.settings.SimpleCard
import com.example.ava.ui.screens.settings.SettingRow
import com.example.ava.ui.screens.settings.ModernSwitch
import com.example.ava.ui.screens.settings.SettingsDivider
import com.example.ava.ui.screens.settings.getAccentColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val LabelColorLight = Color(0xFF334155)
private val LabelColorDark = Color(0xFFF1F5F9)
private val SubLabelColor = Color(0xFF94A3B8)
private val SlotBackgroundLight = Color(0xFFF9FAFB)
private val SlotBackgroundDark = Color(0xFF2D2D2D)
private val SlotBorderLight = Color(0xFFF1F5F9)
private val SlotBorderDark = Color(0xFF3D3D3D)

@Composable
private fun isDarkMode(): Boolean {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            com.example.ava.ui.screens.home.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
    }
    val isDarkMode by rememberBooleanPreference(
        prefs,
        com.example.ava.ui.screens.home.KEY_DARK_MODE,
        false
    )
    return isDarkMode
}

@Composable
fun QuickEntitySettingsCard(
    enabled: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onEditSlot: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val quickEntityStore = remember { context.quickEntitySettingsStore }
    val isDark = isDarkMode()
    val labelColor = if (isDark) LabelColorDark else LabelColorLight
    val slotBackground = if (isDark) SlotBackgroundDark else SlotBackgroundLight
    val slotBorder = if (isDark) SlotBorderDark else SlotBorderLight
    val accentColor = getAccentColor()
    var quickEntityEnabled by remember { mutableStateOf(false) }
    var haSlotsEnabled by remember { mutableStateOf(false) }
    var slots by remember { mutableStateOf(List(6) { QuickEntitySlot() }) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSlotIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val settings = quickEntityStore.data.first()
        quickEntityEnabled = settings.enableQuickEntity
        haSlotsEnabled = settings.enableHaSlots
        slots = settings.slots
    }

    SimpleCard {
        SettingRow(
            label = stringResource(R.string.settings_quick_entity),
            subLabel = stringResource(R.string.settings_quick_entity_desc)
        ) {
            ModernSwitch(
                checked = quickEntityEnabled,
                enabled = enabled,
                onCheckedChange = { newValue ->
                    quickEntityEnabled = newValue
                    coroutineScope.launch {
                        quickEntityStore.updateData { 
                            it.copy(
                                enableQuickEntity = newValue,
                                enableQuickEntityDisplay = false
                            ) 
                        }
                        if (!newValue) {
                            QuickEntityOverlayService.hide(context)
                        }
                        VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                    }
                }
            )
        }

        if (quickEntityEnabled) {
            SettingsDivider()
            
            SettingRow(
                label = stringResource(R.string.settings_quick_entity_ha_slots),
                subLabel = stringResource(R.string.settings_quick_entity_ha_slots_desc)
            ) {
                ModernSwitch(
                    checked = haSlotsEnabled,
                    enabled = enabled,
                    onCheckedChange = { newValue ->
                        haSlotsEnabled = newValue
                        coroutineScope.launch {
                            quickEntityStore.updateData { 
                                it.copy(enableHaSlots = newValue) 
                            }
                            VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                        }
                    }
                )
            }
            
            SettingsDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_quick_entity_configure),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SubLabelColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )


            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            
            Row(
                horizontalArrangement = if (isLandscape) Arrangement.spacedBy(12.dp) else Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                slots.take(6).forEachIndexed { index, slot ->
                    QuickEntitySlotItem(
                        slot = slot,
                        index = index,
                        onClick = {
                            if (onEditSlot != null) {
                                onEditSlot(index)
                            } else {
                                editingSlotIndex = index
                                showEditDialog = true
                            }
                        },
                        slotBackground = slotBackground,
                        slotBorder = slotBorder,
                        labelColor = labelColor,
                        accentColor = accentColor,
                        isLandscape = isLandscape
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        QuickEntityEditDialog(
            slot = slots.getOrElse(editingSlotIndex) { QuickEntitySlot() },
            slotIndex = editingSlotIndex,
            onDismiss = { showEditDialog = false },
            onConfirm = { newSlot ->
                val newSlots = slots.toMutableList()
                newSlots[editingSlotIndex] = newSlot
                slots = newSlots
                coroutineScope.launch {
                    quickEntityStore.updateData { it.copy(slots = newSlots) }
                    QuickEntityOverlayService.updateSlots(context)
                    VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                }
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun QuickEntitySlotItem(
    slot: QuickEntitySlot,
    index: Int,
    onClick: () -> Unit,
    slotBackground: Color,
    slotBorder: Color,
    labelColor: Color,
    accentColor: Color,
    isLandscape: Boolean = false
) {
    val isEmpty = slot.entityId.isEmpty()
    val iconResId = com.example.ava.ui.components.MdiIconMapper.getIconResId(slot.icon)
    
    val slotSize = if (isLandscape) 56.dp else 44.dp
    val iconSize = if (isLandscape) 24.dp else 20.dp
    val cornerRadius = if (isLandscape) 12.dp else 10.dp

    Box(
        modifier = Modifier
            .size(slotSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (isEmpty) slotBackground else accentColor.copy(alpha = 0.1f))
            .border(1.dp, if (isEmpty) slotBorder else accentColor.copy(alpha = 0.3f), RoundedCornerShape(cornerRadius))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isEmpty) {
            Icon(
                painter = painterResource(R.drawable.mdi_plus),
                contentDescription = null,
                tint = SubLabelColor,
                modifier = Modifier.size(iconSize)
            )
        } else {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = slot.icon,
                tint = accentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}


@Composable
private fun QuickEntityEditDialog(
    slot: QuickEntitySlot,
    slotIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (QuickEntitySlot) -> Unit
) {
    val isDark = isDarkMode()
    val labelColor = if (isDark) LabelColorDark else LabelColorLight
    val slotBackground = if (isDark) SlotBackgroundDark else SlotBackgroundLight
    val slotBorder = if (isDark) SlotBorderDark else SlotBorderLight
    val accentColor = getAccentColor()
    
    var entityId by remember { mutableStateOf(slot.entityId) }
    var icon by remember { mutableStateOf(slot.icon.ifEmpty { "mdi:home-assistant" }) }
    var selectedColor by remember { mutableStateOf(slot.color) }
    
    val availableIcons = com.example.ava.ui.components.MdiIconMapper.iconMap.toList()
    val availableColors = com.example.ava.ui.components.MdiColorMapper.presetColors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_quick_entity_slot, slotIndex + 1),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_quick_entity_id),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubLabelColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = entityId,
                        onValueChange = { entityId = it },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = labelColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(slotBackground, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (entityId.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.settings_quick_entity_id_hint),
                                        color = SubLabelColor,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.settings_quick_entity_icon),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubLabelColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                        items(availableIcons) { (iconKey, iconResId) ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (icon == iconKey) accentColor.copy(alpha = 0.15f)
                                        else slotBackground
                                    )
                                    .border(
                                        width = if (icon == iconKey) 2.dp else 1.dp,
                                        color = if (icon == iconKey) accentColor else slotBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { icon = iconKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(iconResId),
                                    contentDescription = iconKey,
                                    tint = if (icon == iconKey) accentColor else labelColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        }
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.settings_quick_entity_color),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubLabelColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableColors.forEach { (colorKey, tileColor) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(tileColor.topColor))
                                    .border(
                                        width = if (selectedColor == colorKey) 3.dp else 0.dp,
                                        color = if (selectedColor == colorKey) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable { selectedColor = if (selectedColor == colorKey) "" else colorKey },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == colorKey) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                    if (selectedColor.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_quick_entity_color_auto),
                            fontSize = 10.sp,
                            color = SubLabelColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val autoEntityType = when {
                        entityId.startsWith("switch.") -> "switch"
                        entityId.startsWith("light.") -> "light"
                        entityId.startsWith("button.") -> "button"
                        entityId.startsWith("sensor.") -> "sensor"
                        entityId.startsWith("binary_sensor.") -> "sensor"
                        entityId.startsWith("input_boolean.") -> "switch"
                        entityId.startsWith("fan.") -> "switch"
                        entityId.startsWith("cover.") -> "switch"
                        entityId.startsWith("script.") -> "button"
                        entityId.startsWith("scene.") -> "button"
                        entityId.startsWith("automation.") -> "switch"
                        else -> "switch"
                    }
                    onConfirm(QuickEntitySlot(
                        entityId = entityId,
                        entityType = autoEntityType,
                        icon = icon,
                        label = "",
                        size = slot.size,
                        color = selectedColor
                    ))
                }
            ) {
                Text(
                    text = stringResource(R.string.settings_quick_entity_save),
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.label_cancel),
                    color = SubLabelColor
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
