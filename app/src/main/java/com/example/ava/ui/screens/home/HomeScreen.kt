package com.example.ava.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.ui.ImmersiveMode
import com.example.ava.ui.Screen
import com.example.ava.ui.services.StartStopVoiceSatellite
import com.example.ava.ui.prefs.rememberBooleanPreference
import com.example.ava.ui.theme.SlateBackground
import com.example.ava.ui.theme.SlateText
import com.example.ava.ui.theme.SlateSecondary
import com.example.ava.ui.theme.SlateTertiary
import com.example.ava.ui.theme.AccentBlue
import com.example.ava.ui.theme.AccentGreen

@Composable
fun SunMoonToggle(
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showNewBadge: Boolean = true
) {
    val transition = updateTransition(targetState = isDarkMode, label = "sunMoon")
    
    val sunAlpha by transition.animateFloat(
        transitionSpec = { tween(200) },
        label = "sunAlpha"
    ) { if (it) 0f else 1f }
    
    val sunScale by transition.animateFloat(
        transitionSpec = { tween(300) },
        label = "sunScale"
    ) { if (it) 0.6f else 1f }
    
    val moonAlpha by transition.animateFloat(
        transitionSpec = { tween(200) },
        label = "moonAlpha"
    ) { if (it) 1f else 0f }
    
    val moonScale by transition.animateFloat(
        transitionSpec = { tween(300) },
        label = "moonScale"
    ) { if (it) 1f else 0.3f }
    
    Box(
        modifier = modifier
            .size(28.dp)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.mdi_weather_sunny),
            contentDescription = "Sun",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    alpha = sunAlpha
                    scaleX = sunScale
                    scaleY = sunScale
                }
        )
        Icon(
            painter = painterResource(R.drawable.mdi_weather_night),
            contentDescription = "Moon",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    alpha = moonAlpha
                    scaleX = moonScale
                    scaleY = moonScale
                }
        )
        if (showNewBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(6.dp)
                    .background(Color(0xFFEF4444), CircleShape)
            )
        }
    }
}

const val PREFS_NAME = "ava_home_prefs"
const val KEY_DARK_MODE = "dark_mode"
const val KEY_DARK_MODE_BADGE_SEEN = "dark_mode_badge_seen"
const val KEY_HIDE_HEADER = "hide_header"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    val isDarkMode by rememberBooleanPreference(prefs, KEY_DARK_MODE, false)
    var showBadge by remember { mutableStateOf(!prefs.getBoolean(KEY_DARK_MODE_BADGE_SEEN, false)) }
    var hideHeader by remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_HEADER, false)) }
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    ImmersiveMode(isLandscape = isLandscape)
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isDarkMode) Color.Black else SlateBackground,
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isDarkMode) Color.White else SlateText,
        animationSpec = tween(300),
        label = "textColor"
    )
    val secondaryTextColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF9CA3AF) else SlateSecondary,
        animationSpec = tween(300),
        label = "secondaryTextColor"
    )
    val settingsButtonColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF1F1F1F) else Color.White,
        animationSpec = tween(300),
        label = "settingsButtonColor"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
            
            val isSmallScreen = aspectRatio >= 0.7f && aspectRatio <= 1.3f
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = if (isSmallScreen) 20.dp else 32.dp),
                horizontalArrangement = if (isSmallScreen || hideHeader) Arrangement.End else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                if (!isSmallScreen && !hideHeader) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy((-4).dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ava Pro",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SunMoonToggle(
                                isDarkMode = isDarkMode,
                                onToggle = { 
                                    prefs.edit().putBoolean(KEY_DARK_MODE, !isDarkMode).apply()
                                    if (showBadge) {
                                        showBadge = false
                                        prefs.edit().putBoolean(KEY_DARK_MODE_BADGE_SEEN, true).apply()
                                    }
                                },
                                modifier = Modifier.offset(y = 2.dp),
                                showNewBadge = showBadge
                            )
                        }
                        Text(
                            text = "Home Assistant",
                            fontSize = 14.sp,
                            color = secondaryTextColor
                        )
                    }
                }
                
                Surface(
                    shape = CircleShape,
                    color = settingsButtonColor,
                    shadowElevation = if (isDarkMode) 0.dp else 2.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { navController.navigate(Screen.SETTINGS) { launchSingleTop = true } }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.settings_24px),
                            contentDescription = stringResource(R.string.label_settings),
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                
                Column(
                    modifier = Modifier.offset(y = if (isSmallScreen) (-10).dp else (-40).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StartStopVoiceSatellite(
                        onNavigateToSettings = { navController.navigate(Screen.SETTINGS) { launchSingleTop = true } },
                        isDarkMode = isDarkMode
                    )
                }
            }
            
        }
    }
}
