# Tournament Database - Ktor API

API REST en **Kotlin** avec **Ktor** et **MongoDB**  

## Prérequis

Avant de lancer le projet, assure-toi d'avoir installé :

- **JDK 17 ou supérieur** ([Télécharger ici](https://adoptium.net/))
- **Gradle 8.x** ([Guide d'installation](https://gradle.org/install/))
- **Docker** (pour exécuter MongoDB en conteneur)

### Vérifier les versions installées
```bash
java -version
gradle -v
docker -v
```

## Installation

### Cloner le projet
```bash
git clone https://github.com/GauthierMichon/TournamentDB.git
cd TournamentDB
```

## Lancer l'API

```bash
./start_api.sh
```

## Accéder à la documentation Swagger

Une interface Swagger est disponible pour tester l'API après l'avoir lancé via un navigateur

```bash
http://127.0.0.1:8080/swagger
```

## Exécuter les Tests

```bash
./start_tests.sh
```