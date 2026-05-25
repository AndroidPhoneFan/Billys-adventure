package com.buitrago.billysadventure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    private var isRunning = false
    private val paint = Paint()
    private var billyBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null

    // Player properties
    private var playerX = 100f
    private var playerY = 500f
    private var playerRadius = 50f
    private var velocityY = 0f
    private val gravity = 2.5f
    private val jumpForce = -50f
    private var isGrounded = false

    // Obstacles
    private val obstacles = mutableListOf<Obstacle>()
    private var obstacleSpeed = GameSettings.difficulty.obstacleSpeed
    private val obstacleWidth = 100f
    private val obstacleHeight = 150f
    private var lastSpawnTime: Long = 0

    // Score
    private var score = 0
    private var isGameOver = false

    // Timing
    private var lastTime: Long = System.nanoTime()

    // Sound
    private var soundPool: SoundPool? = null
    private var jumpSoundId: Int = -1
    private var mediaPlayer: MediaPlayer? = null

    init {
        holder.addCallback(this)
        
        // --- HOW TO CHANGE BACKGROUND & SPRITES ---
        // 1. Place 'background.png' and 'billy.png' in app/src/main/res/drawable/
        // 2. Uncomment the lines below:
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        billyBitmap = BitmapFactory.decodeResource(resources, R.drawable.billy)

        // --- HOW TO ADD SOUND ---
        // 1. Right-click 'res' folder -> New -> Android Resource Directory.
        // 2. Select 'raw' as Resource type.
        // 3. Place 'jump.mp3' and 'music.mp3' in res/raw/
        
        // Initialize Sound Effects (SoundPool)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
        
        jumpSoundId = soundPool?.load(context, R.raw.jump, 1) ?: -1

        // Initialize Background Music (MediaPlayer)
        if (GameSettings.soundEnabled) {
        mediaPlayer = MediaPlayer.create(context, R.raw.music)
            mediaPlayer?.isLooping = true
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        thread = Thread(this)
        thread?.start()
        
        if (GameSettings.soundEnabled) {
            mediaPlayer?.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        var retry = true
        while (retry) {
            try {
                thread?.join()
                retry = false
            } catch (ignore: InterruptedException) {
                // Keep trying to join the thread
            }
        }
        soundPool?.release()
        soundPool = null
        
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun run() {
        while (isRunning) {
            if (!holder.surface.isValid) continue

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                val currentTime = System.nanoTime()
                // Convert nanoseconds to seconds for delta time
                val deltaTime = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                update(deltaTime)
                drawGame(canvas)
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun update(deltaTime: Float) {
        if (isGameOver) {
            if (score > GameSettings.highScore) {
                GameSettings.highScore = score
                GameSettings.save(context)
            }
            return
        }

        val groundY = height * 0.8f // Ground is at 80% of screen height

        // Spawn obstacles based on difficulty
        val currentTime = System.nanoTime()
        if (currentTime - lastSpawnTime > GameSettings.difficulty.spawnRate && width > 0) {
            obstacles.add(Obstacle(width.toFloat(), groundY - obstacleHeight, obstacleWidth, obstacleHeight))
            lastSpawnTime = currentTime
        }

        // Apply gravity
        velocityY += gravity

        // Update position
        playerY += velocityY

        // Ground collision
        if (playerY + playerRadius > groundY) {
            playerY = groundY - playerRadius
            velocityY = 0f
            isGrounded = true
        } else {
            isGrounded = false
        }

        // Update obstacles
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            obs.x -= obstacleSpeed

            // Collision detection (Simple AABB)
            if (playerX + playerRadius > obs.x &&
                playerX - playerRadius < obs.x + obs.width &&
                playerY + playerRadius > obs.y &&
                playerY - playerRadius < obs.y + obs.height
            ) {
                isGameOver = true
            }

            // Recycle obstacle
            if (obs.x + obs.width < 0) {
                iterator.remove()
                score++
            }
        }
    }

    private fun drawGame(canvas: Canvas) {
        // 1. Draw Background
        val bg = backgroundBitmap
        if (bg != null) {
            // Draw bitmap stretched to fill the screen
            canvas.drawBitmap(bg, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), null)
        } else {
            canvas.drawColor(Color.parseColor("#87CEEB")) // Fallback Sky Blue
        }

        // 2. Draw Ground
        paint.color = Color.parseColor("#4CAF50") // Grass Green
        val groundY = height * 0.8f
        canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), paint)

        // 3. Draw Billy (Player)
        paint.color = if (isGameOver) Color.GRAY else Color.RED
        
        val billy = billyBitmap
        if (billy != null) {
            val destRect = RectF(playerX - playerRadius, playerY - playerRadius, playerX + playerRadius, playerY + playerRadius)
            canvas.drawBitmap(billy, null, destRect, paint)
        } else {
            canvas.drawCircle(playerX, playerY, playerRadius, paint)
        }

        // 4. Draw Obstacles
        paint.color = Color.parseColor("#8B4513") // Brown
        for (obs in obstacles) {
            canvas.drawRect(obs.x, obs.y, obs.x + obs.width, obs.y + obs.height, paint)
        }

        // 5. Draw Score
        paint.color = Color.WHITE
        paint.textSize = 60f
        canvas.drawText("Score: $score", 50f, 100f, paint)
        canvas.drawText("High Score: ${GameSettings.highScore}", 50f, 170f, paint)

        // 6. Draw Game Over
        if (isGameOver) {
            paint.color = Color.BLACK
            paint.textSize = 100f
            canvas.drawText("GAME OVER", width / 2f - 250f, height / 2f, paint)
            paint.textSize = 50f
            canvas.drawText("Tap to Restart", width / 2f - 150f, height / 2f + 100f, paint)
        }

        // 7. Apply Brightness Overlay
        if (GameSettings.brightness < 1.0f) {
            // Max alpha 220 so it's never completely black
            val alpha = ((1.0f - GameSettings.brightness) * 220).toInt()
            paint.color = Color.BLACK
            paint.alpha = alpha
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.alpha = 255 // Reset alpha
        }
    }

    private fun resetGame() {
        score = 0
        isGameOver = false
        playerY = 500f
        velocityY = 0f
        obstacles.clear()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                performClick()
                if (isGameOver) {
                    resetGame()
                } else if (isGrounded) {
                    velocityY = jumpForce
                    if (GameSettings.soundEnabled && jumpSoundId != -1) {
                        soundPool?.play(jumpSoundId, 1f, 1f, 0, 0, 1f)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Allow horizontal movement via touch
                if (!isGameOver) {
                    playerX = event.x
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    // Helper class for obstacles
    class Obstacle(var x: Float, var y: Float, var width: Float, var height: Float)
}
