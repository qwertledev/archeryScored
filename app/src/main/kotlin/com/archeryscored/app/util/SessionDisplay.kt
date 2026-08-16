package com.archeryscored.app.util

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SESSION_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d '·' h:mm a", Locale.getDefault())

/** e.g. "Fri, Aug 16 · 8:52 AM" */
fun formatSessionDateTime(instant: Instant): String =
    instant.toJavaInstant().atZone(ZoneId.systemDefault()).format(SESSION_DATE_TIME_FORMATTER)

/** Sessions are named automatically from when they were created, plus an optional archer-entered label. */
fun sessionDisplayName(createdAt: Instant, label: String?): String {
    val dayTime = formatSessionDateTime(createdAt)
    val trimmedLabel = label?.trim()
    return if (trimmedLabel.isNullOrEmpty()) dayTime else "$dayTime — $trimmedLabel"
}
