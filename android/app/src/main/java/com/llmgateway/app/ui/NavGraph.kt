package com.llmgateway.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.llmgateway.app.data.api.ApiClient
import com.llmgateway.app.ui.theme.Black
import com.llmgateway.app.ui.theme.White
import com.llmgateway.app.ui.screens.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val tokenStore = ApiClient.getTokenStore()
    val scope = rememberCoroutineScope()

    // 认证状态：checking(验证中) / loggedIn / loggedOut
    var authState by remember { mutableStateOf("checking") }

    // 启动时验证 token 有效性
    LaunchedEffect(Unit) {
        val token = tokenStore.tokenValue
        if (token.isEmpty()) {
            authState = "loggedOut"
        } else {
            try {
                ApiClient.service.me()
                authState = "loggedIn"
            } catch (e: Exception) {
                tokenStore.clearToken()
                authState = "loggedOut"
            }
        }
    }

    // 监听全局 401 事件，自动跳转登录
    LaunchedEffect(Unit) {
        ApiClient.unauthorizedEvents.collect {
            authState = "loggedOut"
            navController.navigate(Screen.Login.route) { popUpTo(0) }
        }
    }

    if (authState == "checking") {
        Box(Modifier.fillMaxSize().background(White), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).background(Black), contentAlignment = Alignment.Center) {
                    Text("G", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Black, strokeWidth = 2.dp)
            }
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (authState == "loggedIn") Screen.Main.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    authState = "loggedIn"
                    navController.navigate(Screen.Main.route) { popUpTo(0) }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {
                    scope.launch { tokenStore.clearToken() }
                    authState = "loggedOut"
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                }
            )
        }
    }
}
