package com.codingvibe.twitchbackend.dao

import com.codingvibe.twitchbackend.model.Preference
import com.codingvibe.twitchbackend.model.UserPrefs
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneOffset

class UserPrefsDao(private val database: Database, private val objectMapper: ObjectMapper) {
    object Users : Table() {
        val id = integer("id")
        val accessToken = text("access_token")
        val twitchId = varchar("twitch_id", length = 50)
        val config = text("config")
        val createdAt = datetime("created_at")
        val updatedAt = datetime("updated_at")
        override val primaryKey = PrimaryKey(id)
    }

    fun userExists(twitchId: String): Boolean {
        return transaction {
            Users.select { Users.twitchId eq twitchId }.singleOrNull() != null
        }
    }

    fun getUserPref(twitchId: String): UserPrefs? {
        database.run {
            return transaction {
                Users.select { Users.twitchId eq twitchId }.singleOrNull()?.let {
                    toUserPrefs(it)
                }
            }
        }
    }

    fun setUserPref(twitchId: String, preferences: List<Preference>): UserPrefs {
        database.run {
            val prefs = getUserPref(twitchId)
                ?: return transaction {
                    Users.insert {
                        it[Users.twitchId] = twitchId
                        it[config] = objectMapper.writeValueAsString(preferences)
                    }
                }.resultedValues!!.first().let { toUserPrefs(it) }

            transaction {
                Users.update({ Users.twitchId eq twitchId }) {
                    it[config] = objectMapper.writeValueAsString(preferences)
                }
            }
            return prefs.copy(prefs = preferences)
        }
    }

    fun setAccessToken(accessToken: String, twitchId: String) {
        database.run {
            if (!userExists(twitchId)) {
                return transaction {
                    Users.insert {
                        it[Users.twitchId] = twitchId
                        it[Users.accessToken] = accessToken
                    }
                }
            }

            return transaction {
                Users.update({ Users.twitchId eq twitchId }) {
                    it[Users.accessToken] = accessToken
                }
            }
        }
    }

    private fun toUserPrefs(resultRow: ResultRow): UserPrefs {
        return UserPrefs(
            id = resultRow[Users.id],
            twitchId = resultRow[Users.twitchId],
            prefs = if (!resultRow[Users.config].isNullOrEmpty()) objectMapper.readValue(resultRow[Users.config]) else emptyList(),
            createdAt = resultRow[Users.createdAt].toInstant(ZoneOffset.UTC),
            updatedAt = resultRow[Users.updatedAt].toInstant(ZoneOffset.UTC)
        )
    }
}