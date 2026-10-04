package com.llmgateway.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

@Composable
fun ConsoleScreen() {
    var usage by remember { mutableStateOf<UsageStats?>(null) }
    var range by remember { mutableStateOf("today") }
    val scope = rememberCoroutineScope()

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
                    FeatureEntry("API Key", Icons.Default.Key, Modifier.weight(1f))
                    FeatureEntry("渠道配置", Icons.Default.Cloud, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureEntry("请求日志", Icons.Default.Receipt, Modifier.weight(1f))
                    FeatureEntry("微调任务", Icons.Default.Tune, Modifier.weight(1f))
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
private fun FeatureEntry(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable { }
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
