package com.buitrago.billysadventure

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private var isGameActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load saved settings
        GameSettings.load(this)
        
        setContentView(R.layout.activity_main)

        val menuLayout = findViewById<LinearLayout>(R.id.menuLayout)
        val settingsLayout = findViewById<LinearLayout>(R.id.settingsLayout)
        val gameContainer = findViewById<FrameLayout>(R.id.gameContainer)

        // Sync UI with loaded settings
        val checkSound = findViewById<CheckBox>(R.id.checkSound)
        checkSound.isChecked = GameSettings.soundEnabled
        
        val seekBrightness = findViewById<SeekBar>(R.id.seekBrightness)
        seekBrightness.progress = (GameSettings.brightness * 100).toInt()
        
        val radioDifficulty = findViewById<RadioGroup>(R.id.radioDifficulty)
        when (GameSettings.difficulty) {
            GameSettings.Difficulty.EASY -> radioDifficulty.check(R.id.radioEasy)
            GameSettings.Difficulty.MEDIUM -> radioDifficulty.check(R.id.radioMed)
            GameSettings.Difficulty.HARD -> radioDifficulty.check(R.id.radioHard)
        }

        // Menu Buttons
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            menuLayout.visibility = View.GONE
            gameContainer.addView(GameView(this))
            isGameActive = true
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            menuLayout.visibility = View.GONE
            settingsLayout.visibility = View.VISIBLE
        }

        // Settings Controls
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            settingsLayout.visibility = View.GONE
            menuLayout.visibility = View.VISIBLE
            GameSettings.save(this) // Save when leaving settings
        }

        checkSound.setOnCheckedChangeListener { _, isChecked ->
            GameSettings.soundEnabled = isChecked
            GameSettings.save(this)
        }

        seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                GameSettings.brightness = progress / 100f
                GameSettings.save(this@MainActivity)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        radioDifficulty.setOnCheckedChangeListener { _, checkedId ->
            GameSettings.difficulty = when (checkedId) {
                R.id.radioEasy -> GameSettings.Difficulty.EASY
                R.id.radioHard -> GameSettings.Difficulty.HARD
                else -> GameSettings.Difficulty.MEDIUM
            }
            GameSettings.save(this)
        }
    }

    override fun onBackPressed() {
        val menuLayout = findViewById<LinearLayout>(R.id.menuLayout)
        val settingsLayout = findViewById<LinearLayout>(R.id.settingsLayout)
        val gameContainer = findViewById<FrameLayout>(R.id.gameContainer)

        if (settingsLayout.visibility == View.VISIBLE) {
            settingsLayout.visibility = View.GONE
            menuLayout.visibility = View.VISIBLE
        } else if (isGameActive) {
            // Use a temporary list to avoid ConcurrentModificationException if needed, 
            // but removeAllViews is safe here as we only have one view usually.
            gameContainer.removeAllViews()
            menuLayout.visibility = View.VISIBLE
            isGameActive = false
        } else {
            super.onBackPressed()
        }
    }
}
