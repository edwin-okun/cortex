package com.cortex.app.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.cortex.app.domain.model.SessionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SessionConfigStore(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val maxReviews = intPreferencesKey("max_reviews")
        val maxNewLessons = intPreferencesKey("max_new_lessons")
        val maxConsecutiveSameTrack = intPreferencesKey("max_consecutive_same_track")
        val maxConsecutiveSameTier = intPreferencesKey("max_consecutive_same_tier")
        val prioritizeOverdue = booleanPreferencesKey("prioritize_overdue")
    }

    fun observe(): Flow<SessionConfig> = dataStore.data.map { prefs ->
        SessionConfig(
            maxReviews = prefs[Keys.maxReviews] ?: SessionConfig().maxReviews,
            maxNewLessons = prefs[Keys.maxNewLessons] ?: SessionConfig().maxNewLessons,
            maxConsecutiveSameTrack = prefs[Keys.maxConsecutiveSameTrack]
                ?: SessionConfig().maxConsecutiveSameTrack,
            maxConsecutiveSameTier = prefs[Keys.maxConsecutiveSameTier]
                ?: SessionConfig().maxConsecutiveSameTier,
            prioritizeOverdue = prefs[Keys.prioritizeOverdue] ?: SessionConfig().prioritizeOverdue,
        )
    }

    suspend fun update(transform: (SessionConfig) -> SessionConfig) {
        val current = observe().first()
        val updated = transform(current)
        dataStore.edit { prefs ->
            prefs[Keys.maxReviews] = updated.maxReviews
            prefs[Keys.maxNewLessons] = updated.maxNewLessons
            prefs[Keys.maxConsecutiveSameTrack] = updated.maxConsecutiveSameTrack
            prefs[Keys.maxConsecutiveSameTier] = updated.maxConsecutiveSameTier
            prefs[Keys.prioritizeOverdue] = updated.prioritizeOverdue
        }
    }
}
