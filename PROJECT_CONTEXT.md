# PROJECT_CONTEXT

> 이 파일은 Claude Code 새 세션 시작 시 이전 대화의 맥락을 이어받기 위한 핸드오프 문서입니다.
> 새 세션 첫 메시지로 "PROJECT_CONTEXT.md 읽고 시작" 한 줄이면 컨텍스트가 복원됩니다.

## 사용자 프로필

- **본업**: Spring Boot 기반 백엔드 개발자 (Java)
  - 회사 프로젝트: `C:\OW\workspace\ows-log` (OW 로그 서비스, Spring Boot 3.3.6 + Java 21 + MariaDB + MyBatis)
  - IDE: STS (Spring Tool Suite, Eclipse 기반)
- **Android 개발**: **이번이 처음**. Kotlin도 처음.
- **개발 환경**: Windows 11, PowerShell

> 백엔드는 능숙하니까 백엔드 영역(Spring Boot, REST API, DB)은 깊이 있는 설명 생략 가능.
> Android/Kotlin/Compose는 기초부터 친절하게 설명 필요.

## 프로젝트 비전

**습관 관리 + 캐릭터 꾸미기 게임** (Habitica + Finch 류)

- 사용자가 좋은 습관을 실천 → 포인트 적립
  - 활동 예: **걷기, 책 읽기, 물 마시기** (확정은 아니지만 이 방향)
- 포인트로 캐릭터 꾸미기 (의상, 머리, 액세서리 등)
- 사용자끼리 캐릭터 자랑/비교, 소셜 피드, 랭킹

**캐릭터 스타일**: **2D 종이인형** (싸이월드, 티스토리 스타일의 레이어 합성)

**최종 목표**: **Google Play Store 정식 출시** (학습용 아님)

## 결정된 기술 스택

| 영역 | 기술 |
|------|------|
| Android 앱 (클라이언트) | **Kotlin + Jetpack Compose** |
| Build Config | Kotlin DSL (build.gradle.kts) |
| 백엔드 | **Spring Boot** (사용자 전문 영역) |
| DB | MariaDB (사내 표준과 동일) |
| 인증 | Google Sign-In |
| 걷기 측정 | **Health Connect API** (안드로이드 공식 표준, Google Fit 후속) |
| 앱-서버 통신 | Retrofit + OkHttp |
| 이미지 로딩 | Coil |
| 푸시 알림 | Firebase Cloud Messaging |
| 서버 호스팅 | AWS Lightsail 또는 Oracle Cloud Free 등급 (예정) |

## 첫 프로젝트 설정값 (Android Studio)

- **Project name**: MyApp1 (현재 폴더)
- **Template**: Empty Activity (Compose 기반, Empty Views Activity 아님)
- **Language**: Kotlin
- **Minimum SDK**: API 24 (Android 7.0 Nougat)
- **Build configuration language**: Kotlin DSL (build.gradle.kts)

## 단계별 로드맵 (총 6~12개월 예상)

### Phase 0 — Android 기초 학습 (4주) ← **현재 단계**
- Kotlin + Compose 기본 문법 익히기
- 튜토리얼 앱 2~3개 만들기 (Hello World, 카운터, 리스트)
  - [x] Hello World (템플릿 기본)
  - [x] **카운터** — `CounterScreen.kt`. 학습 개념: `@Composable`, 상태(`remember`+`mutableIntStateOf`), 리컴포지션, `Column`/`Row`/`Spacer`, `Button`/`OutlinedButton`/`onClick`. 에뮬레이터에서 +1×3 → 3 동작 확인 완료.
  - [x] **리스트 (LazyColumn)** — `HabitListScreen.kt`. "오늘의 습관" 미니 트래커. 학습 개념: `data class`(Habit), `LazyColumn`+`items(key=...)`, `Card`+`Modifier.clickable`, 불변 리스트 상태 교체(`map`+`copy`), 파생값(`count{}`), `Modifier.weight(1f)`, 함수 분리(`HabitRow`). 항목 탭→✅ 토글 + 상단 `n/5 완료` 갱신 확인 완료.
  - 코드 구조: `MainActivity.kt`(화면 선택만, 현재 `HabitListScreen` 표시) / `CounterScreen.kt` / `HabitListScreen.kt` 로 분리.
