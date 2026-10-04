package com.llmgateway.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llmgateway.app.data.api.ApiClient
import com.llmgateway.app.data.model.ModelInfo
import com.llmgateway.app.ui.components.Badge
import com.llmgateway.app.ui.components.BadgeType
import com.llmgateway.app.ui.theme.*
import kotlinx.coroutines.launch

private val categories = listOf("全部", "对话", "代码", "图像", "嵌入")

@Composable
fun ModelsScreen() {
    var models by remember { mutableStateOf(listOf<ModelInfo>()) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("全部") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun loadModels() {
        loading = true
        error = ""
        scope.launch {
            try {
                models = ApiClient.service.modelCatalog().models
                loading = false
            } catch (e: Exception) {
                error = "加载失败，请检查网络后重试"
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadModels() }

    val filtered = models.filter {
        (category == "全部" || it.category == categoryMap(category)) &&
        (search.isEmpty() || it.name.contains(search, true) || it.provider.contains(search, true))
    }

    Column(Modifier.fillMaxSize().background(White)) {
        // 标题
        Text("模型大全", fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp))

        // 搜索框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgSecondary),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = search, onValueChange = { search = it },
                cursorBrush = SolidColor(Black),
                textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (search.isEmpty()) Text("搜索模型或厂商", color = TextMuted, fontSize = 13.sp)
                    inner()
                }
            )
            Spacer(Modifier.width(12.dp))
        }

        Spacer(Modifier.height(14.dp))

        // 分类 Tab
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val selected = category == cat
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Black else BgSecondary)
                        .clickable { category = cat }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(cat, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) White else TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 内容区域
        when {
            loading -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Black, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            }
            error.isNotEmpty() -> {
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error, color = Error, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Black, RoundedCornerShape(8.dp))
                            .clickable { loadModels() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, null, tint = Black, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("重新加载", color = Black, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { model ->
                        ModelCard(model)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCard(model: ModelInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgSecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(model.name.first().toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(model.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text("${model.provider} · ${model.context}", fontSize = 11.sp, color = TextSecondary)
        }
        Badge(model.category, if (model.category == "chat") BadgeType.Dark else BadgeType.Default)
    }
}

private fun categoryMap(cat: String) = when (cat) {
    "对话" -> "chat"
    "代码" -> "code"
    "图像" -> "image"
    "嵌入" -> "embedding"
    else -> ""
}
