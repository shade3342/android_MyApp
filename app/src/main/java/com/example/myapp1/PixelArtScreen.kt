package com.example.myapp1

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapp1.ui.theme.MyApp1Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// 픽셀 아트 기능 — 갤러리(허브) + 편집기 두 화면으로 구성.
//   PixelArtFeature : '픽셀 아트' 탭의 입구. 갤러리 ↔ 편집기 전환을 remember 상태로 관리(입문용).
//   PixelArtGallery : 저장된 그림 목록 + '새로 그리기'. 탭하면 편집, ✕로 삭제.
//   PixelArtEditor  : 실제 그리는 화면. 새 그림(initial=null) 또는 기존 그림 재수정(initial=문서).
//
//   빈 칸 = '투명'(알파 0). 캔버스엔 체커보드(바둑판)로 투명 영역을 표시한다.
//   저장(프로젝트) = 앱 내부 원본 해상도 PNG(재수정용). 내보내기 = 갤러리에 확대 투명 PNG.
// ─────────────────────────────────────────────────────────────────────────────

private val GRID_OPTIONS = listOf(16, 24, 32)
private const val DEFAULT_GRID = 16

// 빈 칸 = 완전 투명.
private val TRANSPARENT = Color.Transparent.toArgb()

// 투명 영역을 보여주는 체커보드 두 색.
private val CHECKER_LIGHT = Color(0xFFFFFFFF)
private val CHECKER_DARK = Color(0xFFD9D9D9)

private enum class Tool { PENCIL, ERASER, FILL, EYEDROPPER }

private val PALETTE = listOf(
    Color(0xFF000000), Color(0xFF555555), Color(0xFF9E9E9E), Color(0xFFCCCCCC),
    Color(0xFFFFFFFF), Color(0xFFB71C1C), Color(0xFFF44336), Color(0xFFFF9800),
    Color(0xFFFFC107), Color(0xFFFFEB3B), Color(0xFFCDDC39), Color(0xFF8BC34A),
    Color(0xFF4CAF50), Color(0xFF009688), Color(0xFF00BCD4), Color(0xFF2196F3),
    Color(0xFF3F51B5), Color(0xFF673AB7), Color(0xFF9C27B0), Color(0xFFE91E63),
    Color(0xFFF8BBD0), Color(0xFF795548), Color(0xFFFFE0B2), Color(0xFFFFCC80),
)

// ── 기능 입구: 갤러리 ↔ 편집기 전환 ──
@Composable
fun PixelArtFeature(modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<PixelDoc?>(null) } // 편집 중인 문서(새 그림이면 null)

    if (editing) {
        PixelArtEditor(
            initial = current,
            modifier = modifier,
            onBack = { editing = false }
        )
    } else {
        PixelArtGallery(
            modifier = modifier,
            onNew = { current = null; editing = true },
            onOpen = { doc -> current = doc; editing = true },
            onResume = { draft -> current = draft; editing = true }
        )
    }
}

