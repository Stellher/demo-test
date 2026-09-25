package com.example.logvarremote.ui

data class HomeUiState(
    val installed: Boolean = false,
    val running: Boolean = false,
    val busy: Boolean = false,
    val stage: String = "检查运行时…",
    val progress: Float? = null,
    val anime: String = "凡人修仙传",
    val episode: String = "1",
    val result: String = "",
    val error: String? = null
)
