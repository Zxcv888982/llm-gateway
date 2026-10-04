package com.llmgateway.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llmgateway.app.data.api.ApiClient
import com.llmgateway.app.data.model.AuthRequest
import com.llmgateway.app.ui.components.*
import com.llmgateway.app.ui.theme.*
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun handleError(e: Exception, isLogin: Boolean): String {
        return when (e) {
            is ConnectException, is UnknownHostException -> "无法连接服务器，请检查网络或后端地址"
            is SocketTimeoutException -> "连接超时，请稍后重试"
            else -> {
                val msg = e.message ?: ""
                when {
                    msg.contains("401") -> if (isLogin) "用户名或密码错误" else "当前密码错误"
                    msg.contains("409") -> "用户名已存在"
                    msg.contains("400") -> "请检查输入格式"
                    else -> if (isLogin) "登录失败，请稍后重试" else "注册失败，请稍后重试"
                }
            }
        }
    }

    fun submit() {
        if (username.isBlank()) { error = "请输入用户名"; return }
        if (password.length < 6) { error = "密码至少 6 位"; return }
        if (!isLogin && password != confirmPassword) { error = "两次密码不一致"; return }

        error = ""
        loading = true
        scope.launch {
            try {
                val resp = if (isLogin)
                    ApiClient.service.login(AuthRequest(username, password))
                else
                    ApiClient.service.register(AuthRequest(username, password))
                ApiClient.getTokenStore().saveToken(resp.token)
                onLoginSuccess()
            } catch (e: Exception) {
                error = handleError(e, isLogin)
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Black), contentAlignment = Alignment.Center) {
            Text("G", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("LLM Gateway", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(48.dp))

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BgSecondary).padding(4.dp)
        ) {
            TabItem("登录", isLogin, Modifier.weight(1f)) { isLogin = true; error = "" }
            TabItem("注册", !isLogin, Modifier.weight(1f)) { isLogin = false; error = "" }
        }
        Spacer(Modifier.height(24.dp))

        InputField(username, { username = it }, Modifier.fillMaxWidth(), "用户名", leadingIcon = Icons.Default.Person)
        Spacer(Modifier.height(12.dp))
        InputField(password, { password = it }, Modifier.fillMaxWidth(), "密码", isPassword = true, leadingIcon = Icons.Default.Lock)
        if (!isLogin) {
            Spacer(Modifier.height(12.dp))
            InputField(confirmPassword, { confirmPassword = it }, Modifier.fillMaxWidth(), "确认密码", isPassword = true, leadingIcon = Icons.Default.Lock)
        }
        Spacer(Modifier.height(16.dp))

        if (error.isNotEmpty()) {
            Text(error, color = Error, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (loading || username.isBlank() || password.length < 6) TextMuted else Black)
                .clickable(enabled = !loading && username.isNotBlank() && password.length >= 6) { submit() },
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
            } else {
                Text(if (isLogin) "登录" else "注册", color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(16.dp))

        val context = LocalContext.current
        Text(
            "忘记密码？",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.clickable {
                Toast.makeText(context, "请联系管理员重置密码", Toast.LENGTH_SHORT).show()
            }
        )
        Spacer(Modifier.height(16.dp))
        Text("默认管理员 admin / admin123", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun TabItem(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) TextPrimary else TextSecondary)
    }
}
