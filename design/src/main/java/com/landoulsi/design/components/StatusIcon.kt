package com.landoulsi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.landoulsi.design.SuccessGreen

enum class StatusIconVariant {
    Success,
    Error,
    Warning,
}

@Composable
fun DesignStatusIcon(
    variant: StatusIconVariant,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    icon: ImageVector? = null,
) {
    val (backgroundColor, defaultIcon, contentColor) = when (variant) {
        StatusIconVariant.Success -> Triple(
            SuccessGreen,
            Icons.Filled.Check,
            androidx.compose.ui.graphics.Color.White,
        )
        StatusIconVariant.Error -> Triple(
            MaterialTheme.colorScheme.error,
            Icons.Filled.Close,
            MaterialTheme.colorScheme.onError,
        )
        StatusIconVariant.Warning -> Triple(
            MaterialTheme.colorScheme.tertiary,
            Icons.Filled.Warning,
            MaterialTheme.colorScheme.onTertiary,
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon ?: defaultIcon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