// ── 갤러리: 저장된 그림 목록 + 새로 그리기 ──
@Composable
private fun PixelArtGallery(
    modifier: Modifier = Modifier,
    onNew: () -> Unit,
    onOpen: (PixelDoc) -> Unit,
    onResume: (PixelDoc) -> Unit
) {
    val context = LocalContext.current
    var docs by remember { mutableStateOf<List<PixelDoc>>(emptyList()) }
    var draft by remember { mutableStateOf<PixelDoc?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<PixelDoc?>(null) }

    // 화면이 (다시) 보일 때마다 저장 폴더 + 임시저장(draft)을 읽어 갱신. reloadKey 로 강제 새로고침.
    LaunchedEffect(reloadKey) {
        loading = true
        val loaded = withContext(Dispatchers.IO) {
            val list = PixelArtStore.list(context).map { PixelArtStore.load(it) }
            val d = PixelArtStore.loadDraft(context)
            list to d
        }
        docs = loaded.first
        draft = loaded.second
        loading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "픽셀 아트",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "도트를 찍어 나만의 아이템을 만들어요 🎨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onNew) { Text("＋ 새로 그리기") }
        }
        // 이어서 그리기: 저장 안 한 작업 중 그림(draft)이 있으면 노출.
        draft?.let { d ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, Color(0xFFBDBDBD))
                ) {
                    PixelImage(d.pixels, d.gridSize, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { onResume(d) }) { Text("이어서 그리기 (임시저장)") }
            }
        }
        Spacer(Modifier.height(16.dp))

        when {
            loading -> Text("불러오는 중…", style = MaterialTheme.typography.bodyMedium)
            docs.isEmpty() -> Text(
                "아직 저장된 그림이 없어요.\n'＋ 새로 그리기'로 시작해 보세요.",
                style = MaterialTheme.typography.bodyLarge
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gridItems(docs, key = { it.file?.name ?: "" }) { doc ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .border(1.dp, Color(0xFFBDBDBD))
                    ) {
                        // 썸네일(투명 영역은 체커보드로 보임). 탭하면 편집.
                        PixelImage(
                            pixels = doc.pixels,
                            gridSize = doc.gridSize,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onOpen(doc) }
                        )
                        // 삭제 버튼(우상단)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color(0x99000000), CircleShape)
                                .clickable { pendingDelete = doc },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    // 삭제 확인
    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    toDelete.file?.let { PixelArtStore.delete(it) }
                    pendingDelete = null
                    reloadKey++ // 목록 새로고침
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("취소") } },
            title = { Text("이 그림을 삭제할까요?") },
            text = { Text("삭제하면 되돌릴 수 없어요.") }
        )
    }
}