- 게임 본격 개발은 아직 시작 안 함

> ✅ **Phase 0 튜토리얼 3종(Hello World·카운터·리스트) 완료.** Compose 핵심(선언형 UI, 상태/리컴포지션, 리스트)을 손으로 익힘.

### 학습 메모 — 상태 수명 & 생명주기 (2026-06-02)
- **Activity 생명주기**: 화면 회전·언어변경 등 "구성 변경" 시 Activity가 파괴·재생성됨. `remember`만 둔 상태는 함께 소멸 → 실증함(회전 시 체크 리셋).
- **`rememberSaveable` 함정(중요·실증 완료)**: `List<Habit>` 같은 도메인 객체에 쓰면 **회전은 버티지만 프로세스 강제 종료(process death) 시 크래시**. 원인: 회전=Bundle 메모리 유지(직렬화X), 프로세스종료=Parcel 직렬화 필요 → `Habit`은 Parcelable 아님 → `IllegalArgumentException: Parcel: unknown type`. 결론: 단순값엔 OK, 도메인 데이터엔 부적합 → **Room 으로 가야 함.**
- 교훈: "AI는 자신 있게 틀린다, 검증하라" + "한 조건에서 됐다고 옳은 게 아니다(엣지 케이스 직접 테스트)". 협업 방식 = **내가 코드 작성 → 사용자는 리뷰어로 검증.**

### Phase 1 — 로컬 MVP 진행 상황 (2026-06-02)
- [x] **1단계: 의존성 추가** — Room 2.8.2(runtime/ktx/compiler), KSP 2.2.10-2.0.2, lifecycle-viewmodel-compose 2.9.4. 빌드 통과.
  - ⚠️ AGP 9 'built-in Kotlin' + KSP 충돌 → `gradle.properties`에 `android.disallowKotlinSourceSets=false` 추가로 해결.
- [x] **2단계: 데이터 계층** (2026-06-11) — `HabitEntity`(@Entity) + `HabitDao`(@Dao) + `AppDatabase`(@Database). 빌드(kspDebugKotlin) 통과.
  - **결정: 자동생성(@Insert/@Update) 대신 명시적 @Query SQL 사용.** 이유: 쿼리가 소스에 그대로 보이는 편이 가독성↑(사용자 선호). 토글은 `UPDATE habits SET done=:done WHERE id=:id`(done 컬럼만), 시드는 `INSERT INTO habits(emoji,name,done) VALUES(...)`(id는 autoGenerate라 생략).
  - 검토 방식: KSP 생성물 `app/build/generated/ksp/debug/kotlin/.../HabitDao_Impl.kt`를 직접 열어 실제 SQL 확인함(magic을 블랙박스로 두지 않음). @Insert는 `nullif(?,0)`+전 어댑터, @Query는 우리가 쓴 SQL 1:1 — 대조 완료.
