package com.mohammad.orion.domain.settings

/** Names are short, single-line display labels; null denotes the localized default. */
fun normalizeAssistantName(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.length > 40) return null
    if (trimmed.any { Character.isISOControl(it) || (Character.getType(it) == Character.FORMAT.toInt() && it != '\u200c' && it != '\u200d') || Character.isSurrogate(it) }) return null
    return trimmed
}

sealed interface AssistantNameState {
    data object Loading : AssistantNameState
    data object Unavailable : AssistantNameState
    data class Ready(val name: String?) : AssistantNameState
}
