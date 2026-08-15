package com.skyportalthor.app.data

data class QuickTeam(
    val id: String,
    val name: String,
    val playerOneUri: String,
    val playerTwoUri: String? = null
)
