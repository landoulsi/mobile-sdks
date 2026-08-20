package com.landoulsi.payment.shared.validation

import java.util.Calendar

actual fun currentYearTwoDigit(): Int {
    return Calendar.getInstance().get(Calendar.YEAR) % 100
}

actual fun currentMonth(): Int {
    return Calendar.getInstance().get(Calendar.MONTH) + 1
}