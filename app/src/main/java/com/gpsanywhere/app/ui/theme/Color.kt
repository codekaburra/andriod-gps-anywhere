package com.gpsanywhere.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Liquid Glass (warm) ──────────────────────────────────────────────────────
val LiquidOrange = Color(0xFFFF8A5C)               // primary accent
val LiquidOrangeBright = Color(0xFFFF9F6B)         // gradient start / dark-mode primary

// ── Classic Glassmorphism ────────────────────────────────────────────────────
val GlassIndigo = LiquidOrange                     // primary (light)
val GlassIndigoLight = LiquidOrangeBright          // primary (dark)
val GlassGreen = Color(0xFF4CAF50)                 // start button
val StopRed = Color(0xFFEF5350)                    // stop / error

// ── Autumn palette (light theme) ────────────────────────────────────────────
// Every light-mode colour below is derived from these five, so retuning one
// keeps the theme coherent.
val MapleSpice = Color(0xFF692721)
val BurntOrange = Color(0xFF8B4729)
val SageGreen = Color(0xFF818865)
val Gold = Color(0xFFBB8D3F)
val MossGreen = Color(0xFF45492D)

// White cards on a warm off-white app background, separated by a hairline
// rather than a heavy outline — the previous two-beige stack read as muddy.
val BackgroundBase = Color(0xFFF8F4EC)
val SurfaceWhite = Color(0xFFFFFFFF)
val BorderSubtle = Color(0xFFE5DFD3)
val SelectedPill = Color(0xFFF0E6D8)

// ── Role assignments (light) ────────────────────────────────────────────────
// Primary leads, secondary marks edit/delete, accent handles map controls and
// tags; the two greens carry text and everything unselected.
val LightPrimary = MapleSpice
val LightSecondary = BurntOrange
val LightAccent = Gold
val LightNeutralLight = SageGreen
val LightNeutralDark = MossGreen

// ── Ember palette (dark theme) ──────────────────────────────────────────────
// Six colours, assigned by what their contrast allows rather than by taste:
//  · DarkRed reads 1.8:1 on the background — invisible as an icon, but white on
//    it is 10:1, so it is a filled button only.
//  · AmberGold, Cornsilk and CarrotOrange all fail against white (2.2/1.7/2.4)
//    and must carry dark content; they read 7–11:1 as icons on the background.
//  · Mahogany is the only one that works both ways (3.4:1 as an icon, 5.3:1
//    under white), so it takes the role that is drawn both ways.
val DarkRed = Color(0xFF8B0000)
val Cornsilk = Color(0xFFEDC373)
val SteelBlue = Color(0xFF1A2B3C)
val CarrotOrange = Color(0xFFED9121)
val Mahogany = Color(0xFFC04000)
val AmberGold = Color(0xFFF59E0B)

// ── Role assignments (dark) ─────────────────────────────────────────────────
val DarkPrimary = AmberGold          // transport, start, nav selected, slider
val DarkOnPrimary = SteelBlue        // 6.7:1 on amber; white would be 2.2:1
val DarkSecondary = CarrotOrange     // edit
val DarkDestructive = Mahogany       // delete/stop, drawn as icon and as fill
val DarkDestructiveFill = DarkRed    // the loud filled destructive buttons
val DarkNeutralLight = Cornsilk      // markers, muted text
val DarkSurfaceBase = SteelBlue

// ── Glass surfaces ──────────────────────────────────────────────────────────
val GlassBackgroundLight = BackgroundBase
val GlassSurfaceLight = SurfaceWhite
val GlassSurfaceVariantLight = SurfaceWhite
// Moss on the base reads 7.9:1; sage, the secondary tone, 3.6:1.
val GlassTextLight = LightNeutralDark
val GlassMutedLight = LightNeutralLight
val GlassBorderLight = BorderSubtle
val GlassNavLight = BackgroundBase

val GlassBackgroundDark = Color(0xFF101A24)   // Steel Blue, darkened for the page ground
val GlassSurfaceDark = DarkSurfaceBase.copy(alpha = 0.85f)
val GlassSurfaceVariantDark = DarkSurfaceBase.copy(alpha = 0.75f)
val GlassTextDark = Color(0xFFF1F5F9)
val GlassMutedDark = GlassTextDark.copy(alpha = 0.65f)
val GlassBorderDark = Color(0xFF475569).copy(alpha = 0.5f)
val GlassNavDark = DarkSurfaceBase.copy(alpha = 0.95f)

// ── Glass card / button helpers ─────────────────────────────────────────────
// Neutral translucent-white edge — pure glassmorphism, no colour tint.
val GlassCardBorder = Color.White.copy(alpha = 0.35f)

// ── Location card / icon button palette (cool, watercolour-friendly) ─────────
val CardFill = Color.White.copy(alpha = 0.20f)
val CardFillSelected = Color.White.copy(alpha = 0.65f)
val CardBorder = Color.White.copy(alpha = 0.30f)
val CardNameText = GlassTextLight             // light mode, non-selected name
val CardCoordText = GlassMutedLight           // light mode, non-selected coords
val CardNameTextDark = Color(0xFFF1F5F9)      // dark mode, non-selected name
val CardCoordTextDark = Color(0xFFCBD5E1)     // dark mode, non-selected coords
val CardNameTextSelected = LightNeutralDark  // selected name
val CardCoordTextSelected = LightNeutralLight // selected coords

val NavSelected = DarkPrimary


// ── Slider (yellow functional accent) ───────────────────────────────────────
// The track colours are theme-dependent, so they live on AppAccent rather than
// here: a single constant shared by both themes leaks the light tint into dark.
val SliderThumb = DarkPrimary
val SliderTrackDark = DarkSurfaceBase.copy(alpha = 0.9f)

// ── Neutral buttons (back/revert) ───────────────────────────────────────────
val NeutralButtonBg = SurfaceWhite
val NeutralButtonContent = LightNeutralDark

// ── Map ─────────────────────────────────────────────────────────────────────
val MapPolyline = 0xFF45492D.toInt()  // Moss Green — map markers and the route line

// ── Tag colors ──────────────────────────────────────────────────────────────
// A translucent neutral grey in both themes, so the pill reads as a soft chip
// rather than a coloured badge. Dark mode tints white over the dark artwork;
// light mode tints a warm charcoal, kept dark enough for white lettering.
val TagChipFillLight = Color(0xFF55554F).copy(alpha = 0.75f)

// ── Legacy aliases (keep existing code compiling during migration) ──────────
val CandyGreen = GlassGreen
val CandyYellow = DarkPrimary


// ── Golden Hour (Dark Mode) — replaced by Glass Dark ────────────────────────