- [x] **3단계: `HabitViewModel`** (2026-06-11) — `AndroidViewModel`. `dao.observeAll()` → `stateIn`으로 `StateFlow<List<HabitEntity>>` 노출. `init`에서 `count()==0`이면 기본 습관 5개 시드(insert 루프). `toggle(habit)`은 `updateDone(id, !done)`. 컴파일 통과.
- [x] **4단계: UI 연결** (2026-06-11) — `HabitListScreen`에서 `rememberSaveable` 제거. `viewModel()` + `collectAsState()`로 DB 구독, 탭→`viewModel.toggle()`. 상태 보유 화면(`HabitListScreen`)과 stateless 화면(`HabitListContent`) 분리 → Preview는 가짜 데이터로 동작. 옛 `Habit` data class 삭제(이제 `HabitEntity` 사용). 컴파일 통과.
- [x] **5단계: 검증** (2026-06-15) — 에뮬레이터(`emulator-5554`, Pixel_8)에서 실증 완료. ① 시드 5개 정상 표시("0/5 완료") ② 2개 탭→✅ 토글 + 상단 "2/5 완료" 갱신 ③ **`adb shell am force-stop`(process death) 후 재실행 시 체크 2개·카운트 모두 유지, 크래시 없음** = Phase 1 핵심 목표 달성. rememberSaveable 시절 `Parcel: unknown type` 크래시 사라짐 확인. Room DB가 단일 진실 공급원으로 동작, SQLite(`habit.db`)에서 상태 복원됨.
  - 검증 방법 메모: Git Bash에서 adb `/sdcard` 경로 변환 막으려면 `export MSYS_NO_PATHCONV=1`, screencap pull 대상은 `C:/tmp/`. 빌드 `./gradlew :app:assembleDebug` 25s.

> ✅ **Phase 1 로컬 MVP의 데이터 영속성(Room) 완성 + 실증 검증 완료.** rememberSaveable→Room 마이그레이션의 목표(프로세스 종료 후 상태 유지)를 손으로 확인함. 다음 작업 후보: 습관 추가/삭제 UI, 날짜별 기록(오늘이 아닌 어제/통계), 또는 Phase 1의 다른 축인 종이인형/캐릭터 시작.

- Room↔백엔드 매핑: `@Entity`=테이블/VO, `@Dao`=MyBatis Mapper, `@Database`=DataSource/SqlSessionFactory, `ViewModel`=Service 계층.

### Phase 1 — 종이인형 캐릭터 축 시작 (2026-06-15)
- [x] **캐릭터 레이어 합성 1차** — `CharacterScreen.kt` + placeholder 벡터 드로어블 6종. 에뮬레이터 실증 완료.
  - **핵심 메커니즘(습득)**: 같은 viewport(100×150)로 그린 그림들을 `Box`에 쌓으면 z-order 합성이 됨(먼저 쓴 게 뒤, 나중 게 앞). 순서 = 배경→몸→머리→옷. 모든 레이어가 같은 좌표계라 자동 정렬 → 실제 PNG 일러스트로 **같은 자리에 갈아끼우기만** 하면 됨(지금은 단순 도형 placeholder라 일러스트 병목에 안 묶임).
  - 드로어블: `char_bg`(배경), `char_body`(피부=머리원+몸통+팔다리), `hair_short`/`hair_long`, `outfit_red`/`outfit_blue`. 머리·옷은 얼굴/피부 위에 얹혀 가리는 '오버레이' 레이어. 머리는 이마만 덮어 얼굴이 보이게 함.
  - UI: `Layer`(=`Box` 채우는 `Image`, `painterResource`+`ContentScale.Fit`+`aspectRatio(100/150)`), `CharacterPreview`(stateless 합성), 버튼으로 `hairIndex`/`outfitIndex`를 `%`로 순환 swap. 상태는 `remember`(아직 미영속 — 꾸미기 저장은 다음 과제).
  - `MainActivity`: 입문용 화면 전환 추가 — `enum Screen{HABITS,CHARACTER}` + `remember` 상태 + 상단 탭 버튼 2개(`when`으로 분기). 네비게이션 라이브러리는 아직 안 씀(과함).
  - 학습 개념: `Box`(겹침) vs `Column/Row`(나란히), z-order, 벡터 드로어블 viewport/pathData(cubic bezier 로 원 근사), `painterResource`, `Image`/`ContentScale`, `enum`+`when` 화면 분기.
- [ ] **다음 후보**: ① 꾸미기 선택을 DB/DataStore 에 영속화(습관처럼 process death 후 유지) ② 레이어/아이템 종류 늘리기(얼굴표정, 배경, 액세서리) ③ 포인트 경제(습관 완료→포인트→아이템 잠금해제) 연결.

