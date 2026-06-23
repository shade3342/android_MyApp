package com.example.myapp1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.myapp1.ui.theme.MyApp1Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// 종이인형(2D 레이어 합성) 화면 — Phase 1의 '캐릭터 꾸미기' 축.
//
//   레이어 합성: Box 안에 같은 자리에 겹쳐 쌓는다(z-order). 배경 → 몸 → 머리 → 옷 → 내 픽셀 아이템들.
//   캐릭터 무대 자체를 하나의 '커다란 픽셀 화면'(GRID_COLS×GRID_ROWS 격자)으로 본다.
//   픽셀 아이템(픽셀 아트 탭에서 저장한 그림)은 '여러 개' 붙일 수 있고, 각각:
//     - 격자 칸(col,row) 위에 '물려서' 놓이고, 드래그하면 옆 칸으로 한 칸씩 스냅 이동.
//     - 크기는 '몇 칸을 차지하는지(spanCells)'로 조절, 앞/뒤로 레이어 순서 변경, 빼기.
//   아이템 = PlacedItem(그림 + 격자 좌표 col,row + 한 변이 차지하는 칸 수). 목록 순서 = z-order(뒤가 위).

// 캐릭터 무대 격자 — 가로 16칸 × 세로 24칸(2:3 비율, 칸은 정사각형).
private const val GRID_COLS = 16
private const val GRID_ROWS = 24
private val CANVAS_W = 200.dp
private val CANVAS_H = 300.dp
private val CELL = CANVAS_W / GRID_COLS   // = 12.5dp. (CANVAS_H/GRID_ROWS 도 동일 → 정사각 칸)

private val hairOptions = listOf(R.drawable.hair_short, R.drawable.hair_long)
private val outfitOptions = listOf(R.drawable.outfit_red, R.drawable.outfit_blue)

// 캐릭터 위에 얹은 픽셀 아이템 한 개. 위치는 px 가 아니라 '격자 칸 좌표'.
private data class PlacedItem(
    val id: Long,
    val doc: PixelDoc,
    val col: Int,       // 격자 칸 가로 좌표(좌상단 기준, 0..GRID_COLS-spanCells)
    val row: Int,       // 〃 세로
    val spanCells: Int  // 한 변이 차지하는 칸 수
)

