package com.mohammad.orion.assistant

enum class AssistantSelection { SELECTED, NOT_SELECTED, UNAVAILABLE }

/** Selection and availability are kept independent to support OEMs without the assistant role. */
fun assistantSelection(available: Boolean, selected: Boolean): AssistantSelection = when {
    !available -> AssistantSelection.UNAVAILABLE
    selected -> AssistantSelection.SELECTED
    else -> AssistantSelection.NOT_SELECTED
}
