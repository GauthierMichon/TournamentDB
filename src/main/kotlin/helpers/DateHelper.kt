package com.betclic.helpers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    return dateTime.format(formatter)
}