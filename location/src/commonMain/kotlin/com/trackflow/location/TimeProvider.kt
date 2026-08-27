package com.trackflow.location

interface TimeProvider {
    fun currentTimeMillis(): Long
}

private const val MILLIS_PER_DAY = 86_400_000L
private const val MILLIS_PER_HOUR = 3_600_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_SECOND = 1_000L

/**
 * The single wire-format timestamp for the whole SDK: RFC 3339 / ISO-8601, UTC, millisecond
 * precision (e.g. "2026-05-12T07:14:00.000Z"). All platforms and paths that emit a
 * `Location.timestamp` must go through this, not their own ad hoc formatting.
 */
fun TimeProvider.currentTimestamp(): String = formatEpochMillisAsRfc3339(currentTimeMillis())

fun formatEpochMillisAsRfc3339(epochMillis: Long): String {
    val daysSinceEpoch = epochMillis.floorDiv(MILLIS_PER_DAY)
    val millisOfDay = epochMillis.mod(MILLIS_PER_DAY)

    val hours = millisOfDay / MILLIS_PER_HOUR
    val minutes = (millisOfDay / MILLIS_PER_MINUTE) % 60L
    val seconds = (millisOfDay / MILLIS_PER_SECOND) % 60L
    val millisOfSecond = millisOfDay % MILLIS_PER_SECOND

    val (year, month, day) = civilDateFromEpochDay(daysSinceEpoch)

    return buildString {
        append(year.toString().padStart(4, '0'))
        append('-')
        append(month.toString().padStart(2, '0'))
        append('-')
        append(day.toString().padStart(2, '0'))
        append('T')
        append(hours.toString().padStart(2, '0'))
        append(':')
        append(minutes.toString().padStart(2, '0'))
        append(':')
        append(seconds.toString().padStart(2, '0'))
        append('.')
        append(millisOfSecond.toString().padStart(3, '0'))
        append('Z')
    }
}

/**
 * Howard Hinnant's days-since-epoch -> proleptic-Gregorian civil date algorithm
 * (http://howardhinnant.github.io/date_algorithms.html#civil_from_days). Used instead of
 * java.time/Foundation date APIs so the same logic runs identically on every KMP target.
 */
private fun civilDateFromEpochDay(daysSinceEpoch: Long): Triple<Long, Int, Int> {
    val z = daysSinceEpoch + 719_468L
    val era = (if (z >= 0) z else z - 146_096L).floorDiv(146_097L)
    val dayOfEra = z - era * 146_097L
    val yearOfEra = (dayOfEra - dayOfEra / 1460L + dayOfEra / 36_524L - dayOfEra / 146_096L) / 365L
    val year = yearOfEra + era * 400L
    val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime = (5L * dayOfYear + 2L) / 153L
    val day = (dayOfYear - (153L * monthPrime + 2L) / 5L + 1L).toInt()
    val month = (if (monthPrime < 10L) monthPrime + 3L else monthPrime - 9L).toInt()
    return Triple(if (month <= 2) year + 1L else year, month, day)
}
