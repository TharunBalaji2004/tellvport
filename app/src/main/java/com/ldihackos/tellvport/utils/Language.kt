package com.ldihackos.tellvport.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale
import android.content.SharedPreferences
import android.os.Build

fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun Context.setLocale(language: String): Context {
    val locale = Locale(language)
    Locale.setDefault(locale)

    val config = Configuration(resources.configuration)
    config.setLocale(locale)

    return createConfigurationContext(config)
}

object LanguagePref {
    private const val PREF_NAME = "language_pref"
    private const val KEY_LANGUAGE = "key_language"

    fun saveLanguage(context: Context, language: String) {
        val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun getLanguage(context: Context): String {
        val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_LANGUAGE, "en") ?: "en" // Default to English
    }
}
