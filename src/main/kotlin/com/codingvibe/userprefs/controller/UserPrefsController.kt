package com.codingvibe.userprefs.controller

import com.codingvibe.userprefs.model.Preference
import com.codingvibe.userprefs.model.PreferenceName
import com.codingvibe.userprefs.service.TwitchService
import com.codingvibe.userprefs.service.UserPrefsService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/v1/api")
class UserPrefsController(
    private val userPrefsService: UserPrefsService,
    private val twitchService: TwitchService
) {
    @GetMapping("/prefs")
    fun getUserPrefs(@RequestParam twitchId: String): PreferencesResponse {
        if (twitchId.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Twitch ID required for preferences. Query with param twitchId")
        }
        return toPreferencesResponse(userPrefsService.getPrefs(twitchId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Preferences for Twitch user with ID $twitchId not found"))
    }

    @PutMapping("/prefs/{twitchId}")
    suspend fun updatePrefs(@PathVariable("twitchId") twitchId: String?, @RequestHeader("X-Twitch-Token") twitchToken: String?,
                            @RequestBody preferences: PreferencesResponse): PreferencesResponse {
        if (twitchId.isNullOrEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Twitch ID is required")
        }
        if (twitchToken.isNullOrEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Twitch auth token required in X-Twitch-Token header")
        }
        validateTwitchId(twitchId, twitchToken)
        return toPreferencesResponse(userPrefsService.updatePrefs(twitchId, fromPreferencesResponse(preferences)))
    }

    private suspend fun validateTwitchId(requestUser: String, requestToken: String) {
        val actualTwitchName = twitchService.getUser(requestToken)
        if (requestUser != actualTwitchName?.displayName && requestUser != actualTwitchName?.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Token user does not match passed user")
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
}

data class PreferencesResponse (
    val prefs: List<PreferenceResponse>
)

data class PreferenceResponse (
    val name: String,
    val value: String,
)