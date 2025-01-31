package com.betclic.models.player

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDTO(
    val playerId: String? = null,
    val pseudo: String,
    val points: Float? = null,
    var rank: Int? = null,
)