package com.betclic

import com.betclic.controllers.tournamentsController
import com.betclic.services.TournamentService
import io.ktor.server.application.*
import org.litote.kmongo.*
import com.mongodb.client.MongoDatabase
import io.ktor.server.plugins.swagger.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSwagger()
    val database = configureMongoDB()
    val tournamentService = TournamentService(database)
    tournamentsController(tournamentService)
}

fun configureMongoDB(): MongoDatabase {
    val connectionString = "mongodb://localhost:27017"
    val client = KMongo.createClient(connectionString)
    return client.getDatabase("TournamentDB")
}

fun Application.configureSwagger() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}