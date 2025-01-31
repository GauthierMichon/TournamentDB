package com.betclic.models.tournament

import com.betclic.models.player.Player
import kotlinx.serialization.Serializable

@Serializable
data class Tournament(
    val _id: String? = null,
    val name: String,
    val isOpen: Boolean,
    val startDate: String,
    val endDate: String? = null,
    val players: List<Player>
)
