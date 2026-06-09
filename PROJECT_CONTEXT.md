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
- [ ] 2단계: 데이터 계층 — `HabitEntity`(@Entity) + `HabitDao`(@Dao) + `AppDatabase`(@Database)
- [ ] 3단계: `HabitViewModel` (상태 보유 + DB 연결, Flow)
- [ ] 4단계: UI를 ViewModel에 연결 (`HabitListScreen`의 rememberSaveable 제거 → DB가 source of truth)
- [ ] 5단계: 검증 — 프로세스 강제 종료 후에도 데이터 유지되는지 확인
- 참고: 현재 `HabitListScreen.kt`는 사용자가 `rememberSaveable`로 바꿔둔 상태(프로세스 종료 시 크래시 잠재) → 4단계에서 교체 예정.
- Room↔백엔드 매핑: `@Entity`=테이블, `@Dao`=MyBatis Mapper, `@Database`=DataSource, `ViewModel`=Service 계층.

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
