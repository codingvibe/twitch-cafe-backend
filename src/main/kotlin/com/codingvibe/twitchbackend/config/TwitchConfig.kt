package com.codingvibe.twitchbackend.config

import com.github.philippheuer.credentialmanager.CredentialManager
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.auth.providers.TwitchIdentityProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.crypto.spec.SecretKeySpec

@Configuration
open class TwitchConfig {
    @Bean
    open fun twitchEventSubApiSecret(env: Environment): SecretKeySpec {
        return SecretKeySpec(env!!.getRequiredProperty("twitch.eventSubSecret").toByteArray(), "HmacSHA256")
    }

    @Bean
    open fun twitchCredentialProvider(env: Environment): CredentialManager {
        val clientId = env!!.getRequiredProperty("twitch.clientId")
        val clientSecret = env!!.getRequiredProperty("twitch.clientSecret")
        val redirectUrl = env!!.getRequiredProperty("twitch.redirectUrl")
        return CredentialManagerBuilder.builder().build().also {
            it.registerIdentityProvider(TwitchIdentityProvider(clientId, clientSecret, redirectUrl))
        }
    }

    @Bean
    open fun twitchClient(env:Environment, credentialManager: CredentialManager): TwitchClient {
        return TwitchClientBuilder.builder()
            .withEnableChat(true)
            .withEnableHelix(true)
            .withCredentialManager(credentialManager)
        .build()
    }
}