### Phase 1 — 픽셀 아트 에디터 (완성형) (2026-06-22)
- [x] **완성형 픽셀 아트 에디터** — `PixelArtScreen.kt`. 처음엔 "탭으로 한 칸 칠하기"만 되는 최소 버전이었으나, 사용자 요청으로 상용 에디터급으로 대폭 보강. 에뮬레이터(`emulator-5554`, API 36) 전 기능 실증 완료.
  - **엔진 교체(핵심 결정)**: 칸 1개=Box 1개 방식(수백 Composable)은 도구/드래그/32×32(1024칸)를 감당 못 함 → **`Canvas` 1개에 `drawRect`로 직접 렌더 + `pointerInput` 제스처**로 재작성. 픽셀 = `IntArray`(ARGB 정수, `Color.toArgb()`). IntArray 내부 변경은 Compose가 감지 못 하므로 `revision`(Int 상태)을 1씩 올려 Canvas 재그리기를 트리거(그리기 람다가 revision을 읽음).
  - **기능**: ✏️연필·🧽지우개·🪣채우기(flood fill, 스택 기반)·💧스포이드(캔버스 색 추출 후 연필 자동전환), **드래그 연속 그리기**(`awaitEachGesture`+`awaitFirstDown`+`drag`, down/move 모두 consume해 부모 스크롤 차단), **실행취소/다시실행**(획 시작 시 스냅샷, before≠after일 때만 기록, 상한 50), 팔레트 24색+**커스텀 색 만들기**(RGB 슬라이더 `AlertDialog`, 미리보기 실시간), **격자선 토글**, **캔버스 크기 16/24/32**(`remember(gridSize)`로 캔버스·히스토리 자동 리셋), 전체 지우기.
  - **기기 로컬 저장(PNG)**: `savePixelArtToDevice()` — IntArray→`Bitmap.setPixels`→정수배 확대(보간 끔=nearest neighbor, 16×16→1024×1024). **Android 10(Q)+는 MediaStore `RELATIVE_PATH=Pictures/PixelArt`+`IS_PENDING`으로 저장(런타임 권한 불필요, 갤러리 노출)**, 9 이하는 앱 전용 외부저장소 fallback. IO 스레드(`Dispatchers.IO`)에서 실행 후 Toast로 결과 알림. **실증**: `저장됨: Pictures/PixelArt/pixelart_*.png` 토스트 + `/sdcard/Pictures/PixelArt/`에 6.7KB 파일 생성 + pull해 1024×1024 또렷한 도트 PNG 확인. AndroidManifest 권한 추가 없음(Q+ 경로라 불필요).
  - **검증 항목(전부 OK)**: 드래그 대각선 연속 그리기 / 빨강 flood fill / 취소→복원·다시→재적용 / 스포이드 추출+연필전환 / RGB 슬라이더로 초록 만들어 팔레트 추가·선택 / 32×32 전환 시 캔버스 초기화 / 격자선 끄기 / PNG 저장·내용 확인.
  - 학습 개념: **Canvas+drawRect 직접 렌더**(위젯 트리 대신 버퍼 그리기), `pointerInput`/`awaitEachGesture`/`drag`(저수준 제스처, consume로 부모 가로채기 차단), IntArray+`revision` 상태로 비관찰 데이터 강제 리컴포지션, undo/redo 스냅샷 스택, **MediaStore scoped storage 저장**(Q+ 무권한), `Bitmap` nearest-neighbor 확대.
  - 검증 메모: adb는 PATH에 없음 → `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe` 직접 호출. 다이얼로그/요소 정확 좌표는 `uiautomator dump`로 bounds 확인. 큰 스크린샷은 Read 거부될 수 있어 `System.Drawing`으로 축소 후 확인. 저장 파일 검증은 `adb shell ls /sdcard/Pictures/PixelArt/` + `pull`.