@Composable
fun CharacterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var hairIndex by remember { mutableIntStateOf(0) }
    var outfitIndex by remember { mutableIntStateOf(0) }

    // 저장된 픽셀 아트 목록(탭하면 캐릭터에 추가).
    var pixelDocs by remember { mutableStateOf<List<PixelDoc>>(emptyList()) }
    LaunchedEffect(Unit) {
        pixelDocs = withContext(Dispatchers.IO) {
            PixelArtStore.list(context).map { PixelArtStore.load(it) }
        }
    }

    // 착용 중인 아이템들 + 선택된 아이템 + id 발급기.
    val items = remember { mutableStateListOf<PlacedItem>() }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var nextId by remember { mutableLongStateOf(1L) }

    fun update(id: Long, transform: (PlacedItem) -> PlacedItem) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) items[i] = transform(items[i])
    }

    // 격자 칸 단위 '상대' 이동. 드래그가 한 칸 분량을 넘으면 dCol/dRow(±1…)이 들어와 그만큼 옮긴다.
    //   현재 좌표 기준으로 더한 뒤 무대 밖으로 안 나가게 clamp(아이템이 차지하는 칸 수만큼 여유를 둠).
    fun moveByCells(id: Long, dCol: Int, dRow: Int) = update(id) {
        it.copy(
            col = (it.col + dCol).coerceIn(0, GRID_COLS - it.spanCells),
            row = (it.row + dRow).coerceIn(0, GRID_ROWS - it.spanCells)
        )
    }

    // 크기(차지 칸 수) 변경 — 칸 수가 바뀌면 무대를 벗어날 수 있으니 위치도 다시 clamp.
    fun setSpan(id: Long, newSpan: Int) = update(id) {
        val span = newSpan.coerceIn(2, minOf(GRID_COLS, GRID_ROWS))
        it.copy(
            spanCells = span,
            col = it.col.coerceIn(0, GRID_COLS - span),
            row = it.row.coerceIn(0, GRID_ROWS - span)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "내 캐릭터",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "머리·옷·픽셀 아이템으로 나만의 캐릭터를 꾸며요 🎀",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        // 캐릭터를 둥근 카드 무대 위에 올려 통일감 있는 '전시' 느낌으로.
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                CharacterPreview(
                    hairRes = hairOptions[hairIndex],
                    outfitRes = outfitOptions[outfitIndex],
                    items = items,
                    selectedId = selectedId,
                    onSelect = { selectedId = it },
                    onMove = { id, dCol, dRow -> moveByCells(id, dCol, dRow) }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { hairIndex = (hairIndex + 1) % hairOptions.size },
                modifier = Modifier.weight(1f)
            ) { Text("머리 바꾸기") }
            Button(
                onClick = { outfitIndex = (outfitIndex + 1) % outfitOptions.size },
                modifier = Modifier.weight(1f)
            ) { Text("옷 바꾸기") }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // ── 선택된 아이템 조절 패널 ──
        val selected = items.firstOrNull { it.id == selectedId }
        if (selected != null) {
            Text(
                "선택된 아이템 — 격자 위에서 드래그하면 한 칸씩 이동",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("크기", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = selected.spanCells.toFloat(),
                    onValueChange = { v -> setSpan(selected.id, v.roundToInt()) },
                    valueRange = 2f..minOf(GRID_COLS, GRID_ROWS).toFloat(),
                    steps = minOf(GRID_COLS, GRID_ROWS) - 3, // 2..16 사이 정수 칸에만 멈추도록
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("${selected.spanCells}칸", modifier = Modifier.width(44.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallBtn("뒤로", outlined = true) {
                    // 목록 맨 앞으로 = 가장 뒤(아래) 레이어로.
                    val i = items.indexOfFirst { it.id == selected.id }
                    if (i > 0) { val it = items.removeAt(i); items.add(0, it) }
                }
                SmallBtn("앞으로", outlined = true) {
                    // 목록 맨 뒤로 = 가장 앞(위) 레이어로.
                    val i = items.indexOfFirst { it.id == selected.id }
                    if (i in 0 until items.lastIndex) { val it = items.removeAt(i); items.add(it) }
                }
                SmallBtn("빼기", outlined = true) {
                    items.removeAll { it.id == selected.id }
                    selectedId = null
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── 내가 그린 픽셀 아이템 (탭해서 추가) ──
        Text(
            text = "내가 그린 픽셀 아이템 (탭해서 추가)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (pixelDocs.isEmpty()) {
            Text(
                "‘픽셀 아트’ 탭에서 그림을 저장하면 여기서 캐릭터에 붙일 수 있어요.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pixelDocs, key = { it.file?.name ?: "" }) { doc ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(1.dp, Color(0xFFBDBDBD))
                            .clickable {
                                val id = nextId; nextId++
                                // 기본 8칸, 무대 중앙 칸에 놓는다.
                                val span = 8
                                items.add(
                                    PlacedItem(
                                        id = id,
                                        doc = doc,
                                        col = (GRID_COLS - span) / 2,
                                        row = (GRID_ROWS - span) / 2,
                                        spanCells = span
                                    )
                                )
                                selectedId = id
                            }
                    ) {
                        PixelImage(
                            pixels = doc.pixels,
                            gridSize = doc.gridSize,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SmallBtn(label: String, outlined: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) {
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
    }
}

// 순수 합성 위젯. 무대(격자) + 머리/옷 + 착용 아이템들을 그리고, 아이템 격자 스냅 이동/선택을 위임.
//   onMove(id, dCol, dRow) = '상대' 이동(드래그가 한 칸을 넘을 때마다 ±1칸씩). 부모가 현재 좌표에 더하고 clamp.
@Composable
private fun CharacterPreview(
    hairRes: Int,
    outfitRes: Int,
    items: List<PlacedItem>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onMove: (Long, Int, Int) -> Unit
) {
    Box(modifier = Modifier.size(width = CANVAS_W, height = CANVAS_H)) {
        Layer(R.drawable.char_bg)
        Layer(R.drawable.char_body)
        Layer(hairRes)
        Layer(outfitRes)

        // ── 격자 라인 ── 무대 전체를 GRID_COLS×GRID_ROWS 로 나눈 얇은 보조선. 아이템이 이 칸에 물린다.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width / GRID_COLS
            val ch = size.height / GRID_ROWS
            val line = Color(0x22000000)
            for (i in 0..GRID_COLS) drawLine(line, Offset(i * cw, 0f), Offset(i * cw, size.height), 1f)
            for (i in 0..GRID_ROWS) drawLine(line, Offset(0f, i * ch), Offset(size.width, i * ch), 1f)
        }

        // 착용 아이템들(목록 순서 = z-order, 뒤가 위). 투명 칸은 안 그려 캐릭터가 비친다.
        items.forEach { item ->
            val isSelected = item.id == selectedId
            PixelImage(
                pixels = item.doc.pixels,
                gridSize = item.doc.gridSize,
                transparentAsChecker = false,
                modifier = Modifier
                    // 좌상단 기준으로 '칸 좌표 × 칸 크기'만큼 밀어 격자에 정확히 맞춘다.
                    .align(Alignment.TopStart)
                    .offset { IntOffset((item.col * CELL.toPx()).roundToInt(), (item.row * CELL.toPx()).roundToInt()) }
                    .size(CELL * item.spanCells)
                    .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary) else Modifier)
                    .pointerInput(item.id) {
                        // 누르면 선택, 드래그하면 한 칸 단위로 스냅 이동.
                        //   드래그 픽셀 이동량을 누적(accX/accY)해 칸 수로 환산하고,
                        //   '아직 적용 안 한 칸 차이(dCol/dRow)'만 부모에 상대 이동으로 보낸다.
                        //   (현재 좌표를 직접 읽지 않으므로 pointerInput 이 안 바뀌어도 항상 옳게 동작)
                        val cellPx = CELL.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            onSelect(item.id)
                            down.consume()
                            var accX = 0f
                            var accY = 0f
                            var appliedCols = 0
                            var appliedRows = 0
                            drag(down.id) { change ->
                                accX += change.positionChange().x
                                accY += change.positionChange().y
                                val targetCols = (accX / cellPx).roundToInt()
                                val targetRows = (accY / cellPx).roundToInt()
                                val dCol = targetCols - appliedCols
                                val dRow = targetRows - appliedRows
                                if (dCol != 0 || dRow != 0) {
                                    appliedCols = targetCols
                                    appliedRows = targetRows
                                    onMove(item.id, dCol, dRow)
                                }
                                change.consume()
                            }
                        }
                    }
            )
        }
    }
}

@Composable
private fun Layer(resId: Int) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(100f / 150f),
        contentScale = ContentScale.Fit
    )
}

@Preview(showBackground = true)
@Composable
fun CharacterScreenPreview() {
    MyApp1Theme {
        CharacterScreen()
    }
}
