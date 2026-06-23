package com.example.myapp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapp1.ui.theme.MyApp1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApp1Theme {
                AppRoot()
            }
        }
    }
}

// 보여줄 화면 종류. 각 화면의 탭 라벨/아이콘(이모지)을 enum 에 함께 담아 네비게이션 바가 바로 쓰게 한다.
//   enum = '정해진 몇 개 중 하나'를 안전하게 표현(백엔드의 enum 과 동일).
private enum class Screen(val label: String, val emoji: String) {
    HABITS("습관", "🔥"),
    CHARACTER("캐릭터", "🧑‍🎤"),
    PIXEL("픽셀 아트", "🎨")
}

// 앱의 최상위 화면. Scaffold 의 '하단 바' 슬롯에 네비게이션을 두고, 본문에 선택된 화면을 그린다.
//   기존: 상단에 밋밋한 Button/OutlinedButton 3개 → 지금: 요즘 앱 표준인 하단 네비게이션 바.
//   아직 네비게이션 라이브러리 없이 'remember 상태로 화면 골라 그리기' 수준의 입문용 전환.
@Composable
private fun AppRoot() {
    // 지금 보고 있는 화면. 기본은 습관 목록.
    var current by remember { mutableStateOf(Screen.HABITS) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // NavigationBar = Material3 표준 하단 바. 선택된 항목 뒤에 '알약(pill)' 모양 강조가 자동으로 깔린다.
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = current == screen,
                        onClick = { current = screen },
                        icon = { Text(screen.emoji, style = MaterialTheme.typography.titleLarge) },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // innerPadding = 시스템 바(상태바)와 하단 네비게이션 바에 안 가리도록 본문에 줄 여백.
        Box(modifier = Modifier.padding(innerPadding)) {
            when (current) {
                Screen.HABITS -> HabitListScreen()
                Screen.CHARACTER -> CharacterScreen()
                Screen.PIXEL -> PixelArtFeature()
            }
        }
    }
}
