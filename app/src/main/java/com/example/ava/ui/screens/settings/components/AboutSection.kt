package com.example.ava.ui.screens.settings.components

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ava.R
import java.net.NetworkInterface

private val SlateText = Color(0xFF1E293B)
private val SlateSecondary = Color(0xFF94A3B8)
private val SlateTertiary = Color(0xFFCBD5E1)
private val CardBorder = Color(0xFFE2E8F0)

@Composable
fun AboutSection() {
    val context = LocalContext.current
    
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            "v${packageInfo.versionName}"
        } catch (e: Exception) {
            "v0.0.0"
        }
    }
    
    val deviceIp = remember {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList()
            interfaces?.find { it.displayName.contains("wlan") || it.displayName.contains("eth") }
                ?.inetAddresses?.toList()
                ?.find { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
                ?.hostAddress ?: "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/knoop7/Ava"))
                context.startActivity(intent)
            }
        ) {
            Row(
                modifier = Modifier
                    .border(1.dp, CardBorder, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.github_24px),
                    contentDescription = "GitHub",
                    tint = SlateText,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.github_project),
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        
        Text(
            text = stringResource(R.string.ava_version, versionName),
            color = SlateSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = stringResource(R.string.device_ip, deviceIp),
            color = SlateSecondary,
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        
        Text(
            text = stringResource(R.string.original_author),
            color = SlateTertiary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.secondary_dev),
            color = SlateTertiary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}
