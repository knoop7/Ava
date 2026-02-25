package com.example.ava.ui.screens.settings.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ava.R
import com.example.ava.ui.screens.settings.SettingsDivider
import com.example.ava.ui.screens.settings.getAccentColor
import com.example.ava.ui.screens.settings.getDialogBackground
import com.example.ava.ui.screens.settings.getLabelColor
import com.example.ava.ui.screens.settings.getTitleColor

@Composable
fun SharedRingtonePickerDialog(
    ringtones: List<Pair<String, String>>,
    currentUri: String,
    context: android.content.Context,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onSelectExternal: () -> Unit
) {
    var tempSelectedUri by remember { mutableStateOf(currentUri) }
    var currentRingtone by remember { mutableStateOf<Ringtone?>(null) }

    AlertDialog(
        onDismissRequest = {
            currentRingtone?.stop()
            onDismiss()
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = getTitleColor()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ringtones.forEach { (itemTitle, uri) ->
                    val isSelected = uri == tempSelectedUri
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentRingtone?.stop()
                                tempSelectedUri = uri
                                if (uri.isNotEmpty() && !uri.startsWith("asset://")) {
                                    try {
                                        val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uri))
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            ringtone?.isLooping = false
                                        }
                                        ringtone?.play()
                                        currentRingtone = ringtone
                                    } catch (e: Exception) {
                                        try {
                                            val mediaPlayer = MediaPlayer()
                                            mediaPlayer.setDataSource(context, Uri.parse(uri))
                                            mediaPlayer.setAudioAttributes(
                                                AudioAttributes.Builder()
                                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                                    .build()
                                            )
                                            mediaPlayer.prepare()
                                            mediaPlayer.start()
                                            mediaPlayer.setOnCompletionListener { it.release() }
                                        } catch (e2: Exception) {}
                                    }
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            modifier = Modifier.padding(end = 8.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = getAccentColor(),
                                unselectedColor = Color(0xFF94A3B8)
                            )
                        )
                        Text(
                            text = itemTitle,
                            fontSize = 14.sp,
                            color = if (isSelected) getAccentColor() else getLabelColor(),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (itemTitle != ringtones.last().first) {
                        SettingsDivider()
                    }
                }

                SettingsDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentRingtone?.stop()
                            onSelectExternal()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.select_external_audio),
                        fontSize = 14.sp,
                        color = getAccentColor(),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                currentRingtone?.stop()
                onConfirm(tempSelectedUri)
            }) {
                Text(
                    text = stringResource(R.string.label_ok),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = getAccentColor()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                currentRingtone?.stop()
                onDismiss()
            }) {
                Text(
                    text = stringResource(R.string.label_cancel),
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = getDialogBackground()
    )
}
