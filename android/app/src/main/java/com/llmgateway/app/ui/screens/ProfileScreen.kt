package com.llmgateway.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llmgateway.app.data.api.ApiClient
import com.llmgateway.app.data.model.ChangePasswordRequest
import com.llmgateway.app.data.model.User
import com.llmgateway.app.ui.components.InputField
import com.llmgateway.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    var user by remember { mutableStateOf<User?>(null) }
    var showChangePassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { try { user = ApiClient.service.me().user } catch (_: Exception) {} }
    }

    Column(Modifier.fillMaxSize().background(White)) {
        Text("我的", fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 16.dp))

        // 用户信息卡
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Black)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(White), contentAlignment = Alignment.Center) {
                Text(user?.username?.firstOrNull()?.uppercase() ?: "U", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Black)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(user?.username ?: "-", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = White)
                Spacer(Modifier.height(2.dp))
                Text(if (user?.role == "admin") "管理员" else "普通用户", fontSize = 12.sp, color = White.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$${"%.2f".format(user?.balance ?: 0.0)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                Text("余额", fontSize = 11.sp, color = White.copy(alpha = 0.6f))
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(White)
                .border(1.dp, Border, RoundedCornerShape(14.dp))
        ) {
            MenuItem("额度中心", Icons.Default.AccountBalanceWallet)
            Divider(modifier = Modifier.padding(start = 52.dp), color = Border, thickness = 0.5.dp)
            MenuItem("微调任务", Icons.Default.Tune)
            Divider(modifier = Modifier.padding(start = 52.dp), color = Border, thickness = 0.5.dp)
            MenuItem("API 文档", Icons.Default.Description)
            Divider(modifier = Modifier.padding(start = 52.dp), color = Border, thickness = 0.5.dp)
            MenuItem("修改密码", Icons.Default.Lock) { showChangePassword = true }
            Divider(modifier = Modifier.padding(start = 52.dp), color = Border, thickness = 0.5.dp)
            MenuItem("设置", Icons.Default.Settings)
        }

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(White)
                .border(1.dp, Border, RoundedCornerShape(14.dp))
        ) {
            MenuItem("关于", Icons.Default.Info)
            Divider(modifier = Modifier.padding(start = 52.dp), color = Border, thickness = 0.5.dp)
            MenuItem("帮助与反馈", Icons.Default.HelpOutline)
        }

        Spacer(Modifier.weight(1f))

        Text(
            "退出登录",
            color = Error,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .clickable {
                    scope.launch {
                        ApiClient.getTokenStore().clearToken()
                        onLogout()
                    }
                }
        )
        Spacer(Modifier.height(16.dp))
    }

    // 修改密码弹窗
    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onSuccess = { showChangePassword = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Text("修改密码", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 16.dp))

            InputField(oldPassword, { oldPassword = it }, Modifier.fillMaxWidth(), "当前密码", isPassword = true)
            Spacer(Modifier.height(12.dp))
            InputField(newPassword, { newPassword = it }, Modifier.fillMaxWidth(), "新密码（至少6位）", isPassword = true)
            Spacer(Modifier.height(12.dp))
            InputField(confirmPassword, { confirmPassword = it }, Modifier.fillMaxWidth(), "确认新密码", isPassword = true)
            Spacer(Modifier.height(16.dp))

            if (error.isNotEmpty()) {
                Text(error, color = Error, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                        .background(BgSecondary).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) { Text("取消", fontSize = 14.sp, color = TextSecondary) }

                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (loading) TextMuted else Black)
                        .clickable(enabled = !loading) {
                            if (newPassword.length < 6) { error = "新密码至少 6 位"; return@clickable }
                            if (newPassword != confirmPassword) { error = "两次密码不一致"; return@clickable }
                            error = ""
                            loading = true
                            scope.launch {
                                try {
                                    ApiClient.service.changePassword(ChangePasswordRequest(oldPassword, newPassword))
                                    onSuccess()
                                } catch (e: Exception) {
                                    error = if (e.message?.contains("401") == true) "当前密码错误" else "修改失败，请稍后重试"
                                } finally { loading = false }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = White, strokeWidth = 2.dp)
                    else Text("确认修改", fontSize = 14.sp, color = White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(title: String, icon: ImageVector, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}
