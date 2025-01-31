package com.betclic.models.tournament

import com.betclic.models.player.PlayerDAO
import kotlinx.serialization.Serializable

@Serializable
data class TournamentDAO(
    val _id: String? = null,
    val name: String,
    val isOpen: Boolean,
    val startDate: String,
    val endDate: String?,
    val players: List<PlayerDAO>
)