// ── 편집기 ──
@Composable
private fun PixelArtEditor(
    initial: PixelDoc?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var gridSize by remember { mutableIntStateOf(initial?.gridSize ?: DEFAULT_GRID) }
    // 픽셀 버퍼. 기존 그림이면 그 픽셀로 시작, 새 그림이면 전부 투명.
    var pixels by remember {
        mutableStateOf(initial?.pixels?.copyOf() ?: IntArray(gridSize * gridSize) { TRANSPARENT })
    }
    var revision by remember { mutableIntStateOf(0) }       // 칸 내부 변경 시 Canvas 재그리기 신호
    var currentFile by remember { mutableStateOf(initial?.file) } // 저장 대상(있으면 덮어쓰기)

    val undoStack = remember { ArrayDeque<IntArray>() }
    val redoStack = remember { ArrayDeque<IntArray>() }
    var historyVersion by remember { mutableIntStateOf(0) }

    var tool by remember { mutableStateOf(Tool.PENCIL) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var showGrid by remember { mutableStateOf(true) }
    val customColors = remember { mutableStateListOf<Color>() }
    var showColorPicker by remember { mutableStateOf(false) }

    // 캔버스 크기 변경 — 내용을 좌상단(0,0) 기준으로 보존한다.
    //   작게: 큰 캔버스의 좌상단 일부만 남고 바깥은 잘림.
    //   크게: 작은 그림이 큰 캔버스의 좌상단 부분이 되고 나머지는 투명.
    //   (크기가 바뀌면 픽셀 배열 길이 자체가 달라져 기존 undo 스냅샷과 안 맞으므로 히스토리는 리셋)
    fun resizeCanvas(newSize: Int) {
        if (newSize == gridSize) return
        val old = pixels
        val oldN = gridSize
        val overlap = minOf(oldN, newSize)
        val new = IntArray(newSize * newSize) { TRANSPARENT }
        for (r in 0 until overlap) {
            for (c in 0 until overlap) {
                new[r * newSize + c] = old[r * oldN + c]
            }
        }
        gridSize = newSize
        pixels = new
        undoStack.clear(); redoStack.clear(); historyVersion++; revision++
    }
    fun commitHistory(before: IntArray) {
        if (before.contentEquals(pixels)) return
        undoStack.addLast(before)
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear(); historyVersion++
    }
    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(pixels)
        pixels = undoStack.removeLast()
        historyVersion++; revision++
    }
    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(pixels)
        pixels = redoStack.removeLast()
        historyVersion++; revision++
    }
    fun clearAll() {
        val before = pixels.copyOf()
        for (i in pixels.indices) pixels[i] = TRANSPARENT
        revision++; commitHistory(before)
    }

    @Suppress("UNUSED_VARIABLE")
    val hv = historyVersion
    val canUndo = undoStack.isNotEmpty()
    val canRedo = redoStack.isNotEmpty()

    // ── 자동 임시저장 ── '새 그림'(아직 저장 안 함, currentFile==null)일 때만.
    //   historyVersion 은 획 확정·undo·redo·전체지우기·크기변경마다 1 증가하므로, 그때마다 draft 갱신.
    //   비어 있으면 draft 삭제(이어 그릴 게 없음). 저장(프로젝트화) 시엔 onSuccess 에서 draft 삭제.
    LaunchedEffect(historyVersion) {
        if (currentFile == null) {
            val snapshot = pixels.copyOf()
            val n = gridSize
            withContext(Dispatchers.IO) {
                if (snapshot.all { it ushr 24 == 0 }) PixelArtStore.clearDraft(context)
                else PixelArtStore.saveDraft(context, snapshot, n)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onBack,
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) { Text("← 갤러리") }
            Spacer(Modifier.width(12.dp))
            Text(
                if (initial?.file == null) "새 그림" else "그림 편집",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(Modifier.height(12.dp))

        // ── 캔버스 ──
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(width = 2.dp, color = Color.Black)
                .pointerInput(gridSize, tool, selectedColor) {
                    fun toIndex(off: Offset): Int {
                        if (size.width == 0 || size.height == 0) return -1
                        val c = (off.x / (size.width.toFloat() / gridSize)).toInt()
                        val r = (off.y / (size.height.toFloat() / gridSize)).toInt()
                        if (c < 0 || c >= gridSize || r < 0 || r >= gridSize) return -1
                        return r * gridSize + c
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val startIdx = toIndex(down.position)
                        when (tool) {
                            Tool.EYEDROPPER -> {
                                if (startIdx >= 0) {
                                    val picked = pixels[startIdx]
                                    selectedColor = Color(picked)
                                    // 투명을 찍으면 지우개로, 색을 찍으면 연필로 전환.
                                    tool = if (picked ushr 24 == 0) Tool.ERASER else Tool.PENCIL
                                }
                            }
                            Tool.FILL -> {
                                if (startIdx >= 0) {
                                    val before = pixels.copyOf()
                                    floodFill(pixels, gridSize, startIdx, selectedColor.toArgb())
                                    revision++
                                    commitHistory(before)
                                }
                            }
                            Tool.PENCIL, Tool.ERASER -> {
                                val before = pixels.copyOf()
                                val paint = if (tool == Tool.ERASER) TRANSPARENT else selectedColor.toArgb()
                                if (startIdx >= 0 && pixels[startIdx] != paint) {
                                    pixels[startIdx] = paint; revision++
                                }
                                drag(down.id) { change ->
                                    val idx = toIndex(change.position)
                                    if (idx >= 0 && pixels[idx] != paint) {
                                        pixels[idx] = paint; revision++
                                    }
                                    change.consume()
                                }
                                commitHistory(before)
                            }
                        }
                    }
                }
        ) {
            @Suppress("UNUSED_VARIABLE")
            val rev = revision
            val px = pixels
            val cellW = size.width / gridSize
            val cellH = size.height / gridSize
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val color = px[r * gridSize + c]
                    val topLeft = Offset(c * cellW, r * cellH)
                    val cell = Size(cellW, cellH)
                    if (color ushr 24 == 0) {
                        // 투명 → 체커보드
                        drawRect(if ((r + c) % 2 == 0) CHECKER_LIGHT else CHECKER_DARK, topLeft, cell)
                    } else {
                        drawRect(Color(color), topLeft, cell)
                    }
                }
            }
            if (showGrid) {
                val lineColor = Color(0x33000000)
                for (i in 0..gridSize) {
                    drawLine(lineColor, Offset(i * cellW, 0f), Offset(i * cellW, size.height), 1f)
                    drawLine(lineColor, Offset(0f, i * cellH), Offset(size.width, i * cellH), 1f)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolChip("✏️ 연필", tool == Tool.PENCIL) { tool = Tool.PENCIL }
            ToolChip("🧽 지우개", tool == Tool.ERASER) { tool = Tool.ERASER }
            ToolChip("🪣 채우기", tool == Tool.FILL) { tool = Tool.FILL }
            ToolChip("💧 스포이드", tool == Tool.EYEDROPPER) { tool = Tool.EYEDROPPER }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionChip("↩️ 취소", enabled = canUndo) { undo() }
            ActionChip("↪️ 다시", enabled = canRedo) { redo() }
            ToolChip(if (showGrid) "▦ 격자 끄기" else "▦ 격자 켜기", showGrid) { showGrid = !showGrid }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("크기", style = MaterialTheme.typography.labelLarge)
            GRID_OPTIONS.forEach { s ->
                ToolChip("${s}×$s", gridSize == s) { resizeCanvas(s) }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── 팔레트 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 현재 붓 색(지우개면 체커보드 표시)
            if (tool == Tool.ERASER) {
                CheckerSwatch(Modifier.size(44.dp).border(2.dp, Color.Black))
            } else {
                Box(Modifier.size(44.dp).background(selectedColor).border(2.dp, Color.Black))
            }
            Spacer(Modifier.width(8.dp))
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(PALETTE + customColors) { color ->
                    val isSelected = tool != Tool.ERASER && color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.Black else Color(0xFFBDBDBD)
                            )
                            .clickable {
                                selectedColor = color
                                if (tool == Tool.ERASER) tool = Tool.PENCIL
                            }
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { showColorPicker = true },
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) { Text("＋") }
        }
        Spacer(Modifier.height(16.dp))

        // ── 액션: 전체 지우기 / 저장(프로젝트) / 내보내기(PNG) ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { clearAll() },
                modifier = Modifier.weight(1f),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) { Text("전체 지우기", maxLines = 1, style = MaterialTheme.typography.labelLarge) }

            Button(
                onClick = {
                    val doc = PixelDoc(pixels.copyOf(), gridSize, currentFile)
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) { PixelArtStore.save(context, doc) }
                        }
                        result.onSuccess {
                            currentFile = it // 이후 저장은 같은 파일에 덮어쓰기(= draft 모드 해제)
                            PixelArtStore.clearDraft(context) // 프로젝트로 저장됐으니 임시저장 비움
                            Toast.makeText(context, "저장됨 (갤러리에서 다시 편집 가능)", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, "저장 실패: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) { Text("저장", maxLines = 1, style = MaterialTheme.typography.labelLarge) }

            Button(
                onClick = {
                    val doc = PixelDoc(pixels.copyOf(), gridSize, null)
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) { PixelArtStore.exportToGallery(context, doc) }
                        }
                        result.onSuccess {
                            Toast.makeText(context, "내보냄: $it", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            Toast.makeText(context, "내보내기 실패: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) { Text("내보내기", maxLines = 1, style = MaterialTheme.typography.labelLarge) }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initial = if (selectedColor.alpha == 0f) Color.Black else selectedColor,
            onDismiss = { showColorPicker = false },
            onConfirm = { picked ->
                customColors.add(picked)
                selectedColor = picked
                if (tool == Tool.ERASER) tool = Tool.PENCIL
                showColorPicker = false
            }
        )
    }
}

// 픽셀 배열을 그대로 그리는 위젯(격자선 없음). 썸네일·미리보기·캐릭터 레이어용.
//   public — 캐릭터 화면에서도 저장된 픽셀 아트를 레이어로 재사용한다.
//   transparentAsChecker = true  : 투명 칸을 체커보드로 표시(편집기/갤러리 — 투명임을 보여줘야 함).
//                          false : 투명 칸은 아예 안 그림(캐릭터 위 오버레이 — 아래 레이어가 비쳐야 함).
@Composable
fun PixelImage(
    pixels: IntArray,
    gridSize: Int,
    modifier: Modifier = Modifier,
    transparentAsChecker: Boolean = true
) {
    Canvas(modifier) {
        val cellW = size.width / gridSize
        val cellH = size.height / gridSize
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val color = pixels[r * gridSize + c]
                val topLeft = Offset(c * cellW, r * cellH)
                val cell = Size(cellW, cellH)
                if (color ushr 24 == 0) {
                    if (transparentAsChecker) {
                        drawRect(if ((r + c) % 2 == 0) CHECKER_LIGHT else CHECKER_DARK, topLeft, cell)
                    }
                    // transparentAsChecker=false 면 투명 칸은 건너뜀(아래 레이어 노출)
                } else {
                    drawRect(Color(color), topLeft, cell)
                }
            }
        }
    }
}

// 작은 체커보드 스와치(지우개 선택 시 현재 색 자리에 표시).
@Composable
private fun CheckerSwatch(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = 4
        val cw = size.width / n
        val ch = size.height / n
        for (r in 0 until n) for (c in 0 until n) {
            drawRect(
                if ((r + c) % 2 == 0) CHECKER_LIGHT else CHECKER_DARK,
                Offset(c * cw, r * ch), Size(cw, ch)
            )
        }
    }
}

