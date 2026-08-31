package com.landoulsi.tutorial.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landoulsi.tutorial.model.CarouselConfig
import com.landoulsi.tutorial.model.IndicatorStyle
import com.landoulsi.tutorial.model.OnboardingFlow
import com.landoulsi.tutorial.model.PagerConfig
import com.landoulsi.tutorial.model.TutorialPage
import com.landoulsi.tutorial.tracker.TutorialTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Full-screen customizable onboarding pager with swipe gestures, page indicators, and tracker integration.
 */
@Composable
fun TutorialPager(
    pages: List<TutorialPage>,
    modifier: Modifier = Modifier,
    pagerConfig: PagerConfig = PagerConfig.Default,
    indicatorStyle: IndicatorStyle = IndicatorStyle.ExpandingPill(),
    tracker: TutorialTracker? = null,
    tutorialId: String? = null,
    onFinished: () -> Unit = {},
    onSkip: (() -> Unit)? = null,
    onPageChanged: ((pageIndex: Int, page: TutorialPage) -> Unit)? = null,
    header: (@Composable (currentPage: Int, pageCount: Int, onSkip: (() -> Unit)?) -> Unit)? = null,
    footer: (@Composable (currentPage: Int, pageCount: Int, isLastPage: Boolean, onNext: () -> Unit, onPrev: () -> Unit, onFinish: () -> Unit) -> Unit)? = null,
    indicator: (@Composable (pagerState: PagerState, pageCount: Int) -> Unit)? = null,
    pageContent: (@Composable (page: TutorialPage, pageIndex: Int) -> Unit)? = null
) {
    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = 0) { pages.size }
    val coroutineScope = rememberCoroutineScope()

    // Track active page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                val page = pages.getOrNull(pageIndex)
                if (page != null) {
                    onPageChanged?.invoke(pageIndex, page)
                    if (tracker != null && tutorialId != null) {
                        tracker.completeStep(tutorialId, page.id, pageIndex)
                    }
                }
            }
    }

    val handleFinish = {
        if (tracker != null && tutorialId != null) {
            coroutineScope.launch {
                tracker.completeTutorial(tutorialId)
            }
        }
        onFinished()
    }

    val handleSkip: (() -> Unit)? = onSkip?.let { skipCallback ->
        {
            if (tracker != null && tutorialId != null) {
                coroutineScope.launch {
                    tracker.skipTutorial(tutorialId)
                }
            }
            skipCallback()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Slot
            if (header != null) {
                header(pagerState.currentPage, pages.size, handleSkip)
            } else {
                DefaultPagerHeader(
                    canSkip = handleSkip != null,
                    onSkip = { handleSkip?.invoke() }
                )
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = pagerConfig.userScrollEnabled && pagerConfig.isSwipeEnabled,
                contentPadding = PaddingValues(horizontal = pagerConfig.contentPaddingDp.dp),
                pageSpacing = pagerConfig.pageSpacingDp.dp
            ) { pageIndex ->
                val page = pages[pageIndex]
                if (pageContent != null) {
                    pageContent(page, pageIndex)
                } else {
                    DefaultTutorialPageContent(
                        page = page,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    )
                }
            }

            // Indicator Slot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (indicator != null) {
                    indicator(pagerState, pages.size)
                } else {
                    PageIndicator(
                        pagerState = pagerState,
                        pageCount = pages.size,
                        style = indicatorStyle,
                        onPageSelected = { targetPage ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }
                    )
                }
            }

            // Footer Slot
            val isLastPage = pagerState.currentPage == pages.size - 1
            val onNext = {
                if (!isLastPage) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    handleFinish()
                }
            }
            val onPrev = {
                if (pagerState.currentPage > 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            }

            if (footer != null) {
                footer(
                    pagerState.currentPage,
                    pages.size,
                    isLastPage,
                    { onNext() },
                    onPrev,
                    handleFinish
                )
            } else {
                DefaultPagerFooter(
                    currentPage = pagerState.currentPage,
                    pageCount = pages.size,
                    isLastPage = isLastPage,
                    onNext = { onNext() },
                    onPrev = onPrev,
                    onFinish = handleFinish
                )
            }
        }
    }
}

