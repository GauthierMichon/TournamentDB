package com.betclic.models.player

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val playerId: String? = null,
    val pseudo: String,
    val points: Float
)