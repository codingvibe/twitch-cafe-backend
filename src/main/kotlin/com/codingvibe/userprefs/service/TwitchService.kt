package com.codingvibe.userprefs.service

import com.codingvibe.userprefs.external.twitch.TwitchApi
import com.codingvibe.userprefs.external.twitch.TwitchUser

class TwitchService (private val twitchApi: TwitchApi,
                     private val twitchClientId: String,
                     private val redirectUrl: String) {
    suspend fun getUser(authToken: String): TwitchUser? {
        val response = twitchApi.getUser("Bearer $authToken", twitchClientId)
        if (!response.isSuccessful) {
            return null
        }
        return response.body()?.data?.get(0)
    }

    fun getLoginUrl(state: String):String {
        return "https://id.twitch.tv/oauth2/authorize?client_id=${twitchClientId}&redirect_uri=${redirectUrl}&state=${state}&response_type=token&scope=user%3Aread%3Aemail"
    }
}