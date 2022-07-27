package com.codingvibe.userprefs.controller

import com.codingvibe.userprefs.service.CafePreference
import com.codingvibe.userprefs.service.CafePrefsService
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/v1/api/cafe")

class CafePreferencesController(
    private val cafePrefsService: CafePrefsService
) {
    @GetMapping("/prefs")
    @CrossOrigin("http://localhost:3000", "https://prefs.codingvibe.dev")
    fun getValidPreferences(): List<CafePreferencesResponse> {
        return toCafePreferencesResponse(cafePrefsService.getCafePreferences())
    }

    private fun toCafePreferencesResponse(prefs: List<CafePreference>) : List<CafePreferencesResponse> {
        return prefs.map { CafePreferencesResponse(it.name.name, it.values) }
    }
}

data class CafePreferencesResponse (
    val name: String,
    val values: List<String>
)