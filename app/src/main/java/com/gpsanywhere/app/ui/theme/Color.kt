package com.gpsanywhere.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Liquid Glass (warm) ──────────────────────────────────────────────────────
val LiquidOrange = Color(0xFFFF8A5C)               // primary accent
val LiquidOrangeBright = Color(0xFFFF9F6B)         // gradient start / dark-mode primary
val LiquidOrangeDeep = Color(0xFFFF7A4D)           // gradient end
val LiquidBorder = Color(0xFFFFB38A)               // warm glass border
val LiquidGlow = Color(0xFFFFF0E6)                 // soft inner radial glow

// ── Classic Glassmorphism ────────────────────────────────────────────────────
val GlassIndigo = LiquidOrange                     // primary (light)
val GlassIndigoLight = LiquidOrangeBright          // primary (dark)
val GlassGreen = Color(0xFF4CAF50)                 // start button
val StopRed = Color(0xFFEF5350)                    // stop / error

// ── Glass surfaces ──────────────────────────────────────────────────────────
val GlassBackgroundLight = Color(0xFFFFF6EF)
val GlassSurfaceLight = Color.White.copy(alpha = 0.85f)
val GlassSurfaceVariantLight = Color.White.copy(alpha = 0.75f)
val GlassTextLight = Color(0xFF1E2937)
val GlassMutedLight = Color(0xFF94A3B8)
val GlassBorderLight = Color(0xFFCBD5E1).copy(alpha = 0.6f)
val GlassNavLight = Color.White.copy(alpha = 0.95f)

val GlassBackgroundDark = Color(0xFF0F172A)
val GlassSurfaceDark = Color(0xFF1E2937).copy(alpha = 0.85f)
val GlassSurfaceVariantDark = Color(0xFF1E2937).copy(alpha = 0.75f)
val GlassTextDark = Color(0xFFF1F5F9)
val GlassMutedDark = Color(0xFF94A3B8)
val GlassBorderDark = Color(0xFF475569).copy(alpha = 0.5f)
val GlassNavDark = Color(0xFF1E2937).copy(alpha = 0.95f)

// ── Glass card / button helpers ─────────────────────────────────────────────
// Neutral translucent-white edge — pure glassmorphism, no colour tint.
val GlassCardBorder = Color.White.copy(alpha = 0.35f)

// ── Location card / icon button palette (cool, watercolour-friendly) ─────────
val CardFill = Color.White.copy(alpha = 0.20f)
val CardFillSelected = Color.White.copy(alpha = 0.65f)
val CardBorder = Color.White.copy(alpha = 0.30f)
val CardBorderSelected = Color(0xFFF59E0B).copy(alpha = 0.95f)
val CardNameText = Color(0xFF1F2937)          // light mode, non-selected name
val CardCoordText = Color(0xFF64748B)         // light mode, non-selected coords
val CardNameTextDark = Color(0xFFF1F5F9)      // dark mode, non-selected name
val CardCoordTextDark = Color(0xFFCBD5E1)     // dark mode, non-selected coords
val CardNameTextSelected = Color(0xFF111827)  // selected name (white-ish card, both modes)
val CardCoordTextSelected = Color(0xFF334155) // selected coords

val NavSelected = Color(0xFFF59E0B)
val NavUnselected = Color(0xFF94A3B8)

val IconInactive = Color(0xFF64748B).copy(alpha = 0.85f)
val IconActive = Color(0xFFF59E0B)
val IconActiveBg = Color(0xFFFEF3C7).copy(alpha = 0.60f)

// ── Slider (yellow functional accent) ───────────────────────────────────────
val SliderThumb = Color(0xFFF59E0B)
val SliderActiveTrack = Color(0xFFF59E0B)
val SliderInactiveTrack = Color(0xFFE8D5C8).copy(alpha = 0.6f)

// ── Neutral buttons (back/revert) ───────────────────────────────────────────
val NeutralButtonBg = Color(0xFFF5EDE6)
val NeutralButtonContent = Color(0xFF6B5544)

// ── Map ─────────────────────────────────────────────────────────────────────
val MapPolylineBlue = 0xFFF59E0B.toInt()
val MapPinOrange = 0xFFF59E0B.toInt()

// ── Tag colors ──────────────────────────────────────────────────────────────
val CandyTagColors = listOf(
    Color(0xFFFF8A5C),  // warm orange
    Color(0xFFFFB38A),  // peach
    Color(0xFFE9967A),  // dark salmon
    Color(0xFFF4A460),  // sandy
    Color(0xFFFFC04D)   // amber gold
)

// ── Legacy aliases (keep existing code compiling during migration) ──────────
val CandyOrange = GlassIndigo
val CandyGreen = GlassGreen
val CandyBlue = GlassIndigo
val CandyPink = Color(0xFF8B5CF6)
val CandyYellow = Color(0xFFF59E0B)
val DustyRose = GlassIndigo
val SoftPurple = GlassIndigo

val CandyBackground = GlassBackgroundLight
val CandySurface = GlassSurfaceLight
val CandySurfaceVariant = GlassSurfaceVariantLight
val CandyText = GlassTextLight
val CandyMuted = GlassMutedLight
val CandyBorder = GlassBorderLight

// ── Golden Hour (Dark Mode) — replaced by Glass Dark ────────────────────────
val GoldenCream = Color(0xFFE3D2B4)
val GoldenOlive = Color(0xFF927956)
val GoldenCopper = GlassIndigoLight
val GoldenSlate = Color(0xFF465248)
val GoldenInk = GlassBackgroundDark

val GoldenBackgroundDark = GlassBackgroundDark
val GoldenSurfaceDark = GlassSurfaceDark
val GoldenSurfaceVariantDark = GlassSurfaceVariantDark
val GoldenTextOnDark = GlassTextDark
val GoldenMutedOnDark = GlassMutedDark
val GoldenBorderDark = GlassBorderDark

val ErrorRed = StopRed
