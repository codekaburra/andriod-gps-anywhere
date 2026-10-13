package com.gpsanywhere.app.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gpsanywhere.app.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    @param:DrawableRes val illustration: Int
)

private val pages = listOf(
    OnboardingPage(R.string.onboarding_step_1_title, R.string.onboarding_step_1_body, R.drawable.onboarding_pixel_about_phone),
    OnboardingPage(R.string.onboarding_step_2_title, R.string.onboarding_step_2_body, R.drawable.onboarding_mock_location),
    OnboardingPage(R.string.onboarding_step_3_title, R.string.onboarding_step_3_body, R.drawable.onboarding_import_locations),
    OnboardingPage(
        R.string.onboarding_step_4_title,
        R.string.onboarding_step_4_body,
        R.drawable.onboarding_location_list
    ),
    OnboardingPage(
        R.string.onboarding_step_5_title,
        R.string.onboarding_step_5_body,
        R.drawable.onboarding_custom_coordinates
    ),
    OnboardingPage(
        R.string.onboarding_step_6_title,
        R.string.onboarding_step_6_body,
        R.drawable.onboarding_speed_panel
    ),
    OnboardingPage(
        R.string.onboarding_step_7_title,
        R.string.onboarding_step_7_body,
        R.drawable.onboarding_route_view
    ),
    OnboardingPage(
        R.string.onboarding_step_8_title,
        R.string.onboarding_step_8_body,
        R.drawable.onboarding_add_location
    ),
    OnboardingPage(
        R.string.onboarding_step_9_title,
        R.string.onboarding_step_9_body,
        R.drawable.onboarding_route_editor
    ),
    OnboardingPage(
        R.string.onboarding_step_10_title,
        R.string.onboarding_step_10_body,
        R.drawable.onboarding_route_csv
    )
)

private val Paper = Color(0xFFF8F2E7)
private val Ink = Color(0xFF493A32)
private val Sage = Color(0xFF697E67)

@Composable
fun OnboardingDialog(
    isReplay: Boolean = false,
    onDismiss: () -> Unit = {},
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_title),
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp
                    )
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_skip), color = Sage)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex ->
                    val page = pages[pageIndex]
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(page.illustration),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(page.title),
                            color = Ink,
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(page.body),
                            color = Ink,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == pagerState.currentPage) 10.dp else 7.dp)
                                .background(
                                    if (index == pagerState.currentPage) Sage else Color(0xFFD2C9B9),
                                    CircleShape
                                )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(
                        R.string.onboarding_step_n_of,
                        pagerState.currentPage + 1,
                        pages.size
                    ),
                    color = Sage,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (pagerState.currentPage == pages.lastIndex) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Sage)
                ) {
                    Text(
                        text = stringResource(
                            when {
                                pagerState.currentPage != pages.lastIndex -> R.string.action_next
                                isReplay -> R.string.onboarding_done
                                else -> R.string.onboarding_go_to_settings
                            }
                        ),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
