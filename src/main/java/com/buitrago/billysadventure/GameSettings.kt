package com.buitrago.billysadventure

import android.content.Context
import android.content.SharedPreferences

object GameSettings {
    private const val PREFS_NAME = "BillysAdventurePrefs"
    
    var soundEnabled: Boolean = true
    var brightness: Float = 1.0f
    var difficulty: Difficulty = Difficulty.MEDIUM
    var highScore: Int = 0

    enum class Difficulty(val obstacleSpeed: Float, val spawnRate: Long) {
        EASY(7f, 3_000_000_000L),
        MEDIUM(12f, 2_000_000_000L),
        HARD(18f, 1_200_000_000L)
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        soundEnabled = prefs.getBoolean("soundEnabled", true)
        brightness = prefs.getFloat("brightness", 1.0f)
        highScore = prefs.getInt("highScore", 0)
        val diffName = prefs.getString("difficulty", Difficulty.MEDIUM.name)
        difficulty = try { Difficulty.valueOf(diffName!!) } catch (e: Exception) { Difficulty.MEDIUM }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("soundEnabled", soundEnabled)
            putFloat("brightness", brightness)
            putInt("highScore", highScore)
            putString("difficulty", difficulty.name)
            apply()
        }
    }
}
