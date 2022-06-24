package com.codingvibe.userprefs.service

import com.codingvibe.userprefs.external.twitch.TwitchApi
import com.codingvibe.userprefs.external.twitch.TwitchUser

class TwitchService (private val twitchApi: TwitchApi, private val twitchClientId: String) {
    suspend fun getUser(authToken: String): TwitchUser? {
        val response = twitchApi.getUser("Bearer $authToken", twitchClientId)
        if (!response.isSuccessful) {
            return null
        }
        return response.body()?.data?.get(0)
    }
}