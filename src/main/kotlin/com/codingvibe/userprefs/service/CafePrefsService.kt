package com.codingvibe.userprefs.service

import com.codingvibe.userprefs.dao.CafePrefsDao
import com.codingvibe.userprefs.model.PreferenceName

class CafePrefsService(private val cafePrefsDao: CafePrefsDao) {
    fun getCafePreferences(): List<CafePreference> {
        return cafePrefsDao.getCafePrefs()
    }
}

data class CafePreference(
    val name: PreferenceName,
    val values: List<String>
)