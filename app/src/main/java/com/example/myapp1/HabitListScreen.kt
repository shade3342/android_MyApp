package com.example.myapp1

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapp1.ui.theme.MyApp1Theme

// 데이터 모델. Kotlin 'data class' = 백엔드의 DTO/record 같은 것.
//   equals()/hashCode()/toString()/copy() 를 컴파일러가 자동 생성해줌.
//   id = 리스트에서 각 항목을 구분하는 고유 키 (DB의 PK 같은 역할).
data class Habit(
    val id: Int,
    val emoji: String,
    val name: String,
    val done: Boolean = false
)

@Composable
fun HabitListScreen(modifier: Modifier = Modifier) {
    // 습관 목록을 '상태'로 보관. 항목의 done 이 바뀌면 새 리스트로 교체 → 화면 자동 갱신.
    var habits by rememberSaveable {
        mutableStateOf(
            listOf(
                Habit(1, "🚶", "30분 걷기"),
                Habit(2, "📖", "책 10쪽 읽기"),
                Habit(3, "💧", "물 8잔 마시기"),
                Habit(4, "🧘", "5분 명상하기"),
                Habit(5, "😴", "밤 11시 전 자기"),
            )
        )
    }

    // 파생 값: 매 리컴포지션마다 done 개수를 다시 계산. 별도 상태로 둘 필요 없음.
    val doneCount = habits.count { it.done }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "오늘의 습관", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$doneCount / ${habits.size} 완료",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn = 스크롤되는 리스트. '화면에 보이는 항목만' 그려서 항목이 많아도 빠름.
        //   (옛 Android 의 RecyclerView 를 한 줄로 대체한 것)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // items(...) 안에서 각 항목을 어떻게 그릴지 한 번만 적으면 전체에 반복 적용됨.
            //   key = 항목 고유 식별자. 순서가 바뀌어도 Compose 가 같은 항목을 추적해 효율적.
            items(habits, key = { it.id }) { habit ->
                HabitRow(
                    habit = habit,
                    onToggle = {
                        // 불변(immutable) 방식 — 원본을 직접 수정하지 않고,
                        // 클릭된 항목만 done 을 뒤집은 '새 리스트'를 만들어 통째로 교체한다.
                        habits = habits.map {
                            if (it.id == habit.id) it.copy(done = !it.done) else it
                        }
                    }
                )
            }
        }
    }
}

// 리스트의 한 줄(습관 카드). 화면 조각을 작은 함수로 쪼개면 재사용/가독성이 좋아진다.
//   private = 이 파일 안에서만 쓰는 내부용 함수라는 표시.
@Composable
private fun HabitRow(habit: Habit, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() } // 카드 전체를 탭하면 onToggle 실행
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = habit.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyLarge,
                // weight(1f) = 남는 가로 공간을 이 Text 가 모두 차지 → 오른쪽 체크표시를 끝으로 민다.
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (habit.done) "✅" else "⬜",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitListScreenPreview() {
    MyApp1Theme {
        HabitListScreen()
    }
}
