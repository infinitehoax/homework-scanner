package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // INFO, ERROR, DEBUG
    val tag: String,
    val message: String,
    val details: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

object ApiLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(level: String, tag: String, message: String, details: String? = null) {
        val entry = LogEntry(level = level, tag = tag, message = message, details = details)
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry) // Add to top
        if (currentLogs.size > 200) {
            currentLogs.removeLast()
        }
        _logs.value = currentLogs
    }

    fun info(tag: String, message: String) = log("INFO", tag, message)
    fun error(tag: String, message: String, details: String? = null) = log("ERROR", tag, message, details)
    fun clear() {
        _logs.value = emptyList()
    }
}
