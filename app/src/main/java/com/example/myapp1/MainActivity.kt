package com.example.myapp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.myapp1.ui.theme.MyApp1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApp1Theme {
                // Scaffold = 앱 화면의 기본 뼈대(상단바/하단바/본문 영역 등 자리 제공).
                // innerPadding = 시스템 바(상태바 등)에 안 가리도록 본문에 줄 여백.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 현재 보여줄 화면. CounterScreen() 으로 바꾸면 카운터가 나옴.
                    HabitListScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
