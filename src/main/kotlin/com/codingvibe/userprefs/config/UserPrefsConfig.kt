package com.codingvibe.userprefs.config

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.codingvibe.userprefs.dao.CafePrefsDao
import com.codingvibe.userprefs.dao.UserPrefsDao
import com.codingvibe.userprefs.external.twitch.TwitchApi
import com.codingvibe.userprefs.service.CafePrefsService
import com.codingvibe.userprefs.service.TwitchService
import com.codingvibe.userprefs.service.UserPrefsService
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.jetbrains.exposed.sql.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File
import java.net.URLEncoder
import java.nio.file.Files
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.concurrent.TimeUnit


@Configuration
@EnableWebMvc
open class UserPrefsConfig : WebMvcConfigurer {
    @Autowired
    private val env: Environment? = null

    @Bean
    open fun userPrefsDao(database: Database, objectMapper: ObjectMapper): UserPrefsDao {
        return UserPrefsDao(database, objectMapper)
    }

    @Bean
    open fun cafePrefsDao(database: Database, objectMapper: ObjectMapper): CafePrefsDao {
        return CafePrefsDao(database, objectMapper)
    }

    @Bean
    open fun getDatabase(): Database {
        val connectionString = env!!.getRequiredProperty("spring.datasource.url")
        val driverName = env!!.getRequiredProperty("spring.datasource.driver-class-name")
        val username = env!!.getRequiredProperty("spring.datasource.username")
        val password = env!!.getRequiredProperty("spring.datasource.password")
        return Database.connect(url = connectionString, driver = driverName, user = username, password = password)
    }

    @Bean
    open fun prefsService(dao: UserPrefsDao, cafePrefsService: CafePrefsService): UserPrefsService {
        return UserPrefsService(dao, cafePrefsService)
    }

    @Bean
    open fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .registerModule(ParameterNamesModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Bean
    open fun twitchApi(objectMapper: ObjectMapper): TwitchApi {
        val twitchApi = env!!.getRequiredProperty("twitch.baseUrl")
        val retrofit =  Retrofit.Builder().baseUrl(twitchApi)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build()
        return retrofit.create(TwitchApi::class.java)
    }

    @Bean
    open fun twitchService(twitchApi: TwitchApi): TwitchService {
        val twitchClientId = env!!.getRequiredProperty("twitch.clientId")
        val redirectUrl = URLEncoder.encode(env!!.getRequiredProperty("twitch.redirectUrl"), "UTF-8")
        return TwitchService(twitchApi, twitchClientId, redirectUrl)
    }

    @Bean
    open fun twitchStateCache(): Cache<String, Instant> {
        return CacheBuilder.newBuilder()
            .expireAfterWrite(60*5, TimeUnit.SECONDS)
            .maximumSize(100)
            .build()
    }

    @Bean
    open fun rsaAlgorithm(): Algorithm {
        val keyFactory = KeyFactory.getInstance("RSA")

        val publicKeyPath = env!!.getRequiredProperty("rsa.publicKey.path")
        val publicKeyFile = File(publicKeyPath)
        val publicKeyBytes: ByteArray = Files.readAllBytes(publicKeyFile.toPath())
        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(publicKeyBytes)
        val publicKey = keyFactory.generatePublic(publicKeySpec) as RSAPublicKey

        val privateKeyPath = env!!.getRequiredProperty("rsa.privateKey.path")
        val privateKeyFile = File(privateKeyPath)
        val privateKeyBytes: ByteArray = Files.readAllBytes(privateKeyFile.toPath())
        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        val privateKey = keyFactory.generatePrivate(privateKeySpec) as RSAPrivateKey

        return Algorithm.RSA256(publicKey, privateKey)
    }

    @Bean
    open fun jwtVerifier(algorithm: Algorithm): JWTVerifier {
        return JWT.require(algorithm)
            .withIssuer("codingvibe")
            .withSubject("twitch-cafe-prefs")
            .withClaimPresence("twitchToken")
            .build()
    }

    @Bean
    open fun cafePrefsService(dao: CafePrefsDao): CafePrefsService {
        return CafePrefsService(dao)
    }
}