/**
 * Convenience entry point for an [OnboardingFlow] configuration.
 */
@Composable
fun OnboardingScreen(
    flow: OnboardingFlow,
    modifier: Modifier = Modifier,
    tracker: TutorialTracker? = null,
    onFinished: () -> Unit = {},
    onSkip: (() -> Unit)? = null,
    onPageChanged: ((pageIndex: Int, page: TutorialPage) -> Unit)? = null
) {
    TutorialPager(
        pages = flow.pages,
        modifier = modifier,
        pagerConfig = flow.pagerConfig,
        indicatorStyle = flow.indicatorStyle,
        tracker = tracker,
        tutorialId = flow.id,
        onFinished = onFinished,
        onSkip = onSkip,
        onPageChanged = onPageChanged
    )
}

/**
 * Feature card carousel with support for card peeking, auto-advancing coroutine timer,
 * and pause-on-drag interaction.
 */
@Composable
fun <T> FeatureCarousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    carouselConfig: CarouselConfig = CarouselConfig.Default,
    indicatorStyle: IndicatorStyle? = IndicatorStyle.ExpandingPill(),
    onItemClick: ((T) -> Unit)? = null,
    itemContent: @Composable (item: T, index: Int) -> Unit
) {
    if (items.isEmpty()) return

    val actualPageCount = items.size
    val pagerState = rememberPagerState(initialPage = 0) { actualPageCount }
    val coroutineScope = rememberCoroutineScope()

    // Auto-advance coroutine timer (auto-pauses when user drags/touches)
    if (carouselConfig.isAutoScrollEnabled && actualPageCount > 1) {
        LaunchedEffect(
            pagerState,
            carouselConfig.isAutoScrollEnabled,
            carouselConfig.autoScrollIntervalMs,
            actualPageCount
        ) {
            while (true) {
                delay(carouselConfig.autoScrollIntervalMs)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = if (carouselConfig.infiniteLoop) {
                        (pagerState.currentPage + 1) % actualPageCount
                    } else {
                        if (pagerState.currentPage < actualPageCount - 1) {
                            pagerState.currentPage + 1
                        } else {
                            0
                        }
                    }
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = carouselConfig.peekOffsetDp.dp),
            pageSpacing = carouselConfig.pageSpacingDp.dp,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
        ) { pageIndex ->
            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = (1f - (pageOffset * 0.08f)).coerceIn(0.88f, 1f)
            val alpha = (1f - (pageOffset * 0.3f)).coerceIn(0.6f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                itemContent(items[pageIndex], pageIndex)
            }
        }

        if (indicatorStyle != null && actualPageCount > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            PageIndicator(
                pagerState = pagerState,
                pageCount = actualPageCount,
                style = indicatorStyle,
                onPageSelected = { targetPage ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            )
        }
    }
}

/**
 * Feature card carousel specifically displaying a collection of [TutorialPage] models.
 */
@Composable
fun TutorialFeatureCarousel(
    pages: List<TutorialPage>,
    modifier: Modifier = Modifier,
    carouselConfig: CarouselConfig = CarouselConfig.Default,
    indicatorStyle: IndicatorStyle? = IndicatorStyle.ExpandingPill(),
    onPageClick: ((TutorialPage) -> Unit)? = null
) {
    FeatureCarousel(
        items = pages,
        modifier = modifier,
        carouselConfig = carouselConfig,
        indicatorStyle = indicatorStyle,
        onItemClick = onPageClick
    ) { page, _ ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(carouselConfig.aspectRatio),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            onClick = { onPageClick?.invoke(page) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                if (page.badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = page.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun DefaultPagerHeader(
    canSkip: Boolean,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = canSkip,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DefaultTutorialPageContent(
    page: TutorialPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (page.badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = page.badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Placeholder graphic area
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1.2f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = page.title.take(1),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun DefaultPagerFooter(
    currentPage: Int,
    pageCount: Int,
    isLastPage: Boolean,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentPage > 0) {
            OutlinedButton(
                onClick = onPrev,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Previous")
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Button(
            onClick = {
                if (isLastPage) onFinish() else onNext()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isLastPage) "Get Started" else "Next")
        }
    }
}
