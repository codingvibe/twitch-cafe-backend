package com.codingvibe.userprefs.config

import com.codingvibe.userprefs.dao.UserPrefsDao
import com.codingvibe.userprefs.external.twitch.TwitchApi
import com.codingvibe.userprefs.service.TwitchService
import com.codingvibe.userprefs.service.UserPrefsService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory


@Configuration
open class UserPrefsConfig {
    @Autowired
    private val env: Environment? = null

    @Bean
    open fun prefsDao(objectMapper: ObjectMapper): UserPrefsDao {
        val connectionString = env!!.getRequiredProperty("spring.datasource.url")
        val driverName = env!!.getRequiredProperty("spring.datasource.driver-class-name")
        val username = env!!.getRequiredProperty("spring.datasource.username")
        val password = env!!.getRequiredProperty("spring.datasource.password")
        return UserPrefsDao(objectMapper, connectionString, driverName, username, password)
    }

    @Bean
    open fun prefsService(dao: UserPrefsDao): UserPrefsService {
        return UserPrefsService(dao)
    }

    @Bean
    open fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
    }

    @Bean
    open fun twitchApi(): TwitchApi {
        val twitchApi = env!!.getRequiredProperty("twitch.baseUrl")
        val retrofit =  Retrofit.Builder().baseUrl(twitchApi)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
        return retrofit.create(TwitchApi::class.java)
    }

    @Bean
    open fun twitchService(twitchApi: TwitchApi): TwitchService {
        val twitchClientId = env!!.getRequiredProperty("twitch.clientId")
        return TwitchService(twitchApi, twitchClientId)
    }
}