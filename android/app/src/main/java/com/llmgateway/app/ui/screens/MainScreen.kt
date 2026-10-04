package com.llmgateway.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llmgateway.app.ui.theme.*

private val tabs = listOf(
    BottomTab("聊天", Icons.Default.Chat),
    BottomTab("模型", Icons.Default.GridView),
    BottomTab("控制台", Icons.Default.Analytics),
    BottomTab("我的", Icons.Default.Person),
)

private data class BottomTab(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(onLogout: () -> Unit) {
    var selected by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = White,
        bottomBar = {
            NavigationBar(
                containerColor = White,
                tonalElevation = 0.dp,
                modifier = Modifier.height(64.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = {
                            Icon(
                                tab.icon, null,
                                tint = if (selected == index) Black else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected == index) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                                color = if (selected == index) Black else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Black,
                            unselectedIconColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selected) {
                0 -> ChatScreen()
                1 -> ModelsScreen()
                2 -> ConsoleScreen()
                3 -> ProfileScreen(onLogout)
            }
        }
    }
}
