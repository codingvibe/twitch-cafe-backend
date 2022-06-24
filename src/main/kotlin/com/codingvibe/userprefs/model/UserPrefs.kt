package com.codingvibe.userprefs.model

import java.time.Instant

data class UserPrefs (
    val id: Int,
    val twitchId: String,
    val prefs: List<Preference>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class Preference (
    val name: PreferenceName,
    val value: String,
)