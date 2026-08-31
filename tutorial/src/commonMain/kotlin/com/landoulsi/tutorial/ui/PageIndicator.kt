package com.landoulsi.tutorial.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landoulsi.tutorial.model.IndicatorStyle

internal fun Long.toComposeColor(): Color {
    val a = ((this shr 24) and 0xFF).toInt()
    val r = ((this shr 16) and 0xFF).toInt()
    val g = ((this shr 8) and 0xFF).toInt()
    val b = (this and 0xFF).toInt()
    return Color(red = r, green = g, blue = b, alpha = a)
}

/**
 * Animated page indicator component supporting dots, expanding pills, progress bar, and numeric counter.
 */
@Composable
fun PageIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    style: IndicatorStyle = IndicatorStyle.ExpandingPill(),
    onPageSelected: ((Int) -> Unit)? = null
) {
    PageIndicator(
        currentPage = pagerState.currentPage,
        pageCount = pageCount,
        currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
        modifier = modifier,
        style = style,
        onPageSelected = onPageSelected
    )
}

/**
 * Stateless page indicator rendering current page progress and animations.
 */
@Composable
fun PageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
    currentPageOffsetFraction: Float = 0f,
    style: IndicatorStyle = IndicatorStyle.ExpandingPill(),
    onPageSelected: ((Int) -> Unit)? = null
) {
    if (pageCount <= 1) return

    when (style) {
        is IndicatorStyle.Dots -> {
            DotsIndicator(
                pageCount = pageCount,
                currentPage = currentPage,
                style = style,
                modifier = modifier,
                onPageSelected = onPageSelected
            )
        }
        is IndicatorStyle.ExpandingPill -> {
            ExpandingPillIndicator(
                pageCount = pageCount,
                currentPage = currentPage,
                currentPageOffsetFraction = currentPageOffsetFraction,
                style = style,
                modifier = modifier,
                onPageSelected = onPageSelected
            )
        }
        is IndicatorStyle.ProgressBar -> {
            ProgressBarIndicator(
                pageCount = pageCount,
                currentPage = currentPage,
                currentPageOffsetFraction = currentPageOffsetFraction,
                style = style,
                modifier = modifier
            )
        }
        is IndicatorStyle.NumericCounter -> {
            NumericCounterIndicator(
                pageCount = pageCount,
                currentPage = currentPage,
                style = style,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DotsIndicator(
    pageCount: Int,
    currentPage: Int,
    style: IndicatorStyle.Dots,
    modifier: Modifier = Modifier,
    onPageSelected: ((Int) -> Unit)? = null
) {
    val activeColor = style.activeColorHex.toComposeColor()
    val inactiveColor = style.inactiveColorHex.toComposeColor()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(style.spacingDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until pageCount) {
            val isSelected = index == currentPage
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) activeColor else inactiveColor,
                animationSpec = tween(durationMillis = 300),
                label = "dot_color_$index"
            )
            val animatedScale by animateFloatAsState(
                targetValue = if (isSelected) 1.25f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "dot_scale_$index"
            )

            Box(
                modifier = Modifier
                    .size((style.dotSizeDp * animatedScale).dp)
                    .clip(CircleShape)
                    .background(animatedColor)
                    .then(
                        if (onPageSelected != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPageSelected(index) }
                        } else Modifier
                    )
            )
        }
    }
}

@Composable
private fun ExpandingPillIndicator(
    pageCount: Int,
    currentPage: Int,
    currentPageOffsetFraction: Float,
    style: IndicatorStyle.ExpandingPill,
    modifier: Modifier = Modifier,
    onPageSelected: ((Int) -> Unit)? = null
) {
    val activeColor = style.activeColorHex.toComposeColor()
    val inactiveColor = style.inactiveColorHex.toComposeColor()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(style.spacingDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until pageCount) {
            val isSelected = index == currentPage
            val animatedWidth by animateDpAsState(
                targetValue = if (isSelected) style.activeWidthDp.dp else style.inactiveWidthDp.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "pill_width_$index"
            )
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) activeColor else inactiveColor,
                animationSpec = tween(durationMillis = 300),
                label = "pill_color_$index"
            )

            Box(
                modifier = Modifier
                    .height(style.dotHeightDp.dp)
                    .width(animatedWidth)
                    .clip(RoundedCornerShape((style.dotHeightDp / 2f).dp))
                    .background(animatedColor)
                    .then(
                        if (onPageSelected != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPageSelected(index) }
                        } else Modifier
                    )
            )
        }
    }
}

@Composable
private fun ProgressBarIndicator(
    pageCount: Int,
    currentPage: Int,
    currentPageOffsetFraction: Float,
    style: IndicatorStyle.ProgressBar,
    modifier: Modifier = Modifier
) {
    val activeColor = style.activeColorHex.toComposeColor()
    val inactiveColor = style.inactiveColorHex.toComposeColor()

    val continuousProgress = ((currentPage + currentPageOffsetFraction + 1f) / pageCount.toFloat())
        .coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = continuousProgress,
        animationSpec = tween(durationMillis = 200),
        label = "progress_bar_anim"
    )

    Box(
        modifier = modifier
            .height(style.heightDp.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape((style.heightDp / 2f).dp))
            .background(inactiveColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(activeColor)
        )
    }
}

@Composable
private fun NumericCounterIndicator(
    pageCount: Int,
    currentPage: Int,
    style: IndicatorStyle.NumericCounter,
    modifier: Modifier = Modifier
) {
    val textColor = style.textColorHex.toComposeColor()
    val bgColor = style.backgroundColorHex.toComposeColor()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(style.cornerRadiusDp.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${currentPage + 1} / $pageCount",
            color = textColor,
            fontSize = style.fontSizeSp.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
