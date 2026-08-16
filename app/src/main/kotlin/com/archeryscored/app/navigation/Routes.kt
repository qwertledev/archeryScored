package com.archeryscored.app.navigation

object Routes {
    const val HOME = "home"
    const val NEW_SESSION = "session/new"
    const val SESSION = "session/{sessionId}"
    const val ADD_END = "session/{sessionId}/add-end"
    const val CAPTURE = "session/{sessionId}/capture"
    const val MANUAL_ENTRY = "session/{sessionId}/manual-entry"
    const val DIAGRAM_ENTRY = "session/{sessionId}/diagram-entry"
    const val REVIEW = "session/{sessionId}/end/{endId}/review"

    fun session(sessionId: Long) = "session/$sessionId"
    fun addEnd(sessionId: Long) = "session/$sessionId/add-end"
    fun capture(sessionId: Long) = "session/$sessionId/capture"
    fun manualEntry(sessionId: Long) = "session/$sessionId/manual-entry"
    fun diagramEntry(sessionId: Long) = "session/$sessionId/diagram-entry"
    fun review(sessionId: Long, endId: Long) = "session/$sessionId/end/$endId/review"
}
