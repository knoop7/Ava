package com.example.ava.ui.screens.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ava.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogScope.ActionDialog(
    title: String = "",
    description: String = "",
    confirmEnabled: Boolean = true,
    onDismissRequest: () -> Unit = {},
    onConfirmRequest: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    
    val accentColor = androidx.compose.ui.graphics.Color(0xFF4F46E5) 
    val titleColor = androidx.compose.ui.graphics.Color(0xFF1E293B) 
    val labelColor = androidx.compose.ui.graphics.Color(0xFF334155) 
    val subLabelColor = androidx.compose.ui.graphics.Color(0xFF94A3B8) 
    
    BasicAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = androidx.compose.ui.graphics.Color.White,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = labelColor,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Box(modifier = Modifier.weight(weight = 1f, fill = false)) {
                    content()
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.align(Alignment.End)
                ) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            closeDialog()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.label_cancel),
                            color = subLabelColor,
                            fontSize = 14.sp
                        )
                    }
                    TextButton(
                        enabled = confirmEnabled,
                        onClick = {
                            onConfirmRequest()
                            closeDialog()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.label_ok),
                            color = if (confirmEnabled) accentColor else subLabelColor,
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Stable
class DialogScope {
    private val _isDialogOpen = MutableStateFlow(false)
    val isDialogOpen get() = _isDialogOpen.asStateFlow()

    fun openDialog() {
        _isDialogOpen.value = true
    }

    fun closeDialog() {
        _isDialogOpen.value = false
    }
}