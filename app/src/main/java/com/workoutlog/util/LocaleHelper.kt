package com.workoutlog.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Wraps a [Context] with a specific locale so that every string resource
 * lookup inside an Activity uses the user-selected language.
 *
 * Call this from [android.app.Activity.attachBaseContext]:
 *
 * ```kotlin
 * override fun attachBaseContext(newBase: Context) {
 *     val lang = LanguagePreferences.getLanguage(newBase)
 *     super.attachBaseContext(LocaleHelper.wrapContext(newBase, lang))
 * }
 * ```
 */
object LocaleHelper {

    fun wrapContext(context: Context, languageCode: String): Context {
        // Resource locale — must match the values-XX resource folder.
        // "sr" loads values-sr/ correctly on all API levels.
        val resourceLocale = Locale.forLanguageTag(languageCode)

        // Default locale used by date/number formatters (DateTimeFormatter, etc.).
        // Serbian resources use Cyrillic as the default script, but the app is
        // written in Latin, so we point formatters at sr-Latn so month names
        // come out in Latin script instead of Cyrillic.
        val defaultLocale = if (languageCode == "sr")
            Locale.forLanguageTag("sr-Latn")
        else
            resourceLocale

        Locale.setDefault(defaultLocale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(resourceLocale)
        return context.createConfigurationContext(config)
    }
}