### Phase 1 — 픽셀 아트: 투명 배경 + 갤러리/재수정 (2026-06-22)
- [x] **투명 배경 + 갤러리(허브) + 저장/재수정/내보내기/삭제** — `PixelArtScreen.kt` 재구성 + `PixelArtStore.kt` 신규. 에뮬레이터 전 흐름 실증 완료.
  - **포맷 결정(사용자 질문)**: 픽셀 아트는 **PNG가 정답**(무손실+알파+범용). JPEG는 손실·알파X라 부적합. → PNG 유지하되 **투명 배경**으로.
  - **저장 구조(사용자 선택: 앱 보관 + 내보내기 분리)**: ① **저장(프로젝트)** = 앱 내부 `filesDir/pixelart/proj_*.png`에 **원본 해상도**(16/24/32px) PNG(무손실, 알파). 원본 해상도라 다시 열면 픽셀·크기 100% 복원 → 정확한 재수정. ② **내보내기** = 휴대폰 갤러리 `Pictures/PixelArt`에 확대(≈1024px) 투명 PNG(공유·자산용). 저장소 로직은 `PixelArtStore`(list/load/save/delete/exportToGallery)로 분리.
  - **투명 처리**: 빈 칸 = `Color.Transparent`(알파 0). 캔버스/썸네일에서 투명 칸은 **체커보드(바둑판, 흰+연회색)** 로 표시(`drawRect`, `(r+c)%2`). 지우개 = 투명으로 칠하기. 스포이드로 투명 찍으면 지우개로 자동전환.
  - **화면 구조**: `PixelArtFeature`(탭 입구, 갤러리↔편집기 `remember` 전환) → `PixelArtGallery`(저장 목록 `LazyVerticalGrid` 썸네일 + `＋ 새로 그리기`, 썸네일 탭=편집, ✕=삭제 확인 다이얼로그) → `PixelArtEditor(initial: PixelDoc?)`(새 그림=null / 기존=문서 로드). 썸네일은 `PixelImage`(격자선 없는 `Canvas`)로 렌더. `MainActivity`는 `PixelArtFeature()` 호출로 변경.
  - **상태 모델 변경**: 픽셀을 `var pixels by mutableStateOf(IntArray)`로(크기변경/undo/redo/load 시 배열 **재할당** → Canvas가 pixels-state 읽어 재그림) + 그리기 중 in-place 변경은 `revision++`로 트리거(두 경로 병행). undo/redo도 배열 스왑 방식.
  - **실증(전부 OK)**: 빈 갤러리 안내 → 새로 그리기 → 투명 캔버스에 대각선 → 저장(토스트, 내부 `proj_*.png` 131B 생성) → 갤러리 썸네일(투명=체커보드) → 썸네일 탭 시 "그림 편집"으로 픽셀 복원 → 내보내기(갤러리 1024×1024, **pull해 알파 검사: 대각선 A=255 불투명/빈칸 A=0 투명** 확인) → ✕ 삭제 확인 다이얼로그 → 삭제 후 빈 상태·내부 파일 0개.
  - 학습 개념: 알파 채널/투명 PNG, 체커보드 표현, **앱 내부 저장소(filesDir, 무권한) vs 공용 MediaStore** 구분, PNG round-trip(저장↔로드)로 무손실 재수정, `BitmapFactory.decodeFile`+`getPixels`로 복원, 화면 내 서브 네비게이션(라이브러리 없이 `remember`로 갤러리↔편집기).
  - 검증 메모: 에뮬 재부팅 후 `/sdcard/s.png` 권한 오류 → screencap은 `/data/local/tmp/s.png` 사용. 앱 내부 파일은 `adb shell run-as com.example.myapp1 ls files/pixelart`로 확인.
  - 다음 작업: ① 캐릭터 연동 ③ 크기 변경 내용 보존 → 아래에서 완료.

