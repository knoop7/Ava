package com.example.ava.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.preference.PreferenceManager
import com.example.ava.R
import com.example.ava.services.QuickEntityOverlayService
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.settings.QuickEntitySlot
import com.example.ava.settings.quickEntitySettingsStore
import com.example.ava.ui.components.MdiColorMapper
import com.example.ava.ui.components.MdiIconMapper
import com.example.ava.ui.screens.settings.getAccentColor
import com.example.ava.ui.prefs.rememberBooleanPreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val LabelColorLight = Color(0xFF334155)
private val LabelColorDark = Color(0xFFF1F5F9)
private val SubLabelColor = Color(0xFF94A3B8)
private val SlotBackgroundLight = Color(0xFFF9FAFB)
private val SlotBackgroundDark = Color(0xFF2D2D2D)
private val SlotBorderLight = Color(0xFFF1F5F9)
private val SlotBorderDark = Color(0xFF3D3D3D)
private val CardBackgroundLight = Color(0xFFFFFFFF)
private val CardBackgroundDark = Color(0xFF1F1F1F)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntityEditScreen(
    slotIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val quickEntityStore = remember { context.quickEntitySettingsStore }
    val coroutineScope = rememberCoroutineScope()
    val isDark = isDarkMode()
    val labelColor = if (isDark) LabelColorDark else LabelColorLight
    val slotBackground = if (isDark) SlotBackgroundDark else SlotBackgroundLight
    val cardBackground = if (isDark) CardBackgroundDark else CardBackgroundLight
    val screenBackground = if (isDark) Color.Black else Color(0xFFF9FAFB)
    val accentColor = getAccentColor()

    var slot by remember { mutableStateOf(QuickEntitySlot()) }
    var entityId by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("mdi:home-assistant") }
    var color by remember { mutableStateOf("") }

    LaunchedEffect(slotIndex) {
        val settings = quickEntityStore.data.first()
        slot = settings.slots.getOrElse(slotIndex) { QuickEntitySlot() }
        entityId = slot.entityId
        icon = slot.icon.ifEmpty { "mdi:home-assistant" }
        color = slot.color
    }


    fun saveSlot() {

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
        val newSlot = QuickEntitySlot(
            entityId = entityId,
            entityType = autoEntityType,
            icon = icon,
            label = "",
            color = color
        )
        coroutineScope.launch {
            quickEntityStore.updateData { settings ->
                val newSlots = settings.slots.toMutableList()
                while (newSlots.size <= slotIndex) {
                    newSlots.add(QuickEntitySlot())
                }
                newSlots[slotIndex] = newSlot
                settings.copy(slots = newSlots)
            }
            QuickEntityOverlayService.updateSlots(context)
        }
    }

    val availableIcons = MdiIconMapper.iconMap.entries
        .filter { 
            !it.key.contains("weather") && 
            !it.key.endsWith("-off") && 
            !it.key.endsWith("-open") && 
            !it.key.endsWith("-closed") &&
            it.key != "mdi:lightbulb-off"
        }
        .map { it.key to it.value }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_quick_entity_slot, slotIndex + 1),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBackground
                )
            )
        },
        containerColor = screenBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_quick_entity_id),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_quick_entity_supported_types),
                        fontSize = 12.sp,
                        color = SubLabelColor,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = entityId,
                        onValueChange = { 
                            entityId = it
                            saveSlot()
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
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
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_quick_entity_icon),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(240.dp)
                    ) {
                        items(availableIcons) { (iconKey, iconResId) ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (icon == iconKey) accentColor.copy(alpha = 0.15f)
                                        else slotBackground
                                    )
                                    .border(
                                        width = if (icon == iconKey) 2.dp else 0.dp,
                                        color = if (icon == iconKey) accentColor else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        icon = iconKey
                                        saveSlot()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(iconResId),
                                    contentDescription = iconKey,
                                    tint = if (icon == iconKey) accentColor else labelColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_quick_entity_color),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (color.isEmpty()) accentColor.copy(alpha = 0.1f) else slotBackground)
                            .border(
                                width = if (color.isEmpty()) 2.dp else 0.dp,
                                color = if (color.isEmpty()) accentColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                color = ""
                                saveSlot()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFD60A),
                                            Color(0xFF34C759),
                                            Color(0xFF007AFF),
                                            Color(0xFFFF2D55)
                                        )
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.settings_quick_entity_color_auto),
                            fontSize = 14.sp,
                            color = labelColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var showCustomColorDialog by remember { mutableStateOf(false) }
                    var selectedHue by remember { mutableStateOf(0f) }
                    var selectedSaturation by remember { mutableStateOf(1f) }
                    var selectedBrightness by remember { mutableStateOf(1f) }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (color.startsWith("#")) accentColor.copy(alpha = 0.1f) else slotBackground)
                            .border(
                                width = if (color.startsWith("#")) 2.dp else 0.dp,
                                color = if (color.startsWith("#")) accentColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { showCustomColorDialog = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (color.startsWith("#")) {
                                        try { Color(android.graphics.Color.parseColor(color)) } 
                                        catch (e: Exception) { Color.Gray }
                                    } else {
                                        Color.Gray
                                    }
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (color.startsWith("#")) stringResource(R.string.settings_quick_entity_color_custom) + " " + color else stringResource(R.string.settings_quick_entity_color_custom),
                            fontSize = 14.sp,
                            color = labelColor
                        )
                    }
                    
                    if (showCustomColorDialog) {
                        val controller = com.github.skydoves.colorpicker.compose.rememberColorPickerController()
                        var hexColor by remember { mutableStateOf("#FF0000") }
                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        val pickerSize = if (isLandscape) 140.dp else 200.dp
                        
                        AlertDialog(
                            onDismissRequest = { showCustomColorDialog = false },
                            title = { Text(stringResource(R.string.settings_quick_entity_color_custom)) },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    com.github.skydoves.colorpicker.compose.HsvColorPicker(
                                        modifier = Modifier.size(pickerSize),
                                        controller = controller,
                                        onColorChanged = { colorEnvelope ->
                                            hexColor = "#" + colorEnvelope.hexCode.takeLast(6)
                                        }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    com.github.skydoves.colorpicker.compose.BrightnessSlider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(26.dp),
                                        controller = controller
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        color = hexColor
                                        saveSlot()
                                        showCustomColorDialog = false
                                    }
                                ) {
                                    Text(stringResource(android.R.string.ok))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCustomColorDialog = false }) {
                                    Text(stringResource(android.R.string.cancel))
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(MdiColorMapper.presetColors) { (colorName, tileColor) ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(tileColor.topColor))
                                    .border(
                                        width = if (color == colorName) 3.dp else 0.dp,
                                        color = if (color == colorName) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        color = colorName
                                        saveSlot()
                                    }
                            )
                        }
                    }
                }
            }

            if (entityId.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        entityId = ""
                        icon = "mdi:home-assistant"
                        color = ""
                        saveSlot()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444))
                    )
                ) {
                    Text(stringResource(R.string.settings_quick_entity_clear_slot))
                }
            }
        }
    }
}
