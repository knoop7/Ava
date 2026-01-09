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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.ui.Screen
import com.example.ava.ui.services.StartStopVoiceSatellite


private val SlateBackground = Color(0xFFF8FAFC)
private val SlateText = Color(0xFF1E293B)
private val SlateSecondary = Color(0xFF64748B)
private val SlateTertiary = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
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
                    .padding(horizontal = 24.dp, vertical = if (isSmallScreen) 12.dp else 24.dp),
                horizontalArrangement = if (isSmallScreen) Arrangement.End else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                if (!isSmallScreen) {
                    Column {
                        Text(
                            text = "Ava",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateText,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "Voice Assistant",
                            fontSize = 14.sp,
                            color = SlateSecondary
                        )
                    }
                }
                
                
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { navController.navigate(Screen.SETTINGS) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.settings_24px),
                            contentDescription = stringResource(R.string.label_settings),
                            tint = SlateSecondary,
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
                        onNavigateToSettings = { navController.navigate(Screen.SETTINGS) }
                    )
                }
            }
            
        }
    }
}