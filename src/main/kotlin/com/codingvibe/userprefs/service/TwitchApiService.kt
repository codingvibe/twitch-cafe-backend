package com.codingvibe.userprefs.service

import com.github.twitch4j.TwitchClient
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.helix.domain.User
import com.netflix.hystrix.exception.HystrixRuntimeException
import java.util.logging.Logger


class TwitchApiService (private val twitchClientId: String,
                        private val redirectUrl: String,
                        private val twitchClient: TwitchClient,
                        private val websocketEventService: WebsocketEventService,
                        private val serverSideCommands: List<String>
) {
    init {
        twitchClient.chat.connect()
        twitchClient.chat.joinChannel("codingvibe")
        twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java) { event -> onChannelMessage(event) }
    }

    companion object {
        val currentlySeenChatters: MutableMap<String,HashSet<String>> = hashMapOf()
        val logger = Logger.getLogger(this::class.java.name)
    }

    fun getUser(authToken: String): User? {
        try {
            return twitchClient.helix.getUsers(authToken, null, null).execute().users.firstOrNull()
        } catch (ex: HystrixRuntimeException) {
            logger.warning("Error getting user with auth token ${authToken}: ${ex.cause?.message}")
        }
        return null
    }

    fun getLoginUrl(state: String):String {
        return "https://id.twitch.tv/oauth2/authorize?client_id=${twitchClientId}&redirect_uri=${redirectUrl}&state=${state}&response_type=token&scope=user%3Aread%3Aemail"
    }

    fun clearChatters(channelName: String) {
        currentlySeenChatters[channelName]?.clear()
    }

    private fun onChannelMessage(event: ChannelMessageEvent) {
        if (!currentlySeenChatters.containsKey(event.channel.name)) {
            currentlySeenChatters[event.channel.name] = hashSetOf()
        }
        if (currentlySeenChatters[event.channel.name]?.contains(event.user.name) != true) {
            currentlySeenChatters[event.channel.name]?.add(event.user.name)
            websocketEventService.sendTwitchCommand(event.channel.name, WebsocketNotificationType.FIRST_CHAT, event.user.name)
        }

        serverSideCommands.firstOrNull { event.message.contains(it) }?.let {
            websocketEventService.sendTwitchCommand(event.channel.name, WebsocketNotificationType.CHAT_COMMAND, event.user.name, it?.replace("!",""))
        }
    }
}