package com.betclic.converters

import com.betclic.helpers.formatDateTime
import com.betclic.models.player.PlayerDTO
import com.betclic.models.tournament.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

fun Tournament.toDTO(playersWithRank: List<PlayerDTO>?): TournamentDTO {
    return TournamentDTO(
        _id = this._id,
        name = this.name,
        isOpen = this.isOpen,
        startDate = this.startDate,
        endDate = this.endDate,
        players = playersWithRank ?: this.players.map { it.toDTO() }
    )
}

fun TournamentDTO.toDomain(): Tournament {
    return Tournament(
        _id = this._id,
        name = this.name,
        isOpen = this.isOpen ?: true,
        startDate = this.startDate ?: formatDateTime(LocalDateTime.now()),
        endDate = this.endDate,
        players = this.players?.map { it.toDomain() } ?: emptyList()
    )
}

fun Tournament.toDAO(): TournamentDAO {
    return TournamentDAO(
        _id = this._id ?: UUID.randomUUID().toString(),
        name = this.name,
        isOpen = this.isOpen,
        startDate = this.startDate,
        endDate = this.endDate,
        players = this.players.map { it.toDAO() }
    )
}

fun TournamentDAO.toDomain(): Tournament {
    return Tournament(
        _id = this._id,
        name = this.name,
        isOpen = this.isOpen,
        startDate = this.startDate,
        endDate = this.endDate,
        players = this.players.map { it.toDomain() }
    )
}