@Composable
private fun RowScope.ToolChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val mod = Modifier.weight(1f)
    if (selected) {
        Button(onClick = onClick, modifier = mod, contentPadding = ButtonDefaults.TextButtonContentPadding) {
            Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = mod, contentPadding = ButtonDefaults.TextButtonContentPadding) {
            Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RowScope.ActionChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    var r by remember { mutableFloatStateOf(initial.red) }
    var g by remember { mutableFloatStateOf(initial.green) }
    var b by remember { mutableFloatStateOf(initial.blue) }
    val current = Color(r, g, b)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text("추가") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        title = { Text("색 만들기") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(current)
                        .border(1.dp, Color.Gray)
                )
                Spacer(Modifier.height(12.dp))
                ColorSlider("R", r) { r = it }
                ColorSlider("G", g) { g = it }
                ColorSlider("B", b) { b = it }
            }
        }
    )
}

@Composable
private fun ColorSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(20.dp))
        Slider(value = value, onValueChange = onChange, modifier = Modifier.weight(1f))
        Text("${(value * 255).toInt()}", modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }
}

// flood fill — 시작 칸과 상하좌우로 연결된 같은 색 영역을 newColor 로 채운다(스택 기반 반복).
private fun floodFill(px: IntArray, n: Int, startIdx: Int, newColor: Int) {
    val target = px[startIdx]
    if (target == newColor) return
    val stack = ArrayDeque<Int>()
    stack.addLast(startIdx)
    while (stack.isNotEmpty()) {
        val idx = stack.removeLast()
        if (px[idx] != target) continue
        px[idx] = newColor
        val r = idx / n
        val c = idx % n
        if (c > 0) stack.addLast(idx - 1)
        if (c < n - 1) stack.addLast(idx + 1)
        if (r > 0) stack.addLast(idx - n)
        if (r < n - 1) stack.addLast(idx + n)
    }
}

@Preview(showBackground = true)
@Composable
fun PixelArtFeaturePreview() {
    MyApp1Theme {
        PixelArtFeature()
    }
}
