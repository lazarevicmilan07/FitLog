package com.workoutlog.data.preferences

import android.content.Context
import androidx.core.content.edit

/**
 * Synchronous SharedPreferences wrapper for the selected language code.
 *
 * We deliberately use SharedPreferences (not DataStore) here because this is
 * read inside attachBaseContext — before Hilt, coroutines, or DataStore are
 * initialized. A synchronous read is the only safe option at that lifecycle point.
 */
object LanguagePreferences {

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "language_code"
    const val DEFAULT = "en"

    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT) ?: DEFAULT

    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_LANGUAGE, languageCode) }
    }
}

/** All languages the app supports, in display order. */
data class AppLanguage(val code: String, val nativeName: String)

val supportedLanguages = listOf(
    AppLanguage("en", "English"),
    AppLanguage("es", "Español"),
    AppLanguage("pt", "Português"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("fr", "Français"),
    AppLanguage("ru", "Русский"),
    AppLanguage("sr", "Srpski")
)
