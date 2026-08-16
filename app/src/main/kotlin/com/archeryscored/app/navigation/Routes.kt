package com.archeryscored.app.navigation

object Routes {
    const val HOME = "home"
    const val NEW_SESSION = "session/new"
    const val SESSION = "session/{sessionId}"
    const val CAPTURE = "session/{sessionId}/capture"
    const val REVIEW = "session/{sessionId}/end/{endId}/review"

    fun session(sessionId: Long) = "session/$sessionId"
    fun capture(sessionId: Long) = "session/$sessionId/capture"
    fun review(sessionId: Long, endId: Long) = "session/$sessionId/end/$endId/review"
}
