package com.myhealth.app.data.social

import com.myhealth.app.data.secure.SecureTokenStore
import com.myhealth.core.models.Badge
import com.myhealth.core.models.Challenge
import com.myhealth.core.models.Friend
import com.myhealth.core.models.Streak
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class FriendsResponse(val friends: List<Friend>)
@Serializable
data class FriendResponse(val friend: Friend)
@Serializable
data class ChallengesResponse(val challenges: List<Challenge>)
@Serializable
data class ChallengeResponse(val challenge: Challenge)
@Serializable
data class LeaderboardEntry(
    val id: Int = 0,
    val challenge_id: Int = 0,
    val user_id: Int = 0,
    val score: Double = 0.0,
    val name: String = "",
    val email: String = "",
)
@Serializable
data class LeaderboardResponse(val entries: List<LeaderboardEntry>)

@Singleton
class SocialRepository @Inject constructor(
    private val tokenStore: SecureTokenStore,
    private val apiBaseUrl: com.myhealth.app.network.ApiBaseUrl,
) {
    private val baseUrl: String get() = "${apiBaseUrl.value}/api/social"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun authHeader() = "Bearer ${tokenStore.jwt}"

    suspend fun getFriends(): List<Friend> {
        val resp: FriendsResponse = client.get("$baseUrl/friends") {
            header("Authorization", authHeader())
        }.body()
        return resp.friends
    }

    suspend fun addFriend(name: String, handle: String): Friend {
        val resp: FriendResponse = client.post("$baseUrl/friend") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(mapOf("name" to name, "handle" to handle))
        }.body()
        return resp.friend
    }

    suspend fun removeFriend(id: Int) {
        client.delete("$baseUrl/friend/$id") {
            header("Authorization", authHeader())
        }
    }

    suspend fun getChallenges(): List<Challenge> {
        val resp: ChallengesResponse = client.get("$baseUrl/challenges") {
            header("Authorization", authHeader())
        }.body()
        return resp.challenges
    }

    suspend fun createChallenge(title: String, kind: String, days: Int, target: Double): Challenge {
        val now = java.time.Instant.now()
        val end = now.plus(java.time.Duration.ofDays(days.toLong()))
        val resp: ChallengeResponse = client.post("$baseUrl/challenge") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "title" to title,
                "kind" to kind,
                "starts_at" to now.toString(),
                "ends_at" to end.toString(),
                "target" to target.toString(),
            ))
        }.body()
        return resp.challenge
    }

    suspend fun joinChallenge(id: Int) {
        client.post("$baseUrl/challenge/$id/join") {
            header("Authorization", authHeader())
        }
    }

    suspend fun getLeaderboard(challengeId: Int): List<LeaderboardEntry> {
        val resp: LeaderboardResponse = client.get("$baseUrl/leaderboard?challenge=$challengeId") {
            header("Authorization", authHeader())
        }.body()
        return resp.entries
    }

    suspend fun submitScore(challengeId: Int, score: Double) {
        client.post("$baseUrl/leaderboard/score") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(mapOf("challenge_id" to challengeId.toString(), "score" to score.toString()))
        }
    }
}
