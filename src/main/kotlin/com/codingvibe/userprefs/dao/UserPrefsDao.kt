package com.codingvibe.userprefs.dao

import com.codingvibe.userprefs.model.Preference
import com.codingvibe.userprefs.model.UserPrefs
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PGobject
import java.time.ZoneOffset

class UserPrefsDao(private val objectMapper: ObjectMapper, private val jdbcConnectionString: String,
                   private val driverClassName: String, private val username: String, private val password: String) {
    init {
        Database.connect(jdbcConnectionString, driver = driverClassName, user = username, password = password)
    }

    object Users : Table() {
        val id = integer("id")
        val twitchId = varchar("twitch_id", length = 50)
        val config = text("config")
        val createdAt = datetime("created_at")
        val updatedAt = datetime("updated_at")
        override val primaryKey = PrimaryKey(id)
    }

    fun getUserPref(twitchId: String): UserPrefs? {
        return transaction {
            Users.select { Users.twitchId eq twitchId }.singleOrNull()?.let {
                toUserPrefs(it)
            }
        }

    }

    fun setUserPref(twitchId: String, preferences: List<Preference>): UserPrefs {
        val prefs = getUserPref(twitchId)
            ?: return transaction {
                Users.insert {
                    it[Users.twitchId] = twitchId
                    it[config] = objectMapper.writeValueAsString(preferences)
                }
            }.resultedValues!!.first().let { toUserPrefs(it) }

        transaction {
            Users.update ({ Users.twitchId eq twitchId }) {
                it[config] = objectMapper.writeValueAsString(preferences)
            }
        }
        return prefs.copy(prefs = preferences)
    }

    private fun toUserPrefs(resultRow: ResultRow): UserPrefs {
        return UserPrefs(
            id = resultRow[Users.id],
            twitchId = resultRow[Users.twitchId],
            prefs = objectMapper.readValue(resultRow[Users.config]),
            createdAt = resultRow[Users.createdAt].toInstant(ZoneOffset.UTC),
            updatedAt = resultRow[Users.updatedAt].toInstant(ZoneOffset.UTC)
        )
    }
}