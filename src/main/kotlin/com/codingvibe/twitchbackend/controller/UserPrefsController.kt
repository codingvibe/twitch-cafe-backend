package com.codingvibe.twitchbackend.controller

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.codingvibe.twitchbackend.model.Preference
import com.codingvibe.twitchbackend.model.PreferenceName
import com.codingvibe.twitchbackend.service.TwitchApiService
import com.codingvibe.twitchbackend.service.UserPrefsService
import com.google.common.cache.Cache
import liquibase.repackaged.org.apache.commons.text.RandomStringGenerator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.time.Instant


@RestController
@RequestMapping("/v1/api")
class UserPrefsController(
    private val userPrefsService: UserPrefsService,
    private val twitchApiService: TwitchApiService,
    private val twitchStateCache: Cache<String, Instant>,
    private val rsaAlgorithm: Algorithm,
    private val jwtVerifier: JWTVerifier,
    private val allowedOrigins: Array<String>
) {
    companion object {
        val randomStringBuilder: RandomStringGenerator = RandomStringGenerator.Builder()
            .withinRange('0'.code, 'z'.code)
            .filteredBy(Character::isLetterOrDigit)
            .build()
    }
    @GetMapping("/login")
    fun redirectToLogin(): ResponseEntity<Void> {
        val state = randomStringBuilder.generate(20)
        twitchStateCache.put(state, Instant.now())
        val loginUrl = twitchApiService.getLoginUrl(state)
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(loginUrl)).build()
    }

    @GetMapping("/authenticate")
    @CrossOrigin("http://localhost:3000", "https://prefs.codingvibe.dev")
    fun authenticate(@RequestHeader("X-Twitch-State") twitchState: String?,
                     @RequestHeader("X-Twitch-Token") twitchToken: String?): StateValidationResponse {
        if (twitchToken.isNullOrEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Twitch auth token required in X-Twitch-Token header")
        }
        if (twitchState.isNullOrEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Twitch state required in X-Twitch-State header")
        }
        val storedState = twitchStateCache.getIfPresent(twitchState)
        if (storedState != null) {
            twitchStateCache.invalidate(twitchState)
            val twitchUser = twitchApiService.getUser(twitchToken)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Twitch Token. Try logging in again.")

            try {
                val token = JWT.create()
                    .withIssuer("codingvibe")
                    .withSubject("twitch-cafe-prefs")
                    .withClaim("twitchToken", twitchToken)
                    .withExpiresAt(Instant.now().plusSeconds(30*60))
                    .sign(rsaAlgorithm);
                userPrefsService.setAccessToken(twitchUser.login, token)
                return StateValidationResponse(
                    accessToken = token,
                    createdAt = storedState
                )
            } catch (exception: JWTVerificationException) {
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Issue validating identity. Try again later.")
            }
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unrecognized login attempt. Try again.")
    }

    @GetMapping("/prefs")
    @CrossOrigin("http://localhost:3000", "https://prefs.codingvibe.dev")
    fun getUserPrefs(@RequestParam twitchId: String?,
                             @RequestHeader("Authorization") authString: String?): PreferencesResponse {
        try {
            val twitchUsername = getTwitchId(twitchId, authString)
            return toPreferencesResponse(userPrefsService.getPrefs(twitchUsername)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Preferences for Twitch user with ID $twitchUsername not found"))
        } catch(e: JWTVerificationException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Malformed or expired auth token")
        } catch(e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Twitch ID or Authorization header required for preferences.")
        }
    }

    @PutMapping("/prefs")
    @CrossOrigin("http://localhost:3000", "https://prefs.codingvibe.dev")
    fun updatePrefs(@RequestHeader("Authorization") authString: String?,
                    @RequestBody preferences: PreferencesResponse): PreferencesResponse {
        if (authString.isNullOrEmpty()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Auth required")
        }
        if (!authString.startsWith("Bearer ")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed auth");
        }
        try {
            val twitchUser = getTwitchIdFromAuthString(authString)
            return toPreferencesResponse(
                userPrefsService.updatePrefs(
                    twitchUser,
                    fromPreferencesResponse(preferences)
                )
            )
        } catch(e: JWTVerificationException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Malformed or expired auth token")
        } catch(e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message);
        }
    }

    private fun toPreferencesResponse(prefs: List<Preference>): PreferencesResponse {
        val respPrefs = prefs.map { PreferenceResponse(name = it.name.name, value = it.value) }
        return PreferencesResponse(prefs = respPrefs)
    }

    private fun fromPreferencesResponse(prefs: PreferencesResponse): List<Preference> {
        return prefs.prefs.map {
            try {
                Preference(name = PreferenceName.valueOf(it.name), value = it.value)
            } catch (e: IllegalArgumentException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unrecognized preference name ${it.name}.")
            }
        }
    }

    private fun getTwitchId(twitchId: String?, authString: String?): String {
        if (!twitchId.isNullOrEmpty()) {
            return twitchId
        }
        if (!authString.isNullOrEmpty()) {
            return getTwitchIdFromAuthString(authString)
        }
        throw IllegalArgumentException("Couldn't find Twitch ID")
    }

    private fun getTwitchIdFromAuthString(authString: String): String {
        if (!authString.startsWith("Bearer ")) {
            throw JWTVerificationException("Auth string malformed")
        }
        val token = authString.replace("Bearer ", "")
        val jwt = jwtVerifier.verify(token)
        val twitchToken = jwt.getClaim("twitchToken")
        return twitchApiService.getUser(twitchToken.asString())?.login ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Reauthenticate with Twitch.")
    }
}

data class StateValidationResponse (
    val accessToken: String? = null,
    val createdAt: Instant? = null
)
data class PreferencesResponse (
    val prefs: List<PreferenceResponse>
)

data class PreferenceResponse (
    val name: String,
    val value: String,
)