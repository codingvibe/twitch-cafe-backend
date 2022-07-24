package com.codingvibe.userprefs.dao

import com.codingvibe.userprefs.model.PreferenceName
import com.codingvibe.userprefs.service.CafePreference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction

class CafePrefsDao (private val database: Database, private val objectMapper: ObjectMapper){
    object Prefs: Table() {
        val id = integer("id")
        val name = varchar("name", length = 50)
        val validValues = text("valid_values")
        val createdAt = datetime("created_at")
        val updatedAt = datetime("updated_at")
        override val primaryKey = PrimaryKey(id)
    }

    fun getCafePrefs(): List<CafePreference> {
        database.run {
            return transaction {
                Prefs.selectAll().map { toCafePref(it) }
            }
        }
    }

    private fun toCafePref(resultRow: ResultRow): CafePreference {
        return CafePreference(
            name = PreferenceName.valueOf(resultRow[Prefs.name]),
            values = objectMapper.readValue(resultRow[Prefs.validValues])
        )
    }
}