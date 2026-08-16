package com.ticketcheck.offline.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Premium dark palette ────────────────────────────────────────────────
// Deep space-blue base, violet→indigo→cyan brand gradient, emerald/rose/
// amber semantic colors. Gold is reserved for small premium accents.

val BgDeep = Color(0xFF060913)
val BgElevated = Color(0xFF0C1220)
val SurfaceGlass = Color(0xFF121A2C)

val Primary = Color(0xFF8B5CF6)
val PrimaryDim = Color(0xFF6D4FE0)
val Indigo = Color(0xFF6366F1)
val Accent = Color(0xFF38BDF8)

val Success = Color(0xFF34D399)
val SuccessDeep = Color(0xFF10B981)
val ErrorRed = Color(0xFFF87171)
val ErrorDeep = Color(0xFFEF4444)
val WarningAmber = Color(0xFFFBBF24)
val WarningDeep = Color(0xFFF59E0B)
val Gold = Color(0xFFF5C66B)

val OnDark = Color(0xFFF4F6FB)
val OnDarkMuted = Color(0xFF9AA3B8)

val BrandGradient = listOf(Primary, Indigo, Accent)
val SuccessGradient = listOf(Success, Accent)
val DangerGradient = listOf(Color(0xFFDC2626), ErrorDeep)

// ─── Premium light palette ───────────────────────────────────────────────
val BgLight = Color(0xFFF3F5FB)
val BgElevatedLight = Color(0xFFFFFFFF)
val OnLight = Color(0xFF111726)
val OnLightMuted = Color(0xFF5B6478)

// Semantic aliases used across the app.
val GreenValid = Success
val RedInvalid = ErrorRed
val AmberUsed = WarningAmber
