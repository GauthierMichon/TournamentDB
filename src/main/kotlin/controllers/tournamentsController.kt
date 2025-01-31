package com.betclic.controllers

import com.betclic.models.player.PlayerDTO
import com.betclic.models.tournament.TournamentDTO
import com.betclic.services.TournamentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.tournamentsController(tournamentService: TournamentService) {
    routing {
        route("/tournaments") {
            get {
                try {
                    val isOpen = call.request.queryParameters["isOpen"]?.toBoolean()
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                    val page = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                    val tournaments = tournamentService.getAllTournaments()
                        .filter { isOpen == null || it.isOpen == isOpen }
                        .drop(page*limit)
                        .take(limit)

                    val jsonResponse = Json.encodeToString(tournaments)
                    call.respondText(jsonResponse, ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error fetching tournaments: $e")
                }
            }

            get("/{id}") {
                try {
                    val id = call.parameters["id"]
                    if (id.isNullOrBlank()) {
                        call.respondText("Missing or invalid ID", status = HttpStatusCode.BadRequest)
                        return@get
                    }

                    val tournament = tournamentService.getTournamentById(id)
                    if (tournament == null) {
                        call.respond(HttpStatusCode.NotFound, "Tournament not found")
                    } else {
                        val jsonResponse = Json.encodeToString(tournament)
                        call.respondText(jsonResponse, ContentType.Application.Json)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error fetching tournament by ID: $e")
                }
            }

            get("/{id}/players") {
                val idTournament = call.parameters["id"]
                if (idTournament.isNullOrBlank()) {
                    call.respondText("Missing or invalid ID", status = HttpStatusCode.BadRequest)
                    return@get
                }

                try {
                    val players = tournamentService.getAllPlayersFromTournament(idTournament)
                    val jsonResponse = Json.encodeToString(players)
                    call.respondText(jsonResponse, ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotFound, "Error fetching players: $e")
                }
            }

            get("/{id}/players/{playerId}") {
                val idTournament = call.parameters["id"]
                val idPlayer = call.parameters["playerId"]

                if (idTournament.isNullOrBlank() || idPlayer.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing or invalid tournament or player ID")
                    return@get
                }

                try {
                    val player = tournamentService.getPlayerFromTournamentById(idTournament, idPlayer)
                    if (player == null) {
                        call.respond(HttpStatusCode.NotFound, "Tournament not found")
                    } else {
                        val jsonResponse = Json.encodeToString(player)
                        call.respondText(jsonResponse, ContentType.Application.Json)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotFound, "Error fetching player by ID: $e")
                }
            }


            post {
                val tournamentDTO = try {
                    Json.decodeFromString<TournamentDTO>(call.receiveText())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid tournament data")
                    return@post
                }

                try {
                    val tournamentId = tournamentService.addTournament(tournamentDTO)
                    val tournamentApiLocation = "${call.request.origin.scheme}://${call.request.host()}:${call.request.port()}/tournaments/$tournamentId"
                    call.respond(HttpStatusCode.Created, tournamentApiLocation)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error adding tournament: $e")
                }
            }

            post("/{id}/players") {
                val idTournament = call.parameters["id"]

                if (idTournament.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing or invalid tournament ID")
                    return@post
                }

                val playerDTO = try {
                    Json.decodeFromString<PlayerDTO>(call.receiveText())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid player data: ${e.message}")
                    return@post
                }

                try {
                    tournamentService.addPlayerToTournament(idTournament, playerDTO)
                    call.respond(HttpStatusCode.OK, "Player added successfully to the tournament")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error adding player to the tournament: ${e.message}")
                }
            }

            post("/{id}/close") {
                val idTournament = call.parameters["id"]

                if (idTournament.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing or invalid tournament ID")
                    return@post
                }

                try {
                    val isClose = tournamentService.closeTournament(idTournament)
                    if (isClose == null) {
                        call.respond(HttpStatusCode.NotFound, "Tournament not found")
                    } else if (isClose) {
                        call.respond(HttpStatusCode.OK, "Tournament closed successfully")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Tournament $idTournament is already closed")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error closing tournament: ${e.message}")
                }
            }

            put("/{id}/players/{playerId}/points") {
                val idTournament = call.parameters["id"]
                val idPlayer = call.parameters["playerId"]

                if (idTournament.isNullOrBlank() || idPlayer.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing or invalid tournament or player ID")
                    return@put
                }

                val newPoints = try {
                    call.receiveText().toFloat()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid points value: $e")
                    return@put
                }

                try {
                    val isUpdate = tournamentService.updatePlayerPoints(idTournament, idPlayer, newPoints)
                    if (isUpdate == null) {
                        call.respond(HttpStatusCode.NotFound, "Tournament with ID $idTournament or Player with ID $idPlayer not found")
                    } else {
                        call.respond(HttpStatusCode.OK, "Player points updated successfully")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error update player points: $e")
                }
            }

            put("/{id}/players/{playerId}/steal/{targetId}") {
                val idTournament = call.parameters["id"]
                val thiefId = call.parameters["playerId"]
                val targetId = call.parameters["targetId"]

                if (idTournament.isNullOrBlank() || thiefId.isNullOrBlank() || targetId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing or invalid tournament or player ID")
                    return@put
                }

                val pointsToSteal = try {
                    call.receiveText().toFloat()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid points value: ${e.message}")
                    return@put
                }

                try {
                    val isUpdate = tournamentService.stealPoints(idTournament, thiefId, targetId, pointsToSteal)
                    if (isUpdate == null) {
                        call.respond(HttpStatusCode.NotFound, "Tournament with ID $idTournament or Player with ID $thiefId or Player with ID $targetId not found")
                    } else {
                        call.respond(HttpStatusCode.OK, "Successfully stole player points")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error stealing points: ${e.message}")
                }
            }

        }
    }
}