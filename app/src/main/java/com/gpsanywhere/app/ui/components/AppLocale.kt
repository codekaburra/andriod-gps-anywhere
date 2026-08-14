package com.gpsanywhere.app.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * The locale-adjusted context behind the in-app language setting.
 *
 * The app applies its language by providing a `createConfigurationContext`
 * wrapper as [LocalContext], which every `stringResource()` then resolves
 * against. That works on the screens themselves, but not inside a
 * `ModalBottomSheet`, `Dialog` or `Popup`: each of those hosts its content in a
 * separate window whose ComposeView re-provides the Android composition locals,
 * putting the activity's own unwrapped context back as [LocalContext]. The
 * result was an add-location sheet whose title was Chinese — it is passed in as
 * a parameter, resolved outside — while every label inside it was English.
 *
 * This local is not one of the Android locals, so it survives the crossing.
 */
val LocalAppLocaleContext = staticCompositionLocalOf<Context?> { null }

/**
 * Re-applies the in-app language inside a dialog or bottom-sheet window.
 *
 * Wrap the content of anything that renders in its own window. On the SYSTEM
 * language setting there is no wrapper to restore and this is a no-op.
 */
@Composable
fun ProvideAppLocale(content: @Composable () -> Unit) {
    val localized = LocalAppLocaleContext.current
    if (localized == null) {
        content()
    } else {
        CompositionLocalProvider(LocalContext provides localized, content = content)
    }
}
