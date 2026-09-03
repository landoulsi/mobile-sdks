package com.landoulsi.update.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fixed spacing scale used across update UI components. */
object UpdateSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Corner radii consistent with drop-in sheet, card inputs, and buttons. */
object UpdateRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 999.dp
}

/** Elevation tokens for bottom sheet, cards, and floating buttons. */
object UpdateElevation {
    val none = 0.dp
    val xs = 1.dp
    val sm = 2.dp
    val md = 4.dp
    val lg = 8.dp
    val xl = 16.dp
}

/** Type scale sizes used in update forms and confirmations. */
object UpdateTypeSize {
    val display = 32.sp
    val headline = 24.sp
    val title = 20.sp
    val body = 16.sp
    val label = 14.sp
    val caption = 12.sp
}
