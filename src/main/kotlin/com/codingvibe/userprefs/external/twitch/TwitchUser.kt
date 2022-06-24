package com.codingvibe.userprefs.external.twitch

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TwitchUser (
    val id: String,
    val login: String,
    @JsonProperty("display_name")
    val displayName: String,
    val type: String,
    @JsonProperty("broadcaster_type")
    val broadcasterType: String,
    val description: String,
    @JsonProperty("profile_image_url")
    val profileImageUrl: String,
    @JsonProperty("offline_image_url")
    val offlineImageUrl: String,
    @JsonProperty("view_count")
    val viewCount: Int,
    @JsonProperty("created_at")
    val createdAt: Instant
)

data class TwitchUserResponse(
    val data: List<TwitchUser>
)