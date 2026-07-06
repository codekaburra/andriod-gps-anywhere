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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: Int,
    val body: Int
)

private val pages = listOf(
    OnboardingPage(R.string.onboarding_step_1_title, R.string.onboarding_step_1_body),
    OnboardingPage(R.string.onboarding_step_2_title, R.string.onboarding_step_2_body),
    OnboardingPage(R.string.onboarding_step_3_title, R.string.onboarding_step_3_body)
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
        title = { Text(stringResource(R.string.onboarding_title)) },
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
                            text = stringResource(pages[page].title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(pages[page].body),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_step_n_of, pagerState.currentPage + 1, pages.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_skip))
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
                        stringResource(
                            if (pagerState.currentPage == pages.size - 1) R.string.action_got_it
                            else R.string.action_next
                        )
                    )
                }
            }
        }
    )
}
