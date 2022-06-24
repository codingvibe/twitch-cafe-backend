package com.codingvibe.userprefs.external.twitch

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface TwitchApi {
    @GET("users")
    suspend fun getUser(@Header("Authorization") authHeader: String, @Header("Client-Id") clientId: String) : Response<TwitchUserResponse?>
}