### Phase 1 — 픽셀↔캐릭터 연동 + 크기 변경 내용 보존 (2026-06-22)
- [x] **#1 저장한 픽셀 아트를 캐릭터에 아이템 레이어로 연결** — `CharacterScreen.kt` 확장. 앱의 두 축(픽셀 에디터 ↔ 종이인형)이 처음 연결됨.
  - `PixelArtStore.list/load`로 저장된 그림을 읽어(`LaunchedEffect`+`Dispatchers.IO`) "내가 그린 픽셀 아이템" `LazyRow`(없음 + 썸네일들)로 표시. 선택하면 `CharacterPreview`의 `Box`(배경→몸→머리→옷) **맨 위에** 가운데 140dp 레이어로 합성.
  - 핵심: `PixelImage`에 `transparentAsChecker: Boolean` 추가 → 캐릭터 위 오버레이는 `false`로 줘서 **투명 칸을 안 그려 캐릭터가 비치게**(썸네일/편집기는 `true`로 체커보드 표시). `PixelImage`를 public 으로 승격해 화면 간 재사용.
  - 실증: 픽셀 탭에서 ㄱ자 그려 저장 → 캐릭터 탭에 썸네일 자동 등장 → 선택 시 몸통 위 합성(투명부로 캐릭터 비침) → '없음'으로 떼기. 탭 전환 시마다 목록 갱신.
- [x] **#3 캔버스 크기 변경 시 내용 보존(좌상단 기준)** — 기존 `resetCanvas`(초기화) → `resizeCanvas`로 교체. `overlap=min(old,new)` 만큼 좌상단(0,0) 영역을 새 배열로 복사.
  - 작게: 큰 캔버스의 좌상단 일부만 남고 바깥 잘림. 크게: 작은 그림이 큰 캔버스 좌상단 부분이 되고 나머지 투명. (배열 길이가 바뀌어 기존 undo 스냅샷과 안 맞으므로 히스토리는 리셋)
  - 실증: 16×16 좌상단 ㄱ자 → 32×32(좌상단 보존, 나머지 투명) → 16×16(좌상단 16칸 복원) 확인.
  - [ ] **다음 후보**: ① 아이템 위치/크기 조절(지금은 가운데 고정) ② 픽셀 아이템 여러 개 동시 착용/레이어 순서 ③ 작업 중 그림 자동 임시저장(앱 종료 후 복원) ④ 이름 붙여 저장/정렬 ⑤ 포인트 경제(습관→포인트→아이템 잠금해제).

### Phase 1 — UI 디자인 전면 개편 (2026-06-23)
- [x] **최신 트렌드 반영 디자인 개편** — 테마 3종 + 4개 화면. 에뮬레이터(`emulator-5554`, Pixel_8) 전 화면 실증 완료.
  - **테마(전 화면 자동 전파)**: `Color.kt` — `dynamicColor`(배경화면 색 추종, 기기마다 색 달라 브랜드 없음)를 **끄고** 고정 브랜드 팔레트 도입(보라 primary `#6C5CE7` / 코랄 secondary / 민트 tertiary / 살짝 보랏빛 오프화이트 배경, 라이트·다크 둘 다). `Theme.kt` — 새 색 스킴 + **전역 `Shapes`**(모서리 둥글기 키움) → 모든 Card/Button/Dialog 한꺼번에 둥글어짐. `Type.kt` — headline ExtraBold/Bold + 자간 조여 타이포 위계 강화.
  - **MainActivity**: 상단 밋밋한 Button/OutlinedButton 3개 → **하단 네비게이션 바**(Material3 `NavigationBar`+`NavigationBarItem`, 선택 항목 뒤 알약(pill) 강조 자동). 아이콘은 이모지(🔥/🧑‍🎤/🎨)라 의존성 추가 없음. `onCreate`의 Scaffold를 `AppRoot`로 옮겨 `bottomBar` 슬롯 사용.
  - **HabitListScreen**(최대 변화): **진행률 카드**(primaryContainer 위 큰 `%` + `animateFloatAsState`로 차오르는 둥근 `LinearProgressIndicator`) + **배지형 습관 카드**(이모지를 둥근 색 원 배지에, 완료 시 카드가 민트로 물들고 취소선 + 커스텀 **원형 체크 인디케이터**). 기존 이모지 `✅/⬜` → 톤 일관된 원형 체크. `animateColorAsState`로 완료 전환 부드럽게.
  - **CharacterScreen / PixelArtScreen**: 캐릭터를 둥근 카드 무대에 전시, 헤더 `headlineLarge`+서브텍스트로 톤 통일.
  - **실증**: 빌드 `:app:assembleDebug` BUILD SUCCESSFUL → 하단 네비 3탭 전환·알약 강조 / 습관 토글 시 진행률 카드 `60%↔100%`·막대·퍼센트 실시간 갱신 / 완료·미완료 카드 대비(민트+취소선 vs 흰+빈 원) 모두 확인. 크래시 없음.
  - 학습 개념: Material3 **색 역할(role) 체계**(primary/onPrimary/primaryContainer 쌍), `dynamicColor` 끄고 브랜드 고정, 전역 `Shapes`/`Typography` 커스터마이즈, `NavigationBar`(하단 표준), `animateFloatAsState`/`animateColorAsState`, `LinearProgressIndicator(progress={})`.

