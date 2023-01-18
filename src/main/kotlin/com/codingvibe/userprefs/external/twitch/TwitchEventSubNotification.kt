package com.codingvibe.userprefs.external.twitch

import com.codingvibe.userprefs.external.twitch.notifications.CustomChannelPointRewardRedeemed
import com.codingvibe.userprefs.external.twitch.notifications.StreamOnline
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant

// Notification types
enum class NotificationType(val jsonName: String) {
    STREAM_ONLINE("stream.online"),
    CUSTOM_CHANNEL_POINT_REWARD_REDEEMED("channel.channel_points_custom_reward_redemption.add")
}

// Twitch Event Sub Notification base
interface TwitchEventSubNotification {
    val challenge: String?
    val subscription: TwitchEventSubSubscription
}

interface TwitchEventSubSubscription {
    val type: String
    val status: String
    val createdAt: Instant
}

fun getTwitchEventSubPayload(objectMapper: ObjectMapper, body: String): TwitchEventSubNotification? {
    val jsonNode = objectMapper.readTree(body)
    return when (jsonNode.get("subscription")?.get("type")?.asText()) {
        NotificationType.STREAM_ONLINE.jsonName -> objectMapper.readValue(body, StreamOnline::class.java)
        NotificationType.CUSTOM_CHANNEL_POINT_REWARD_REDEEMED.jsonName -> objectMapper.readValue(body, CustomChannelPointRewardRedeemed::class.java)
        else -> null
    }
}