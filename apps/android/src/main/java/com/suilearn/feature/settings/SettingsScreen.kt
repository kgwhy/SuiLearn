package com.suilearn.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.ui.AppSectionCard

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    var confirmReset by remember { mutableStateOf(false) }
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("重置本地数据？") },
            text = { Text("这将清空答题记录、错题本、收藏和练习会话。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    settingsViewModel.onEvent(SettingsEvent.ResetLocalData)
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("取消") } },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            AppSectionCard(title = "我的", subtitle = "管理本地题包与学习数据。") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("当前题包：${uiState.studyPackName ?: "未初始化"}")
                    Text("题包版本：${uiState.packVersion?.toString() ?: "-"}")
                    OutlinedButton(onClick = { confirmReset = true }) {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("重置数据")
                    }
                }
            }
        }
    }
}
