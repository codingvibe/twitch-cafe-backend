package com.codingvibe.userprefs.service

import com.codingvibe.userprefs.dao.UserPrefsDao
import com.codingvibe.userprefs.model.Preference

class UserPrefsService (private val dao: UserPrefsDao,
                        private val cafePrefsService: CafePrefsService) {
    fun getPrefs(twitchId: String): List<Preference>? {
        return dao.getUserPref(twitchId)?.prefs
    }

    fun updatePrefs(twitchId: String, preferences: List<Preference>): List<Preference> {
        val cafePrefs = cafePrefsService.getCafePreferences();
        validatePreferences(preferences, cafePrefs)
        return dao.setUserPref(twitchId, preferences).prefs
    }

    fun setAccessToken(twitchId: String, accessToken: String) {
        return dao.setAccessToken(accessToken, twitchId)
    }

    private fun validatePreferences(inputPrefs: List<Preference>, cafePrefs: List<CafePreference>) {
        val cafePrefsMap = cafePrefs.associate { it.name to it.values }
        inputPrefs.forEach {
            if (cafePrefsMap[it.name]?.isNotEmpty()!! && !cafePrefsMap[it.name]?.contains(it.value)!!)
                throw IllegalArgumentException("Invalid value '${it.value}' found for preference '${it.name}'.")
        }
    }
}