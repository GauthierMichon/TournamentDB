package com.betclic.routing

import com.betclic.controllers.tournamentsController
import com.betclic.models.player.PlayerDTO
import com.betclic.models.tournament.TournamentDTO
import com.betclic.models.tournament.TournamentDAO
import com.betclic.services.TournamentService
import com.mongodb.assertions.Assertions.assertFalse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import org.litote.kmongo.KMongo
import org.litote.kmongo.deleteMany
import org.litote.kmongo.getCollection
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TournamentRoutesTest {

    private val mongoContainer = MongoDBContainer(DockerImageName.parse("mongo:5.0.31"))
    private lateinit var tournamentService: TournamentService

    @BeforeAll
    fun startMongo() {
        mongoContainer.start()
    }

    @AfterAll
    fun stopMongo() {
        mongoContainer.stop()
    }

    @BeforeEach
    fun setup() {
        val mongoClient = KMongo.createClient(mongoContainer.replicaSetUrl)
        val database = mongoClient.getDatabase("testdb")
        database.getCollection<TournamentDAO>("Tournament").deleteMany("{}")
        tournamentService = TournamentService(database)
    }

    @Test
    fun `GET all tournaments should return empty list initially`() = testApplication {
        application { tournamentsController(tournamentService) }

        client.get("/tournaments").apply {
            assertEquals(HttpStatusCode.OK, status)
            val responseTournaments = Json.decodeFromString<List<TournamentDTO>>(bodyAsText())
            assertTrue(responseTournaments.isEmpty())
        }
    }

    @Test
    fun `POST add tournament should be save in database`() = testApplication {
        val tournamentDTO = TournamentDTO(name = "Test Tournament", isOpen = true)

        val tournament2DTO = TournamentDTO(name = "Test Tournament2", isOpen = true)

        application { tournamentsController(tournamentService) }

        client.post("/tournaments") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(tournamentDTO))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
        }

        client.post("/tournaments") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(tournament2DTO))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
        }

        val tournaments = tournamentService.getAllTournaments()
        assertEquals(2, tournaments.size)
        assertEquals("Test Tournament", tournaments[0].name)
    }

    @Test
    fun `GET tournament by ID should return 404 if tournament not found`() = testApplication {
        application { tournamentsController(tournamentService) }

        client.get("/tournaments/999").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `POST add player to tournament should be save in database`() = testApplication {
        val tournamentId = tournamentService.addTournament(
            TournamentDTO(name = "Test Tournament", isOpen = true)
            )

        val playerDTO = PlayerDTO(pseudo = "Gauthier Michon")

        application { tournamentsController(tournamentService) }

        client.post("/tournaments/$tournamentId/players") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(playerDTO))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        val players = tournamentService.getAllPlayersFromTournament(tournamentId)
        assertEquals(1, players.size)
        assertEquals("Gauthier Michon", players[0].pseudo)
    }

    @Test
    fun `PUT update player points should return 404 if player not found`() = testApplication {
        val tournamentId = tournamentService.addTournament(
            TournamentDTO(name = "Test Tournament", isOpen = true)
        )

        application { tournamentsController(tournamentService) }

        client.put("/tournaments/$tournamentId/players/invalid-player/points") {
            contentType(ContentType.Application.Json)
            setBody("50")
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `PUT steal points should correctly update both player scores`() = testApplication {
        val tournamentId = tournamentService.addTournament(
            TournamentDTO(name = "Test Tournament", isOpen = true)
        )

        val thief = PlayerDTO(playerId = "ThiefId", pseudo = "Thief")
        val target = PlayerDTO(playerId = "TargetId", pseudo = "Target")

        tournamentService.addPlayerToTournament(tournamentId, thief)
        tournamentService.addPlayerToTournament(tournamentId, target)

        application { tournamentsController(tournamentService) }

        client.put("/tournaments/$tournamentId/players/ThiefId/steal/TargetId") {
            contentType(ContentType.Application.Json)
            setBody("10")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        val updatedPlayers = tournamentService.getAllPlayersFromTournament(tournamentId)
        val updatedThief = updatedPlayers.find { it.playerId == "ThiefId" }
        val updatedTarget = updatedPlayers.find { it.playerId == "TargetId" }

        assertEquals(60f, updatedThief?.points)
        assertEquals(40f, updatedTarget?.points)
    }

    @Test
    fun `POST close tournament should set isOpen to false and clear players`() = testApplication {
        val tournamentId = tournamentService.addTournament(
            TournamentDTO(name = "Test Tournament", isOpen = true)
        )

        val playerDTO = PlayerDTO(pseudo = "Gauthier Michon")
        tournamentService.addPlayerToTournament(tournamentId, playerDTO)

        application { tournamentsController(tournamentService) }

        client.post("/tournaments/$tournamentId/close").apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        val closedTournament = tournamentService.getTournamentById(tournamentId)
        assertFalse(closedTournament?.isOpen!!)
        assertTrue(closedTournament.players!!.isEmpty())
    }
}
