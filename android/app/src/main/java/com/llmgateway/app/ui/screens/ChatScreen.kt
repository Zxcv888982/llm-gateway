package com.llmgateway.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llmgateway.app.data.api.ApiClient
import com.llmgateway.app.data.model.ChatMessage
import com.llmgateway.app.data.model.ChatRequest
import com.llmgateway.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiMessage(
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(listOf<UiMessage>()) }
    var input by remember { mutableStateOf("") }
    var currentModel by remember { mutableStateOf("gpt-4o") }
    var showModelPicker by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var lastUserMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val models = listOf("gpt-4o", "gpt-4o-mini", "claude-3-5-sonnet", "deepseek-chat", "qwen-plus", "glm-4")

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun sendMessage(userMsg: String) {
        if (isSending) return
        lastUserMessage = userMsg
        isSending = true
        // 移除上一条失败的助手消息（如果有）
        if (messages.isNotEmpty() && messages.last().isError) {
            messages = messages.dropLast(1)
        }
        messages = messages + UiMessage("user", userMsg)
        messages = messages + UiMessage("assistant", "", isStreaming = true)

        scope.launch {
            try {
                val history = messages.dropLast(1).map { ChatMessage(it.role, it.content) }
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.service.chatCompletion(
                        ChatRequest(model = currentModel, messages = history, stream = false)
                    )
                }
                val reply = resp.choices.firstOrNull()?.message?.content ?: ""
                messages = messages.dropLast(1) + UiMessage("assistant", reply)
            } catch (e: Exception) {
                messages = messages.dropLast(1) + UiMessage("assistant", "请求失败，点击重试", isError = true)
            } finally {
                isSending = false
            }
        }
    }

    fun send() {
        if (input.isBlank() || isSending) return
        val userMsg = input.trim()
        input = ""
        sendMessage(userMsg)
    }

    fun retry() {
        if (lastUserMessage.isBlank() || isSending) return
        sendMessage(lastUserMessage)
    }

    Column(Modifier.fillMaxSize().background(White)) {
        // 顶部模型选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showModelPicker = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(currentModel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }

        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Black), contentAlignment = Alignment.Center) {
                            Text("G", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("有什么可以帮你的？", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("选择上方模型，开始对话", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
            items(messages) { msg ->
                MessageBubble(msg, onRetry = { retry() })
            }
        }

        // 输入栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                cursorBrush = SolidColor(Black),
                textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgSecondary)
                    .border(1.dp, if (input.isNotEmpty()) Black else Border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("输入消息...", color = TextMuted, fontSize = 14.sp)
                    inner()
                }
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (input.isNotBlank() && !isSending) Black else TextMuted)
                    .clickable(enabled = input.isNotBlank() && !isSending) { send() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, null, tint = White, modifier = Modifier.size(18.dp))
            }
        }
    }

    // 模型选择弹窗
    if (showModelPicker) {
        ModalBottomSheet(
            onDismissRequest = { showModelPicker = false },
            containerColor = White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text("选择模型", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
                models.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentModel = model
                                showModelPicker = false
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(model, fontSize = 14.sp, color = if (model == currentModel) Black else TextPrimary,
                            fontWeight = if (model == currentModel) FontWeight.SemiBold else FontWeight.Normal)
                        Spacer(Modifier.weight(1f))
                        if (model == currentModel) {
                            Box(Modifier.size(18.dp).clip(RoundedCornerShape(50)).background(Black), contentAlignment = Alignment.Center) {
                                Text("✓", color = White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: UiMessage, onRetry: () -> Unit) {
    val isUser = msg.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isUser) Black else White)
                .border(if (isUser) 0.dp else 1.dp, if (isUser) Color.Transparent else Border, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                when {
                    msg.isStreaming && msg.content.isEmpty() -> "..."
                    msg.isError -> "请求失败，点击重试"
                    else -> msg.content
                },
                color = when {
                    isUser -> White
                    msg.isError -> Error
                    else -> TextPrimary
                },
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        if (msg.isError && !isUser) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Black, RoundedCornerShape(8.dp))
                    .clickable { onRetry() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("重试", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
