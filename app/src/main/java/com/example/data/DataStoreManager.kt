package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "beatflow_settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val IS_DARK_MODE = booleanPreferencesKey("dark_mode")
        val IS_AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val ACCENT_COLOR_INDEX = intPreferencesKey("accent_color_index")
        val IS_GLASS_ENABLED = booleanPreferencesKey("glass_enabled")
        
        // Scan Filter settings
        val IGNORE_SHORTER_THAN_1MIN = booleanPreferencesKey("ignore_shorter_than_1min")
        val IGNORE_SMALLER_THAN_100KB = booleanPreferencesKey("ignore_smaller_than_100kb")
        val IGNORE_HIDDEN = booleanPreferencesKey("ignore_hidden")
        val IGNORE_DUPLICATES = booleanPreferencesKey("ignore_duplicates")
        val SCAN_DOWNLOADS = booleanPreferencesKey("scan_downloads")
        val SCAN_SD_CARD = booleanPreferencesKey("scan_sd_card")

        // Playback Preferences
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val CROSSFADE_PLAYBACK = booleanPreferencesKey("crossfade_playback")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        
        // Profile picture
        val PROFILE_IMAGE_PATH = stringPreferencesKey("profile_image_path")
        
        // Custom scan folder path
        val CUSTOM_SCAN_FOLDER_PATH = stringPreferencesKey("custom_scan_folder_path")
    }

    val settingsFlow: Flow<BeatFlowSettings> = context.dataStore.data.map { preferences ->
        BeatFlowSettings(
            userName = preferences[USER_NAME] ?: "",
            isOnboardingCompleted = preferences[IS_ONBOARDING_COMPLETED] ?: false,
            isDarkMode = preferences[IS_DARK_MODE] ?: true, // Default to Dark mode
            isAmoledMode = preferences[IS_AMOLED_MODE] ?: true, // Default to Amoled Friendly Dark Mode
            accentColorIndex = preferences[ACCENT_COLOR_INDEX] ?: 0, // Green/Cyan glow accent by default
            isGlassEnabled = preferences[IS_GLASS_ENABLED] ?: false, // Default to glass blur off as requested
            ignoreShorterThan1Min = preferences[IGNORE_SHORTER_THAN_1MIN] ?: true,
            ignoreSmallerThan100KB = preferences[IGNORE_SMALLER_THAN_100KB] ?: true,
            ignoreHidden = preferences[IGNORE_HIDDEN] ?: true,
            ignoreDuplicates = preferences[IGNORE_DUPLICATES] ?: true,
            scanDownloads = preferences[SCAN_DOWNLOADS] ?: true,
            scanSDCard = preferences[SCAN_SD_CARD] ?: true,
            gaplessPlayback = preferences[GAPLESS_PLAYBACK] ?: true,
            crossfadePlayback = preferences[CROSSFADE_PLAYBACK] ?: false,
            playbackSpeed = preferences[PLAYBACK_SPEED] ?: 1.0f,
            profileImagePath = preferences[PROFILE_IMAGE_PATH] ?: "",
            customScanFolderPath = preferences[CUSTOM_SCAN_FOLDER_PATH] ?: ""
        )
    }

    suspend fun updateUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateThemeSettings(isDarkMode: Boolean, isAmoledMode: Boolean, isGlassEnabled: Boolean, accentColorIndex: Int) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDarkMode
            preferences[IS_AMOLED_MODE] = isAmoledMode
            preferences[IS_GLASS_ENABLED] = isGlassEnabled
            preferences[ACCENT_COLOR_INDEX] = accentColorIndex
        }
    }

    suspend fun updateScanFilters(
        shorterThan1Min: Boolean,
        smallerThan100KB: Boolean,
        hidden: Boolean,
        duplicates: Boolean,
        scanDownloads: Boolean,
        scanSDCard: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[IGNORE_SHORTER_THAN_1MIN] = shorterThan1Min
            preferences[IGNORE_SMALLER_THAN_100KB] = smallerThan100KB
            preferences[IGNORE_HIDDEN] = hidden
            preferences[IGNORE_DUPLICATES] = duplicates
            preferences[SCAN_DOWNLOADS] = scanDownloads
            preferences[SCAN_SD_CARD] = scanSDCard
        }
    }

    suspend fun updatePlaybackSettings(gapless: Boolean, crossfade: Boolean, speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[GAPLESS_PLAYBACK] = gapless
            preferences[CROSSFADE_PLAYBACK] = crossfade
            preferences[PLAYBACK_SPEED] = speed
        }
    }

    suspend fun updateProfileImage(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE_PATH] = path
        }
    }

    suspend fun updateCustomScanFolderPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_SCAN_FOLDER_PATH] = path
        }
    }
}

data class BeatFlowSettings(
    val userName: String,
    val isOnboardingCompleted: Boolean,
    val isDarkMode: Boolean,
    val isAmoledMode: Boolean,
    val accentColorIndex: Int,
    val isGlassEnabled: Boolean,
    val ignoreShorterThan1Min: Boolean,
    val ignoreSmallerThan100KB: Boolean,
    val ignoreHidden: Boolean,
    val ignoreDuplicates: Boolean,
    val scanDownloads: Boolean,
    val scanSDCard: Boolean,
    val gaplessPlayback: Boolean,
    val crossfadePlayback: Boolean,
    val playbackSpeed: Float,
    val profileImagePath: String,
    val customScanFolderPath: String
)
