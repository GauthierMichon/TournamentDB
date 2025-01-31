package com.betclic.models.tournament

import com.betclic.models.player.PlayerDTO
import kotlinx.serialization.Serializable

@Serializable
data class TournamentDTO(
    val _id: String? = null,
    val name: String,
    val isOpen: Boolean? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val players: List<PlayerDTO>? = emptyList()
)
