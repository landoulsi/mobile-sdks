package com.landoulsi.payment.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fixed spacing scale used across payment UI components. */
object PaymentSpacing {
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
object PaymentRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 999.dp
}

/** Elevation tokens for bottom sheet, cards, and floating buttons. */
object PaymentElevation {
    val none = 0.dp
    val xs = 1.dp
    val sm = 2.dp
    val md = 4.dp
    val lg = 8.dp
    val xl = 16.dp
}

/** Type scale sizes used in payment forms and confirmations. */
object PaymentTypeSize {
    val display = 32.sp
    val headline = 24.sp
    val title = 20.sp
    val body = 16.sp
    val label = 14.sp
    val caption = 12.sp
}
