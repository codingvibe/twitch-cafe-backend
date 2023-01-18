package com.codingvibe.userprefs.controller

import com.codingvibe.userprefs.external.twitch.getTwitchEventSubPayload
import com.codingvibe.userprefs.external.twitch.notifications.CustomChannelPointRewardRedeemed
import com.codingvibe.userprefs.external.twitch.notifications.StreamOnline
import com.codingvibe.userprefs.service.TwitchApiService
import com.codingvibe.userprefs.service.UserPrefsService
import com.codingvibe.userprefs.service.WebsocketEventService
import com.codingvibe.userprefs.service.WebsocketNotificationType
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.logging.Logger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


@RestController
@RequestMapping("/v1/api")
class TwitchEventSub(
    private val objectMapper: ObjectMapper,
    private val eventSubSecret: SecretKeySpec,
    private val allowedOrigins: Array<String>,
    private val websocketEventService: WebsocketEventService,
    private val twitchApiService: TwitchApiService
) {

    companion object {
        const val TWITCH_MESSAGE_ID = "twitch-eventsub-message-id"
        const val TWITCH_MESSAGE_TIMESTAMP = "twitch-eventsub-message-timestamp"
        const val TWITCH_MESSAGE_SIGNATURE = "twitch-eventsub-message-signature"
        const val TWITCH_MESSAGE_TYPE = "twitch-eventsub-message-type"
        const val MESSAGE_TYPE_VERIFICATION = "webhook_callback_verification"
        const val MESSAGE_TYPE_NOTIFICATION = "notification"
        const val MESSAGE_TYPE_REVOCATION = "revocation"

        const val HMAC_PREFIX = "sha256="

        val logger = Logger.getLogger(this::class.java.name)
    }

    @PostMapping("/{channel}/start-notifications")
    fun startNotifications(@PathVariable channel: String): ResponseEntity<Void> {
        websocketEventService.unpauseNotifications(channel)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/eventsub")
    fun processEventSubMessage(@RequestHeader(TWITCH_MESSAGE_ID) id: String,
                               @RequestHeader(TWITCH_MESSAGE_TIMESTAMP) timestamp: String,
                               @RequestHeader(TWITCH_MESSAGE_SIGNATURE) signature: String,
                               @RequestHeader(TWITCH_MESSAGE_TYPE) type: String,
                               @RequestBody body: String): ResponseEntity<String?> {
        val hmac = HMAC_PREFIX + getHmac(eventSubSecret, id, timestamp, body)
        if (hmac != signature) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Bad HMAC. Very, VEEEERY bad HMAC")
        }

        val message = getTwitchEventSubPayload(objectMapper, body)
        when (type) {
            MESSAGE_TYPE_VERIFICATION -> return ResponseEntity.ok(message?.challenge)
            MESSAGE_TYPE_NOTIFICATION -> {
                when (message) {
                    is StreamOnline -> {
                        twitchApiService.clearChatters(message.event.broadcasterUserLogin)
                        websocketEventService.clearNotificationsQueue(message.event.broadcasterUserLogin)
                        websocketEventService.pauseNotifications(message.event.broadcasterUserLogin, 5*60)
                        return ResponseEntity.ok().build()
                    }
                    is CustomChannelPointRewardRedeemed -> {
                        websocketEventService.sendTwitchCommand(
                            channel = message.event.broadcasterUserLogin,
                            type = WebsocketNotificationType.POINTS_REDEMPTION,
                            username = message.event.userLogin,
                            command = message.event.reward.title
                        )
                        return ResponseEntity.ok().build()
                    }
                }
                return ResponseEntity.ok().build()
            }
            MESSAGE_TYPE_REVOCATION -> {
                logger.severe("You've been cut off from message type ${message?.subscription?.type} for reason ${message?.subscription?.status}")
                logger.severe(message.toString())
                return ResponseEntity.ok().build()
            }
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unrecognized message type $type")
        }
    }

    private fun getHmac(secretKey: SecretKeySpec, messageId: String, messageTimestamp: String, body: String): String {
        val mac: Mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val encryptedBytes = mac.doFinal((messageId + messageTimestamp + body).toByteArray())
        return encryptedBytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}