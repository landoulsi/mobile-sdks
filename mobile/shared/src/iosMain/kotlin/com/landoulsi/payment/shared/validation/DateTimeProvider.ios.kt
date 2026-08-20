package com.landoulsi.payment.shared.validation

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

actual fun currentYearTwoDigit(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = NSDate())
    return (components.year % 100).toInt()
}

actual fun currentMonth(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = NSDate())
    return components.month.toInt()
}