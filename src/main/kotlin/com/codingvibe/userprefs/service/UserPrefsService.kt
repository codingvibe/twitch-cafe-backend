package com.codingvibe.userprefs.service

import com.codingvibe.userprefs.dao.UserPrefsDao
import com.codingvibe.userprefs.external.twitch.TwitchApi
import com.codingvibe.userprefs.model.Preference

class UserPrefsService (private val dao: UserPrefsDao) {
    fun getPrefs(twitchId: String): List<Preference>? {
        return dao.getUserPref(twitchId)?.prefs
    }

    fun updatePrefs(twitchId: String, preferences: List<Preference>): List<Preference> {
        return dao.setUserPref(twitchId, preferences).prefs
    }
}