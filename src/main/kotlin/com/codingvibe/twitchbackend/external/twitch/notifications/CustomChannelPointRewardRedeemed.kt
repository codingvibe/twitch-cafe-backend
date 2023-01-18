package com.codingvibe.twitchbackend.external.twitch.notifications

import com.codingvibe.twitchbackend.external.twitch.TwitchEventSubNotification
import com.codingvibe.twitchbackend.external.twitch.TwitchEventSubSubscription
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant


data class CustomChannelPointRewardRedeemed (
    override val challenge: String?,
    override val subscription: CustomChannelPointRewardRedeemedSubscription,
    val event: CustomChannelPointRewardRedeemedEvent?
): TwitchEventSubNotification

data class CustomChannelPointRewardRedeemedSubscription(
    override val type: String,
    override val status: String,
    @JsonProperty("created_at")
    override val createdAt: Instant,
    val condition: CustomChannelPointRewardRedeemedCondition
) : TwitchEventSubSubscription

data class CustomChannelPointRewardRedeemedCondition (
    @JsonProperty("broadcaster_user_id")
    val broadcasterUserId: String,
    @JsonProperty("reward_id")
    val rewardId: String?
)

data class CustomChannelPointRewardRedeemedEvent (
    val id: String,
    @JsonProperty("broadcaster_user_id")
    val broadcasterUserId: String,
    @JsonProperty("broadcaster_user_login")
    val broadcasterUserLogin: String,
    @JsonProperty("broadcaster_user_name")
    val broadcasterUserName: String,
    @JsonProperty("user_id")
    val userId: String,
    @JsonProperty("user_login")
    val userLogin: String,
    @JsonProperty("user_name")
    val userName: String,
    @JsonProperty("user_input")
    val userInput: String,
    val reward: CustomChannelPointReward,
    @JsonProperty("redeemed_at")
    val redeemedAt: Instant
)

data class CustomChannelPointReward(
    val id: String,
    val title: String,
    val cost: Long,
    val prompt: String
)