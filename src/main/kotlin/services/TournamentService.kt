package com.betclic.services

import com.betclic.converters.*
import com.betclic.helpers.formatDateTime
import com.betclic.models.player.PlayerDTO
import com.betclic.models.tournament.*
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.*
import java.time.LocalDateTime

class TournamentService(database: MongoDatabase) {
    private val tournamentCollection = database.getCollection<TournamentDAO>("Tournament")

    fun addTournament(tournamentDTO: TournamentDTO): String {
        val tournament = tournamentDTO.toDomain()
        val tournamentDAO = tournament.toDAO()
        tournamentCollection.insertOne(tournamentDAO)
        return tournamentDAO._id ?: throw IllegalStateException("Failed to generate tournament ID")
    }

    fun getAllTournaments(): List<TournamentDTO> {
        val tournaments = tournamentCollection.find().toList()
        return tournaments.map { tournamentDAO ->
            val tournament = tournamentDAO.toDomain()
            val playersWithRank = tournament.players
                .sortedByDescending { it.points }
                .mapIndexed { index, player -> player.toDTO(rank = index + 1) }
            tournament.toDTO(playersWithRank)
        }
    }

    fun getTournamentById(id: String): TournamentDTO? {
        val tournamentDAO = tournamentCollection.findOneById(id) ?: return null

        val tournament = tournamentDAO.toDomain()
        val playersWithRank = tournament.players
            .sortedByDescending { it.points }
            .mapIndexed { index, player -> player.toDTO(rank = index + 1) }

        return tournament.toDTO(playersWithRank)
    }

    fun addPlayerToTournament(idTournament: String, player: PlayerDTO): Boolean {
        val tournamentDAO = tournamentCollection.findOneById(idTournament) ?: throw IllegalArgumentException("Tournament with ID $idTournament not found")

        val playerExists = tournamentDAO.players.any { it.playerId == player.playerId }
        if (playerExists) {
            throw IllegalArgumentException("Player ${player.playerId} is already in the tournament")
        }

        tournamentCollection.updateOneById(
            idTournament,
            push(TournamentDAO::players, player.toDomain().toDAO())
        )
        return true
    }

    fun updatePlayerPoints(idTournament: String, idPlayer: String, newPoints: Float): Boolean? {
        val tournamentDAO = tournamentCollection.findOneById(idTournament) ?: return null
        tournamentDAO.players.find { it.playerId == idPlayer } ?: return null

        val updatedPlayers = tournamentDAO.players.map { player ->
            if (player.playerId == idPlayer) {
                player.copy(points = newPoints)
            } else {
                player
            }
        }

        tournamentCollection.updateOneById(
            idTournament,
            setValue(TournamentDAO::players, updatedPlayers)
        )

        return true

        /*val updateResult = tournamentCollection.updateOne(
            and(
                TournamentDAO::_id eq idTournament,
                TournamentDAO::players / PlayerDAO::playerId eq idPlayer
            ),
            setValue(TournamentDAO::players, newPoints.toInt())
        )*/
    }

    fun getPlayerFromTournamentById(idTournament: String, idPlayer: String): PlayerDTO? {
        val tournamentDAO = tournamentCollection.findOneById(idTournament) ?: return null
        val tournament = tournamentDAO.toDomain()

        val rankedPlayers = tournament.players
            .sortedByDescending { it.points }
            .mapIndexed { index, player -> player.toDTO(rank = index + 1) }

        return rankedPlayers.find { it.playerId == idPlayer }
    }


    fun getAllPlayersFromTournament(idTournament: String): List<PlayerDTO> {
        val tournamentDAO = tournamentCollection.findOneById(idTournament) ?: return emptyList()
        val tournament = tournamentDAO.toDomain()
        return tournament.players
            .sortedByDescending { it.points }
            .mapIndexed { index, player -> player.toDTO(rank = index + 1) }
    }

    fun closeTournament(idTournament: String): Boolean? {
        val tournamentDAO = tournamentCollection.findOneById(idTournament) ?: return null

        if (!tournamentDAO.isOpen) {
            return false
        }

        tournamentCollection.updateOneById(
            idTournament,
            combine(
                setValue(TournamentDAO::isOpen, false),
                setValue(TournamentDAO::players, emptyList()),
                setValue(TournamentDAO::endDate, formatDateTime(LocalDateTime.now()))
            )
        )

        return true
    }

    fun stealPoints(idTournament: String, thiefId: String, targetId: String, pointsToSteal: Float): Boolean? {
        val tournamentDAO = tournamentCollection.findOneById(idTournament) ?: return null

        val players = tournamentDAO.players
        players.find { it.playerId == thiefId } ?: return null
        val target = players.find { it.playerId == targetId } ?: return null

        if (target.points < pointsToSteal) {
            throw IllegalArgumentException("Target player does not have enough points")
        }

        val updatedPlayers = players.map {
            when (it.playerId) {
                thiefId -> it.copy(points = it.points + pointsToSteal)
                targetId -> it.copy(points = it.points - pointsToSteal)
                else -> it
            }
        }

        tournamentCollection.updateOneById(
            idTournament,
            setValue(TournamentDAO::players, updatedPlayers)
        )

        return true
    }

}