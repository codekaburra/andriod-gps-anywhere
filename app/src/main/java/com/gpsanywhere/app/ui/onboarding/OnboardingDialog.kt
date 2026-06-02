package com.gpsanywhere.app.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String
)

private val pages = listOf(
    OnboardingPage(
        title = "Enable Developer Options",
        body = "Go to Settings → About Phone → tap Build Number 7 times to enable Developer Options."
    ),
    OnboardingPage(
        title = "Select Mock Location App",
        body = "Open Developer Options → Select Mock Location App → choose GPS Anywhere."
    ),
    OnboardingPage(
        title = "Grant Permissions",
        body = "Allow location and notification permissions when prompted so spoofing and the status notification work correctly."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup GPS Anywhere") },
        text = {
            Column {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = pages[page].title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = pages[page].body,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Step ${pagerState.currentPage + 1} of ${pages.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Skip")
                }
                TextButton(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onDismiss()
                        }
                    }
                ) {
                    Text(
                        if (pagerState.currentPage == pages.size - 1) "Got it" else "Next"
                    )
                }
            }
        }
    )
}
