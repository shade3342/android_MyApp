package com.example.myapp1.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// 앱 브랜드 팔레트 — 습관 + 캐릭터 꾸미기 게임에 어울리는 '부드럽고 친근한' 톤.
//   기존엔 dynamicColor(배경화면 색 추종)라 기기마다 색이 달라 브랜드가 없었음.
//   이제 아래 고정 팔레트로 어느 기기서나 같은 정체성을 유지한다.
//   핵심: 보라(primary)는 활기, 코랄(secondary)은 포인트, 민트(tertiary)는 보조 강조.
//   Material3의 '색 역할(role)' 체계 — primary/그 위 글자(onPrimary)/옅은 배경(container) 쌍으로 쓴다.
// ─────────────────────────────────────────────────────────────────────────────

// ── Light ──
val BrandPrimary = Color(0xFF6C5CE7)            // 메인 보라(버튼·강조)
val BrandOnPrimary = Color(0xFFFFFFFF)          // 보라 위 글자
val BrandPrimaryContainer = Color(0xFFE7E2FF)   // 옅은 라벤더(진행률 카드 등 넓은 면)
val BrandOnPrimaryContainer = Color(0xFF22136B) // 그 위 진한 글자

val BrandSecondary = Color(0xFFFF6B81)          // 코랄(보조 포인트)
val BrandOnSecondary = Color(0xFFFFFFFF)
val BrandSecondaryContainer = Color(0xFFFFE0E4)
val BrandOnSecondaryContainer = Color(0xFF5C1422)

val BrandTertiary = Color(0xFF00B894)           // 민트(달성/성공 느낌)
val BrandOnTertiary = Color(0xFFFFFFFF)
val BrandTertiaryContainer = Color(0xFFC7F4E8)
val BrandOnTertiaryContainer = Color(0xFF00382C)

val BrandBackground = Color(0xFFF6F5FB)         // 살짝 보랏빛 도는 오프화이트(전체 배경)
val BrandOnBackground = Color(0xFF1B1B26)
val BrandSurface = Color(0xFFFFFFFF)            // 카드 면
val BrandOnSurface = Color(0xFF1B1B26)
val BrandSurfaceVariant = Color(0xFFECEAF4)     // 옅은 회보라(트랙·구분면)
val BrandOnSurfaceVariant = Color(0xFF6B6880)   // 보조 텍스트(회색)
val BrandOutline = Color(0xFFD7D4E4)            // 옅은 외곽선

// ── Dark ──
val BrandPrimaryDark = Color(0xFFC4B9FF)
val BrandOnPrimaryDark = Color(0xFF2A1A78)
val BrandPrimaryContainerDark = Color(0xFF4A3CC0)
val BrandOnPrimaryContainerDark = Color(0xFFE7E2FF)

val BrandSecondaryDark = Color(0xFFFFB1BB)
val BrandOnSecondaryDark = Color(0xFF5C1422)
val BrandSecondaryContainerDark = Color(0xFFA13848)
val BrandOnSecondaryContainerDark = Color(0xFFFFE0E4)

val BrandTertiaryDark = Color(0xFF6FE0C6)
val BrandOnTertiaryDark = Color(0xFF00382C)
val BrandTertiaryContainerDark = Color(0xFF00876B)
val BrandOnTertiaryContainerDark = Color(0xFFC7F4E8)

val BrandBackgroundDark = Color(0xFF121118)
val BrandOnBackgroundDark = Color(0xFFE7E4F0)
val BrandSurfaceDark = Color(0xFF1D1B26)
val BrandOnSurfaceDark = Color(0xFFE7E4F0)
val BrandSurfaceVariantDark = Color(0xFF2C2A38)
val BrandOnSurfaceVariantDark = Color(0xFFB6B2C7)
val BrandOutlineDark = Color(0xFF454258)
