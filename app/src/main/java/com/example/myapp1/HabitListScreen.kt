package com.example.myapp1

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapp1.data.HabitEntity
import com.example.myapp1.ui.theme.MyApp1Theme

// 화면 1: ViewModel 과 연결되는 '상태 보유' 진입점.
//   viewModel() = 이 화면에 묶인 HabitViewModel 을 가져옴(없으면 생성, 회전해도 같은 걸 재사용).
//     백엔드로 치면 Controller 가 Service 를 주입받는 자리.
//   collectAsState() = ViewModel 의 StateFlow 를 구독해 '현재 목록'을 상태로 받음.
//     DB(habits 테이블)가 바뀌면 새 목록이 흘러들어와 이 화면이 자동 리컴포지션(갱신)된다.
//   → 더 이상 화면이 데이터를 '들고' 있지 않다. DB 가 단일 진실 공급원(source of truth).
@Composable
fun HabitListScreen(
    modifier: Modifier = Modifier,
    viewModel: HabitViewModel = viewModel()
) {
    val habits by viewModel.habits.collectAsState()
    // 그리기는 아래 '상태 없는' 화면에 위임. 탭 이벤트는 ViewModel.toggle 로 넘긴다.
    HabitListContent(
        habits = habits,
        onToggle = { habit -> viewModel.toggle(habit) },
        modifier = modifier
    )
}

// 화면 2: '상태 없는(stateless)' 순수 화면. 받은 목록을 그리고, 탭하면 onToggle 만 호출.
//   ViewModel 을 모름 → Preview 에서 가짜 데이터로 그대로 띄울 수 있다(미리보기가 안 깨짐).
@Composable
private fun HabitListContent(
    habits: List<HabitEntity>,
    onToggle: (HabitEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // 파생 값: 매 리컴포지션마다 done 개수를 다시 계산. 별도 상태로 둘 필요 없음.
    val doneCount = habits.count { it.done }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        // ── 헤더: 큰 제목 + 가벼운 인사 ──
        Text(
            text = "오늘의 습관",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "작은 실천이 모여 큰 변화를 만들어요 ✨",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        // ── 진행률 카드: 오늘 달성률을 한눈에 ──
        ProgressCard(doneCount = doneCount, total = habits.size)
        Spacer(modifier = Modifier.height(20.dp))

        // LazyColumn = 스크롤되는 리스트. '화면에 보이는 항목만' 그려서 항목이 많아도 빠름.
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            // key = 항목 고유 식별자(DB PK). 순서가 바뀌어도 Compose 가 같은 항목을 추적.
            items(habits, key = { it.id }) { habit ->
                HabitRow(
                    habit = habit,
                    // 탭하면 ViewModel 에 토글을 요청만 한다. 실제 목록 변경은 DB→Flow 가 처리.
                    onToggle = { onToggle(habit) }
                )
            }
        }
    }
}

// 진행률 카드 — 달성 비율을 큰 퍼센트 + 둥근 막대로 보여준다.
//   primaryContainer(옅은 라벤더) 위에 올려 시선을 끄는 '히어로' 영역.
@Composable
private fun ProgressCard(doneCount: Int, total: Int) {
    val ratio = if (total == 0) 0f else doneCount.toFloat() / total
    // 값이 바뀔 때 막대가 '스르륵' 차오르도록 애니메이션.
    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "progress")
    val percent = (ratio * 100).toInt()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "오늘 달성률",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$doneCount / $total 완료",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

// 리스트의 한 줄(습관 카드). 화면 조각을 작은 함수로 쪼개면 재사용/가독성이 좋아진다.
//   완료 시: 카드가 옅게 물들고, 글자는 흐려지며 취소선, 오른쪽 원이 체크로 채워진다.
@Composable
private fun HabitRow(habit: HabitEntity, onToggle: () -> Unit) {
    // 완료 여부에 따라 카드 배경색을 부드럽게 전환.
    val containerColor by animateColorAsState(
        targetValue = if (habit.done) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        label = "card-bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }, // 카드 전체를 탭하면 onToggle 실행
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 이모지를 둥근 색 배경 안에 넣어 '아이콘 배지'처럼.
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = habit.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = habit.name,
                style = MaterialTheme.typography.titleMedium,
                // 완료되면 글자를 흐리게 + 취소선으로 '끝낸 일' 표시.
                color = if (habit.done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (habit.done) TextDecoration.LineThrough else TextDecoration.None,
                // weight(1f) = 남는 가로 공간을 이 Text 가 모두 차지 → 오른쪽 체크표시를 끝으로 민다.
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            CheckCircle(done = habit.done)
        }
    }
}

// 원형 체크 인디케이터 — 미완료면 빈 원(외곽선), 완료면 채워진 원 + 흰 체크.
//   이모지(✅/⬜)보다 톤이 일관되고 모던하다.
@Composable
private fun CheckCircle(done: Boolean) {
    val bg by animateColorAsState(
        targetValue = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface,
        label = "check-bg"
    )
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (done) Modifier
                else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.onTertiary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// Preview = ViewModel/DB 없이 가짜 데이터로 화면만 미리 보기. 상태 없는 HabitListContent 를 직접 호출.
@Preview(showBackground = true)
@Composable
fun HabitListScreenPreview() {
    MyApp1Theme {
        HabitListContent(
            habits = listOf(
                HabitEntity(1, "🚶", "30분 걷기", done = false),
                HabitEntity(2, "📖", "책 10쪽 읽기", done = true),
                HabitEntity(3, "💧", "물 8잔 마시기", done = false),
            ),
            onToggle = {}
        )
    }
}
