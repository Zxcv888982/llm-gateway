package com.llmgateway.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llmgateway.app.data.api.ApiClient
import com.llmgateway.app.data.model.*
import com.llmgateway.app.ui.components.*
import com.llmgateway.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen() {
    var usage by remember { mutableStateOf<UsageStats?>(null) }
    var range by remember { mutableStateOf("today") }
    var showKeyManager by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(range) {
        scope.launch {
            try { usage = ApiClient.service.usage(range).stats } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().background(White)) {
        Text("控制台", fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp))

        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            // 用量卡片
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("用量概览", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        RangeToggle(range) { range = it }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        StatItem("请求数", usage?.requests?.toString() ?: "-", Modifier.weight(1f))
                        StatItem("Token", formatTokens(usage?.totalTokens), Modifier.weight(1f))
                        StatItem("费用", "$${"%.4f".format(usage?.cost ?: 0.0)}", Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 功能入口
            item {
                SectionTitle("功能")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureEntry("API Key", Icons.Default.Key, Modifier.weight(1f), onClick = { showKeyManager = true })
                    FeatureEntry("渠道配置", Icons.Default.Cloud, Modifier.weight(1f), onClick = {
                        Toast.makeText(context, "敬请期待", Toast.LENGTH_SHORT).show()
                    })
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureEntry("请求日志", Icons.Default.Receipt, Modifier.weight(1f), onClick = {
                        Toast.makeText(context, "敬请期待", Toast.LENGTH_SHORT).show()
                    })
                    FeatureEntry("微调任务", Icons.Default.Tune, Modifier.weight(1f), onClick = {
                        Toast.makeText(context, "敬请期待", Toast.LENGTH_SHORT).show()
                    })
                }
                Spacer(Modifier.height(16.dp))
            }

            // 最近请求
            item {
                SectionTitle("最近请求")
                Spacer(Modifier.height(10.dp))
            }
            item { RecentRequestsList() }
        }
    }

    // Key 管理面板
    if (showKeyManager) {
        KeyManagerBottomSheet(onDismiss = { showKeyManager = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyManagerBottomSheet(onDismiss: () -> Unit) {
    var keys by remember { mutableStateOf(listOf<ApiKey>()) }
    var loading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newKeyName by remember { mutableStateOf("") }
    var createdKey by remember { mutableStateOf<ApiKey?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadKeys() {
        loading = true
        scope.launch {
            try {
                keys = ApiClient.service.listKeys().keys
                loading = false
            } catch (_: Exception) {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadKeys() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            // 标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("API Key 管理", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Black)
                        .clickable { showCreateDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("创建新 Key", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // 新创建的 Key 展示（可复制）
            createdKey?.let { key ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SuccessBg)
                        .padding(12.dp)
                ) {
                    Text("创建成功，请立即复制保存：", fontSize = 12.sp, color = Success, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text(key.key, fontSize = 12.sp, color = TextPrimary, lineHeight = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("点击复制", fontSize = 11.sp, color = Black, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("api_key", key.key))
                            Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                        })
                }
                Spacer(Modifier.height(8.dp))
            }

            // Key 列表
            when {
                loading -> {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Black, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
                keys.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("暂无 API Key", color = TextMuted, fontSize = 13.sp)
                    }
                }
                else -> {
                    keys.forEach { key ->
                        KeyItem(
                            key = key,
                            onDelete = {
                                scope.launch {
                                    try {
                                        ApiClient.service.deleteKey(key.id)
                                        loadKeys()
                                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 创建对话框
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newKeyName = "" },
            containerColor = White,
            title = { Text("创建新 Key", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                BasicTextField(
                    value = newKeyName,
                    onValueChange = { newKeyName = it },
                    cursorBrush = SolidColor(Black),
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgSecondary)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (newKeyName.isEmpty()) Text("输入 Key 名称", color = TextMuted, fontSize = 14.sp)
                        inner()
                    }
                )
            },
            confirmButton = {
                Text(
                    "创建",
                    color = Black,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable(enabled = newKeyName.isNotBlank()) {
                            scope.launch {
                                try {
                                    val newKey = ApiClient.service.createKey(CreateKeyRequest(name = newKeyName.trim()))
                                    createdKey = newKey
                                    showCreateDialog = false
                                    newKeyName = ""
                                    loadKeys()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "创建失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            },
            dismissButton = {
                Text(
                    "取消",
                    color = TextSecondary,
                    modifier = Modifier
                        .clickable { showCreateDialog = false; newKeyName = "" }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        )
    }
}

@Composable
private fun KeyItem(key: ApiKey, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BgSecondary)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(key.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                if (key.key.length > 12) key.key.take(12) + "..." else key.key,
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    if (key.status == "active") "正常" else "禁用",
                    if (key.status == "active") BadgeType.Success else BadgeType.Error
                )
                Spacer(Modifier.width(8.dp))
                Text("${formatTokens(key.usedTokens)} tok", fontSize = 11.sp, color = TextSecondary)
            }
        }
        Icon(
            Icons.Default.Delete,
            contentDescription = "删除",
            tint = Error,
            modifier = Modifier
                .size(20.dp)
                .clickable { onDelete() }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun RangeToggle(current: String, onChange: (String) -> Unit) {
    val ranges = listOf("今日" to "today", "本周" to "week", "本月" to "month")
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(BgSecondary).padding(3.dp)
    ) {
        ranges.forEach { (label, key) ->
            val selected = current == key
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) White else Color.Transparent)
                    .clickable { onChange(key) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) TextPrimary else TextSecondary)
            }
        }
    }
}

@Composable
private fun FeatureEntry(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Black), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun RecentRequestsList() {
    var logs by remember { mutableStateOf(listOf<RequestLog>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch { try { logs = ApiClient.service.logs(10).logs } catch (_: Exception) {} }
    }

    if (logs.isEmpty()) {
        EmptyState(Icons.Default.Receipt, "暂无请求记录")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            logs.forEach { log ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (log.status == "success") Success else Error))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(log.model, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(formatTime(log.createdAt), fontSize = 11.sp, color = TextMuted)
                    }
                    Text("${log.totalTokens} tok", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.width(12.dp))
                    Text("${log.latencyMs}ms", fontSize = 12.sp, color = TextMuted)
                }
            }
        }
    }
}

private fun formatTokens(tokens: Long?): String {
    if (tokens == null) return "-"
    return when {
        tokens >= 1_000_000 -> "%.1fM".format(tokens / 1_000_000.0)
        tokens >= 1_000 -> "%.1fK".format(tokens / 1_000.0)
        else -> tokens.toString()
    }
}

private fun formatTime(ts: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(ts * 1000))
}