### Phase 1 — 캐릭터 화면 격자화 + 아이템 스냅 (2026-06-23)
- [x] **캐릭터 무대를 '커다란 픽셀 격자'로, 아이템이 격자에 물려 한 칸씩 스냅 이동** — `CharacterScreen.kt`. 사용자 요청(아이템 위치 조절 다음 후보 중 ①). 에뮬레이터 실증 완료.
  - **격자 무대**: 캐릭터 미리보기(200×300dp)를 **16칸×24칸 격자**(`GRID_COLS`/`GRID_ROWS`, 정사각 칸 `CELL`=12.5dp)로 보고 `Canvas`로 얇은 격자선 오버레이.
  - **좌표 모델 교체**: `PlacedItem`을 px 오프셋(`offsetX/offsetY`+`sizeDp`) → **격자 칸 좌표(`col`,`row`) + 차지 칸 수(`spanCells`)** 로 변경. 좌상단 기준 `offset{ IntOffset(col*CELL.toPx(), row*CELL.toPx()) }` + `size(CELL*spanCells)` 라 항상 격자에 정확히 물림.
  - **한 칸 스냅 드래그(핵심)**: 드래그 픽셀 누적(accX/accY)을 칸 수로 환산하고 **'아직 적용 안 한 칸 차이'만 ±1칸 상대 이동**(`onMove(id,dCol,dRow)`)으로 부모에 전달. 부모는 현재 좌표에 더하고 `coerceIn(0, GRID-span)`으로 무대 밖 방지. **현재 좌표를 gesture 안에서 직접 읽지 않으므로** `pointerInput`(key=item.id)이 재시작 안 돼도 staleness 없이 항상 정확. 크기는 `2~16칸` 정수 스텝 Slider("N칸"), 변경 시 위치도 재clamp.
  - **실증(전부 OK)**: 격자선 표시 / 아이템 추가 시 격자 정렬(테두리=격자선 일치) / 드래그 → 한 칸 단위 스냅 이동 후에도 정렬 유지 / 크기 8칸→5칸. 빌드 SUCCESSFUL, 크래시 없음.
  - 학습 개념: 격자 스냅(픽셀 누적→칸 환산), **상대 이동으로 pointerInput staleness 회피**, `PointerInputScope`/`Density`의 `toPx()`로 dp↔px, `Canvas` 격자선 렌더, Slider `steps`로 정수 스냅.
  - [ ] **다음 후보**: ① 격자 아이템 배치/꾸미기 상태를 DB/DataStore에 **영속화**(지금은 `remember`라 앱 완전 종료 시 사라짐) ② 머리·옷 선택도 영속화 ③ 포인트 경제 연결.

### Phase 1 — 로컬 MVP (4~6주)
- 서버 없이 로컬 데이터만으로 작동하는 습관 트래커 + 종이인형
- 일러스트 자산 확보 시작 (최소 10~20세트)

