package com.example.ava.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.services.WebViewService
import com.example.ava.ui.Screen


val PureWhiteBackground = Color(0xFFF9FAFB) 


val SlateTextDark = Color(0xFF1E293B) 
val SlateTextLight = Color(0xFF64748B) 
val SlateTextMuted = Color(0xFF94A3B8) 


val RoseGradient = Color(0xFFFFF1F2) 
val SlateGradient = Color(0xFFF8FAFC) 
val VioletGradient = Color(0xFFF5F3FF) 
val BlueGradient = Color(0xFFEFF6FF) 
val AmberGradient = Color(0xFFFFFBEB) 
val GrayGradient = Color(0xFFF9FAFB) 


val RoseIconColor = Color(0xFF9F1239) 
val SlateIconColor = Color(0xFF0F172A) 
val VioletIconColor = Color(0xFF4C1D95) 
val BlueIconColor = Color(0xFF1E3A8A) 
val AmberIconColor = Color(0xFF78350F) 
val GrayIconColor = Color(0xFF374151) 


val RoseBorderColor = Color(0xFFFECDD3) 
val SlateBorderColor = Color(0xFFE2E8F0) 
val VioletBorderColor = Color(0xFFDDD6FE) 
val BlueBorderColor = Color(0xFFBFDBFE) 
val AmberBorderColor = Color(0xFFFDE68A) 
val GrayBorderColor = Color(0xFFE5E7EB) 


data class SettingsGroup(
    val title: String,
    val enTitle: String, 
    val subtitle: String,
    val iconResId: Int,
    val gradientColor: Color,
    val iconColor: Color,
    val borderColor: Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    
    val themeColor = Color(0xFF4F46E5) 
    
    
    val isA64Device = com.example.ava.utils.RootUtils.isQuadCoreA64Device()
    
    
    val settingsGroups = listOfNotNull(
        
        SettingsGroup(
            title = stringResource(R.string.settings_group_connection),
            enTitle = stringResource(R.string.settings_group_connection_en),
            subtitle = stringResource(R.string.settings_group_connection_desc),
            iconResId = R.drawable.wifi_24px,
            gradientColor = BlueGradient,
            iconColor = themeColor,
            borderColor = Color(0xFFF1F5F9),
            route = Screen.SETTINGS_CONNECTION
        ),
        
        SettingsGroup(
            title = stringResource(R.string.settings_group_interaction),
            enTitle = stringResource(R.string.settings_group_interaction_en),
            subtitle = stringResource(R.string.settings_group_interaction_desc),
            iconResId = R.drawable.settings_24px,
            gradientColor = BlueGradient,
            iconColor = themeColor,
            borderColor = Color(0xFFF1F5F9),
            route = Screen.SETTINGS_INTERACTION
        ),
        
        SettingsGroup(
            title = stringResource(R.string.settings_group_service),
            enTitle = stringResource(R.string.settings_group_service_en),
            subtitle = stringResource(R.string.settings_group_service_desc),
            iconResId = R.drawable.cloud_24px,
            gradientColor = BlueGradient,
            iconColor = themeColor,
            borderColor = Color(0xFFF1F5F9),
            route = Screen.SETTINGS_SERVICE
        ),
        
        if (!isA64Device) SettingsGroup(
            title = stringResource(R.string.settings_group_bluetooth),
            enTitle = stringResource(R.string.settings_group_bluetooth_en),
            subtitle = stringResource(R.string.settings_group_bluetooth_desc),
            iconResId = R.drawable.bluetooth_24px,
            gradientColor = BlueGradient,
            iconColor = themeColor,
            borderColor = Color(0xFFF1F5F9),
            route = Screen.SETTINGS_BLUETOOTH
        ) else null,
        
        if (!isA64Device) SettingsGroup(
            title = stringResource(R.string.settings_group_screensaver),
            enTitle = stringResource(R.string.settings_group_screensaver_en),
            subtitle = stringResource(R.string.settings_group_screensaver_desc),
            iconResId = R.drawable.screensaver_24px,
            gradientColor = BlueGradient,
            iconColor = themeColor,
            borderColor = Color(0xFFF1F5F9),
            route = Screen.SETTINGS_SCREENSAVER
        ) else null,
        
        if (!isA64Device) SettingsGroup(
            title = stringResource(R.string.settings_group_browser),
            enTitle = stringResource(R.string.settings_group_browser_en),
            subtitle = stringResource(R.string.settings_group_browser_desc),
            iconResId = R.drawable.globe_24px,
            gradientColor = BlueGradient,
            iconColor = themeColor,
            borderColor = Color(0xFFF1F5F9),
            route = Screen.SETTINGS_BROWSER
        ) else null,
        
        if (!isA64Device) SettingsGroup(
            title = stringResource(R.string.settings_group_experimental),
            enTitle = stringResource(R.string.settings_group_experimental_en),
            subtitle = stringResource(R.string.settings_group_experimental_desc),
            iconResId = R.drawable.experiment_24px,
            gradientColor = BlueGradient,
            iconColor = themeColor,
            borderColor = Color(0xFFF1F5F9),
            route = Screen.SETTINGS_EXPERIMENTAL
        ) else null,
        
        SettingsGroup(
            title = stringResource(R.string.settings_group_root),
            enTitle = stringResource(R.string.settings_group_root_en),
            subtitle = stringResource(R.string.settings_group_root_desc),
            iconResId = R.drawable.settings_24px,
            gradientColor = BlueGradient,
            iconColor = themeColor,
            borderColor = Color(0xFFF1F5F9),
            route = Screen.SETTINGS_ROOT
        )
    )
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PureWhiteBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhiteBackground,
                    titleContentColor = SlateTextDark,
                    navigationIconContentColor = SlateTextDark
                ),
                title = {
                    Text(
                        text = stringResource(R.string.label_settings),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            
                            WebViewService.exitSettings(context)
                            navController.popBackStack() 
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = stringResource(R.string.back),
                            tint = SlateTextDark
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PureWhiteBackground)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) 
        ) {
            items(settingsGroups) { group ->
                SettingsGroupCard(
                    group = group,
                    onClick = { navController.navigate(group.route) }
                )
            }
            
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                com.example.ava.ui.screens.settings.components.AboutSection()
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    group: SettingsGroup,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)) 
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF9FAFB)) 
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(group.iconResId),
                    contentDescription = null,
                    tint = group.iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextDark,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = group.enTitle.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateTextMuted,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = group.subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = SlateTextLight,
                    letterSpacing = 0.3.sp
                )
            }
            
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1D5DB)) 
                )
            }
        }
    }
}