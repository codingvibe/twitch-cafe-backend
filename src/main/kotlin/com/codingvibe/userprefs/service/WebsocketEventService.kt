package com.codingvibe.userprefs.service

import com.codingvibe.userprefs.model.Preference
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.timerTask

class WebsocketEventService (
    private val userPrefsService: UserPrefsService,
    private val simpleMessagingTemplate: SimpMessagingTemplate
) {
    companion object {
        private var notificationsOn: HashMap<String, Boolean> = HashMap()
        private var notificationsQueue: HashMap<String, MutableList<WebsocketNotificationPayload>> = HashMap()
    }

    fun sendTwitchCommand(channel: String, type: WebsocketNotificationType, username: String, command: String? = null) {
        val payload = WebsocketNotificationPayload(
            type = type,
            message = WebsocketNotificationMessage(
                channelName = channel,
                username = username,
                command = command,
                prefs = userPrefsService.getPrefs(username)
            )
        )
        if (notificationsOn[channel] == true) {
            simpleMessagingTemplate.convertAndSend("/topic/twitchCommand", payload)
        } else {
            if (!notificationsQueue.containsKey(channel)) {
                notificationsQueue[channel] = mutableListOf()
            }
            notificationsQueue[channel]?.add(payload)
        }
    }

    fun pauseNotifications(channel: String, timeoutInSeconds: Long) {
        notificationsOn[channel] = false
        Timer().schedule(timerTask {
            processNotificationsQueue(channel)
        }, timeoutInSeconds * 1000)
    }

    fun unpauseNotifications(channel: String) {
        processNotificationsQueue(channel)
    }

    fun clearNotificationsQueue(channel: String) {
        notificationsQueue[channel] = mutableListOf()
    }

    private fun processNotificationsQueue(channel: String) {
        if (notificationsOn[channel] != true) {
            notificationsOn[channel] = true
            notificationsQueue[channel]?.forEach{ simpleMessagingTemplate.convertAndSend("/topic/twitchCommand", it) }
            clearNotificationsQueue(channel)
        }
    }
}

data class WebsocketNotificationPayload (
    val type: WebsocketNotificationType,
    val message: WebsocketNotificationMessage
)

enum class WebsocketNotificationType {
    POINTS_REDEMPTION,
    CHAT_COMMAND,
    FIRST_CHAT
}

data class WebsocketNotificationMessage(
    val channelName: String,
    val username: String,
    val prefs: List<Preference>?,
    val command: String?
)