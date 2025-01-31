package com.betclic.converters

import com.betclic.models.player.Player
import com.betclic.models.player.PlayerDAO
import com.betclic.models.player.PlayerDTO
import java.util.*

fun Player.toDTO(rank: Int? = null): PlayerDTO {
    return PlayerDTO(
        playerId = this.playerId,
        pseudo = this.pseudo,
        points = this.points,
        rank = rank
    )
}

fun PlayerDTO.toDomain(): Player {
    return Player(
        playerId = this.playerId,
        pseudo = this.pseudo,
        points = this.points ?: 50f
    )
}

fun PlayerDAO.toDomain(): Player {
    return Player(
        playerId = this.playerId,
        pseudo = this.pseudo,
        points = this.points
    )
}

fun Player.toDAO(): PlayerDAO {
    return PlayerDAO(
        playerId = this.playerId ?: UUID.randomUUID().toString(),
        pseudo = this.pseudo,
        points = this.points
    )
}