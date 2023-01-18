package com.codingvibe.twitchbackend.external.twitch.notifications

import com.codingvibe.twitchbackend.external.twitch.TwitchEventSubNotification
import com.codingvibe.twitchbackend.external.twitch.TwitchEventSubSubscription
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant


data class StreamOnline (
    override val challenge: String?,
    override val subscription: StreamOnlineSubscription,
    val event: StreamOnlineEvent?
): TwitchEventSubNotification

data class StreamOnlineSubscription(
    override val type: String,
    override val status: String,
    @JsonProperty("created_at")
    override val createdAt: Instant,
    val condition: StreamOnlineCondition
) : TwitchEventSubSubscription

data class StreamOnlineCondition (
    @JsonProperty("broadcaster_user_id")
    val broadcasterUserId: String
)

data class StreamOnlineEvent (
    val id: String,
    @JsonProperty("broadcaster_user_id")
    val broadcasterUserId: String,
    @JsonProperty("broadcaster_user_login")
    val broadcasterUserLogin: String,
    @JsonProperty("broadcaster_user_name")
    val broadcasterUserName: String,
    val type: String,
    @JsonProperty("started_at")
    val startedAt: Instant
)