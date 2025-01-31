package com.betclic.models.player

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDAO(
    val playerId: String? = null,
    val pseudo: String,
    val points: Float
)