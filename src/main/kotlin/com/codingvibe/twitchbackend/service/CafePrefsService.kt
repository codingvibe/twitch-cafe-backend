package com.codingvibe.twitchbackend.service

import com.codingvibe.twitchbackend.dao.CafePrefsDao
import com.codingvibe.twitchbackend.model.PreferenceName

class CafePrefsService(private val cafePrefsDao: CafePrefsDao) {
    fun getCafePreferences(): List<CafePreference> {
        return cafePrefsDao.getCafePrefs()
    }
}

data class CafePreference(
    val name: PreferenceName,
    val values: List<String>
)