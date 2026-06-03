package com.gpsanywhere.app.ui.theme

import androidx.compose.ui.graphics.Color

// Galaxy / Cosmic theme palette
// Deep space darks with vibrant nebula accents (indigo + cyan glows)

val GalaxyBackgroundDark = Color(0xFF0A0C1A)
val GalaxySurfaceDark = Color(0xFF12162B)
val GalaxyPrimary = Color(0xFF6366F1)       // indigo
val GalaxyPrimaryLight = Color(0xFF818CF8)  // lighter indigo for dark mode
val GalaxyAccent = Color(0xFF22D3EE)        // bright cyan "star glow" for active custom location
val GalaxyMuted = Color(0xFF64748B)
val GalaxyTextOnDark = Color(0xFFE0E7FF)    // soft lavender-white

val BackgroundLight = Color(0xFFF0F4FF)
val SurfaceLight = Color(0xFFFFFFFF)
val ErrorRed = Color(0xFFEF4444)

// Legacy aliases for minimal breakage (point to new palette)
val PrimaryBlue = GalaxyPrimary
val PrimaryBlueLight = GalaxyPrimaryLight
val AccentGreen = GalaxyAccent
val BackgroundDark = GalaxyBackgroundDark
val SurfaceDark = GalaxySurfaceDark
val TextMuted = GalaxyMuted
