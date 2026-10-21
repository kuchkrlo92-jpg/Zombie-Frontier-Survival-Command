package com.example.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object GameSettings {
    private const val PREFS_NAME = "zombie_frontier_settings"
    private const val KEY_SOUND = "setting_sound"
    private const val KEY_MUSIC = "setting_music"
    private const val KEY_VIBRATION = "setting_vibration"
    private const val KEY_GRAPHICS = "setting_graphics" // "LOW", "MEDIUM", "HIGH"
    private const val KEY_DAMAGE_NUMBERS = "setting_damage_numbers"
    private const val KEY_JOYSTICK_SENSITIVITY = "setting_joystick_sensitivity" // 0.5f to 2.0f
    private const val KEY_DIFFICULTY = "setting_difficulty" // "EASY", "NORMAL", "HARD"

    fun isSoundEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun isMusicEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_MUSIC, true)

    fun setMusicEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_MUSIC, enabled).apply()
    }

    fun isVibrationEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_VIBRATION, true)

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun getGraphicsQuality(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_GRAPHICS, "HIGH") ?: "HIGH"

    fun setGraphicsQuality(context: Context, quality: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_GRAPHICS, quality).apply()
    }

    fun isShowDamageNumbers(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_DAMAGE_NUMBERS, true)

    fun setShowDamageNumbers(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_DAMAGE_NUMBERS, enabled).apply()
    }

    fun getJoystickSensitivity(context: Context): Float =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getFloat(KEY_JOYSTICK_SENSITIVITY, 1.0f)

    fun setJoystickSensitivity(context: Context, sensitivity: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putFloat(KEY_JOYSTICK_SENSITIVITY, sensitivity).apply()
    }

    fun getDifficulty(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DIFFICULTY, "NORMAL") ?: "NORMAL"

    fun setDifficulty(context: Context, difficulty: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_DIFFICULTY, difficulty).apply()
    }

    fun restoreDefaults(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SOUND, true)
            .putBoolean(KEY_MUSIC, true)
            .putBoolean(KEY_VIBRATION, true)
            .putString(KEY_GRAPHICS, "HIGH")
            .putBoolean(KEY_DAMAGE_NUMBERS, true)
            .putFloat(KEY_JOYSTICK_SENSITIVITY, 1.0f)
            .putString(KEY_DIFFICULTY, "NORMAL")
            .apply()
    }

    fun triggerVibration(context: Context, durationMs: Long) {
        if (!isVibrationEnabled(context)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
            // Safe fallback if hardware vibration is unavailable
        }
    }
}
