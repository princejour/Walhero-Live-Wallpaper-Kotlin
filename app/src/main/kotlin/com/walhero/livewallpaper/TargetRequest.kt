package com.walhero.livewallpaper

import android.content.Context

object TargetRequest {
    const val KEY_TARGET = "target_request"
    const val HOME = "home"
    const val LOCK = "lock"
    const val BOTH = "both"

    fun write(context: Context, target: String) {
        context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TARGET, target)
            .apply()
    }

    fun read(context: Context): String {
        return context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TARGET, HOME) ?: HOME
    }

    fun clear(context: Context) {
        context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TARGET)
            .apply()
    }
}