### Phase 2 — 백엔드 연동 (4주)
- Spring Boot 서버, Google Sign-In, 데이터 동기화

### Phase 3 — 소셜 기능 (4~6주)
- 친구, 피드, 랭킹

### Phase 4 — Play Store 출시 (2~4주)
- 개인정보 처리방침, 데이터 안전성, 베타 테스트, 정식 출시
- 개발자 계정 $25 일회성

## 식별된 리스크 / 제약

1. **일러스트가 최대 병목**: 종이인형은 의상/머리/배경 등 수십~수백 개 자산 필요. 직접 못 그리면 외주 비용 큼. AI 생성은 저작권 이슈로 상용 앱에 부적합.
2. **Play Store 출시는 Phase 4지 Phase 1이 아님**: 일단 Phase 1 목표만 잡고, 만들면서 진짜 출시할 만한지 판단.
3. **첫 프로젝트는 단순할수록 좋음**: 기능 추가보다 빼는 것이 더 중요.

## 현재 작업 상태 (2026-06-02 기준)

- [x] Android Studio 설치 (winget으로 설치, 버전 2025.3.4.7)
- [x] SDK 초기 설치 마법사 완료 (Platform 36.1, Build-Tools 36.1/37, Emulator)
- [x] Welcome 화면 진입 확인
- [x] 첫 프로젝트 생성 (Empty Activity Compose 템플릿) — 표준 구조 정상
- [x] Gradle Sync 완료 확인 — CLI `gradlew :app:assembleDebug` BUILD SUCCESSFUL (58s), `app-debug.apk` 생성
- [x] 에뮬레이터(AVD) 생성 — Device Manager에서 `Pixel_8` (API 36 system image) 생성
- [x] ▶ 실행해서 "Hello Android" 확인 — 에뮬레이터 부팅(WHPX 정상) → APK 설치 → 화면에 "Hello Android!" 출력 확인

> ✅ **Phase 0 환경 구축 완료.** 에뮬레이터 `emulator-5554`로 빌드→설치→실행 전체 파이프라인 검증됨.
> WHPX 하이퍼바이저 정상 동작 확인(이전 AEHD 경고는 무시 가능 확정).
> 다음 세션부터는 Phase 0 학습(Kotlin/Compose 문법, 카운터·리스트 튜토리얼)으로 진행.

### 검증된 빌드 환경 (2026-06-02)
- AGP 9.2.1 / Gradle 9.4.1 / Kotlin 2.2.10 / Compose BOM 2026.02.01 / minSdk 24 / target·compileSdk 36
- **JDK**: `gradle.properties`에 `org.gradle.java.home`로 Android Studio 번들 JBR 고정.
  - 시스템 JAVA_HOME(JDK 21, 백엔드용)·다른 프로젝트엔 무영향. 이 프로젝트 전용 설정.
- `libandroidx.graphics.path.so` strip 경고는 무해(무시).

> ⚠️ AEHD 하이퍼바이저 드라이버 서비스 시작 실패 메시지가 떴으나 무시. Windows 11에서는 WHPX로 대체됨. 에뮬레이터 단계에서 실제 문제 발생 시 재검토.
> ℹ️ 현재 SDK에 system-images / cmdline-tools 없음 → AVD 생성 시 시스템 이미지 다운로드 필요(Device Manager가 자동 처리).

## 새 세션에서 시작할 때 추천 첫 명령

```
PROJECT_CONTEXT.md 읽고 현재 작업 상태 섹션의 다음 미완료 항목부터 진행해줘.
```

## 이전 세션 위치

- 작업 디렉토리: `C:\OW\workspace\ows-log`
- 대화 흐름 요약:
  1. Spring Boot 프로젝트 실행 트러블슈팅 (NoClassDefFoundError → Gradle Refresh, 포트 충돌 → 프로세스 강제 종료)
  2. STS Perspective/Package Explorer UI 이슈
  3. → Android 앱 개발 시작 (이 문서)
